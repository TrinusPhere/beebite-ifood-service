package com.fidecode.biteflowifood.presentation.rest;

import com.fidecode.biteflowifood.application.usecase.SendIfoodChatMessageUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/ifood/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IfoodChatResource {

    @Inject
    SendIfoodChatMessageUseCase sendIfoodChatMessageUseCase;

    @POST
    @Path("/stores/{storeId}/orders/{orderId}/messages")
    public Response sendMessage(@PathParam("storeId") String storeId,
                                @PathParam("orderId") String orderId,
                                Map<String, String> body) {
        String message = body.get("message");
        if (message == null || message.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Message cannot be empty")).build();
        }

        sendIfoodChatMessageUseCase.execute(storeId, orderId, message);
        return Response.ok().build();
    }
}
