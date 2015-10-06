package de.kune.phoenix.client.messaging;

import java.util.Collection;

public interface Processor<T> {

	void process(Collection<T> object);
	
	T getLastProcessedObject();
	
}
