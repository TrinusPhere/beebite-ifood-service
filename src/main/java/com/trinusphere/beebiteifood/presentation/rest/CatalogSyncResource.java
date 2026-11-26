package com.trinusphere.beebiteifood.presentation.rest;

import com.trinusphere.beebiteifood.application.usecase.CompareCatalogUseCase;
import com.trinusphere.beebiteifood.application.usecase.ExportCatalogToIfoodUseCase;
import com.trinusphere.beebiteifood.application.usecase.ImportCatalogFromIfoodUseCase;
import com.trinusphere.beebiteifood.application.usecase.MergeCatalogUseCase;
import com.trinusphere.beebiteifood.application.usecase.ProvideValidIfoodTokenUseCase;
import com.trinusphere.beebiteifood.domain.credential.entity.IfoodCredential;
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
    ProvideValidIfoodTokenUseCase provideValidIfoodTokenUseCase;

    @POST
    @Path("/import/{storeId}")
    public Response importCatalogFromIfood(@PathParam("storeId") UUID storeId) {
        IfoodCredential credential = provideValidIfoodTokenUseCase.execute(storeId);
        importCatalogUseCase.execute(storeId, credential.getCatalogId(), credential.getMerchantId(), credential.getAccessToken());
        return Response.ok().entity("{\"message\": \"Catálogo importado com sucesso\"}").build();
    }

    @POST
    @Path("/export/{storeId}")
    public Response exportCatalogToIfood(@PathParam("storeId") UUID storeId) {
        IfoodCredential credential = provideValidIfoodTokenUseCase.execute(storeId);
        exportCatalogUseCase.execute(storeId, credential.getCatalogId(), credential.getMerchantId(), credential.getAccessToken());
        return Response.ok().entity("{\"message\": \"Catálogo exportado com sucesso\"}").build();
    }

    @GET
    @Path("/compare/{storeId}")
    public Response compareCatalog(@PathParam("storeId") UUID storeId) {
        IfoodCredential credential = provideValidIfoodTokenUseCase.execute(storeId);
        var diff = compareCatalogUseCase.execute(storeId, credential.getCatalogId(), credential.getMerchantId(), credential.getAccessToken());
        return Response.ok(diff).build();
    }

    @POST
    @Path("/merge/{storeId}")
    public Response mergeCatalog(@PathParam("storeId") UUID storeId, MergeCatalogUseCase.MergeRequest request) {
        IfoodCredential credential = provideValidIfoodTokenUseCase.execute(storeId);
        mergeCatalogUseCase.execute(storeId, credential.getCatalogId(), credential.getMerchantId(), credential.getAccessToken(), request);
        return Response.ok().entity("{\"message\": \"Merge executado com sucesso\"}").build();
    }
}
