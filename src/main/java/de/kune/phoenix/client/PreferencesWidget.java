package de.kune.phoenix.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

import de.kune.phoenix.client.crypto.PublicKey;

public class PreferencesWidget extends Composite {

	interface PreferencesUiBinder extends UiBinder<Widget, PreferencesWidget> {
	}

	private static PreferencesUiBinder uiBinder = GWT.create(PreferencesUiBinder.class);

	@UiField
	HTMLPanel informationBodyPanel;

	public PreferencesWidget() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	@UiHandler("backToConversationsClickArea")
	void handleBackToConversationsClick(ClickEvent evt) {
		addStyleName("hide-on-phone");
	}

	public void setPublicKey(PublicKey publicKey) {
		 HTMLPanel panel = new HTMLPanel(publicKey.getEncodedKey());
		 panel.addStyleName("alert");
		 panel.addStyleName("disabled");
		 informationBodyPanel.add(panel);
	}

}
