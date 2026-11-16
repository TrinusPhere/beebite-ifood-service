package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.BiteflowOrderCancelledEvent;
import com.trinusphere.beebiteifood.application.port.IfoodOrderClient;
import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;
import com.trinusphere.beebiteifood.domain.credential.exception.CredentialNotFoundException;
import com.trinusphere.beebiteifood.domain.credential.repository.IfoodCredentialRepository;

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
