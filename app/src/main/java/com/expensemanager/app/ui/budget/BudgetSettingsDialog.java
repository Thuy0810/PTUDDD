package com.expensemanager.app.ui.budget;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.DialogBudgetSettingsBinding;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class BudgetSettingsDialog extends Dialog {

    private final DialogBudgetSettingsBinding binding;
    private final String uid;

    private String selectedLanguage = "Tiếng Việt";
    private String selectedCurrency = "VND (₫)";
    private String selectedDecimal = "0";
    private String selectedSymbolPosition = "Sau số tiền";

    public BudgetSettingsDialog(@NonNull Context context, String uid) {
        super(context);
        this.uid = uid;
        this.binding = DialogBudgetSettingsBinding.inflate(LayoutInflater.from(context));
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

        binding.textLanguageValue.setText(selectedLanguage);
        binding.textCurrencyValue.setText(selectedCurrency);
        binding.textDecimalValue.setText(selectedDecimal);
        binding.textSymbolPositionValue.setText(selectedSymbolPosition);

        binding.textLanguageValue.setOnClickListener(v -> showLanguagePicker());
        binding.textCurrencyValue.setOnClickListener(v -> showCurrencyPicker());
        binding.textDecimalValue.setOnClickListener(v -> showDecimalPicker());
        binding.textSymbolPositionValue.setOnClickListener(v -> showSymbolPositionPicker());

        binding.btnSave.setOnClickListener(v -> saveSettings());
    }

    private void showLanguagePicker() {
        String[] items = {"Tiếng Việt", "English"};
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Ngôn ngữ")
                .setItems(items, (d, which) -> {
                    selectedLanguage = items[which];
                    binding.textLanguageValue.setText(selectedLanguage);
                })
                .show();
    }

    private void showCurrencyPicker() {
        String[] items = {"VND (₫)", "USD ($)", "EUR (€)", "JPY (¥)"};
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Đơn vị tiền tệ")
                .setItems(items, (d, which) -> {
                    selectedCurrency = items[which];
                    binding.textCurrencyValue.setText(selectedCurrency);
                })
                .show();
    }

    private void showDecimalPicker() {
        String[] items = {"0", "1", "2"};
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Số thập phân")
                .setItems(items, (d, which) -> {
                    selectedDecimal = items[which];
                    binding.textDecimalValue.setText(selectedDecimal);
                })
                .show();
    }

    private void showSymbolPositionPicker() {
        String[] items = {"Trước số tiền", "Sau số tiền"};
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Vị trí ký hiệu tiền")
                .setItems(items, (d, which) -> {
                    selectedSymbolPosition = items[which];
                    binding.textSymbolPositionValue.setText(selectedSymbolPosition);
                })
                .show();
    }

    private void saveSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("language", selectedLanguage);
        settings.put("currency", selectedCurrency);
        settings.put("decimalPlaces", selectedDecimal);
        settings.put("symbolPosition", selectedSymbolPosition);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid)
                .collection("settings").document("budget")
                .set(settings)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Đã lưu cài đặt", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lưu thất bại", Toast.LENGTH_SHORT).show()
                );
    }
}
