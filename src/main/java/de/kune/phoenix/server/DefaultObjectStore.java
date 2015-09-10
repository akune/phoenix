package de.kune.phoenix.server;

import static java.lang.String.format;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import de.kune.phoenix.shared.Identifiable;

public class DefaultObjectStore<T extends Identifiable<I>, I> implements ObjectStore<T> {
	private Map<I, T> objects = new LinkedHashMap<>();
	private ReadWriteLock objectsLock = new ReentrantReadWriteLock();
	private Condition objectAdded = objectsLock.writeLock().newCondition();
	private ReadWriteLock listenersLock = new ReentrantReadWriteLock();
	private ConcurrentMap<Predicate<T>, Set<ObjectStoreListener<T>>> listeners = new ConcurrentHashMap<>();
	private static final AtomicLong sequence = new AtomicLong(0L);

	@Override
	public void add(T object) {
		objectsLock.writeLock().lock();
		try {
			objects.put(object.getId(), object);
			objectAdded.signalAll();
			invokeObjectAddedListeners(object);
		} finally {
			objectsLock.writeLock().unlock();
		}
	}

	@Override
	public void update(T object) {
		objectsLock.writeLock().lock();
		try {
			if (objects.containsKey(object.getId())) {
				objects.put(object.getId(), object);
				invokeObjectUpdatedListener(object);
			} else {
				add(object);
			}
		} finally {
			objectsLock.writeLock().unlock();
		}
	}

	@Override
	public Set<T> get() {
		objectsLock.readLock().lock();
		try {
			return new LinkedHashSet<T>(objects.values());
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
		for (Iterator<T> it = objects.values().iterator(); it.hasNext();) {
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
		invokeObjectRemovedListener(object);
		objectsLock.writeLock().unlock();
	}

	@Override
	public void clear() {
		objectsLock.writeLock().lock();
		try {
			objects.clear();
			for (T object : objects.values()) {
				invokeObjectRemovedListener(object);
			}
		} finally {
			objectsLock.writeLock().unlock();
		}
	}

	private void invokeListener(T object, BiConsumer<T, ObjectStoreListener<T>> action) {
		listenersLock.readLock().lock();
		try {
			for (Entry<Predicate<T>, Set<ObjectStoreListener<T>>> e : listeners.entrySet()) {
				if (e.getKey().test(object)) {
					for (ObjectStoreListener<T> l : e.getValue()) {
						action.accept(object, l);
					}
				}
			}
		} finally {
			listenersLock.readLock().unlock();
		}
	}

	private void invokeObjectAddedListeners(T object) {
		invokeListener(object, new BiConsumer<T, ObjectStoreListener<T>>() {
			@Override
			public void accept(T t, ObjectStoreListener<T> l) {
				l.added(t);
			}
		});
	}

	private void invokeObjectUpdatedListener(T object) {
		invokeListener(object, new BiConsumer<T, ObjectStoreListener<T>>() {
			@Override
			public void accept(T t, ObjectStoreListener<T> l) {
				l.updated(t);
			}
		});
	}

	private void invokeObjectRemovedListener(T object) {
		invokeListener(object, new BiConsumer<T, ObjectStoreListener<T>>() {
			@Override
			public void accept(T t, ObjectStoreListener<T> l) {
				l.removed(t);
			}
		});
	}

	@Override
	public void addListener(Predicate<T> predicate, ObjectStoreListener<T> listener) {
		listenersLock.writeLock().lock();
		try {
			listeners.putIfAbsent(predicate, new HashSet<ObjectStoreListener<T>>());
			listeners.get(predicate).add(listener);
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

	@Override
	public String generateId() {
		return format("%025d", sequence.getAndIncrement());
	}
}