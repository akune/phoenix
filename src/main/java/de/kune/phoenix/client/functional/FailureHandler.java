package de.kune.phoenix.client.functional;

import com.google.gwt.core.shared.GWT;

@FunctionalInterface
public interface FailureHandler<T> {

	static <T> FailureHandler<T> nothing() {
		return new FailureHandler<T>() {
			@Override
			public void handle(Throwable t, T payload) {
			}
		};
	}
	
	static <T> FailureHandler<T> log() {
		return new FailureHandler<T>() {
			@Override
			public void handle(Throwable t, T payload) {
				GWT.log("failure with payload [" + payload + "]", t);
			}
		};
	}

	void handle(Throwable t, T payload);

}
