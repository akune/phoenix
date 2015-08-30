package de.kune.phoenix.client.messaging;

import de.kune.phoenix.shared.Message;

public interface MessageCallback {

	void handleReceivedMessage(Message message, byte[] content);
	
}
