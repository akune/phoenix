package de.kune.phoenix.server;

import java.util.Set;
import java.util.function.Predicate;

import de.kune.phoenix.shared.Identifiable;
import de.kune.phoenix.shared.Sequenced;

/**
 * Specifies an object store.
 *
 * @param <T>
 *            the identifiable, sequenced element type
 * @param <I>
 *            the id type
 * @param <S>
 *            the sequence type
 */
// TODO: Refactor sequence facet into its own interface
public interface ObjectStore<T extends Identifiable<I> & Sequenced<S>, I, S extends Comparable<S>> {

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
	 * Generates a store-unique sequence id.
	 * 
	 * @return a sequence id
	 */
	S generateSequenceKey();

	/**
	 * Checks if this store contains a message with the specified id.
	 * 
	 * @param id
	 *            the id
	 * @return true if the store contains the specified message, else false
	 */
	boolean contains(I id);

}
