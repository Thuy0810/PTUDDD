package com.expensemanager.app.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.databinding.ItemTransactionBinding;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {
    public interface OnItemClick { void onClick(Transaction t); }

    private List<Transaction> items = new ArrayList<>();
    private Map<String, Category> categoryMap = new HashMap<>();
    private Map<String, Wallet> walletMap = new HashMap<>();
    private OnItemClick listener;

    public void setItems(List<Transaction> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCategoryMap(Map<String, Category> map) {
        this.categoryMap = map != null ? map : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setWalletMap(Map<String, Wallet> map) {
        this.walletMap = map != null ? map : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setOnItemClick(OnItemClick listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionBinding b = ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Transaction t = items.get(position);
        String type = t.getType();
        holder.bind(t, type, categoryMap, walletMap, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemTransactionBinding binding;

        VH(ItemTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Transaction t, String type,
                   Map<String, Category> categoryMap,
                   Map<String, Wallet> walletMap,
                   OnItemClick listener) {

            String categoryName = "";
            String iconEmoji = getDefaultIcon(type);
            int iconBgColor = getDefaultBgColor(type);

            Category cat = categoryMap.get(t.getCategoryId());
            if (cat != null) {
                categoryName = cat.getName();
                String emoji = cat.getIconKey();
                if (!TextUtils.isEmpty(emoji) && emoji.length() <= 4) {
                    iconEmoji = emoji;
                }
                String color = cat.getColorHex();
                if (!TextUtils.isEmpty(color)) {
                    try {
                        iconBgColor = Color.parseColor(color);
                    } catch (Exception ignored) {}
                }
            } else if (t.getCategoryId() != null && !t.getCategoryId().isEmpty()) {
                categoryName = binding.getRoot().getContext()
                        .getString(R.string.category_deleted);
            }

            // --- Icon ---
            binding.textCategoryIcon.setText(iconEmoji);
            try {
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(iconBgColor);
                binding.viewIconBg.setBackground(bg);
            } catch (Exception e) {
                binding.viewIconBg.setBackgroundResource(R.drawable.bg_circle_primary);
            }

            // --- Category / Type label ---
            if (Transaction.TYPE_TRANSFER.equals(type)) {
                binding.textCategory.setText(R.string.transfer);
            } else {
                binding.textCategory.setText(categoryName);
            }

            // --- Wallet / transfer route ---
            String walletText = "";
            if (Transaction.TYPE_TRANSFER.equals(type)) {
                Wallet from = walletMap.get(t.getFromWalletId());
                Wallet to = walletMap.get(t.getToWalletId());
                String fromName = from != null ? from.getName()
                        : binding.getRoot().getContext().getString(R.string.wallet_deleted);
                String toName = to != null ? to.getName()
                        : binding.getRoot().getContext().getString(R.string.wallet_deleted);
                walletText = fromName + " → " + toName;
            } else {
                Wallet w = walletMap.get(t.getWalletId());
                if (w != null) {
                    walletText = binding.getRoot().getContext()
                            .getString(R.string.wallet_label, w.getName());
                } else if (t.getWalletId() != null && !t.getWalletId().isEmpty()) {
                    walletText = binding.getRoot().getContext()
                            .getString(R.string.wallet_deleted);
                }
            }
            binding.textWallet.setText(walletText);

            // --- Date + time ---
            binding.textDateTime.setText(DateUtils.formatDateTime(t.getDateAsDate()));

            // --- Note ---
            String note = t.getNote();
            if (!TextUtils.isEmpty(note)) {
                binding.textNote.setText(note);
                binding.textNote.setVisibility(View.VISIBLE);
            } else {
                binding.textNote.setVisibility(View.GONE);
            }

            // --- Amount ---
            String amountStr;
            int amountColor;
            if (Transaction.TYPE_TRANSFER.equals(type)) {
                amountStr = MoneyFormat.format(t.getAmount());
                amountColor = ContextCompat.getColor(
                        binding.getRoot().getContext(), R.color.text_secondary);
            } else if (Transaction.TYPE_INCOME.equals(type)) {
                amountStr = MoneyFormat.formatSigned(t.getAmount(), Transaction.TYPE_INCOME);
                amountColor = ContextCompat.getColor(
                        binding.getRoot().getContext(), R.color.income_green);
            } else {
                amountStr = MoneyFormat.formatSigned(t.getAmount(), Transaction.TYPE_EXPENSE);
                amountColor = ContextCompat.getColor(
                        binding.getRoot().getContext(), R.color.expense_red);
            }
            binding.textAmount.setText(amountStr);
            binding.textAmount.setTextColor(amountColor);

            // --- Click ---
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onClick(t);
            });
        }

        private String getDefaultIcon(String type) {
            if (Transaction.TYPE_INCOME.equals(type)) return "\uD83D\uDCB0";
            if (Transaction.TYPE_EXPENSE.equals(type)) return "\uD83D\uDCB8";
            return "\uD83D\uDCB3";
        }

        private int getDefaultBgColor(String type) {
            if (Transaction.TYPE_INCOME.equals(type)) {
                return ContextCompat.getColor(
                        binding.getRoot().getContext(), R.color.income_green);
            }
            if (Transaction.TYPE_EXPENSE.equals(type)) {
                return ContextCompat.getColor(
                        binding.getRoot().getContext(), R.color.expense_red);
            }
            return ContextCompat.getColor(
                    binding.getRoot().getContext(), R.color.primary);
        }
    }
}
