package com.expensemanager.app.ui.budget;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.DialogBudgetSettingsBinding;

public class BudgetSettingsDialog extends Dialog {

    private final DialogBudgetSettingsBinding binding;
    private final SharedPreferences prefs;

    private String selectedLanguage = "Tiếng Việt";
    private String selectedCurrency = "vnd";
    private String selectedDecimal = "0";
    private String selectedSymbolPosition = "Sau số tiền";

    public BudgetSettingsDialog(@NonNull Context context, String uid) {
        super(context);
        this.binding = DialogBudgetSettingsBinding.inflate(LayoutInflater.from(context));
        // Lưu theo uid để mỗi user có cấu hình riêng
        this.prefs = context.getSharedPreferences("budget_settings_" + uid, Context.MODE_PRIVATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(binding.getRoot());

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        loadSavedSettings();
        setupClickListeners();
    }

    private void loadSavedSettings() {
        selectedLanguage = prefs.getString("language", "Tiếng Việt");
        selectedCurrency = prefs.getString("currency", "vnd");
        selectedDecimal = prefs.getString("decimalPlaces", "0");
        selectedSymbolPosition = prefs.getString("symbolPosition", "Sau số tiền");

        binding.textLanguageValue.setText(selectedLanguage);
        binding.textCurrencyValue.setText(selectedCurrency);
        binding.textDecimalValue.setText(selectedDecimal);
        binding.textSymbolPositionValue.setText(selectedSymbolPosition);
    }

    private void setupClickListeners() {
        binding.textLanguageValue.setOnClickListener(v -> showLanguagePicker());
        binding.textCurrencyValue.setOnClickListener(v -> showCurrencyPicker());
        binding.textDecimalValue.setOnClickListener(v -> showDecimalPicker());
        binding.textSymbolPositionValue.setOnClickListener(v -> showSymbolPositionPicker());

        binding.btnSave.setOnClickListener(v -> saveSettings());
        binding.btnCancel.setOnClickListener(v -> dismiss());
    }

    private void showLanguagePicker() {
        String[] items = {getContext().getString(R.string.language_vietnamese)};
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(getContext().getString(R.string.language))
                .setItems(items, (d, which) -> {
                    selectedLanguage = items[which];
                    binding.textLanguageValue.setText(selectedLanguage);
                })
                .show();
    }

    private void showCurrencyPicker() {
        String[] items = {"vnd"};
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(getContext().getString(R.string.j1_currency_unit))
                .setItems(items, (d, which) -> {
                    selectedCurrency = items[which];
                    binding.textCurrencyValue.setText(selectedCurrency);
                })
                .show();
    }

    private void showDecimalPicker() {
        String[] items = {"0", "1", "2"};
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(getContext().getString(R.string.j1_decimal_places))
                .setItems(items, (d, which) -> {
                    selectedDecimal = items[which];
                    binding.textDecimalValue.setText(selectedDecimal);
                })
                .show();
    }

    private void showSymbolPositionPicker() {
        String[] items = {getContext().getString(R.string.j1_after_amount)};
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(getContext().getString(R.string.j1_symbol_position))
                .setItems(items, (d, which) -> {
                    selectedSymbolPosition = items[which];
                    binding.textSymbolPositionValue.setText(selectedSymbolPosition);
                })
                .show();
    }

    private void saveSettings() {
        // Cài đặt chỉ lưu vào SharedPreferences (không cần Firestore).
        // Trước đây code có lưu thêm lên Firestore gây thừa và vi phạm kiến trúc.
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("language", selectedLanguage);
        editor.putString("currency", selectedCurrency);
        editor.putString("decimalPlaces", selectedDecimal);
        editor.putString("symbolPosition", selectedSymbolPosition);
        editor.apply();

        Toast.makeText(getContext(), getContext().getString(R.string.j1_settings_saved), Toast.LENGTH_SHORT).show();
        dismiss();
    }
}
