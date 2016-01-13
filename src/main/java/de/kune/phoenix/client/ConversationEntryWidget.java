package de.kune.phoenix.client;

import java.util.Arrays;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Widget;

public class ConversationEntryWidget extends Composite implements HasClickHandlers {
	interface ConversationEntryUiBinder extends UiBinder<Widget, ConversationEntryWidget> {
	}

	private static ConversationEntryUiBinder uiBinder = GWT.create(ConversationEntryUiBinder.class);
	private final String conversationId;

	@UiField
	InlineHTML title;

	@UiField
	InlineHTML unreadMessagesBadge;

	@UiField
	InlineHTML closeConversationClickArea;

	// @UiHandler("closeConversation")
	// void handleCloseConversationClick(ClickHandler handler) {
	// CloseEvent.fire(this, this);
	// }

	public ConversationEntryWidget(String conversationId) {
		this.conversationId = conversationId;
		initWidget(uiBinder.createAndBindUi(this));
		setUnreadMessageCount(0);
	}

	public String getConversationId() {
		return conversationId;
	}

	@Override
	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return addDomHandler(handler, ClickEvent.getType());
	}

	public void activate() {
		addStyleName("active");
		setUnreadMessageCount(0);
	}

	public void deactivate() {
		removeStyleName("active");
	}

	public void setTitle(String title) {
		this.title.setText(title);
	}

	public int getUnreadMessageCount() {
		if (unreadMessagesBadge.isVisible()) {
			return Integer.parseInt(unreadMessagesBadge.getText());
		} else {
			return 0;
		}
	}

	public void incrementUnreadMessageCount() {
		setUnreadMessageCount(getUnreadMessageCount() + 1);
	}

	public void setUnreadMessageCount(int unreadMessageCount) {
		unreadMessagesBadge.setVisible(unreadMessageCount > 0);
		unreadMessagesBadge.setText(Integer.toString(unreadMessageCount));
	}

	public HandlerRegistration addCloseClickHandler(ClickHandler handler) {
		return closeConversationClickArea.addDomHandler(handler, ClickEvent.getType());
	}

	public boolean isActive() {
		return Arrays.asList(getStyleName().split(" ")).contains("active");
	}

}
