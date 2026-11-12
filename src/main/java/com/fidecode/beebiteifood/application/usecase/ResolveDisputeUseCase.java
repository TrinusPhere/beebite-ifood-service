package com.fidecode.beebiteifood.application.usecase;

import com.fidecode.beebiteifood.application.dto.IfoodAcceptDisputeRequest;
import com.fidecode.beebiteifood.application.dto.IfoodAlternativeDisputeRequest;
import com.fidecode.beebiteifood.application.dto.IfoodRejectDisputeRequest;
import com.fidecode.beebiteifood.application.port.IfoodOrderClient;
import com.fidecode.beebiteifood.domain.credential.entity.IfoodCredential;
import com.fidecode.beebiteifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.beebiteifood.domain.credential.repository.IfoodCredentialRepository;
import com.fidecode.beebiteifood.domain.shared.exception.IfoodIntegrationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Use Case — Resolve uma contestação na Plataforma de Negociação (Handshake) do iFood.
 * Permite aceitar o cancelamento, rejeitar com justificativa ou propor alternativa
 * (tempo extra ou reembolso/benefício) antes do vencimento (expiresAt).
 */
@ApplicationScoped
public class ResolveDisputeUseCase {

    private static final Logger LOG = Logger.getLogger(ResolveDisputeUseCase.class);

    private final IfoodCredentialRepository credentialRepository;
    private final IfoodOrderClient orderClient;

    @Inject
    public ResolveDisputeUseCase(IfoodCredentialRepository credentialRepository,
                                 IfoodOrderClient orderClient) {
        this.credentialRepository = credentialRepository;
        this.orderClient = orderClient;
    }

    public void accept(UUID storeId, String disputeId, IfoodAcceptDisputeRequest request) {
        String token = getValidTokenOrThrow(storeId);
        orderClient.acceptDispute(token, disputeId, request);
        LOG.infof("[DISPUTE] Disputa %s aceita para store=%s", disputeId, storeId);
    }

    public void reject(UUID storeId, String disputeId, IfoodRejectDisputeRequest request) {
        String token = getValidTokenOrThrow(storeId);
        orderClient.rejectDispute(token, disputeId, request);
        LOG.infof("[DISPUTE] Disputa %s rejeitada para store=%s (reason=%s)", disputeId, storeId, request.reason());
    }

    public void proposeAlternative(UUID storeId, String disputeId, IfoodAlternativeDisputeRequest request) {
        String token = getValidTokenOrThrow(storeId);
        orderClient.proposeDisputeAlternative(token, disputeId, request);
        LOG.infof("[DISPUTE] Alternativa proposta para disputa %s store=%s (type=%s)", disputeId, storeId, request.type());
    }

    private String getValidTokenOrThrow(UUID storeId) {
        IfoodCredential cred = credentialRepository.findByStoreId(storeId)
                .orElseThrow(() -> new CredentialNotFoundException(storeId.toString()));

        if (!cred.isEnabled()) {
            throw new IfoodIntegrationException("Integração iFood desabilitada para a loja " + storeId);
        }
        if (!cred.isTokenValid()) {
            throw new IfoodIntegrationException("Token iFood expirado para a loja " + storeId + ". Aguarde o ciclo de renovação.");
        }
        return cred.getAccessToken();
    }
}
