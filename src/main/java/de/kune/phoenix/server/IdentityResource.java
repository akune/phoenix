package de.kune.phoenix.server;

import static de.kune.phoenix.shared.util.ArrayUtils.contains;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.kune.phoenix.shared.Identity;
import de.kune.phoenix.shared.Message;

@Path("identity")
public class IdentityResource {

	@Inject
	private Map<String, Identity> identityStore = new HashMap<>();

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response post(Identity identity) {
		identityStore.put(identity.getId(), identity);
		return Response.status(200).build();
	}

	@GET
	@Path("{identity}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@PathParam("identity") String identity) {
		if (identityStore.containsKey(identity)) {
			return Response.status(200).entity(identityStore.get(identity)).build();
		} else {
			return Response.status(404).build();
		}
	}

}
