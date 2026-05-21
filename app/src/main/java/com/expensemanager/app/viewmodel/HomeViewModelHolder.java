package com.expensemanager.app.viewmodel;

/**
 * Cung cấp HomeViewModel singleton — tránh ViewModelProvider khi lifecycle lệch phiên bản.
 */
public final class HomeViewModelHolder {
    private static HomeViewModel instance;

    private HomeViewModelHolder() {}

    public static synchronized HomeViewModel get() {
        if (instance == null) {
            instance = new HomeViewModel();
        }
        return instance;
    }

    public static synchronized void clear() {
        instance = null;
    }
}
