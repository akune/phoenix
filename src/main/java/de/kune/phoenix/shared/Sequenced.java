package de.kune.phoenix.shared;

public interface Sequenced<T extends Comparable<T>> {

	T getSequenceKey();
	
}
