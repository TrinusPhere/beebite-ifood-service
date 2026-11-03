package com.fidecode.biteflowifood.presentation.rest;

import com.fidecode.biteflowifood.application.dto.IfoodCancellationReason;
import com.fidecode.biteflowifood.application.usecase.GetCancellationReasonsUseCase;
import com.fidecode.biteflowifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IfoodOrderResourceTest {

    @Mock
    GetCancellationReasonsUseCase getCancellationReasonsUseCase;

    @InjectMocks
    IfoodOrderResource resource;

    private final UUID storeId = UUID.randomUUID();

    @Test
    void shouldReturnBadRequestWhenStoreIdIsNull() {
        Response response = resource.getCancellationReasons("order-123", null);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verifyNoInteractions(getCancellationReasonsUseCase);
    }

    @Test
    void shouldReturnOkWithReasonsWhenSuccessful() {
        List<IfoodCancellationReason> reasons = List.of(
                new IfoodCancellationReason("501", "PROBLEMAS DE SISTEMA"),
                new IfoodCancellationReason("503", "ITEM INDISPONÍVEL")
        );
        when(getCancellationReasonsUseCase.execute(storeId, "order-123")).thenReturn(reasons);

        Response response = resource.getCancellationReasons("order-123", storeId);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(reasons, response.getEntity());
    }

    @Test
    void shouldReturnNotFoundWhenStoreNotFound() {
        when(getCancellationReasonsUseCase.execute(storeId, "order-123"))
                .thenThrow(new CredentialNotFoundException(storeId.toString()));

        Response response = resource.getCancellationReasons("order-123", storeId);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void shouldReturnBadGatewayWhenIntegrationFails() {
        when(getCancellationReasonsUseCase.execute(storeId, "order-123"))
                .thenThrow(new IfoodIntegrationException("iFood offline"));

        Response response = resource.getCancellationReasons("order-123", storeId);

        assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), response.getStatus());
    }
}
