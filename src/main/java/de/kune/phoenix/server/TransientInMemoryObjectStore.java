package de.kune.phoenix.server;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.kune.phoenix.shared.Identifiable;

public class TransientInMemoryObjectStore<T extends Identifiable<I>, I> extends LockingObjectStore<T, I> implements ObjectStore<T, I> {
	private Map<I, T> objects = new LinkedHashMap<>();

	@Override
	protected void doClear() {
		objects.clear();
	}

	@Override
	protected boolean doesContain(I id) {
		return objects.containsKey(id);
	}

	@Override
	protected void doPut(T object) {
		objects.put(object.getId(), object);
	}

	@Override
	protected Set<T> doGetAll() {
		return new LinkedHashSet<T>(objects.values());
	}

	@Override
	protected Iterator<T> doIterate() {
		return objects.values().iterator();
	}

	@Override
	protected T doGetAny() {
		return objects.values().stream().findAny().orElse(null);
	}

	@Override
	protected void doRemove(I id) {
		objects.remove(id);
		
	}
}