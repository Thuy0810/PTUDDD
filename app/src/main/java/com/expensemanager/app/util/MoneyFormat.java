package com.expensemanager.app.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class MoneyFormat {
    private static final NumberFormat FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));

    private MoneyFormat() {}

    public static String format(double amount) {
        return FORMAT.format(amount) + " đ";
    }
}
