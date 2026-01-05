package io.github.albertus82.git.gui.preference;

import static io.github.albertus82.git.gui.preference.PageDefinition.GENERAL;

import java.awt.SystemTray;
import java.net.Proxy;
import java.time.ZoneId;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.widgets.Composite;

import io.github.albertus82.git.config.LanguageConfigAccessor;
import io.github.albertus82.git.gui.CloseDialog;
import io.github.albertus82.git.gui.GitAutoSyncGui;
import io.github.albertus82.git.gui.TrayIcon;
import io.github.albertus82.git.resources.Language;
import io.github.albertus82.git.resources.Messages;
import io.github.albertus82.jface.preference.FieldEditorDetails;
import io.github.albertus82.jface.preference.FieldEditorDetails.FieldEditorDetailsBuilder;
import io.github.albertus82.jface.preference.FieldEditorFactory;
import io.github.albertus82.jface.preference.IPreference;
import io.github.albertus82.jface.preference.LocalizedLabelsAndValues;
import io.github.albertus82.jface.preference.PreferenceDetails;
import io.github.albertus82.jface.preference.PreferenceDetails.PreferenceDetailsBuilder;
import io.github.albertus82.jface.preference.StaticLabelsAndValues;
import io.github.albertus82.jface.preference.field.DefaultBooleanFieldEditor;
import io.github.albertus82.jface.preference.field.DefaultComboFieldEditor;
import io.github.albertus82.jface.preference.field.EnhancedStringFieldEditor;
import io.github.albertus82.jface.preference.page.IPageDefinition;

public enum Preference implements IPreference {

	LANGUAGE(new PreferenceDetailsBuilder(GENERAL).defaultValue(LanguageConfigAccessor.DEFAULT_LANGUAGE).build(), new FieldEditorDetailsBuilder(DefaultComboFieldEditor.class).labelsAndValues(Preference.getLanguageComboOptions()).build()),

	CLIENT_ID(new PreferenceDetailsBuilder(GENERAL).build(), new FieldEditorDetailsBuilder(EnhancedStringFieldEditor.class).emptyStringAllowed(false).textLimit(32).build()),
	START_MINIMIZED(new PreferenceDetailsBuilder(GENERAL).defaultValue(GitAutoSyncGui.Defaults.START_MINIMIZED).separate().build(), new FieldEditorDetailsBuilder(DefaultBooleanFieldEditor.class).build()),
	MINIMIZE_TRAY(new PreferenceDetailsBuilder(GENERAL).defaultValue(TrayIcon.Defaults.MINIMIZE_TRAY).build(), new FieldEditorDetailsBuilder(DefaultBooleanFieldEditor.class).disabled(!SystemTray.isSupported()).build()),
	CONFIRM_CLOSE(new PreferenceDetailsBuilder(GENERAL).defaultValue(CloseDialog.Defaults.CONFIRM_CLOSE).build(), new FieldEditorDetailsBuilder(DefaultBooleanFieldEditor.class).build()),
	SYNC_ON_START(new PreferenceDetailsBuilder(GENERAL).defaultValue(GitAutoSyncGui.Defaults.SYNC_ON_START).build(), new FieldEditorDetailsBuilder(DefaultBooleanFieldEditor.class).build());

