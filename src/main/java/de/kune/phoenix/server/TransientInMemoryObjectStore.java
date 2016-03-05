package de.kune.phoenix.server;

import static java.lang.String.format;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import de.kune.phoenix.shared.Identifiable;
import de.kune.phoenix.shared.Sequenced;

public class TransientInMemoryObjectStore<T extends Identifiable<I> & Sequenced<String>, I>
		extends LockingObjectStore<T, I, String> {
	private Map<I, T> objects = new LinkedHashMap<>();

	private static final AtomicLong sequence = new AtomicLong(0L);

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

	@Override
	protected String doGenerateSequenceKey() {
		return format("%025d", sequence.getAndIncrement());
	}

}