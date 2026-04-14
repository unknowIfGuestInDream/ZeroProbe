package com.tlcsdm.fxtemplate.model;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * A wrapper class for Locale that displays the language name in its native form.
 * For example: English, 中文, 日本語
 */
public class DisplayLocale {

    private static final Map<String, String> DISPLAY_NAMES = Map.of(
        "en", "English",
        "zh", "\u4e2d\u6587",
        "ja", "\u65e5\u672c\u8a9e"
    );

    private final Locale locale;

    public DisplayLocale(Locale locale) {
        this.locale = Objects.requireNonNull(locale, "Locale cannot be null");
    }

    /**
     * Get the wrapped Locale.
     *
     * @return the Locale
     */
    public Locale getLocale() {
        return locale;
    }

    @Override
    public String toString() {
        return DISPLAY_NAMES.getOrDefault(locale.getLanguage(), locale.getDisplayLanguage(locale));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        DisplayLocale other = (DisplayLocale) obj;
        return locale.getLanguage().equals(other.locale.getLanguage());
    }

    @Override
    public int hashCode() {
        return locale.getLanguage().hashCode();
    }
}
