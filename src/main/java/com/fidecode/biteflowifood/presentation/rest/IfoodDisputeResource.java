package com.fidecode.biteflowifood.presentation.rest;

import com.fidecode.biteflowifood.application.dto.IfoodAcceptDisputeRequest;
import com.fidecode.biteflowifood.application.dto.IfoodAlternativeDisputeRequest;
import com.fidecode.biteflowifood.application.dto.IfoodRejectDisputeRequest;
import com.fidecode.biteflowifood.application.usecase.ResolveDisputeUseCase;
import com.fidecode.biteflowifood.domain.credential.exception.CredentialNotFoundException;
import com.fidecode.biteflowifood.domain.shared.exception.IfoodIntegrationException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

/**
 * Endpoints REST para negociação de contestações / cancelamentos (Handshake Platform) do iFood.
 */
@Path("/api/v1/ifood/disputes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IfoodDisputeResource {

    private final ResolveDisputeUseCase resolveDisputeUseCase;

    @Inject
    public IfoodDisputeResource(ResolveDisputeUseCase resolveDisputeUseCase) {
        this.resolveDisputeUseCase = resolveDisputeUseCase;
    }

    /**
     * Aceita a contestação/cancelamento do pedido na plataforma Handshake.
     */
    @POST
    @Path("/{disputeId}/accept")
    public Response acceptDispute(@PathParam("disputeId") String disputeId,
                                  @QueryParam("storeId") UUID storeId,
                                  IfoodAcceptDisputeRequest request) {
        if (storeId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("storeId é obrigatório").build();
        }
        try {
            resolveDisputeUseCase.accept(storeId, disputeId, request);
            return Response.noContent().build();
        } catch (CredentialNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage()).build();
        } catch (IfoodIntegrationException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(e.getMessage()).build();
        }
    }

    /**
     * Rejeita a contestação na plataforma Handshake informando o motivo.
     */
    @POST
    @Path("/{disputeId}/reject")
    public Response rejectDispute(@PathParam("disputeId") String disputeId,
                                  @QueryParam("storeId") UUID storeId,
                                  IfoodRejectDisputeRequest request) {
        if (storeId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("storeId é obrigatório").build();
        }
        try {
            resolveDisputeUseCase.reject(storeId, disputeId, request);
            return Response.noContent().build();
        } catch (CredentialNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage()).build();
        } catch (IfoodIntegrationException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(e.getMessage()).build();
        }
    }

    /**
     * Propõe uma alternativa de negociação (ex: tempo adicional, benefício) ao cliente.
     */
    @POST
    @Path("/{disputeId}/alternative")
    public Response proposeAlternative(@PathParam("disputeId") String disputeId,
                                       @QueryParam("storeId") UUID storeId,
                                       IfoodAlternativeDisputeRequest request) {
        if (storeId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("storeId é obrigatório").build();
        }
        try {
            resolveDisputeUseCase.proposeAlternative(storeId, disputeId, request);
            return Response.noContent().build();
        } catch (CredentialNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(e.getMessage()).build();
        } catch (IfoodIntegrationException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(e.getMessage()).build();
        }
    }
}
