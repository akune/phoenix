package de.kune.phoenix.server;

import java.util.Set;
import java.util.function.Predicate;

import de.kune.phoenix.shared.Identifiable;

/**
 * Specifies an object store.
 *
 * @param <T>
 *            the element type
 */
public interface ObjectStore<T extends Identifiable<?>> {

	/**
	 * Specifies an object store listener
	 *
	 * @param <T>
	 *            the element type
	 */
	interface ObjectStoreListener<T> {
		/**
		 * Indicates that the specified element has been added to the observed
		 * store.
		 * 
		 * @param object
		 *            the element
		 */
		void added(T object);

		/**
		 * Indicates that the specified element has been removed from the
		 * observed store.
		 * 
		 * @param object
		 *            the element
		 */
		void removed(T object);

		/**
		 * Indicates that the specified element has been updated in the observed
		 * store.
		 * 
		 * @param object
		 *            the element
		 */
		void updated(T object);
	}

	/**
	 * Adds the specified element to this store.
	 * 
	 * @param object
	 *            the element
	 */
	void add(T object);

	/**
	 * Returns all elements in this store.
	 * 
	 * @return a set of all elements
	 */
	Set<T> get();

	/**
	 * Removes all elements from this store.
	 */
	void clear();

	/**
	 * Returns all matching elements from this store.
	 * 
	 * @param predicate
	 *            the predicate
	 * @return a set of matching elements
	 */
	Set<T> get(Predicate<T> predicate);

	/**
	 * Returns all matching elements from this store and waits for at least one
	 * element to be added if no match exists.
	 * 
	 * @param predicate
	 *            the predicate
	 * @return a set of at least one matching element
	 */
	Set<T> await(Predicate<T> predicate);

	/**
	 * Removes all matching elements from this store.
	 * 
	 * @param predicate
	 *            the predicate
	 */
	void remove(Predicate<T> predicate);

	/**
	 * Removes the element from this store
	 * 
	 * @param object
	 *            the element to remove
	 */
	void remove(T object);

	/**
	 * Returns any element of this store.
	 * 
	 * @return an element or null if this store is empty
	 */
	T any();

	/**
	 * Adds the specified listener.
	 * 
	 * @param predicate
	 *            the predicate
	 * @param listener
	 *            the listener
	 */
	void addListener(Predicate<T> predicate, ObjectStoreListener<T> listener);

	/**
	 * Removes the specified listener.
	 * 
	 * @param predicate
	 *            the predicate
	 * @param listener
	 *            the listener
	 */
	void removeListener(Predicate<T> predicate, ObjectStoreListener<T> listener);

	/**
	 * Generates a store-unique id.
	 * 
	 * @return an id string
	 */
	String generateId();

	/**
	 * Updates the specified object. Adds the object to this store if it does
	 * not exist.
	 * 
	 * @param object
	 *            the object
	 */
	void update(T object);

	/**
	 * Checks if this store contains a message with the specified id.
	 * 
	 * @param id
	 *            the id
	 * @return true if the store contains the specified message, else false
	 */
	boolean contains(String id);

}
