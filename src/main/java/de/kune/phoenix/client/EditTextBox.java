package de.kune.phoenix.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class EditTextBox extends Composite implements HasHandlers {

	interface EditTextFieldUiBinder extends UiBinder<Widget, EditTextBox> {
	}

	private static EditTextFieldUiBinder uiBinder = GWT.create(EditTextFieldUiBinder.class);

	@UiField
	TextBox textBox;

	@UiField
	Button editButton;

	@UiField
	Button cancelEditButton;

	@UiField
	Button saveButton;

	@UiField
	HTMLPanel buttonGroupPanel;

	private String text = "";

	public EditTextBox() {
		initWidget(uiBinder.createAndBindUi(this));
		cancelEditButton.removeFromParent();
		saveButton.removeFromParent();
	}

	@UiHandler("saveButton")
	public void saveButtonClick(ClickEvent evt) {
		save();
	}

	private void save() {
		if (!getText().equals(textBox.getText())) {
			setText(textBox.getText());
			invokeUpdateHandlers();
		}
		cancelEdit();
	}

	private void invokeUpdateHandlers() {
		fireEvent(new UpdateEvent(this));
	}

	@UiHandler("textBox")
	public void textBoxUpDown(KeyDownEvent evt) {
		if (evt.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
			save();
		} else if (evt.getNativeKeyCode() == KeyCodes.KEY_ESCAPE) {
			cancelEdit();
		}
	}

	@UiHandler("cancelEditButton")
	public void cancelEditButtonClick(ClickEvent evt) {
		cancelEdit();
	}

	private void cancelEdit() {
		setText(text);
		saveButton.removeFromParent();
		cancelEditButton.removeFromParent();
		buttonGroupPanel.add(editButton);
		textBox.setEnabled(false);
	}

	@UiHandler("editButton")
	public void editButtonClick(ClickEvent evt) {
		edit();
	}

	public void edit() {
		buttonGroupPanel.add(saveButton);
		buttonGroupPanel.add(cancelEditButton);
		editButton.removeFromParent();
		textBox.setEnabled(true);
		textBox.selectAll();
		textBox.setFocus(true);
	}

	public void setText(String text) {
		this.text = text;
		textBox.setText(text);
	}

	public String getText() {
		return text;
	}

	public void setPlaceholder(String placeholder) {
		textBox.getElement().setAttribute("placeholder", placeholder);
	}

	public HandlerRegistration addUpdateHandler(UpdateHandler updateHandler) {
		return addHandler(updateHandler, UpdateEvent.TYPE);
	}

}
