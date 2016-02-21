package de.kune.phoenix.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import de.kune.phoenix.client.functional.SendMessageHandler;
import de.kune.phoenix.shared.Message;

public class ConversationWidget extends Composite {
	interface ConversationUiBinder extends UiBinder<Widget, ConversationWidget> {
	}

	private static ConversationUiBinder uiBinder = GWT.create(ConversationUiBinder.class);

	@UiField
	InlineHTML title;

	@UiField
	TextBox messageTextBox;

	@UiField
	Button sendButton;

	@UiField
	HTMLPanel messageAreaPanel;

	private final String conversationId;

	private boolean needsScrollingDown;
	
	private SendMessageHandler sendMessageHandler;
	
	private Map<String, HTMLPanel> messagePanels = new HashMap<>();

	public void setSendMessageHandler(SendMessageHandler sendMessageHandler) {
		this.sendMessageHandler = sendMessageHandler;
	}

	public String getConversationId() {
		return conversationId;
	}

	public void setTitle(String title) {
		this.title.setText(title);
	}

	public ConversationWidget(String conversationId) {
		this.conversationId = conversationId;
		initWidget(uiBinder.createAndBindUi(this));
		messageTextBox.getElement().setAttribute("placeholder", "Message");
	}
	
	@UiHandler("backToConversationsClickArea")
	void handleBackToConversationsClick(ClickEvent evt) {
		addStyleName("hide-on-phone");
	}

	@UiHandler("messageTextBox")
	void handleMessageTextBoxKeyUp(KeyUpEvent evt) {
		updateSendButtonEnabledState();
		if (evt.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
			performSend();
		}
	}

	void updateSendButtonEnabledState() {
		if (messageTextBox.getValue().isEmpty()) {
			sendButton.addStyleName("disabled");
		} else {
			sendButton.removeStyleName("disabled");
		}
	}

	@UiHandler("sendButton")
	void sendButtonClick(ClickEvent evt) {
		performSend();
	}

	void performSend() {
		String messageString = messageTextBox.getValue();
		if (!messageString.isEmpty()) {
			GWT.log("sending: " + messageString);
			messageTextBox.setValue("");
			updateSendButtonEnabledState();
			if (sendMessageHandler != null) {
				Message message = sendMessageHandler.sendMessage(getConversationId(), messageString);
				addSendingMessage(message.getId(), messageString);
			}
		}
	}

	public void activate() {
		addStyleName("active");
		removeStyleName("hide-on-phone");
		Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
			public void execute() {
				messageTextBox.getElement().focus();
				if (needsScrollingDown) {
					scrollDown();
				}
			}
		});
	}

	public void deactivate() {
		removeStyleName("active");
		addStyleName("hide-on-phone");
		messageTextBox.setFocus(false);
	}

	private boolean isScrolledDown() {
		int scrollTop = messageAreaPanel.getElement().getScrollTop();
		int scrollHeight = messageAreaPanel.getElement().getScrollHeight();
		int clientHeight = messageAreaPanel.getElement().getClientHeight();
		return scrollTop + 5 > scrollHeight - clientHeight;
	}

	public boolean isActive() {
		return Arrays.asList(getStyleName().split(" ")).contains("active");
	}

	private void scrollDown() {
		if (!isActive()) {
			needsScrollingDown = true;
		} else {
			needsScrollingDown = false;
		}
		GWT.log("scrolling down");
		messageAreaPanel.getElement().setScrollTop(messageAreaPanel.getElement().getScrollHeight()-messageAreaPanel.getElement().getClientHeight());
	}
	
	private HTMLPanel getMessagePanel(String messageId, String message) {
		HTMLPanel result = messagePanels.get(messageId);
		if (result == null) {
			result = new HTMLPanel("<div class=\"processing\"></div>" + message);
			result.addStyleName("message");
			result.setTitle(messageId);
			messagePanels.put(messageId, result);
		} else {
			result.getElement().setInnerHTML(message);
		}
		return result;
	}

	public void addReceivedMessage(String messageId, String message) {
		HTMLPanel messagePanel = getMessagePanel(messageId, message);
		messagePanel.addStyleName("pull-left");
		boolean scrollDown = isScrolledDown() || !isActive();
		messageAreaPanel.add(messagePanel);
		if (scrollDown) {
			scrollDown();
		}
	}

	public void addSentMessage(String messageId, String message) {
		HTMLPanel messagePanel = getMessagePanel(messageId, message);
		messagePanel.addStyleName("pull-right");
		boolean scrollDown = isScrolledDown() || !isActive();
		messageAreaPanel.add(messagePanel);
		if (scrollDown) {
			scrollDown();
		}
	}

	public void addSendingMessage(String messageId, String message) {
		HTMLPanel messagePanel = getMessagePanel(messageId, message);
		messagePanel.addStyleName("pull-right");
		messagePanel.addStyleName("pending");
		boolean scrollDown = isScrolledDown() || !isActive();
		messageAreaPanel.add(messagePanel);
		if (scrollDown) {
			scrollDown();
		}
	}
	
}
