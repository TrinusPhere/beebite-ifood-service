package com.fidecode.beebiteifood.application.usecase;

import com.fidecode.beebiteifood.application.dto.BiteflowOrderStatusChangedEvent;
import com.fidecode.beebiteifood.application.dto.IfoodOrderDetails;
import com.fidecode.beebiteifood.application.port.IfoodOrderClient;
import com.fidecode.beebiteifood.domain.credential.entity.IfoodCredential;
import com.fidecode.beebiteifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.beebiteifood.domain.credential.repository.IfoodCredentialRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

/**
 * Use Case — Sincroniza o status de um pedido iFood com a API do iFood.
 * Chamado quando o BiteFlow publica um evento de mudança de status.
 *
 * Mapeamento BiteFlow → iFood:
 * - PREPARO  → confirmOrder() + startPreparation()
 * - EM_ROTA  → readyToPickup() + dispatchOrder()
 */
@ApplicationScoped
public class SyncStatusToIfoodUseCase {

    private static final Logger LOG = Logger.getLogger(SyncStatusToIfoodUseCase.class);

    private final IfoodCredentialRepository credentialRepository;
    private final IfoodOrderClient orderClient;
    private final ProvideValidIfoodTokenUseCase tokenUseCase;

    @Inject
    public SyncStatusToIfoodUseCase(IfoodCredentialRepository credentialRepository,
                                     IfoodOrderClient orderClient,
                                     ProvideValidIfoodTokenUseCase tokenUseCase) {
        this.credentialRepository = credentialRepository;
        this.orderClient = orderClient;
        this.tokenUseCase = tokenUseCase;
    }

    public void execute(BiteflowOrderStatusChangedEvent event) {
        IfoodCredential cred = credentialRepository.findByStoreId(event.storeId())
                .orElseThrow(() -> new CredentialNotFoundException(event.storeId().toString()));

        if (!cred.isEnabled()) {
            LOG.infof("[SYNC-STATUS] Integração desabilitada para store=%s, ignorando.", event.storeId());
            return;
        }

        cred = tokenUseCase.ensureValid(cred);
        String token = cred.getAccessToken();

        String ifoodOrderId = event.ifoodOrderId();

        switch (event.newStatus()) {
            case "PREPARO" -> {
                if (event.changedAt() != null) {
                    long elapsedMinutes = java.time.Duration.between(event.changedAt(), java.time.Instant.now()).toMinutes();
                    if (elapsedMinutes > 8) {
                        LOG.warnf("[SYNC-STATUS] Alerta de SLA: confirmação do pedido %s disparada %d min após o evento. Risco de cancelamento automático pelo iFood (código 902).",
                                ifoodOrderId, elapsedMinutes);
                    }
                }
                orderClient.confirmOrder(token, ifoodOrderId);
                orderClient.startPreparation(token, ifoodOrderId);
                LOG.infof("[SYNC-STATUS] Pedido confirmado no iFood: %s", ifoodOrderId);
            }
            case "EM_ROTA" -> {
                // Documentação oficial do iFood: readyToPickup é obrigatório para TAKEOUT, DINE_IN e DELIVERY
                orderClient.readyToPickup(token, ifoodOrderId);
                LOG.infof("[SYNC-STATUS] Pedido pronto para retirada/despacho (readyToPickup): %s", ifoodOrderId);

                // dispatch só é permitido para DELIVERY com entrega própria (deliveredBy = MERCHANT).
                // Para TAKEOUT, DINE_IN ou entrega parceira (deliveredBy = IFOOD), dispatch NUNCA deve ser chamado.
                try {
                    IfoodOrderDetails details = orderClient.getOrderDetails(token, ifoodOrderId);
                    boolean isDelivery = details != null && "DELIVERY".equalsIgnoreCase(details.orderType());
                    boolean isMerchantDelivery = isDelivery
                            && details.delivery() != null
                            && "MERCHANT".equalsIgnoreCase(details.delivery().deliveredBy());

                    if (isMerchantDelivery) {
                        orderClient.dispatchOrder(token, ifoodOrderId);
                        LOG.infof("[SYNC-STATUS] Pedido despachado na entrega própria (dispatchOrder): %s", ifoodOrderId);
                    } else {
                        LOG.infof("[SYNC-STATUS] Pedido %s não requer dispatch (tipo=%s, entrega=%s)",
                                ifoodOrderId,
                                details != null ? details.orderType() : "desconhecido",
                                (details != null && details.delivery() != null) ? details.delivery().deliveredBy() : "n/a");
                    }
                } catch (Exception e) {
                    LOG.warnf(e, "[SYNC-STATUS] Não foi possível verificar detalhes para dispatch do pedido %s: %s",
                            ifoodOrderId, e.getMessage());
                }
            }
            default -> LOG.debugf("[SYNC-STATUS] Status '%s' não requer ação no iFood", event.newStatus());
        }
    }
}
