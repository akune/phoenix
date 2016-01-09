package de.kune.phoenix.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import de.kune.phoenix.client.functional.SearchHandler;
import de.kune.phoenix.client.functional.SendMessageHandler;

public class ChatClientWidget extends Composite {
	interface MyUiBinder extends UiBinder<Widget, ChatClientWidget> {
	}

	private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

	@UiField
	HTMLPanel newConversationPanel;

	@UiField
	HTMLPanel conversationsPanel;

	@UiField
	TextBox searchTextBox;

	@UiField
	HTMLPanel chatClientContainer;

	@UiField
	HTMLPanel informationPanel;

	@UiField
	HTMLPanel informationBodyPanel;

	@UiField
	Button searchButton;

	@UiField
	HTMLPanel conversationEntriesPanel;

	private SearchHandler searchHandler;

	private SendMessageHandler sendMessageHandler;

	@UiHandler("cancelCreateConversation")
	void handleCancelCreateConversationClick(ClickEvent evt) {
		newConversationPanel.addStyleName("hidden");
		conversationsPanel.removeStyleName("hidden");
		searchTextBox.setFocus(false);
		searchTextBox.setValue("");
		updateSearchButtonEnabledState();
	}

	@UiHandler("createConversation")
	void handleCreateConversationClick(ClickEvent evt) {
		newConversationPanel.removeStyleName("hidden");
		conversationsPanel.addStyleName("hidden");
		searchTextBox.setFocus(true);
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

	void sendMessage(String conversationId, String message) {
		if (sendMessageHandler != null) {
			sendMessageHandler.sendMessage(conversationId, message);
		}
	}

	public void addReceivedMessage(String conversationId, String message) {
		ConversationWidget conversationWidget = getConversationWidget(conversationId);
		if (conversationWidget != null) {
			getConversationWidget(conversationId).addReceivedMessage(message);
		}
		ConversationEntryWidget entry = getConversationEntryWidget(conversationId);
		if (entry != null && !entry.isActive()) {
			entry.incrementUnreadMessageCount();
		}
	}

	public void addSentMessage(String conversationId, String message) {
		getConversationWidget(conversationId).addSentMessage(message);
	}

	public void addConversation(String conversationId, String title) {
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
	}

	protected void closeConversation(String conversationId) {
		getConversationWidget(conversationId).removeFromParent();
		getConversationEntryWidget(conversationId).removeFromParent();
		activateConversation(null);
	}

	public ChatClientWidget() {
		initWidget(uiBinder.createAndBindUi(this));
		searchTextBox.getElement().setAttribute("placeholder", "Name or hash");

		// addConversation("new-conversation", "Unknown participant");
		// addConversation("another-conversation", "Another unknown
		// participant");
		// String[] m = new String[] { "Ahoy!", "Hello there...", "What are you
		// doing?", "Are you okay?",
		// "My head feels funny.", "I can't see!" };
		// Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand()
		// {
		// @Override
		// public boolean execute() {
		// addReceivedMessage("new-conversation", m[(int) (Math.random() *
		// m.length)]);
		// return true;
		// }
		// }, 1500);
		// Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand()
		// {
		// @Override
		// public boolean execute() {
		// addReceivedMessage("another-conversation", m[(int) (Math.random() *
		// m.length)]);
		// return true;
		// }
		// }, 2500);
	}

	public void addInformationEntry(String text) {
		HTMLPanel panel = new HTMLPanel(text);
		panel.addStyleName("alert");
		panel.addStyleName("disabled");
		informationBodyPanel.add(panel);
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
			informationPanel.addStyleName("active");
		} else {
			informationPanel.removeStyleName("active");
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

}
