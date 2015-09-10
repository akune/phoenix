package de.kune.phoenix.shared;

import java.util.Collection;

public class Subscriber implements Identifiable<String> {

	private String id;
	private Collection<Subscription> subscriptions;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Collection<Subscription> getSubscriptions() {
		return subscriptions;
	}

	public void setSubscriptions(Collection<Subscription> subscriptions) {
		this.subscriptions = subscriptions;
	}

}
