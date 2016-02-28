package de.kune.phoenix.server;

import static java.lang.String.format;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import de.kune.phoenix.shared.Identifiable;

public class TransientInMemoryObjectStore<T extends Identifiable<I>, I> implements ObjectStore<T> {
	private Map<I, T> objects = new LinkedHashMap<>();
	private ReadWriteLock objectsLock = new ReentrantReadWriteLock();
	private Condition objectAdded = objectsLock.writeLock().newCondition();
	private static final AtomicLong sequence = new AtomicLong(0L);

	@Override
	public void add(T object) {
		objectsLock.writeLock().lock();
		try {
			if (objects.containsKey(object.getId())) {
				throw new IllegalArgumentException(format("object with id [%s] already exists", object.getId()));
			}
			objects.put(object.getId(), object);
			objectAdded.signalAll();
		} finally {
			objectsLock.writeLock().unlock();
		}
	}

	@Override
	public void update(T object) {
		objectsLock.writeLock().lock();
		try {
			if (!objects.containsKey(object.getId())) {
				throw new IllegalArgumentException(format("object with id [%s] does not exist", object.getId()));
			}
			if (objects.containsKey(object.getId())) {
				objects.put(object.getId(), object);
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

	@Override
	public String generateId() {
		return format("%025d", sequence.getAndIncrement());
	}

	@Override
	public T any() {
		objectsLock.readLock().lock();
		try {
			return objects.values().stream().findAny().orElse(null);
		} finally {
			objectsLock.readLock().unlock();
		}
	}

	public String toString() {
		return getClass().getName() + objects.toString();
	}

	@Override
	public boolean contains(String id) {
		return objects.containsKey(id);
	}
}