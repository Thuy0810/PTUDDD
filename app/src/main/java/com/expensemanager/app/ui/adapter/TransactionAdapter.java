package com.expensemanager.app.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.databinding.ItemTransactionBinding;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {
    public interface OnItemClick { void onClick(Transaction t); }

    private List<Transaction> items = new ArrayList<>();
    private Map<String, Category> categoryMap;
    private OnItemClick listener;

    public void setItems(List<Transaction> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCategoryMap(Map<String, Category> map) {
        this.categoryMap = map;
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
        Category c = categoryMap != null ? categoryMap.get(t.getCategoryId()) : null;
        holder.binding.textCategory.setText(c != null ? c.getName() : t.getCategoryId());
        holder.binding.textNote.setText(t.getNote());
        holder.binding.textDate.setText(DateUtils.formatDisplay(t.getDateAsDate()));
        holder.binding.textAmount.setText(
                (Transaction.TYPE_INCOME.equals(t.getType()) ? "+" : "-")
                        + MoneyFormat.format(t.getAmount()));
        int color = Transaction.TYPE_INCOME.equals(t.getType())
                ? Color.parseColor("#10B981") : Color.parseColor("#EF4444");
        holder.binding.textAmount.setTextColor(color);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(t);
        });
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