	//	LOGGING_LEVEL(new PreferenceDetailsBuilder(LOGGING).defaultValue(ApplicationConfig.Defaults.LOGGING_LEVEL.getName()).build(), new FieldEditorDetailsBuilder(DefaultComboFieldEditor.class).labelsAndValues(LoggingPreferencePage.getLoggingLevelComboOptions()).build()),
	//	LOGGING_FILES_ENABLED(new PreferenceDetailsBuilder(LOGGING).separate().defaultValue(ApplicationConfig.Defaults.LOGGING_FILES_ENABLED).build(), new FieldEditorDetailsBuilder(DefaultBooleanFieldEditor.class).build()),
	//	LOGGING_FILES_PATH(new PreferenceDetailsBuilder(LOGGING).parent(LOGGING_FILES_ENABLED).defaultValue(ApplicationConfig.Defaults.LOGGING_FILES_PATH).build(), new FieldEditorDetailsBuilder(EnhancedDirectoryFieldEditor.class).emptyStringAllowed(false).directoryMustExist(false).directoryDialogMessage(() -> Messages.INSTANCE.get("msg.preferences.directory.dialog.message.log")).build()),
	//	LOGGING_FILES_AUTOCLEAN_ENABLED(new PreferenceDetailsBuilder(LOGGING).parent(LOGGING_FILES_ENABLED).defaultValue(ApplicationConfig.Defaults.LOGGING_FILES_AUTOCLEAN_ENABLED).build(), new FieldEditorDetailsBuilder(DefaultBooleanFieldEditor.class).build()),
	//	LOGGING_FILES_AUTOCLEAN_KEEP(new PreferenceDetailsBuilder(LOGGING).parent(LOGGING_FILES_AUTOCLEAN_ENABLED).defaultValue(ApplicationConfig.Defaults.LOGGING_FILES_AUTOCLEAN_KEEP).build(), new FieldEditorDetailsBuilder(ShortFieldEditor.class).numberMinimum(HousekeepingFilter.MIN_HISTORY).build());

	private static final String LABEL_KEY_PREFIX = "label.preferences.";

	private static final FieldEditorFactory fieldEditorFactory = new FieldEditorFactory();

	private final PreferenceDetails preferenceDetails;
	private final FieldEditorDetails fieldEditorDetails;

	Preference(final PreferenceDetails preferenceDetails, final FieldEditorDetails fieldEditorDetails) {
		this.preferenceDetails = preferenceDetails;
		this.fieldEditorDetails = fieldEditorDetails;
		if (preferenceDetails.getName() == null) {
			preferenceDetails.setName(name().toLowerCase(Locale.ROOT).replace('_', '.'));
		}
		if (preferenceDetails.getLabel() == null) {
			preferenceDetails.setLabel(() -> Messages.INSTANCE.get(LABEL_KEY_PREFIX + preferenceDetails.getName()));
		}
	}

	@Override
	public String getName() {
		return preferenceDetails.getName();
	}

	@Override
	public String getLabel() {
		return preferenceDetails.getLabel().get();
	}

	@Override
	public IPageDefinition getPageDefinition() {
		return preferenceDetails.getPageDefinition();
	}

	@Override
	public String getDefaultValue() {
		return preferenceDetails.getDefaultValue();
	}

	@Override
	public IPreference getParent() {
		return preferenceDetails.getParent();
	}

	@Override
	public boolean isRestartRequired() {
		return preferenceDetails.isRestartRequired();
	}

	@Override
	public boolean isSeparate() {
		return preferenceDetails.isSeparate();
	}

	@Override
	public Preference[] getChildren() {
		final Set<Preference> preferences = EnumSet.noneOf(Preference.class);
		for (final Preference item : Preference.values()) {
			if (this.equals(item.getParent())) {
				preferences.add(item);
			}
		}
		return preferences.toArray(new Preference[] {});
	}

	@Override
	public FieldEditor createFieldEditor(final Composite parent) {
		return fieldEditorFactory.createFieldEditor(getName(), getLabel(), parent, fieldEditorDetails);
	}

	public static LocalizedLabelsAndValues getLanguageComboOptions() {
		final Language[] values = Language.values();
		final LocalizedLabelsAndValues options = new LocalizedLabelsAndValues(values.length);
		for (final Language language : values) {
			final Locale locale = language.getLocale();
			final String value = locale.getLanguage();
			options.add(() -> locale.getDisplayLanguage(locale), value);
		}
		return options;
	}

	public static StaticLabelsAndValues getTimeZoneComboOptions() {
		final Collection<String> zones = new TreeSet<>(ZoneId.getAvailableZoneIds());
		final StaticLabelsAndValues options = new StaticLabelsAndValues(zones.size());
		for (final String zone : zones) {
			options.put(zone, zone);
		}
		return options;
	}

	public static StaticLabelsAndValues getProxyTypeComboOptions() {
		final Proxy.Type[] types = Proxy.Type.values();
		final StaticLabelsAndValues options = new StaticLabelsAndValues(types.length - 1);
		for (final Proxy.Type type : types) {
			if (!Proxy.Type.DIRECT.equals(type)) {
				options.put(type.toString(), type.name());
			}
		}
		return options;
	}

}
