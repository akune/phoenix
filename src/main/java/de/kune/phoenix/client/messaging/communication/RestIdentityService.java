package de.kune.phoenix.client.messaging.communication;

import de.kune.phoenix.shared.Identity;
import de.kune.phoenix.shared.Message;
import org.fusesource.restygwt.client.MethodCallback;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/")
public interface RestIdentityService {

    @POST
    @Path("identity")
    @Consumes(MediaType.APPLICATION_JSON)
    void post(Identity identity, MethodCallback<Void> callback);

    @GET
    @Path("identity/{identity}")
    @Produces(MediaType.APPLICATION_JSON)
    void get(
            @PathParam("identity") String identity,
            MethodCallback<Identity> callback);
}
