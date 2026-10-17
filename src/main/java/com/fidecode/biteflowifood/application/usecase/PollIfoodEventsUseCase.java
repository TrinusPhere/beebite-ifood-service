package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.*;
import com.fidecode.biteflowifood.application.port.IfoodAuthClient;
import com.fidecode.biteflowifood.application.port.IfoodChatClient;
import com.fidecode.biteflowifood.application.port.IfoodEventClient;
import com.fidecode.biteflowifood.application.port.IfoodOrderClient;
import com.fidecode.biteflowifood.application.port.OrderEventPublisher;
import com.fidecode.biteflowifood.application.port.SecretEncryptor;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;
import com.fidecode.biteflowifood.domain.event.entity.IfoodEvent;
import com.fidecode.biteflowifood.domain.event.repository.ProcessedEventRepository;
import com.fidecode.biteflowifood.domain.event.valueobject.IfoodOrderStatus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Use Case — PollIfoodEventsUseCase.
 *
 * Executado pelo scheduler a cada 30s. Para cada Store com credencial iFood habilitada:
 * 1. Garante token OAuth válido (renova se necessário)
 * 2. Faz polling de eventos pendentes na API do iFood
 * 3. Filtra duplicatas (idempotência via ProcessedEventRepository)
 * 4. Para PLACED: busca detalhes e publica IfoodOrderPlacedEvent no Pub/Sub
 * 5. Para status com mapeamento: publica IfoodOrderStatusChangedEvent no Pub/Sub
 * 6. Marca todos como processados e envia ACK ao iFood
 */
@ApplicationScoped
public class PollIfoodEventsUseCase {

    private static final Logger LOG = Logger.getLogger(PollIfoodEventsUseCase.class);

    private final IfoodCredentialRepository credentialRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final IfoodAuthClient authClient;
    private final IfoodEventClient eventClient;
    private final IfoodOrderClient orderClient;
    private final IfoodChatClient chatClient;
    private final OrderEventPublisher eventPublisher;
    private final SecretEncryptor secretEncryptor;

    @Inject
    public PollIfoodEventsUseCase(IfoodCredentialRepository credentialRepository,
                                   ProcessedEventRepository processedEventRepository,
                                   IfoodAuthClient authClient,
                                   IfoodEventClient eventClient,
                                   IfoodOrderClient orderClient,
                                   IfoodChatClient chatClient,
                                   OrderEventPublisher eventPublisher,
                                   SecretEncryptor secretEncryptor) {
        this.credentialRepository = credentialRepository;
        this.processedEventRepository = processedEventRepository;
        this.authClient = authClient;
        this.eventClient = eventClient;
        this.orderClient = orderClient;
        this.chatClient = chatClient;
        this.eventPublisher = eventPublisher;
        this.secretEncryptor = secretEncryptor;
    }

    public void execute() {
        List<IfoodCredential> enabledCredentials = credentialRepository.findAllEnabled();
        if (enabledCredentials.isEmpty()) return;

        for (IfoodCredential credential : enabledCredentials) {
            processStore(credential);
        }
    }

    private void processStore(IfoodCredential credential) {
        try {
            ensureValidToken(credential);

            String token = credential.getAccessToken();
            String merchantId = credential.getMerchantId();

            List<IfoodEvent> events = eventClient.pollEvents(token, merchantId);
            if (events.isEmpty()) {
                LOG.debugf("[IFOOD-POLL] store=%s events=0", credential.getStoreId());
                return;
            }

            int processed = 0;
            for (IfoodEvent event : events) {
                if (!processedEventRepository.exists(event.getId())) {
                    handleEvent(credential, event, token);
                    processedEventRepository.markProcessed(event.getId(), credential.getStoreId());
                    processed++;
                } else {
                    LOG.debugf("[IFOOD-POLL] Evento duplicado ignorado: %s", event.getId());
                }
            }

            // ACK de TODOS os eventos (incluindo duplicatas) — o iFood exige ACK mesmo para já processados
            List<String> allEventIds = events.stream().map(IfoodEvent::getId).toList();
            eventClient.acknowledgeEvents(token, allEventIds);

            LOG.infof("[IFOOD-POLL] store=%s events=%d processed=%d",
                    credential.getStoreId(), events.size(), processed);

        } catch (Exception e) {
            LOG.errorf(e, "[IFOOD-POLL] Erro ao processar store=%s: %s",
                    credential.getStoreId(), e.getMessage());
            // Falha nunca derruba o loop — próximo ciclo tentará novamente
        }
    }

