package de.kune.phoenix.client.functional;

@FunctionalInterface
public interface Predicate<T> {

	static <T> Predicate<T> always() {
		return t -> true;
	}

	static <T> Predicate<T> never() {
		return t -> false;
	}

	default Predicate<T> and(Predicate<? super T> p) {
		return new Predicate<T>() {
			public boolean test(T m) {
				return Predicate.this.test(m) && p.test(m);
			}
		};
	}

	default Predicate<T> or(Predicate<? super T> p) {
		return new Predicate<T>() {
			@Override
			public boolean test(T m) {
				return Predicate.this.test(m) || p.test(m);
			}
		};
	}

	boolean test(T t);

}
