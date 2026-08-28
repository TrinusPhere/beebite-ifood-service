package com.fidecode.biteflowifood.presentation.rest;

import com.fidecode.biteflowifood.application.dto.GetStatusResponse;
import com.fidecode.biteflowifood.application.dto.ToggleStoreRequest;
import com.fidecode.biteflowifood.application.usecase.GetIfoodStatusUseCase;
import com.fidecode.biteflowifood.application.usecase.ToggleIfoodStoreUseCase;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@QuarkusTest
class IfoodStatusResourceTest {

    @InjectMock
    GetIfoodStatusUseCase getStatusUseCase;

    @InjectMock
    ToggleIfoodStoreUseCase toggleStoreUseCase;

    @Test
    void shouldReturnStatus() {
        UUID storeId = UUID.randomUUID();
        GetStatusResponse mockResponse = new GetStatusResponse(true, Collections.emptyList());
        when(getStatusUseCase.execute(storeId)).thenReturn(mockResponse);

        given()
            .when().get("/api/v1/ifood/status/" + storeId + "/merchant")
            .then()
            .statusCode(200)
            .body("isOpen", is(true));
    }

    @Test
    void shouldToggleStore() {
        UUID storeId = UUID.randomUUID();
        ToggleStoreRequest request = new ToggleStoreRequest("CLOSE", "Motivo de Teste");

        doNothing().when(toggleStoreUseCase).execute(eq(storeId), any(ToggleStoreRequest.class));

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when().post("/api/v1/ifood/status/" + storeId + "/merchant/toggle")
            .then()
            .statusCode(204);
    }
}