    private void ensureValidToken(IfoodCredential credential) {
        if (!credential.isTokenValid()) {
            LOG.infof("[IFOOD-POLL] Token expirado para store=%s, renovando...", credential.getStoreId());
            String plainSecret = secretEncryptor.decrypt(credential.getEncryptedClientSecret());
            IfoodTokenResponse newToken;
            
            if (credential.getRefreshToken() != null && !credential.getRefreshToken().isBlank()) {
                newToken = authClient.refreshToken(credential.getClientId(), plainSecret, credential.getRefreshToken());
            } else {
                newToken = authClient.authenticate(credential.getClientId(), plainSecret);
            }
            
            credential.updateToken(newToken.accessToken(), newToken.refreshToken(), newToken.expiresIn());
            credentialRepository.save(credential);
        }
    }



    private void handleEvent(IfoodCredential credential, IfoodEvent event, String token) {
        IfoodOrderStatus status;
        try {
            status = IfoodOrderStatus.fromCode(event.getCode());
        } catch (IllegalArgumentException e) {
            LOG.warnf("[IFOOD-POLL] Código de evento desconhecido: %s (eventId=%s)", event.getCode(), event.getId());
            return;
        }

        if (status == IfoodOrderStatus.PLACED) {
            handlePlacedOrder(credential, event, token);
        } else if (status == IfoodOrderStatus.CHAT_MESSAGE) {
            handleChatMessage(credential, event, token);
        } else {
            handleStatusChange(credential, event, status);
        }
    }

    private void handlePlacedOrder(IfoodCredential credential, IfoodEvent event, String token) {
        IfoodOrderDetails details = orderClient.getOrderDetails(token, event.getOrderId());
        IfoodOrderPlacedEvent placedEvent = IfoodEventMapper.toPlacedEvent(details, credential.getStoreId(), event.getId());
        eventPublisher.publishOrderPlaced(placedEvent);
        LOG.infof("[IFOOD-POLL] Novo pedido publicado: displayId=%s storeId=%s",
                details.displayId(), credential.getStoreId());
    }

    private void handleChatMessage(IfoodCredential credential, IfoodEvent event, String token) {
        // Fetch the message from iFood chat API (if needed) or extract from event metadata
        // For standard integration, let's assume we fetch the latest messages or the event contains it
        String messageContent = (String) event.getMetadata().get("text");
        String senderId = (String) event.getMetadata().get("senderId");
        if (messageContent == null) {
             // Se não vier no metadata, buscar as mensagens
             var messages = chatClient.getMessages(token, event.getOrderId());
             if (!messages.isEmpty()) {
                 var lastMessage = messages.get(messages.size() - 1);
                 messageContent = lastMessage.content();
                 senderId = lastMessage.sender();
             }
        }
        if (messageContent != null) {
            IfoodChatMessageReceivedEvent chatEvent = new IfoodChatMessageReceivedEvent(
                    credential.getStoreId(),
                    event.getOrderId(),
                    event.getId(),
                    messageContent,
                    senderId != null ? senderId : "CUSTOMER",
                    java.time.Instant.now()
            );
            eventPublisher.publishChatMessage(chatEvent);
            LOG.infof("[IFOOD-POLL] Chat recebido orderId=%s storeId=%s", event.getOrderId(), credential.getStoreId());
        }
    }

    private void handleStatusChange(IfoodCredential credential, IfoodEvent event, IfoodOrderStatus status) {
        Optional<String> biteflowStatus = status.toBiteflowStatus();
        if (biteflowStatus.isEmpty()) {
            LOG.debugf("[IFOOD-POLL] Evento %s ignorado (sem mapeamento BiteFlow)", status.name());
            return;
        }

        IfoodOrderStatusChangedEvent statusEvent = new IfoodOrderStatusChangedEvent(
                event.getId(),
                credential.getStoreId(),
                event.getOrderId(),
                status.name(),
                biteflowStatus.get(),
                Instant.now()
        );
        eventPublisher.publishOrderStatusChanged(statusEvent);
        LOG.infof("[IFOOD-POLL] Status publicado: orderId=%s newStatus=%s",
                event.getOrderId(), biteflowStatus.get());
    }
}
