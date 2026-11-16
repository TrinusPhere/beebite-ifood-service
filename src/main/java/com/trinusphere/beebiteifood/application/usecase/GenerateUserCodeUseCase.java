package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.IfoodUserCodeResponse;
import com.trinusphere.beebiteifood.application.port.IfoodAuthClient;

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
