package com.trinusphere.beebiteifood.presentation.rest;

import com.trinusphere.beebiteifood.application.dto.CredentialResponse;
import com.trinusphere.beebiteifood.application.dto.ExchangeAuthCodeRequest;
import com.trinusphere.beebiteifood.application.dto.GenerateUserCodeRequest;
import com.trinusphere.beebiteifood.application.dto.SaveCredentialRequest;
import com.trinusphere.beebiteifood.application.usecase.*;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

/**
 * Resource REST para gerenciamento de credenciais iFood.
 *
 * Endpoints:
 * POST   /api/v1/ifood/credentials           — Salvar/atualizar credenciais
 * GET    /api/v1/ifood/credentials/{storeId} — Buscar credenciais de uma store
 * DELETE /api/v1/ifood/credentials/{storeId} — Remover credenciais
 * POST   /api/v1/ifood/credentials/{storeId}/test-connection — Testar conexão
 */
@Path("/api/v1/ifood/credentials")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IfoodCredentialResource {

    @Inject SaveCredentialUseCase saveCredentialUseCase;
    @Inject GetCredentialUseCase getCredentialUseCase;
    @Inject DeleteCredentialUseCase deleteCredentialUseCase;
    @Inject TestConnectionUseCase testConnectionUseCase;
    @Inject GenerateUserCodeUseCase generateUserCodeUseCase;
    @Inject ExchangeAuthCodeUseCase exchangeAuthCodeUseCase;

    @POST
    public Response save(@Valid SaveCredentialRequest request) {
        CredentialResponse response = saveCredentialUseCase.execute(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/user-code")
    public Response generateUserCode(@Valid GenerateUserCodeRequest request) {
        var response = generateUserCodeUseCase.execute(request.clientId());
        return Response.ok(response).build();
    }

    @POST
    @Path("/exchange-token")
    public Response exchangeAuthCode(@Valid ExchangeAuthCodeRequest request) {
        CredentialResponse response = exchangeAuthCodeUseCase.execute(request);
        return Response.ok(response).build();
    }

    @GET
    @Path("/{storeId}")
    public Response get(@PathParam("storeId") UUID storeId) {
        CredentialResponse response = getCredentialUseCase.execute(storeId);
        return Response.ok(response).build();
    }

    @DELETE
    @Path("/{storeId}")
    public Response delete(@PathParam("storeId") UUID storeId) {
        deleteCredentialUseCase.execute(storeId);
        return Response.noContent().build();
    }

    @POST
    @Path("/{storeId}/test-connection")
    public Response testConnection(@PathParam("storeId") UUID storeId) {
        String result = testConnectionUseCase.execute(storeId);
        return Response.ok(result).build();
    }
}
