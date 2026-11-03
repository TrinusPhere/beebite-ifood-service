package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.IfoodCancellationReason;
import com.fidecode.biteflowifood.application.port.IfoodOrderClient;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Use Case — Consulta motivos de cancelamento disponíveis para um pedido iFood.
 * Exigido para homologação de pedidos: a tela do operador deve exibir os motivos
 * retornados pela API antes de solicitar o cancelamento.
 */
@ApplicationScoped
public class GetCancellationReasonsUseCase {

    private static final Logger LOG = Logger.getLogger(GetCancellationReasonsUseCase.class);

    private final IfoodCredentialRepository credentialRepository;
    private final IfoodOrderClient orderClient;

    @Inject
    public GetCancellationReasonsUseCase(IfoodCredentialRepository credentialRepository,
                                         IfoodOrderClient orderClient) {
        this.credentialRepository = credentialRepository;
        this.orderClient = orderClient;
    }

    public List<IfoodCancellationReason> execute(UUID storeId, String ifoodOrderId) {
        IfoodCredential cred = credentialRepository.findByStoreId(storeId)
                .orElseThrow(() -> new CredentialNotFoundException(storeId.toString()));

        if (!cred.isEnabled() || !cred.isTokenValid()) {
            LOG.warnf("[CANCEL-REASONS] Integração desabilitada ou token inválido para store=%s", storeId);
            return Collections.emptyList();
        }

        return orderClient.getCancellationReasons(cred.getAccessToken(), ifoodOrderId);
    }
}
