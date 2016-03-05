package de.kune.phoenix.server;

import static java.lang.String.format;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import de.kune.phoenix.shared.Identifiable;
import de.kune.phoenix.shared.Sequenced;

public abstract class LockingObjectStore<T extends Identifiable<I> & Sequenced<S>, I, S extends Comparable<S>>
		implements ObjectStore<T, I, S> {

	private ReadWriteLock objectsLock = new ReentrantReadWriteLock();
	private Condition objectAdded = objectsLock.writeLock().newCondition();

	@Override
	public void add(T object) {
		objectsLock.writeLock().lock();
		try {
			if (doesContain(object.getId())) {
				throw new IllegalStateException(format("object with id [%s] already exists", object.getId()));
			}
			doPut(object);
			objectAdded.signalAll();
		} finally {
			objectsLock.writeLock().unlock();
		}
	}

	@Override
	public Set<T> get() {
		objectsLock.readLock().lock();
		try {
			return doGetAll();
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
		objectsLock.readLock().lock();
		try {
			Set<T> result = get();
			for (Iterator<T> it = result.iterator(); it.hasNext();) {
				if (!predicate.test(it.next())) {
					it.remove();
				}
			}
			return result;
		} finally {
			objectsLock.readLock().unlock();
		}
	}

	@Override
	public void remove(Predicate<T> predicate) {
		objectsLock.writeLock().lock();
		try {
			for (Iterator<T> it = doIterate(); it.hasNext();) {
				if (predicate.test(it.next())) {
					it.remove();
				}
			}
		} finally {
			objectsLock.writeLock().unlock();
		}
	}

	@Override
	public void remove(T object) {
		objectsLock.writeLock().lock();
		try {
			doRemove(object.getId());
		} finally {
			objectsLock.writeLock().unlock();
		}
	}

	@Override
	public void clear() {
		objectsLock.writeLock().lock();
		try {
			doClear();
		} finally {
			objectsLock.writeLock().unlock();
		}
	}

	@Override
	public S generateSequenceKey() {
		return doGenerateSequenceKey();
	}
	
	protected abstract S doGenerateSequenceKey();
	
	@Override
	public T any() {
		objectsLock.readLock().lock();
		try {
			return doGetAny();
		} finally {
			objectsLock.readLock().unlock();
		}
	}

	public String toString() {
		return getClass().getName() + get().toString();
	}

	@Override
	public boolean contains(I id) {
		objectsLock.readLock().lock();
		try {
			return doesContain(id);
		} finally {
			objectsLock.readLock().unlock();
		}
	}

	protected abstract void doRemove(I id);

	protected abstract void doClear();

	protected abstract boolean doesContain(I id);

	protected abstract void doPut(T object);

	protected abstract Set<T> doGetAll();

	protected abstract Iterator<T> doIterate();

	protected abstract T doGetAny();

}
