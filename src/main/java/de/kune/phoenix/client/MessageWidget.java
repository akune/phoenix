package de.kune.phoenix.client;

import static de.kune.phoenix.client.MessageWidget.Type.RECEIVED;
import static de.kune.phoenix.client.MessageWidget.Type.SENT;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

import de.kune.phoenix.shared.Message;

public class MessageWidget extends Composite {

	public static enum Type {
		SENT, RECEIVED;
	}

	interface MessageUiBinder extends UiBinder<Widget, MessageWidget> {
	}

	private static MessageUiBinder uiBinder = GWT.create(MessageUiBinder.class);

	@UiField
	protected HTMLPanel contentAreaPanel;

	@UiField
	protected HTMLPanel statusAreaPanel;

	private Type type;
	
	public MessageWidget() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	public void setType(Type type) {
		this.type = type;
		if (type == SENT) {
			addStyleName("pull-right");
		} else if (type == RECEIVED) {
			addStyleName("pull-left");
		}
	}

	public void setContent(Message message, String plainText) {
		contentAreaPanel.setTitle(message.toString());
		contentAreaPanel.getElement().setInnerText(plainText);
	}
	
	public void setStatus(String status) {
		statusAreaPanel.getElement().setInnerText(status);
	}

	public String getStatus() {
		return statusAreaPanel.getElement().getInnerText();
	}

	public Type getType() {
		return type;
	}

}
