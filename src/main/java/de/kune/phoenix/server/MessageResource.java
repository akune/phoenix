package de.kune.phoenix.server;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

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

	public static class ObjectStore<T> {
		private Set<T> messages = new LinkedHashSet<T>();
		private ReadWriteLock lock = new ReentrantReadWriteLock();
		private Condition messageAdded = lock.writeLock().newCondition();

		public void add(T message) {
			lock.writeLock().lock();
			try {
				messages.add(message);
				messageAdded.signalAll();
			} finally {
				lock.writeLock().unlock();
			}
		}

		public Set<T> get() {
			lock.readLock().lock();
			try {
				return new LinkedHashSet<T>(messages);
			} finally {
				lock.readLock().unlock();
			}
		}

		private boolean anyMessage(Predicate<T> predicate) {
			for (T m : get()) {
				if (predicate.test(m)) {
					return true;
				}
			}
			return false;
		}

		public Set<T> await(Predicate<T> predicate) {
			lock.writeLock().lock();
			try {
				while (!anyMessage(predicate)) {
					messageAdded.awaitUninterruptibly();
				}
				return get(predicate);
			} finally {
				lock.writeLock().unlock();
			}

		}

		public Set<T> get(Predicate<T> predicate) {
			Set<T> result = get();
			for (Iterator<T> it = result.iterator(); it.hasNext();) {
				if (!predicate.test(it.next())) {
					it.remove();
				}
			}
			return result;
		}

		public void clear() {
			lock.writeLock().lock();
			try {
				messages.clear();
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	private static final String GENERAL = "general";
	private static final ConcurrentMap<String, ObjectStore<Message>> conversations = new ConcurrentHashMap<String, ObjectStore<Message>>();

	@POST
	@Path("message")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response post(Message message) {
		message.setTransmission(new Date());
		getConversation(GENERAL).add(message);
		return Response.status(200).build();
	}

	private ObjectStore<Message> getConversation(String conversationId) {
		ObjectStore<Message> result = conversations.get(conversationId);
		if (result == null) {
			conversations.putIfAbsent(conversationId, new ObjectStore<Message>());
			return conversations.get(conversationId);
		} else {
			return result;
		}
	}

	@GET
	@Path("message")
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@QueryParam("wait") boolean wait, @QueryParam("transmitted-after") Long transmittedAfter,
			@QueryParam("recipient-id") String recipientId) {
		if (wait) {
			return Response.status(200)
					.entity(getConversation(GENERAL).await(predicate(recipientId, toDate(transmittedAfter)))).build();
		} else {
			return Response.status(200)
					.entity(getConversation(GENERAL).get(predicate(recipientId, toDate(transmittedAfter)))).build();
		}
	}

	private boolean contains(String[] recipientIds, String recipientId) {
		for (String item : recipientIds) {
			if (item.equals(recipientId)) {
				return true;
			}
		}
		return false;
	}

	@DELETE
	@Path("message")
	public Response clear() {
		synchronized (getConversation(GENERAL)) {
			getConversation(GENERAL).clear();
		}
		return Response.status(200).build();
	}

	@GET
	@Path("conversation/{conversation}/message")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getFromConversation(@PathParam("conversation") String conversationId,
			@QueryParam("wait") boolean wait, @QueryParam("transmitted-after") Long transmittedAfter,
			@QueryParam("recipient-id") String recipientId) {
		if (wait) {
			return Response.status(200)
					.entity(getConversation(conversationId).await(predicate(recipientId, toDate(transmittedAfter))))
					.build();
		} else {
			return Response.status(200)
					.entity(getConversation(conversationId).get(predicate(recipientId, toDate(transmittedAfter))))
					.build();
		}
	}

	private Date toDate(Long transmittedAfter) {
		return transmittedAfter == null ? null : new Date(transmittedAfter);
	}

	private Predicate<Message> predicate(final String recipientId, final Date transmittedAfterDate) {
		return new Predicate<Message>() {
			@Override
			public boolean test(Message t) {
				return (recipientId == null || t.getRecipientIds() == null
						|| contains(t.getRecipientIds(), recipientId))
						&& (transmittedAfterDate == null || transmittedAfterDate.compareTo(t.getTransmission()) < 0);
			}
		};
	}

	@POST
	@Path("conversation/{conversation}/message")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postToConversation(@PathParam("conversation") String conversationId, Message message) {
		ObjectStore<Message> conversation = getConversation(conversationId);
		message.setTransmission(new Date());
		conversation.add(message);
		return Response.status(200).build();
	}

}
