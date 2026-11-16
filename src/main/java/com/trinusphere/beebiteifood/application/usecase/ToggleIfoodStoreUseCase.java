package com.trinusphere.beebiteifood.application.usecase;

import com.trinusphere.beebiteifood.application.dto.IfoodInterruptionResponse;
import com.trinusphere.beebiteifood.application.dto.ToggleStoreRequest;
import com.trinusphere.beebiteifood.application.port.IfoodMerchantClient;
import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;
import com.trinusphere.beebiteifood.domain.credential.exception.CredentialNotFoundException;
import com.trinusphere.beebiteifood.domain.credential.repository.IfoodCredentialRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Use Case — ToggleIfoodStoreUseCase.
 * Abre ou fecha a loja no iFood criando ou removendo interrupções programadas.
 */
@ApplicationScoped
public class ToggleIfoodStoreUseCase {

    private final IfoodCredentialRepository credentialRepository;
    private final IfoodMerchantClient merchantClient;
    private final ProvideValidIfoodTokenUseCase tokenUseCase;

    @Inject
    public ToggleIfoodStoreUseCase(IfoodCredentialRepository credentialRepository,
                                  IfoodMerchantClient merchantClient,
                                  ProvideValidIfoodTokenUseCase tokenUseCase) {
        this.credentialRepository = credentialRepository;
        this.merchantClient = merchantClient;
        this.tokenUseCase = tokenUseCase;
    }

    public void execute(UUID storeId, ToggleStoreRequest request) {
        IfoodCredential credential = credentialRepository.findByStoreId(storeId)
                .orElseThrow(() -> new CredentialNotFoundException(storeId.toString()));

        tokenUseCase.ensureValid(credential);

        String token = credential.getAccessToken();
        String merchantId = credential.getMerchantId();

        if ("OPEN".equalsIgnoreCase(request.action())) {
            List<IfoodInterruptionResponse> interruptions = merchantClient.getInterruptions(token, merchantId);
            for (IfoodInterruptionResponse interruption : interruptions) {
                merchantClient.deleteInterruption(token, merchantId, interruption.id());
            }
        } else if ("CLOSE".equalsIgnoreCase(request.action())) {
            String desc = request.description() != null ? request.description() : "Fechado pelo operador";
            String start = LocalDateTime.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            String end = LocalDateTime.now().plusHours(12).atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            merchantClient.createInterruption(token, merchantId, desc, start, end);
        } else {
            throw new IllegalArgumentException("Ação inválida: " + request.action());
        }
    }
}
