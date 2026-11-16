package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.*;
import com.trinusphere.beebiteifood.application.port.IfoodEventClient;
import com.trinusphere.beebiteifood.application.port.IfoodOrderClient;
import com.trinusphere.beebiteifood.application.port.OrderEventPublisher;
import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;
import com.trinusphere.beebiteifood.domain.credential.repository.IfoodCredentialRepository;
import com.trinusphere.beebiteifood.domain.event.entity.IfoodEvent;
import com.trinusphere.beebiteifood.domain.event.repository.ProcessedEventRepository;
import com.trinusphere.beebiteifood.domain.event.valueobject.IfoodOrderStatus;
import com.trinusphere.beebiteifood.domain.shared.exception.IfoodAuthenticationException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final IfoodEventClient eventClient;
    private final IfoodOrderClient orderClient;
    private final OrderEventPublisher eventPublisher;
    private final ProvideValidIfoodTokenUseCase tokenUseCase;

    @Inject
    public PollIfoodEventsUseCase(IfoodCredentialRepository credentialRepository,
                                   ProcessedEventRepository processedEventRepository,
                                   IfoodEventClient eventClient,
                                   IfoodOrderClient orderClient,
                                   OrderEventPublisher eventPublisher,
                                   ProvideValidIfoodTokenUseCase tokenUseCase) {
        this.credentialRepository = credentialRepository;
        this.processedEventRepository = processedEventRepository;
        this.eventClient = eventClient;
        this.orderClient = orderClient;
        this.eventPublisher = eventPublisher;
        this.tokenUseCase = tokenUseCase;
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

            String merchantId = credential.getMerchantId();

            List<IfoodEvent> events = pollWithTokenRecovery(credential);
            // Lido depois do polling: pode ter sido renovado se o iFood rejeitou o token anterior.
            String token = credential.getAccessToken();
            if (events.isEmpty()) {
                LOG.debugf("[IFOOD-POLL] store=%s events=0", credential.getStoreId());
                return;
            }

            // Defesa em profundidade: mesmo com o header x-polling-merchants, nunca processa
            // nem confirma (ACK) evento de outro merchant — isso o tiraria da fila da loja correta.
            List<IfoodEvent> ownEvents = new ArrayList<>();
            for (IfoodEvent event : events) {
                if (belongsToMerchant(event, merchantId)) {
                    ownEvents.add(event);
                } else {
                    LOG.warnf("[IFOOD-POLL] Evento %s ignorado: merchant %s diferente do da loja %s",
                            event.getId(), event.getMerchantId(), credential.getStoreId());
                }
            }

            // Documentação iFood: ordem de entrega dos eventos não é garantida; ordenar por createdAt.
            ownEvents.sort(java.util.Comparator.comparing(
                    IfoodEvent::getCreatedAt,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())
            ));

            // IDs a confirmar: duplicatas + eventos tratados com sucesso.
            // Evento que falhou NÃO é confirmado — o iFood o reenvia no próximo ciclo (retentativa).
            List<String> eventIdsToAck = new ArrayList<>();
            int processed = 0;
            int failed = 0;
            for (IfoodEvent event : ownEvents) {
                if (processedEventRepository.exists(event.getId())) {
                    LOG.debugf("[IFOOD-POLL] Evento duplicado ignorado: %s", event.getId());
                    eventIdsToAck.add(event.getId());
                    continue;
                }
                try {
                    handleEvent(credential, event, token);
                    processedEventRepository.markProcessed(event.getId(), credential.getStoreId());
                    eventIdsToAck.add(event.getId());
                    processed++;
                } catch (Exception e) {
                    failed++;
                    LOG.errorf(e, "[IFOOD-POLL] Falha ao processar evento %s (code=%s orderId=%s) store=%s — será reenviado pelo iFood: %s",
                            event.getId(), event.getCode(), event.getOrderId(), credential.getStoreId(), e.getMessage());
                }
            }

            // O iFood exige ACK de todos os eventos recebidos (inclusive os não utilizados).
            eventClient.acknowledgeEvents(token, eventIdsToAck);

            LOG.infof("[IFOOD-POLL] store=%s events=%d processed=%d failed=%d",
                    credential.getStoreId(), events.size(), processed, failed);

        } catch (Exception e) {
            LOG.errorf(e, "[IFOOD-POLL] Erro ao processar store=%s: %s",
                    credential.getStoreId(), e.getMessage());
            // Falha nunca derruba o loop — próximo ciclo tentará novamente
        }
    }

    /** Evento sem merchantId é aceito (o filtro é feito pelo header na API); com merchantId, deve coincidir. */
    private boolean belongsToMerchant(IfoodEvent event, String merchantId) {
        return event.getMerchantId() == null || event.getMerchantId().equals(merchantId);
    }

    private void ensureValidToken(IfoodCredential credential) {
        tokenUseCase.ensureValid(credential);
    }

    /**
     * Faz o polling; se o iFood rejeitar o token (401) — expirado antes da hora ou revogado —,
     * invalida-o, renova e repete a chamada UMA vez. Uma segunda rejeição propaga a exceção.
     */
    private List<IfoodEvent> pollWithTokenRecovery(IfoodCredential credential) {
        try {
            return eventClient.pollEvents(credential.getAccessToken(), credential.getMerchantId());
        } catch (IfoodAuthenticationException e) {
            LOG.warnf("[IFOOD-POLL] iFood rejeitou o token da store=%s (401), renovando e tentando de novo...",
                    credential.getStoreId());
            tokenUseCase.invalidateAndRenew(credential);
            return eventClient.pollEvents(credential.getAccessToken(), credential.getMerchantId());
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
        } else if (status == IfoodOrderStatus.HANDSHAKE_DISPUTE) {
            handleHandshakeDispute(credential, event);
        } else if (status == IfoodOrderStatus.HANDSHAKE_SETTLEMENT) {
            handleHandshakeSettlement(credential, event);
        } else if (status == IfoodOrderStatus.CANCELLATION_REQUESTED) {
            handleCancellationRequested(credential, event);
        } else {
            handleStatusChange(credential, event, status);
        }
    }

    private void handleHandshakeDispute(IfoodCredential credential, IfoodEvent event) {
        Map<String, Object> meta = event.getMetadata() != null ? event.getMetadata() : Collections.emptyMap();
        String disputeId = (String) meta.getOrDefault("disputeId", meta.get("id"));
        String handshakeType = (String) meta.get("handshakeType");
        String expiresAt = (String) meta.get("expiresAt");
        String timeoutAction = (String) meta.get("timeoutAction");

        IfoodDisputeReceivedEvent disputeEvent = new IfoodDisputeReceivedEvent(
                credential.getStoreId(),
                event.getOrderId(),
                disputeId,
                event.getId(),
                handshakeType,
                expiresAt,
                timeoutAction,
                meta
        );
        eventPublisher.publishDisputeReceived(disputeEvent);
        LOG.infof("[IFOOD-POLL] Disputa recebida: disputeId=%s orderId=%s storeId=%s",
                disputeId, event.getOrderId(), credential.getStoreId());
    }

    private void handleHandshakeSettlement(IfoodCredential credential, IfoodEvent event) {
        Map<String, Object> meta = event.getMetadata() != null ? event.getMetadata() : Collections.emptyMap();
        String disputeId = (String) meta.getOrDefault("disputeId", meta.get("id"));
        String status = (String) meta.get("status");

        IfoodDisputeSettledEvent settledEvent = new IfoodDisputeSettledEvent(
                credential.getStoreId(),
                event.getOrderId(),
                disputeId,
                event.getId(),
                status,
                meta
        );
        eventPublisher.publishDisputeSettled(settledEvent);
        LOG.infof("[IFOOD-POLL] Disputa resolvida: disputeId=%s status=%s orderId=%s storeId=%s",
                disputeId, status, event.getOrderId(), credential.getStoreId());
    }

    private void handleCancellationRequested(IfoodCredential credential, IfoodEvent event) {
        Map<String, Object> meta = event.getMetadata() != null ? event.getMetadata() : Collections.emptyMap();
        String reasonCode = meta.get("reason_code") != null ? String.valueOf(meta.get("reason_code")) : null;
        String details = (String) meta.get("details");
        Instant requestedAt = event.getCreatedAt() != null ? event.getCreatedAt() : Instant.now();

        IfoodCancellationRequestedEvent carEvent = new IfoodCancellationRequestedEvent(
                credential.getStoreId(),
                event.getOrderId(),
                event.getId(),
                reasonCode,
                details,
                meta,
                requestedAt
        );
        eventPublisher.publishCancellationRequested(carEvent);
        LOG.infof("[IFOOD-POLL] Cancelamento solicitado pelo cliente: orderId=%s reasonCode=%s storeId=%s",
                event.getOrderId(), reasonCode, credential.getStoreId());
    }

    private void handlePlacedOrder(IfoodCredential credential, IfoodEvent event, String token) {
        IfoodOrderDetails details = orderClient.getOrderDetails(token, event.getOrderId());
        IfoodOrderPlacedEvent placedEvent = IfoodEventMapper.toPlacedEvent(details, credential.getStoreId(), event.getId());
        eventPublisher.publishOrderPlaced(placedEvent);
        LOG.infof("[IFOOD-POLL] Novo pedido publicado: displayId=%s storeId=%s",
                details.displayId(), credential.getStoreId());
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
