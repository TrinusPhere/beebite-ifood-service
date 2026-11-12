package com.fidecode.beebiteifood.presentation.rest;

import com.fidecode.beebiteifood.application.usecase.CompareCatalogUseCase;
import com.fidecode.beebiteifood.application.usecase.ExportCatalogToIfoodUseCase;
import com.fidecode.beebiteifood.application.usecase.ImportCatalogFromIfoodUseCase;
import com.fidecode.beebiteifood.application.usecase.MergeCatalogUseCase;
import com.fidecode.beebiteifood.application.usecase.ProvideValidIfoodTokenUseCase;
import com.fidecode.beebiteifood.domain.credential.entity.IfoodCredential;
import com.fidecode.beebiteifood.domain.credential.repository.IfoodCredentialRepository;
import com.fidecode.beebiteifood.domain.shared.exception.BusinessRuleException;
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

    @Inject
    ProvideValidIfoodTokenUseCase provideValidIfoodTokenUseCase;

    @POST
    @Path("/import/{storeId}")
    public Response importCatalogFromIfood(@PathParam("storeId") UUID storeId) {
        IfoodCredential credential = provideValidIfoodTokenUseCase.execute(storeId);

        importCatalogUseCase.execute(storeId, credential.getMerchantId(), credential.getAccessToken());

        return Response.ok().entity("{\"message\": \"Catálogo importado com sucesso\"}").build();
    }

    @POST
    @Path("/export/{storeId}")
    public Response exportCatalogToIfood(@PathParam("storeId") UUID storeId) {
        IfoodCredential credential = provideValidIfoodTokenUseCase.execute(storeId);

        exportCatalogUseCase.execute(storeId, credential.getMerchantId(), credential.getAccessToken());

        return Response.ok().entity("{\"message\": \"Catálogo exportado com sucesso\"}").build();
    }

    @GET
    @Path("/compare/{storeId}")
    public Response compareCatalog(@PathParam("storeId") UUID storeId) {
        IfoodCredential credential = provideValidIfoodTokenUseCase.execute(storeId);

        var diff = compareCatalogUseCase.execute(storeId, credential.getMerchantId(), credential.getAccessToken());
        return Response.ok(diff).build();
    }

    @POST
    @Path("/merge/{storeId}")
    public Response mergeCatalog(@PathParam("storeId") UUID storeId, MergeCatalogUseCase.MergeRequest request) {
        IfoodCredential credential = provideValidIfoodTokenUseCase.execute(storeId);

        mergeCatalogUseCase.execute(storeId, credential.getMerchantId(), credential.getAccessToken(), request);
        return Response.ok().entity("{\"message\": \"Merge executado com sucesso\"}").build();
    }
}
