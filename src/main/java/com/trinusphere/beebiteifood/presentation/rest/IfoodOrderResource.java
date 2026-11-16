package com.trinusphere.beebiteifood.presentation.rest;

import com.trinusphere.beebiteifood.application.dto.IfoodCancellationReason;
import com.trinusphere.beebiteifood.application.usecase.GetCancellationReasonsUseCase;
import com.trinusphere.beebiteifood.domain.credential.exception.CredentialNotFoundException;
import com.trinusphere.beebiteifood.domain.shared.exception.IfoodIntegrationException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints REST para operações em pedidos iFood no BiteFlow.
 */
@Path("/api/v1/ifood/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IfoodOrderResource {

    private final GetCancellationReasonsUseCase getCancellationReasonsUseCase;

    @Inject
    public IfoodOrderResource(GetCancellationReasonsUseCase getCancellationReasonsUseCase) {
        this.getCancellationReasonsUseCase = getCancellationReasonsUseCase;
    }

    /**
     * Lista os motivos de cancelamento disponíveis para um pedido iFood.
     * Exigido na homologação para que a UI apresente os motivos oficiais da API.
     */
    @GET
    @Path("/{orderId}/cancellation-reasons")
    public Response getCancellationReasons(@PathParam("orderId") String orderId,
                                           @QueryParam("storeId") UUID storeId) {
        if (storeId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("storeId é obrigatório").build();
        }
        try {
            List<IfoodCancellationReason> reasons = getCancellationReasonsUseCase.execute(storeId, orderId);
            return Response.ok(reasons).build();
        } catch (CredentialNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage()).build();
        } catch (IfoodIntegrationException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(e.getMessage()).build();
        }
    }
}
