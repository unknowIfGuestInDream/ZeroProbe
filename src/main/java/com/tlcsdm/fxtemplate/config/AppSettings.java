package com.tlcsdm.fxtemplate.config;

import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import com.tlcsdm.fxtemplate.model.DisplayLocale;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import java.util.Arrays;
import java.util.List;

/**
 * Application settings management using PreferencesFX.
 */
public class AppSettings {

    private static AppSettings instance;

    private final ObjectProperty<DisplayLocale> languageProperty;
    private final ObjectProperty<AppTheme> themeProperty;

    private PreferencesFx preferencesFx;
    private boolean suppressRebuild;

    private AppSettings() {
        languageProperty = new SimpleObjectProperty<>(new DisplayLocale(I18N.getCurrentLocale()));
        themeProperty = new SimpleObjectProperty<>(AppTheme.getSavedTheme());

        languageProperty.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal) && !suppressRebuild) {
                I18N.setLocale(newVal.getLocale());
                rebuildPreferences();
            }
        });

        themeProperty.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                newVal.apply();
                AppTheme.saveTheme(newVal);
            }
        });
    }

    /**
     * Get the singleton instance.
     */
    public static AppSettings getInstance() {
        if (instance == null) {
            instance = new AppSettings();
        }
        return instance;
    }

    /**
     * Get the language property.
     */
    public ObjectProperty<DisplayLocale> languageProperty() {
        return languageProperty;
    }

    /**
     * Get the theme property.
     */
    public ObjectProperty<AppTheme> themeProperty() {
        return themeProperty;
    }

    /**
     * Create and get the PreferencesFx instance.
     */
    public PreferencesFx getPreferencesFx() {
        if (preferencesFx == null) {
            buildPreferences();
        }
        return preferencesFx;
    }

    private void buildPreferences() {
        DisplayLocale savedLocale = new DisplayLocale(I18N.getCurrentLocale());
        List<DisplayLocale> supportedLocales = Arrays.stream(I18N.getSupportedLocales())
            .map(DisplayLocale::new)
            .toList();
        List<AppTheme> themes = Arrays.asList(AppTheme.values());

        suppressRebuild = true;
        try {
            preferencesFx = PreferencesFx.of(AppSettings.class,
                Category.of(I18N.get("settings.general"),
                    Group.of(I18N.get("settings.languageAndTheme"),
                        Setting.of(I18N.get("settings.language"),
                            FXCollections.observableArrayList(supportedLocales),
                            languageProperty),
                        Setting.of(I18N.get("settings.theme"),
                            FXCollections.observableArrayList(themes),
                            themeProperty)
                    )
                )
            ).persistWindowState(true)
                .saveSettings(true)
                .debugHistoryMode(false)
                .buttonsVisibility(false)
                .instantPersistent(true);

            if (!savedLocale.equals(languageProperty.get())) {
                languageProperty.set(savedLocale);
            }
        } finally {
            suppressRebuild = false;
        }
    }

    private void rebuildPreferences() {
        preferencesFx = null;
    }

    /**
     * Apply initial settings (called at application startup).
     */
    public void applyInitialSettings() {
        AppTheme.applySavedTheme();
    }
}
