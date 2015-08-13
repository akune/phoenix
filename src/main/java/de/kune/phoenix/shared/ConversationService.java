package de.kune.phoenix.shared;

import java.util.Date;
import java.util.List;

import de.kune.phoenix.client.crypto.KeyPair;

public interface ConversationService {
	
	public static interface Participant {
		String getId();
		String getScreenName();
		byte[] getPublicKey();
	}
	
	public static interface Message {
		public Participant getSender();
		public Date getTimestamp(); 
		public String getContent();
	}
	
	public static interface Conversation {
		String getId();
		List<Participant> getParticipants();
		List<Message> getAllMessages();
		void sendMessage(String message);
		List<Message> receiveNewMessages();
	}
	
	void login(KeyPair keyPair);
	List<Conversation> getConversations();
	
}
