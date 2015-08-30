package de.kune.phoenix.server;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.kune.phoenix.shared.Message;

@Path("/")
public class MessageResource {

	private static final Set<Message> messages = new LinkedHashSet<Message>();
	private static final ConcurrentMap<String, Set<Message>> conversations = new ConcurrentHashMap<String, Set<Message>>();

	@POST
	@Path("message")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response post(Message message) {
		message.setTransmission(new Date());
		add(messages, message);
		return Response.status(200).build();
	}

	private Set<Message> getConversation(String conversationId) {
		Set<Message> result = conversations.get(conversationId);
		if (result == null) {
			conversations.putIfAbsent(conversationId, new LinkedHashSet<Message>());
			return conversations.get(conversationId);
		} else {
			return result;
		}
	}

	private void add(Set<Message> container, Message message) {
		synchronized (container) {
			container.add(message);
			container.notifyAll();
		}
	}

	@GET
	@Path("message")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@QueryParam("wait") boolean wait, @QueryParam("transmitted-after") Long transmittedAfter,
			@QueryParam("recipient-id") String recipientId) {
		Set<Message> messages = getMessages(transmittedAfter == null ? null : new Date(transmittedAfter), recipientId);
		while (wait
				&& (messages = getMessages(transmittedAfter == null ? null : new Date(transmittedAfter), recipientId))
						.isEmpty()) {
			waitForNewMessages();
		}
		return Response.status(200).entity(messages).build();
	}

	private Set<Message> getMessages(Date transmittedAfter, String recipientId) {
		return getMessages(messages, transmittedAfter, recipientId);
	}

	private Set<Message> getMessages(Set<Message> container, Date transmittedAfter, String recipientId) {
		Set<Message> result = new LinkedHashSet<Message>();
		synchronized (container) {
			for (Message m : container) {
				if (transmittedAfter == null || m.getTransmission().compareTo(transmittedAfter) > 0) {
					if (recipientId == null || m.getRecipientIds() == null
							|| contains(m.getRecipientIds(), recipientId)) {
						result.add(m);
					}
				}
			}
		}
		return result;
	}

	private boolean contains(String[] recipientIds, String recipientId) {
		for (String item : recipientIds) {
			if (item.equals(recipientId)) {
				return true;
			}
		}
		return false;
	}

	private void waitForNewMessages() {
		waitForNewMessages(messages);
	}

	private void waitForNewMessages(Set<Message> container) {
		try {
			synchronized (container) {
				container.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@DELETE
	@Path("message")
	public Response clear() {
		synchronized (messages) {
			messages.clear();
		}
		return Response.status(200).build();
	}

	@GET
	@Path("conversation/{conversation}/message")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFromConversation(@PathParam("conversation") String conversationId,
			@QueryParam("wait") boolean wait, @QueryParam("transmitted-after") Long transmittedAfter,
			@QueryParam("recipient-id") String recipientId) {
		Set<Message> conversation = getConversation(conversationId);
		Set<Message> messages = getMessages(conversation, transmittedAfter == null ? null : new Date(transmittedAfter),
				recipientId);
		while (wait && (messages = getMessages(conversation,
				transmittedAfter == null ? null : new Date(transmittedAfter), recipientId)).isEmpty()) {
			waitForNewMessages(conversation);
		}
		return Response.status(200).entity(messages).build();
	}

	@POST
	@Path("conversation/{conversation}/message")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postToConversation(@PathParam("conversation") String conversationId, Message message) {
		Set<Message> conversation = getConversation(conversationId);
		message.setTransmission(new Date());
		add(conversation, message);
		return Response.status(200).build();
	}

}
