package com.fidecode.biteflowifood.presentation.rest;

import com.fidecode.biteflowifood.application.usecase.CompareCatalogUseCase;
import com.fidecode.biteflowifood.application.usecase.ExportCatalogToIfoodUseCase;
import com.fidecode.biteflowifood.application.usecase.ImportCatalogFromIfoodUseCase;
import com.fidecode.biteflowifood.application.usecase.MergeCatalogUseCase;
import com.fidecode.biteflowifood.domain.credential.entity.IfoodCredential;
import com.fidecode.biteflowifood.domain.credential.repository.IfoodCredentialRepository;
import com.fidecode.biteflowifood.domain.shared.exception.BusinessRuleException;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/v1/ifood/sync")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatalogSyncResource {

    @Inject
    ImportCatalogFromIfoodUseCase importCatalogUseCase;

    @Inject
    ExportCatalogToIfoodUseCase exportCatalogUseCase;

    @Inject
    CompareCatalogUseCase compareCatalogUseCase;

    @Inject
    MergeCatalogUseCase mergeCatalogUseCase;

    @Inject
    IfoodCredentialRepository credentialRepository;

    @POST
    @Path("/import/{storeId}")
    public Response importCatalogFromIfood(@PathParam("storeId") UUID storeId) {
        IfoodCredential credential = credentialRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessRuleException("Credenciais do iFood não configuradas para esta loja."));

        if (!credential.isTokenValid()) {
            throw new BusinessRuleException("O token do iFood é inválido ou expirou.");
        }

        importCatalogUseCase.execute(storeId, credential.getMerchantId(), credential.getAccessToken());

        return Response.ok().entity("{\"message\": \"Catálogo importado com sucesso\"}").build();
    }

    @POST
    @Path("/export/{storeId}")
    public Response exportCatalogToIfood(@PathParam("storeId") UUID storeId) {
        IfoodCredential credential = credentialRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessRuleException("Credenciais do iFood não configuradas para esta loja."));

        if (!credential.isTokenValid()) {
            throw new BusinessRuleException("O token do iFood é inválido ou expirou.");
        }

        exportCatalogUseCase.execute(storeId, credential.getMerchantId(), credential.getAccessToken());

        return Response.ok().entity("{\"message\": \"Catálogo exportado com sucesso\"}").build();
    }

    @GET
    @Path("/compare/{storeId}")
    public Response compareCatalog(@PathParam("storeId") UUID storeId) {
        IfoodCredential credential = credentialRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessRuleException("Credenciais do iFood não configuradas."));

        if (!credential.isTokenValid()) {
            throw new BusinessRuleException("O token do iFood é inválido ou expirou.");
        }

        var diff = compareCatalogUseCase.execute(storeId, credential.getMerchantId(), credential.getAccessToken());
        return Response.ok(diff).build();
    }

    @POST
    @Path("/merge/{storeId}")
    public Response mergeCatalog(@PathParam("storeId") UUID storeId, MergeCatalogUseCase.MergeRequest request) {
        IfoodCredential credential = credentialRepository.findByStoreId(storeId)
                .orElseThrow(() -> new BusinessRuleException("Credenciais do iFood não configuradas."));

        if (!credential.isTokenValid()) {
            throw new BusinessRuleException("O token do iFood é inválido ou expirou.");
        }

        mergeCatalogUseCase.execute(storeId, credential.getMerchantId(), credential.getAccessToken(), request);
        return Response.ok().entity("{\"message\": \"Merge executado com sucesso\"}").build();
    }
}
