package com.expensemanager.app.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.databinding.ItemCategoryBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {
    public interface OnItemLongClick { void onLongClick(Category c); }

    private List<Category> items = new ArrayList<>();
    private Map<String, String> parentNames = new HashMap<>();
    private OnItemLongClick listener;

    public void setItems(List<Category> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    /** Map id danh mục -> tên, dùng để hiển thị tên danh mục cha. */
    public void setParentNames(Map<String, String> parentNames) {
        this.parentNames = parentNames != null ? parentNames : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setOnItemLongClick(OnItemLongClick listener) { this.listener = listener; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemCategoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Category c = items.get(position);
        holder.binding.textCategoryName.setText(c.getName());
        String subtitle = c.getTypeLabel() != null ? c.getTypeLabel() : "";
        if (c.isSubcategory()) {
            String pName = parentNames.get(c.getParentId());
            if (pName != null && !pName.isEmpty()) {
                subtitle = "↳ " + pName;
            }
        }
        holder.binding.textCategoryType.setText(subtitle);

        boolean isIncome = Category.TYPE_INCOME.equals(c.getType());
        holder.binding.textTypeBadge.setText(isIncome ? "Thu" : "Chi");
        holder.binding.textTypeBadge.setBackgroundResource(
                isIncome ? com.expensemanager.app.R.drawable.bg_income_badge
                        : com.expensemanager.app.R.drawable.bg_expense_badge);
        holder.binding.textTypeBadge.setTextColor(holder.itemView.getContext().getColor(
                isIncome ? com.expensemanager.app.R.color.income_green
                        : com.expensemanager.app.R.color.expense_red));

        holder.binding.textCategoryIcon.setText(getCategoryEmoji(c.getIconKey()));

        try {
            int color = Color.parseColor(c.getColorHex());
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(color);
            holder.binding.viewCategoryBg.setBackground(bg);
        } catch (Exception ignored) {}

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onLongClick(c);
            return true;
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
            default: return "📦";
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemCategoryBinding binding;
        VH(ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
