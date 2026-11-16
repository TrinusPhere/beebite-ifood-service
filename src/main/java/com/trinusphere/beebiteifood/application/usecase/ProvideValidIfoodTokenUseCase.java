package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.IfoodTokenResponse;
import com.trinusphere.beebiteifood.application.port.IfoodAuthClient;
import com.trinusphere.beebiteifood.application.port.SecretEncryptor;
import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;
import com.trinusphere.beebiteifood.domain.credential.repository.IfoodCredentialRepository;
import com.trinusphere.beebiteifood.domain.shared.exception.BusinessRuleException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Use Case — ProvideValidIfoodTokenUseCase.
 * Ponto único e centralizado para obtenção e renovação de tokens do iFood.
 * Respeita o teto de 6h e suporta renovação por refresh token ou client_credentials.
 */
@ApplicationScoped
public class ProvideValidIfoodTokenUseCase {

    private static final Logger LOG = Logger.getLogger(ProvideValidIfoodTokenUseCase.class);

    private final IfoodCredentialRepository repository;
    private final IfoodAuthClient authClient;
    private final SecretEncryptor encryptor;

    @Inject
    public ProvideValidIfoodTokenUseCase(IfoodCredentialRepository repository,
                                         IfoodAuthClient authClient,
                                         SecretEncryptor encryptor) {
        this.repository = repository;
        this.authClient = authClient;
        this.encryptor = encryptor;
    }

    public IfoodCredential execute(UUID storeId) {
        IfoodCredential credential = repository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessRuleException("Credenciais do iFood não configuradas para esta loja."));

        return ensureValid(credential);
    }

    public IfoodCredential ensureValid(IfoodCredential credential) {
        if (!credential.isTokenValid()) {
            LOG.infof("[IFOOD-TOKEN] Token inválido ou expirado para store=%s, renovando...", credential.getStoreId());
            renew(credential);
        }
        return credential;
    }

    public IfoodCredential invalidateAndRenew(IfoodCredential credential) {
        LOG.warnf("[IFOOD-TOKEN] Invalidando token rejeitado pelo iFood para store=%s...", credential.getStoreId());
        credential.invalidateToken();
        renew(credential);
        return credential;
    }

    private void renew(IfoodCredential credential) {
        String plainSecret = encryptor.decrypt(credential.getEncryptedClientSecret());
        IfoodTokenResponse tokenResponse;
        if (credential.getRefreshToken() != null && !credential.getRefreshToken().isBlank()) {
            tokenResponse = authClient.refreshToken(credential.getClientId(), plainSecret, credential.getRefreshToken());
        } else {
            tokenResponse = authClient.authenticate(credential.getClientId(), plainSecret);
        }
        credential.updateToken(tokenResponse.accessToken(), tokenResponse.refreshToken(), tokenResponse.expiresIn());
        repository.save(credential);
    }
}
