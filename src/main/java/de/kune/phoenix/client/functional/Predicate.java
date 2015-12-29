package de.kune.phoenix.client.functional;

import java.util.Collection;

import de.kune.phoenix.shared.Message;

@FunctionalInterface
public interface Predicate<T> {

	static <T> Predicate<T> always() {
		return t -> true;
	}

	static <T> Predicate<T> never() {
		return t -> false;
	}

	static Predicate<Message> hasType(Message.Type type) {
		return m -> m.getMessageType() == type;
	}

	static Predicate<Message> hasConversationId(String conversationId) {
		return m -> conversationId.equals(m.getConversationId());
	}

	static Predicate<Message> containsSender(Collection<String> participants) {
		return m -> participants.contains(m.getSenderId());
	}

	default Predicate<T> and(Predicate<? super T> p) {
		return new Predicate<T>() {
			public boolean test(T m) {
				return Predicate.this.test(m) && p.test(m);
			}
		};
	}

	boolean test(T t);

}
