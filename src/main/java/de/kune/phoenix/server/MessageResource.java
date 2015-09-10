package de.kune.phoenix.server;

import static de.kune.phoenix.server.util.ArrayUtils.contains;

import java.util.function.Predicate;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.kune.phoenix.shared.Message;

@Path("/")
public class MessageResource {

	private static final ObjectStore<Message> messageStore = new DefaultObjectStore<Message, String>();

	public static ObjectStore<Message> getMessagStore() {
		return messageStore;
	}

	@POST
	@Path("message")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response post(Message message) {
		message.setTransmission(messageStore.generateId());
		messageStore.add(message);
		return Response.status(200).build();
	}

	@GET
	@Path("message")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@QueryParam("wait") boolean wait, @QueryParam("last-transmission") String lastTransmission,
			@QueryParam("recipient-id") String recipientId, @QueryParam("conversation-id") String conversationId) {
		if (wait) {
			return Response.status(200)
					.entity(messageStore.await(predicate(recipientId, conversationId, lastTransmission))).build();
		} else {
			return Response.status(200)
					.entity(messageStore.get(predicate(recipientId, conversationId, lastTransmission))).build();
		}
	}

	@DELETE
	@Path("message")
	public Response clear() {
		messageStore.clear();
		return Response.status(200).build();
	}

	private Predicate<Message> predicate(final String recipientId, final String conversationId,
			final String lastTransmission) {
		return new Predicate<Message>() {
			@Override
			public boolean test(Message t) {
				return (recipientId == null || t.getRecipientIds() == null
						|| contains(t.getRecipientIds(), recipientId))
						&& (conversationId == t.getConversationId()
								|| conversationId != null && conversationId.equals(t.getConversationId()))
						&& (lastTransmission == null || lastTransmission.compareTo(t.getTransmission()) < 0);
			}
		};
	}

}
