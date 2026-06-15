package com.expensemanager.app.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.databinding.BottomSheetTransactionDetailBinding;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class TransactionDetailBottomSheet extends BottomSheetDialogFragment {
    private BottomSheetTransactionDetailBinding binding;
    private Transaction transaction;
    private Map<String, Category> categoryMap;
    private Map<String, Wallet> walletMap;

    public interface Listener {
        void onEditTransaction(String transactionId);
        void onDeleteTransaction(String transactionId);
    }
    private Listener listener;

    public static TransactionDetailBottomSheet newInstance() {
        return new TransactionDetailBottomSheet();
    }

    public TransactionDetailBottomSheet setData(
            Transaction t,
            Map<String, Category> catMap,
            Map<String, Wallet> walletMap) {
        this.transaction = t;
        this.categoryMap = catMap;
        this.walletMap = walletMap;
        return this;
    }

    public TransactionDetailBottomSheet setListener(Listener l) {
        this.listener = l;
        return this;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetTransactionDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindData();
        setupButtons();
    }

    private void bindData() {
        if (transaction == null) return;

        // Amount + color
        String type = transaction.getType();
        String amountStr = MoneyFormat.formatSigned(transaction.getAmount(), type);
        binding.textAmount.setText(amountStr);

        int amountColor;
        if (transaction.isIncome()) {
            amountColor = ContextCompat.getColor(requireContext(), R.color.income_green);
            binding.textType.setText(R.string.transaction_income);
        } else {
            amountColor = ContextCompat.getColor(requireContext(), R.color.expense_red);
            binding.textType.setText(R.string.transaction_expense);
        }
        binding.textAmount.setTextColor(amountColor);

        // Date + time
        Date date = transaction.getDateAsDate();
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        binding.textDate.setText(dateFmt.format(date));
        binding.textTime.setText(timeFmt.format(date));

        // Category
        binding.layoutCategory.setVisibility(View.VISIBLE);
        Category cat = categoryMap != null ? categoryMap.get(transaction.getCategoryId()) : null;
        binding.textCategory.setText(
                cat != null ? cat.getName() : getString(R.string.category_deleted));

        // Wallet
        binding.layoutWallet.setVisibility(View.VISIBLE);
        Wallet w = walletMap != null ? walletMap.get(transaction.getWalletId()) : null;
        binding.textWallet.setText(
                w != null ? w.getName() : getString(R.string.wallet_deleted));

        // Note
        String note = transaction.getNote();
        if (!TextUtils.isEmpty(note)) {
            binding.layoutNote.setVisibility(View.VISIBLE);
            binding.textNote.setText(note);
        } else {
            binding.layoutNote.setVisibility(View.GONE);
        }

        // Mood
        String mood = transaction.getMood();
        if (!TextUtils.isEmpty(mood)) {
            binding.layoutMood.setVisibility(View.VISIBLE);
            binding.textMood.setText(mood);
        } else {
            binding.layoutMood.setVisibility(View.GONE);
        }

        // CreatedAt
        if (transaction.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "dd/MM/yyyy HH:mm", Locale.getDefault());
            binding.textCreatedAt.setText(sdf.format(transaction.getCreatedAt().toDate()));
        }
    }

    private void setupButtons() {
        binding.btnEdit.setOnClickListener(v -> {
            if (listener != null && transaction.getId() != null) {
                listener.onEditTransaction(transaction.getId());
            }
            dismiss();
        });

        binding.btnDelete.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_confirm_title)
                    .setMessage(R.string.delete_transaction_confirm)
                    .setPositiveButton(R.string.delete, (d, w) -> {
                        if (listener != null && transaction.getId() != null) {
                            listener.onDeleteTransaction(transaction.getId());
                        }
                        dismiss();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
