package com.expensemanager.app.util;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

public final class LocaleHelper {
    public static final String VIETNAMESE = "vi";
    public static final String ENGLISH = "en";

    private LocaleHelper() {}

    public static void setLanguage(String languageTag) {
        if (languageTag == null || languageTag.trim().isEmpty()) {
            languageTag = VIETNAMESE;
        }
        AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageTag));
    }

    /** Mã ngôn ngữ đang dùng; mặc định "vi" nếu chưa đặt. */
    public static String getLanguage() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) {
            return VIETNAMESE;
        }
        Locale locale = locales.get(0);
        return locale != null ? locale.getLanguage() : VIETNAMESE;
    }

    /** True nếu đang dùng tiếng Anh. */
    public static boolean isEnglish() {
        return ENGLISH.equals(getLanguage());
    }
}
