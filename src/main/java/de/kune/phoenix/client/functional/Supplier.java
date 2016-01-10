package de.kune.phoenix.client.functional;

@FunctionalInterface
public interface Supplier<T> {

	/**
	 * Gets a result.
	 *
	 * @return a result
	 */
	T get();
	
}