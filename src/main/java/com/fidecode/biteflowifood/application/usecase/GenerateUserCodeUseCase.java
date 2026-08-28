package com.fidecode.biteflowifood.application.usecase;

import com.fidecode.biteflowifood.application.dto.IfoodUserCodeResponse;
import com.fidecode.biteflowifood.application.port.IfoodAuthClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GenerateUserCodeUseCase {

    private final IfoodAuthClient authClient;

    @Inject
    public GenerateUserCodeUseCase(IfoodAuthClient authClient) {
        this.authClient = authClient;
    }

    public IfoodUserCodeResponse execute(String clientId) {
        return authClient.generateUserCode(clientId);
    }
}
