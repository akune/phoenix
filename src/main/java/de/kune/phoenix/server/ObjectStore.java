package de.kune.phoenix.server;

import java.util.Set;
import java.util.function.Predicate;

import de.kune.phoenix.shared.Identifiable;

public interface ObjectStore<T extends Identifiable<?>> {

	interface ObjectStoreListener<T> {
		void added(T object);
		void removed(T object);
		void updated(T object);
	}

	void add(T object);

	Set<T> get();

	void clear();

	Set<T> get(Predicate<T> predicate);

	Set<T> await(Predicate<T> predicate);

	void remove(Predicate<T> predicate);

	void remove(T object);

	void addListener(Predicate<T> predicate, ObjectStoreListener<T> listener);

	void removeListener(Predicate<T> predicate, ObjectStoreListener<T> listener);
	
	String generateId();

	void update(T object);

}
