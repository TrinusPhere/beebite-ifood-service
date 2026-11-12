package com.fidecode.beebiteifood.presentation.rest;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 * Endpoint de chat legado.
 *
 * @deprecated O iFood NÃO disponibiliza API REST pública nem webhook para troca de mensagens de chat.
 * O chat oficial com clientes e entregadores deve ser integrado exclusivamente via iFood Widget
 * (https://widgets.ifood.com.br/widget.js) no frontend.
 */
@Deprecated(forRemoval = true)
@Path("/ifood/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IfoodChatResource {

    @POST
    @Path("/stores/{storeId}/orders/{orderId}/messages")
    public Response sendMessage(@PathParam("storeId") String storeId,
                                @PathParam("orderId") String orderId,
                                Map<String, String> body) {
        return Response.status(Response.Status.GONE)
                .entity(Map.of(
                        "error", "API de Chat REST descontinuada",
                        "message", "O iFood não possui API REST pública para chat. O chat deve ser integrado via iFood Widget (https://widgets.ifood.com.br/widget.js) no frontend."
                )).build();
    }
}
