package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.CredentialResponse;
import com.fidecode.biteflowifood.application.dto.ExchangeAuthCodeRequest;
import com.fidecode.biteflowifood.application.dto.IfoodTokenResponse;
import com.fidecode.biteflowifood.application.port.IfoodAuthClient;
import com.fidecode.biteflowifood.application.port.SecretEncryptor;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;
import com.fidecode.biteflowifood.domain.shared.exception.BusinessRuleException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ExchangeAuthCodeUseCase {

    private final IfoodAuthClient authClient;
    private final IfoodCredentialRepository credentialRepository;
    private final SecretEncryptor secretEncryptor;

    @Inject
    public ExchangeAuthCodeUseCase(IfoodAuthClient authClient,
                                   IfoodCredentialRepository credentialRepository,
                                   SecretEncryptor secretEncryptor) {
        this.authClient = authClient;
        this.credentialRepository = credentialRepository;
        this.secretEncryptor = secretEncryptor;
    }

    public CredentialResponse execute(ExchangeAuthCodeRequest request) {
        IfoodTokenResponse tokenResponse = authClient.exchangeAuthCode(
                request.clientId(),
                request.clientSecret(),
                request.authorizationCode(),
                request.authorizationCodeVerifier()
        );

        String encryptedSecret = secretEncryptor.encrypt(request.clientSecret());

        IfoodCredential credential = credentialRepository.findByStoreId(request.storeId())
                .orElse(new IfoodCredential(request.storeId(), request.clientId(), encryptedSecret, request.merchantId()));

        credential.setClientId(request.clientId());
        credential.setEncryptedClientSecret(encryptedSecret);
        credential.setMerchantId(request.merchantId());
        credential.setAuthorizationCode(request.authorizationCode());
        credential.setEnabled(true);

        credential.updateToken(tokenResponse.accessToken(), tokenResponse.refreshToken(), tokenResponse.expiresIn());

        credentialRepository.save(credential);

        return new CredentialResponse(
                credential.getId(),
                credential.getStoreId(),
                credential.getClientId(),
                credential.getMerchantId(),
                credential.isEnabled(),
                credential.isTokenValid(),
                credential.getUpdatedAt()
        );
    }
}
