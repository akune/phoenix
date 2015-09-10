package de.kune.phoenix.server;

import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Resource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import de.kune.phoenix.shared.Subscriber;

/**
 * Manages subscriptions for server sent events.
 */
@Path("subscriber")
public class SubscriptionResource {

	@Resource
	private ObjectStore<Subscriber> subscriberStore;

	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public Response postSubscriber(Subscriber subscriber) {
		Subscriber result = subscriber;
		result.setId(subscriberStore.generateId());
		subscriberStore.add(result);
		return status(OK).entity(result).build();
	}

	@PUT
	@Consumes("application/json")
	@Produces("application/json")
	public Response putSubscriber(Subscriber subscriber) {
		Subscriber result = subscriber;
		subscriberStore.update(result);
		return status(OK).entity(result).build();
	}

	@GET
	@Produces("application/json")
	@Path("{subscriber-id}")
	public Response getSubscriber(@PathParam("subscriber-id") final String subscriberId) {
		Set<Subscriber> results = subscriberStore.get(new Predicate<Subscriber>() {
			@Override
			public boolean test(Subscriber t) {
				return t.getId().equals(subscriberId);
			}
		});
		if (results.size() == 1) {
			return status(OK).entity(results.iterator().next()).build();
		} else {
			return status(NOT_FOUND).build();
		}
	}

}
