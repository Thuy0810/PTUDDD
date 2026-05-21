package com.expensemanager.app.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.databinding.ItemWalletBinding;
import com.expensemanager.app.util.MoneyFormat;

import java.util.ArrayList;
import java.util.List;

public class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.VH> {
    public interface OnItemClick { void onClick(Wallet w); }

    private List<Wallet> wallets = new ArrayList<>();
    private List<Transaction> allTx = new ArrayList<>();
    private OnItemClick listener;

    public void setWallets(List<Wallet> wallets) {
        this.wallets = wallets != null ? wallets : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setBalances(List<Transaction> allTx) {
        this.allTx = allTx != null ? allTx : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnItemClick(OnItemClick listener) { this.listener = listener; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemWalletBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Wallet w = wallets.get(position);
        holder.binding.textName.setText(w.getName());
        holder.binding.textType.setText(w.getTypeLabel());

        double bal = w.getCurrentBalance();
        holder.binding.textBalance.setText(MoneyFormat.format(bal));

        // Set icon
        holder.binding.textWalletIcon.setText(getWalletEmoji(w.getType()));

        // Set color background
        String colorHex = w.getColor();
        if (colorHex == null) colorHex = "#FFB84D";
        try {
            int color = Color.parseColor(colorHex);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(color);
            holder.binding.viewWalletBg.setBackground(bg);
        } catch (Exception ignored) {}

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(w);
        });
    }

    private String getWalletEmoji(String type) {
        if (type == null) return "💰";
        switch (type) {
            case "cash": return "💵";
            case "bank": return "🏦";
            case "ewallet": return "📱";
            case "savings": return "🐷";
            default: return "💰";
        }
    }

    @Override
    public int getItemCount() { return wallets.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemWalletBinding binding;
        VH(ItemWalletBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
