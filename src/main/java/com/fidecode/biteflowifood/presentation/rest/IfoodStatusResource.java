package com.fidecode.biteflowifood.presentation.rest;

import com.fidecode.biteflowifood.application.dto.GetStatusResponse;
import com.fidecode.biteflowifood.application.dto.ToggleStoreRequest;
import com.fidecode.biteflowifood.application.usecase.GetIfoodStatusUseCase;
import com.fidecode.biteflowifood.application.usecase.ToggleIfoodStoreUseCase;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/v1/ifood/status")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IfoodStatusResource {

    private final GetIfoodStatusUseCase getStatusUseCase;
    private final ToggleIfoodStoreUseCase toggleStoreUseCase;

    @Inject
    public IfoodStatusResource(GetIfoodStatusUseCase getStatusUseCase,
                               ToggleIfoodStoreUseCase toggleStoreUseCase) {
        this.getStatusUseCase = getStatusUseCase;
        this.toggleStoreUseCase = toggleStoreUseCase;
    }

    @GET
    @Path("/{storeId}/merchant")
    public Response getStatus(@PathParam("storeId") UUID storeId) {
        try {
            GetStatusResponse response = getStatusUseCase.execute(storeId);
            return Response.ok(response).build();
        } catch (IfoodIntegrationException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/{storeId}/merchant/toggle")
    public Response toggleStore(@PathParam("storeId") UUID storeId, ToggleStoreRequest request) {
        try {
            toggleStoreUseCase.execute(storeId, request);
            return Response.noContent().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage()).build();
        } catch (IfoodIntegrationException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(e.getMessage()).build();
        }
    }
}
