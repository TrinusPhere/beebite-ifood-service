package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.BiteflowOrderCancelledEvent;
import com.fidecode.biteflowifood.application.port.IfoodOrderClient;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

/** Use Case — Cancela um pedido iFood a pedido do BiteFlow. */
@ApplicationScoped
public class CancelOnIfoodUseCase {

    private static final Logger LOG = Logger.getLogger(CancelOnIfoodUseCase.class);

    private final IfoodCredentialRepository credentialRepository;
    private final IfoodOrderClient orderClient;
    private final ProvideValidIfoodTokenUseCase tokenUseCase;

    @Inject
    public CancelOnIfoodUseCase(IfoodCredentialRepository credentialRepository,
                                 IfoodOrderClient orderClient,
                                 ProvideValidIfoodTokenUseCase tokenUseCase) {
        this.credentialRepository = credentialRepository;
        this.orderClient = orderClient;
        this.tokenUseCase = tokenUseCase;
    }

    public void execute(BiteflowOrderCancelledEvent event) {
        IfoodCredential cred = credentialRepository.findByStoreId(event.storeId())
                .orElseThrow(() -> new CredentialNotFoundException(event.storeId().toString()));

        if (!cred.isEnabled()) {
            LOG.warnf("[CANCEL-IFOOD] Integração desabilitada para store=%s", event.storeId());
            return;
        }

        cred = tokenUseCase.ensureValid(cred);

        orderClient.requestCancellation(cred.getAccessToken(), event.ifoodOrderId(),
                event.cancellationCode(), event.reason());
        LOG.infof("[CANCEL-IFOOD] Cancelamento solicitado: orderId=%s reason=%s",
                event.ifoodOrderId(), event.reason());
    }
}
