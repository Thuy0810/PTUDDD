package com.expensemanager.app.ui.budget;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Budget;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.databinding.ItemBudgetBinding;
import com.expensemanager.app.util.MoneyFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.VH> {
    public interface OnItemClick { void onClick(Budget b, Category cat); }

    private List<Budget> items = new ArrayList<>();
    private Map<String, Category> categoryMap = new HashMap<>();
    private Map<String, Long> spentMap = new HashMap<>();
    private OnItemClick listener;

    public void setItems(List<Budget> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCategoryMap(Map<String, Category> map) {
        this.categoryMap = map != null ? map : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setSpentMap(Map<String, Long> map) {
        this.spentMap = map != null ? map : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setOnItemClick(OnItemClick listener) { this.listener = listener; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBudgetBinding b = ItemBudgetBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Budget b = items.get(position);
        Category cat = categoryMap.get(b.getCategoryId());
        long spent = spentMap.containsKey(b.getCategoryId()) ? spentMap.get(b.getCategoryId()) : 0L;
        long remaining = b.getLimitAmount() - spent;
        long percent = b.getLimitAmount() > 0 ? (spent * 100 / b.getLimitAmount()) : 0L;
        int progress = (int) Math.min(percent, 100);

        holder.binding.textCategoryName.setText(cat != null ? cat.getName() : b.getCategoryId());
        holder.binding.textSpentInfo.setText(
                MoneyFormat.formatLong(spent) + " / " + MoneyFormat.formatLong(b.getLimitAmount()));
        holder.binding.textRemaining.setText(MoneyFormat.formatLong(Math.max(remaining, 0L)));

        if (remaining < 0) {
            holder.binding.textRemaining.setTextColor(
                    holder.itemView.getContext().getColor(R.color.expense_red));
        } else {
            holder.binding.textRemaining.setTextColor(
                    holder.itemView.getContext().getColor(R.color.text_primary));
        }

        holder.binding.progressBar.setProgress(progress);
        if (percent >= 100) {
            holder.binding.progressBar.setIndicatorColor(
                    holder.itemView.getContext().getColor(R.color.budget_danger));
        } else if (percent >= 80) {
            holder.binding.progressBar.setIndicatorColor(
                    holder.itemView.getContext().getColor(R.color.budget_warning));
        } else {
            holder.binding.progressBar.setIndicatorColor(
                    holder.itemView.getContext().getColor(R.color.budget_safe));
        }

        if (cat != null) {
            try {
                int color = Color.parseColor(cat.getColorHex());
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(color);
                holder.binding.viewCategoryBg.setBackground(bg);
            } catch (Exception ignored) {}
            holder.binding.textCategoryIcon.setText(getCategoryEmoji(cat.getIconKey()));
        }

        if (percent >= 90) {
            holder.binding.textAlert.setVisibility(View.VISIBLE);
            holder.binding.textAlert.setText(
                    String.format("⚠️ Đã dùng %d%% ngân sách!", percent));
        } else if (percent >= 80) {
            holder.binding.textAlert.setVisibility(View.VISIBLE);
            holder.binding.textAlert.setText(
                    String.format("Cảnh báo: %d%% ngân sách đã sử dụng", percent));
        } else {
            holder.binding.textAlert.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(b, cat);
        });
    }

    private String getCategoryEmoji(String iconKey) {
        if (iconKey == null) return "📦";
        switch (iconKey) {
            case "food": return "🍔";
            case "transport": return "🚌";
            case "shopping": return "🛍️";
            case "bills": return "📄";
            case "education": return "📚";
            case "entertainment": return "🎮";
            case "health": return "💊";
            case "family": return "👨‍👩‍👧";
            case "saving": return "💰";
            case "salary": return "💵";
            case "bonus": return "🎁";
            case "gift": return "🎁";
            case "sales": return "🛒";
            case "bank": return "🏦";
            case "cash": return "💵";
            case "ewallet": return "📱";
            default: return "📦";
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemBudgetBinding binding;
        VH(ItemBudgetBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
