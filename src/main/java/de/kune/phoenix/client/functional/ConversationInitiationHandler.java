package de.kune.phoenix.client.functional;

import de.kune.phoenix.client.messaging.Conversation;

@FunctionalInterface
public interface ConversationInitiationHandler {

	void handle(Conversation.Builder conversation);
	
}
