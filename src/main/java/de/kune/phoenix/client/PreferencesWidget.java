package de.kune.phoenix.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import de.kune.phoenix.client.crypto.KeyPair;

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

	public void setKeyPair(KeyPair keyPair) {
		 HTMLPanel panel = new HTMLPanel(keyPair.getPublicKey().getEncodedKey());
		 panel.addStyleName("alert");
		 panel.addStyleName("disabled");
		 informationBodyPanel.add(panel);
		 
		 Anchor publicKeyDownload = new Anchor("Download public key");
		 publicKeyDownload.setHref("data:application/octet-stream;charset=utf-8;base64;headers=Content-Disposition%3A%20attachment%3Bfilename%3Dkey.pub," + keyPair.getPublicKey().getEncodedKey());
		 publicKeyDownload.getElement().setAttribute("download", "key.pub");
		 SimplePanel panel2 = new SimplePanel(publicKeyDownload);
		 Anchor privateKeyDownload = new Anchor("Download private key");
		 privateKeyDownload.setHref("data:application/octet-stream;charset=utf-8;base64;headers=Content-Disposition%3A%20attachment%3Bfilename%3Dkey.priv," + keyPair.getPrivateKey().getEncodedKey());
		 privateKeyDownload.getElement().setAttribute("download", "key.priv");
		 SimplePanel panel3 = new SimplePanel(privateKeyDownload);
		 panel2.addStyleName("alert");
		 panel2.addStyleName("disabled");
		 informationBodyPanel.add(panel2);
		 panel3.addStyleName("alert");
		 panel3.addStyleName("disabled");
		 informationBodyPanel.add(panel3);
	}

}
