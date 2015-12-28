package de.kune.phoenix.server;

import static de.kune.phoenix.server.util.ArrayUtils.contains;

import java.util.List;
import java.util.function.Predicate;

import javax.inject.Inject;
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

@Path("message")
public class MessageResource {

	@Inject
	private ObjectStore<Message> messageStore;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response post(List<Message> messages) {
		for (Message message: messages) {
			message.setSequenceKey(messageStore.generateId());
			messageStore.add(message);
		}
		return Response.status(200).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@QueryParam("wait") boolean wait, @QueryParam("last-sequence-key") String lastSequenceKey,
			@QueryParam("recipient-id") String recipientId, @QueryParam("conversation-id") String conversationId) {
		if (wait) {
			return Response.status(200)
					.entity(messageStore.await(hasRecipient(recipientId).and(hasConversationId(conversationId)).and(wasReceivedAfter(lastSequenceKey))))
					.build();
		} else {
			return Response.status(200).entity(messageStore.get(predicate(recipientId, conversationId))).build();
		}
	}

	private Predicate<? super Message> wasReceivedAfter(String lastSequenceKey) {
		return m->lastSequenceKey == null || lastSequenceKey.compareTo(m.getSequenceKey()) < 0;
	}

	private Predicate<Message> hasConversationId(String conversationId) {
		return m -> (conversationId == null || conversationId.equals(m.getConversationId()));
	}

	@DELETE
	public Response clear() {
		messageStore.clear();
		return Response.status(200).build();
	}

	private Predicate<Message> hasRecipient(String recipientId) {
		return m -> (recipientId == null || contains(m.getRecipientIds(), recipientId));
	}

	private Predicate<Message> predicate(final String recipientId, final String conversationId) {
		return (message) -> (recipientId == null || message.getRecipientIds() == null
				|| contains(message.getRecipientIds(), recipientId))
				&& (conversationId == null || conversationId.equals(message.getConversationId()));
	}

}
