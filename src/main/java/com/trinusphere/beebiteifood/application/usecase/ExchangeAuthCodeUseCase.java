package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.CredentialResponse;
import com.trinusphere.beebiteifood.application.dto.ExchangeAuthCodeRequest;
import com.trinusphere.beebiteifood.application.dto.IfoodTokenResponse;
import com.trinusphere.beebiteifood.application.port.IfoodAuthClient;
import com.trinusphere.beebiteifood.application.port.SecretEncryptor;
import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;
import com.trinusphere.beebiteifood.domain.credential.repository.IfoodCredentialRepository;
import com.trinusphere.beebiteifood.domain.shared.exception.BusinessRuleException;

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
