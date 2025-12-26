package io.github.albertus82.git.gui;

import org.eclipse.swt.widgets.Control;

import io.github.albertus82.git.resources.Messages;
import io.github.albertus82.jface.preference.page.BasePreferencePage;
import io.github.albertus82.util.logging.LoggingSupport;

 
public class LoggingPreferencePage extends BasePreferencePage {

	private String overriddenMessage =   Messages.get("label.preferences.logging.overridden");

	@Override
	protected Control createHeader() {
		if (LoggingSupport.getInitialConfigurationProperty() != null) {
			return createInfoComposite(getFieldEditorParent(), overriddenMessage);
		}
		else {
			return null;
		}
	}

	public String getOverriddenMessage() {
		return overriddenMessage;
	}

	public void setOverriddenMessage(String overriddenMessage) {
		this.overriddenMessage = overriddenMessage;
	}

}
