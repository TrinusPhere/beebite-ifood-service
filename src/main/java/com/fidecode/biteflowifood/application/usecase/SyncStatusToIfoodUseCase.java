package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.BiteflowOrderStatusChangedEvent;
import com.fidecode.biteflowifood.application.port.IfoodOrderClient;
import com.fidecode.biteflowifood.application.port.SecretEncryptor;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;

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
    private final SecretEncryptor encryptor;

    @Inject
    public SyncStatusToIfoodUseCase(IfoodCredentialRepository credentialRepository,
                                     IfoodOrderClient orderClient,
                                     SecretEncryptor encryptor) {
        this.credentialRepository = credentialRepository;
        this.orderClient = orderClient;
        this.encryptor = encryptor;
    }

    public void execute(BiteflowOrderStatusChangedEvent event) {
        IfoodCredential cred = credentialRepository.findByStoreId(event.storeId())
                .orElseThrow(() -> new CredentialNotFoundException(event.storeId().toString()));

        if (!cred.isEnabled()) {
            LOG.infof("[SYNC-STATUS] Integração desabilitada para store=%s, ignorando.", event.storeId());
            return;
        }

        String token = cred.getAccessToken();
        if (!cred.isTokenValid()) {
            LOG.warnf("[SYNC-STATUS] Token inválido para store=%s. Será renovado no próximo ciclo.", event.storeId());
            return;
        }

        String ifoodOrderId = event.ifoodOrderId();

        switch (event.newStatus()) {
            case "PREPARO" -> {
                orderClient.confirmOrder(token, ifoodOrderId);
                orderClient.startPreparation(token, ifoodOrderId);
                LOG.infof("[SYNC-STATUS] Pedido confirmado no iFood: %s", ifoodOrderId);
            }
            case "EM_ROTA" -> {
                orderClient.readyToPickup(token, ifoodOrderId);
                orderClient.dispatchOrder(token, ifoodOrderId);
                LOG.infof("[SYNC-STATUS] Pedido despachado no iFood: %s", ifoodOrderId);
            }
            default -> LOG.debugf("[SYNC-STATUS] Status '%s' não requer ação no iFood", event.newStatus());
        }
    }
}
