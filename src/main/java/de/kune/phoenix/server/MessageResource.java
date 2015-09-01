package de.kune.phoenix.server;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
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

	public static class DefaultObjectStore<T> implements ObjectStore<T> {
		private Set<T> objects = new LinkedHashSet<T>();
		private ReadWriteLock objectsLock = new ReentrantReadWriteLock();
		private Condition objectAdded = objectsLock.writeLock().newCondition();
		private ReadWriteLock listenersLock = new ReentrantReadWriteLock();
		private ConcurrentMap<Predicate<T>, Set<ObjectStoreListener<T>>> listeners = new ConcurrentHashMap<Predicate<T>, Set<ObjectStoreListener<T>>>();

		@Override
		public void add(T object) {
			objectsLock.writeLock().lock();
			try {
				objects.add(object);
				objectAdded.signalAll();
				invokeObjectAddedListeners(object);
			} finally {
				objectsLock.writeLock().unlock();
			}
		}

		@Override
		public Set<T> get() {
			objectsLock.readLock().lock();
			try {
				return new LinkedHashSet<T>(objects);
			} finally {
				objectsLock.readLock().unlock();
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

		@Override
		public Set<T> await(Predicate<T> predicate) {
			objectsLock.writeLock().lock();
			try {
				while (!anyMessage(predicate)) {
					objectAdded.awaitUninterruptibly();
				}
				return get(predicate);
			} finally {
				objectsLock.writeLock().unlock();
			}

		}

		@Override
		public Set<T> get(Predicate<T> predicate) {
			Set<T> result = get();
			for (Iterator<T> it = result.iterator(); it.hasNext();) {
				if (!predicate.test(it.next())) {
					it.remove();
				}
			}
			return result;
		}

		@Override
		public void remove(Predicate<T> predicate) {
			objectsLock.writeLock().lock();
			for (Iterator<T> it = objects.iterator(); it.hasNext();) {
				if (predicate.test(it.next())) {
					it.remove();
				}
			}
			objectsLock.writeLock().unlock();
		}

		@Override
		public void remove(T object) {
			objectsLock.writeLock().lock();
			objects.remove(object);
			objectsLock.writeLock().unlock();
		}

		@Override
		public void clear() {
			objectsLock.writeLock().lock();
			try {
				objects.clear();
			} finally {
				objectsLock.writeLock().unlock();
			}
		}

		private void invokeObjectAddedListeners(T object) {
			listenersLock.readLock().lock();
			try {
				for (Entry<Predicate<T>, Set<ObjectStoreListener<T>>> e : listeners.entrySet()) {
					if (e.getKey().test(object)) {
						for (ObjectStoreListener<T> l : e.getValue()) {
							l.added(object);
						}
					}
				}
			} finally {
				listenersLock.readLock().unlock();
			}
		}

		@Override
		public void addListener(Predicate<T> predicate, ObjectStoreListener<T> listener) {
			listenersLock.writeLock().lock();
			try {
				listeners.putIfAbsent(predicate, new HashSet<ObjectStoreListener<T>>()).add(listener);
			} finally {
				listenersLock.writeLock().unlock();
			}
		}

		@Override
		public void removeListener(Predicate<T> predicate, ObjectStoreListener<T> listener) {
			listenersLock.writeLock().lock();
			try {
				Set<ObjectStoreListener<T>> l = listeners.get(predicate);
				if (l != null) {
					l.remove(listener);
					if (l.isEmpty()) {
						listeners.remove(predicate);
					}
				}
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}

	private static final String GENERAL = "general";
	private static final ConcurrentMap<String, DefaultObjectStore<Message>> conversations = new ConcurrentHashMap<String, DefaultObjectStore<Message>>();

	@POST
	@Path("message")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response post(Message message) {
		message.setTransmission(new Date());
		getConversation(GENERAL).add(message);
		return Response.status(200).build();
	}

	private DefaultObjectStore<Message> getConversation(String conversationId) {
		DefaultObjectStore<Message> result = conversations.get(conversationId);
		if (result == null) {
			conversations.putIfAbsent(conversationId, new DefaultObjectStore<Message>());
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
		DefaultObjectStore<Message> conversation = getConversation(conversationId);
		message.setTransmission(new Date());
		conversation.add(message);
		return Response.status(200).build();
	}

}
