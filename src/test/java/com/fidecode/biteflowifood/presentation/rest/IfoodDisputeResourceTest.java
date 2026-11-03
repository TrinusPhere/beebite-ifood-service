package com.fidecode.biteflowifood.presentation.rest;

import com.fidecode.biteflowifood.application.dto.IfoodAcceptDisputeRequest;
import com.fidecode.biteflowifood.application.dto.IfoodAlternativeDisputeRequest;
import com.fidecode.biteflowifood.application.dto.IfoodRejectDisputeRequest;
import com.fidecode.biteflowifood.application.usecase.ResolveDisputeUseCase;
import com.fidecode.biteflowifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class IfoodDisputeResourceTest {

    @Mock
    ResolveDisputeUseCase resolveDisputeUseCase;

    @InjectMocks
    IfoodDisputeResource resource;

    private final UUID storeId = UUID.randomUUID();
    private final String disputeId = "disp-123";

    @Test
    void shouldReturnBadRequestWhenStoreIdIsNullOnAccept() {
        Response response = resource.acceptDispute(disputeId, null, new IfoodAcceptDisputeRequest("STORE_CANCELLATION", null));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verifyNoInteractions(resolveDisputeUseCase);
    }

    @Test
    void shouldAcceptDisputeSuccessfully() {
        IfoodAcceptDisputeRequest req = new IfoodAcceptDisputeRequest("STORE_CANCELLATION", null);

        Response response = resource.acceptDispute(disputeId, storeId, req);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(resolveDisputeUseCase).accept(storeId, disputeId, req);
    }

    @Test
    void shouldReturnBadRequestWhenStoreIdIsNullOnReject() {
        Response response = resource.rejectDispute(disputeId, null, new IfoodRejectDisputeRequest("HIGH_STORE_DEMAND"));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verifyNoInteractions(resolveDisputeUseCase);
    }

    @Test
    void shouldRejectDisputeSuccessfully() {
        IfoodRejectDisputeRequest req = new IfoodRejectDisputeRequest("HIGH_STORE_DEMAND");

        Response response = resource.rejectDispute(disputeId, storeId, req);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(resolveDisputeUseCase).reject(storeId, disputeId, req);
    }

    @Test
    void shouldReturnBadRequestWhenStoreIdIsNullOnAlternative() {
        Response response = resource.proposeAlternative(disputeId, null, new IfoodAlternativeDisputeRequest("ADDITIONAL_TIME", Map.of("extraMinutes", 15)));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        verifyNoInteractions(resolveDisputeUseCase);
    }

    @Test
    void shouldProposeAlternativeSuccessfully() {
        IfoodAlternativeDisputeRequest req = new IfoodAlternativeDisputeRequest("ADDITIONAL_TIME", Map.of("extraMinutes", 15));

        Response response = resource.proposeAlternative(disputeId, storeId, req);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(resolveDisputeUseCase).proposeAlternative(storeId, disputeId, req);
    }

    @Test
    void shouldReturnNotFoundWhenStoreNotFound() {
        IfoodAcceptDisputeRequest req = new IfoodAcceptDisputeRequest("STORE_CANCELLATION", null);
        doThrow(new CredentialNotFoundException(storeId.toString()))
                .when(resolveDisputeUseCase).accept(storeId, disputeId, req);

        Response response = resource.acceptDispute(disputeId, storeId, req);

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void shouldReturnBadGatewayWhenIntegrationFails() {
        IfoodRejectDisputeRequest req = new IfoodRejectDisputeRequest("HIGH_STORE_DEMAND");
        doThrow(new IfoodIntegrationException("iFood API down"))
                .when(resolveDisputeUseCase).reject(storeId, disputeId, req);

        Response response = resource.rejectDispute(disputeId, storeId, req);

        assertEquals(Response.Status.BAD_GATEWAY.getStatusCode(), response.getStatus());
    }
}
