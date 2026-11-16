package com.trinusphere.beebiteifood.presentation.rest;

import com.trinusphere.beebiteifood.application.dto.GetStatusResponse;
import com.trinusphere.beebiteifood.application.dto.ToggleStoreRequest;
import com.trinusphere.beebiteifood.application.usecase.GetIfoodStatusUseCase;
import com.trinusphere.beebiteifood.application.usecase.ToggleIfoodStoreUseCase;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IfoodStatusResourceTest {

    @Mock
    GetIfoodStatusUseCase getStatusUseCase;

    @Mock
    ToggleIfoodStoreUseCase toggleStoreUseCase;

    @InjectMocks
    IfoodStatusResource resource;

    @Test
    void shouldReturnStatus() {
        UUID storeId = UUID.randomUUID();
        GetStatusResponse mockResponse = new GetStatusResponse(true, Collections.emptyList());
        when(getStatusUseCase.execute(storeId)).thenReturn(mockResponse);

        Response response = resource.getStatus(storeId);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(mockResponse, response.getEntity());
        verify(getStatusUseCase).execute(storeId);
    }

    @Test
    void shouldToggleStore() {
        UUID storeId = UUID.randomUUID();
        ToggleStoreRequest request = new ToggleStoreRequest("CLOSE", "Motivo de Teste");
        doNothing().when(toggleStoreUseCase).execute(eq(storeId), any(ToggleStoreRequest.class));

        Response response = resource.toggleStore(storeId, request);

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        verify(toggleStoreUseCase).execute(storeId, request);
    }
}
