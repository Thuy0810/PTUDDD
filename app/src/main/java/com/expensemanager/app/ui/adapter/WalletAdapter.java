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

        // Icon ví: dùng thư viện icon (vector) + nền tròn màu ví
        com.expensemanager.app.util.WalletIcons.apply(
                holder.binding.textWalletIcon, holder.binding.viewWalletBg,
                w.getIcon(), w.getType(), w.getColor());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(w);
        });
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
