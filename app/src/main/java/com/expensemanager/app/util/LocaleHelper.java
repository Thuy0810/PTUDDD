package com.expensemanager.app.util;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * Quản lý ngôn ngữ ứng dụng (per-app locale) qua AppCompat.
 *
 * <p>Dùng {@link AppCompatDelegate#setApplicationLocales} nên hệ thống tự lưu
 * lựa chọn (đã khai báo service {@code AppLocalesMetadataHolderService} với
 * {@code autoStoreLocales=true} trong manifest) và tự khởi tạo lại các Activity.
 */
public final class LocaleHelper {
    public static final String VIETNAMESE = "vi";
    public static final String ENGLISH = "en";

    private LocaleHelper() {}

    /** Áp dụng ngôn ngữ theo mã ("vi" hoặc "en"). */
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
