package de.kune.phoenix.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import de.kune.phoenix.client.crypto.KeyPair;
import de.kune.phoenix.client.functional.SearchHandler;
import de.kune.phoenix.client.functional.SendMessageHandler;
import de.kune.phoenix.shared.Message;

public class ChatClientWidget extends Composite {
	interface ChatClientUiBinder extends UiBinder<Widget, ChatClientWidget> {
	}

	private static ChatClientUiBinder uiBinder = GWT.create(ChatClientUiBinder.class);

	@UiField
	HTMLPanel newConversationPanel;

	@UiField
	HTMLPanel conversationsPanel;

	@UiField
	TextBox searchTextBox;

	@UiField
	HTMLPanel chatClientContainer;

	@UiField
	PreferencesWidget preferencesPanel;

	@UiField
	Button searchButton;

	@UiField
	HTMLPanel conversationEntriesPanel;
	
	@UiField
	HTMLPanel connectionLostPanel;

	private SearchHandler searchHandler;

	private SendMessageHandler sendMessageHandler;

	@UiHandler("showInfoClickArea")
	void handleShowInfoClick(ClickEvent evt) {
		activateConversation(null);
	}
	
	@UiHandler("cancelCreateConversationClickArea")
	void handleCancelCreateConversationClick(ClickEvent evt) {
		closeSearchPanel();
	}
	
	public void closeSearchPanel() {
		Animations.fadeOut(newConversationPanel).run(150);
		Animations.fadeIn(conversationsPanel).run(150);
		searchTextBox.setFocus(false);
		searchTextBox.setValue("");
		updateSearchButtonEnabledState();
	}

	@UiHandler("createConversationClickArea")
	void handleCreateConversationClick(ClickEvent evt) {
		Animations.fadeIn(newConversationPanel).run(150);
		Animations.fadeOut(conversationsPanel).run(150);
		new Timer() {
			@Override
			public void run() {
				searchTextBox.setFocus(true);
			}
		}.schedule(100);;
	}

	@UiHandler("searchTextBox")
	void handleSearchTextBoxKeyUp(KeyUpEvent evt) {
		updateSearchButtonEnabledState();
		if (evt.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
			performSearch();
		}
	}

	void updateSearchButtonEnabledState() {
		if (searchTextBox.getValue().isEmpty()) {
			searchButton.addStyleName("disabled");
		} else {
			searchButton.removeStyleName("disabled");
		}
	}

	@UiHandler("searchButton")
	void searchButtonClick(ClickEvent evt) {
		performSearch();
	}

	void performSearch() {
		String searchString = searchTextBox.getValue();
		if (!searchString.isEmpty()) {
			GWT.log("searching...");
		}
		if (searchHandler != null) {
			searchHandler.performSearch(searchString);
		}
	}

	Message sendMessage(String conversationId, String message) {
		if (sendMessageHandler != null) {
			return sendMessageHandler.sendMessage(conversationId, message);
		} else {
			return null;
		}
	}

	public void addReceivedMessage(String conversationId, Message message, String plainText) {
		ConversationWidget conversationWidget = getConversationWidget(conversationId);
		if (conversationWidget != null) {
			getConversationWidget(conversationId).addReceivedMessage(message, plainText);
		}
		ConversationEntryWidget entry = getConversationEntryWidget(conversationId);
		if (entry != null && !entry.isActive()) {
			entry.incrementUnreadMessageCount();
		}
	}

	public void addSentMessage(String conversationId, Message message, String plainText) {
		getConversationWidget(conversationId).addSentMessage(message, plainText);
	}

	public ConversationWidget addConversation(String conversationId, String title) {
		ConversationWidget conversation = new ConversationWidget(conversationId);
		conversation.setSendMessageHandler((cId, textMessage) -> sendMessage(conversationId, textMessage));
		conversation.setTitle(title);
		chatClientContainer.add(conversation);
		ConversationEntryWidget entry = new ConversationEntryWidget(conversationId);
		entry.setTitle(title);
		entry.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				activateConversation(conversation.getConversationId());
			}
		});
		entry.addCloseClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				event.stopPropagation();
				closeConversation(conversationId);
				GWT.log("closing " + conversationId);
			}

		});
		conversationEntriesPanel.add(entry);
		return conversation;
	}

	protected void closeConversation(String conversationId) {
		getConversationWidget(conversationId).removeFromParent();
		getConversationEntryWidget(conversationId).removeFromParent();
		activateConversation(null);
	}

	public ChatClientWidget() {
		initWidget(uiBinder.createAndBindUi(this));
		searchTextBox.getElement().setAttribute("placeholder", "Name or hash");
	}

	public void setKeyPair(KeyPair keyPair) {
		preferencesPanel.setKeyPair(keyPair);
	}

	private ConversationEntryWidget getConversationEntryWidget(String conversationId) {
		for (Widget w : conversationEntriesPanel) {
			if (w instanceof ConversationEntryWidget) {
				ConversationEntryWidget cew = (ConversationEntryWidget) w;
				if (cew.getConversationId().equals(conversationId)) {
					return cew;
				}
			}
		}
		return null;
	}

	private ConversationWidget getConversationWidget(String conversationId) {
		for (Widget w : chatClientContainer) {
			if (w instanceof ConversationWidget) {
				ConversationWidget cw = (ConversationWidget) w;
				if (cw.getConversationId().equals(conversationId)) {
					return cw;
				}
			}
		}
		return null;
	}

	public void activateConversation(String conversationId) {
		for (Widget w : conversationEntriesPanel) {
			if (w instanceof ConversationEntryWidget) {
				ConversationEntryWidget cew = (ConversationEntryWidget) w;
				if (cew.getConversationId().equals(conversationId)) {
					cew.activate();
				} else {
					cew.deactivate();
				}

			}
		}
		if (conversationId == null) {
			preferencesPanel.addStyleName("active");
			preferencesPanel.removeStyleName("hide-on-phone");
		} else {
			preferencesPanel.removeStyleName("active");
			preferencesPanel.addStyleName("hide-on-phone");
		}
		for (Widget w : chatClientContainer) {
			if (w instanceof ConversationWidget) {
				ConversationWidget cw = (ConversationWidget) w;
				if (cw.getConversationId().equals(conversationId)) {
					cw.activate();
				} else {
					cw.deactivate();
				}
			}
		}
	}

	public void setSearchHandler(SearchHandler searchHandler) {
		this.searchHandler = searchHandler;
	}

	public void setSendMessageHandler(SendMessageHandler sendMessageHandler) {
		this.sendMessageHandler = sendMessageHandler;

	}

	public void setConnectionState(boolean connected) {
		connectionLostPanel.setVisible(!connected);
	}

}
