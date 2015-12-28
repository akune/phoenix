package de.kune.phoenix.client.functional;

@FunctionalInterface
public interface SuccessHandler<T> {
	
	static <T> SuccessHandler<T> nothing() {
		return new SuccessHandler<T>() {
			@Override
			public void handle(T payload) {
			}
		};
	}

	void handle(T payload);
	
}
