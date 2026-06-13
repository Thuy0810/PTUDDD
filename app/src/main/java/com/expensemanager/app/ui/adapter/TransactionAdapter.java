package com.expensemanager.app.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.databinding.ItemTransactionBinding;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        String note = t.getNote();
        if (note == null) note = "";

        Category c = categoryMap.get(t.getCategoryId());

        if (Transaction.TYPE_TRANSFER.equals(type)) {
            holder.binding.textCategory.setText("Chuyển tiền");
            holder.binding.textNote.setText(note.isEmpty() ? getTransferLabel(t) : note);
        } else {
            holder.binding.textCategory.setText(c != null ? c.getName() : (t.getCategoryId() != null ? t.getCategoryId() : ""));
            holder.binding.textNote.setText(note);
        }

        holder.binding.textDate.setText(DateUtils.formatDisplay(t.getDateAsDate()));

        String amountStr;
        int color;
        if (Transaction.TYPE_TRANSFER.equals(type)) {
            amountStr = MoneyFormat.format(t.getAmount());
            color = Color.parseColor("#6B7280");
        } else if (Transaction.TYPE_INCOME.equals(type)) {
            amountStr = "+" + MoneyFormat.format(t.getAmount());
            color = Color.parseColor("#10B981");
        } else {
            amountStr = "-" + MoneyFormat.format(t.getAmount());
            color = Color.parseColor("#EF4444");
        }
        holder.binding.textAmount.setText(amountStr);
        holder.binding.textAmount.setTextColor(color);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(t);
        });
    }

    private String getTransferLabel(Transaction t) {
        Wallet from = walletMap.get(t.getFromWalletId());
        Wallet to = walletMap.get(t.getToWalletId());
        String fromName = from != null ? from.getName() : "";
        String toName = to != null ? to.getName() : "";
        if (!fromName.isEmpty() && !toName.isEmpty()) {
            return fromName + " → " + toName;
        }
        return "";
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
    }
}
