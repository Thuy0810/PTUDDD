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
import com.expensemanager.app.databinding.ItemBudgetAllocationBinding;
import com.expensemanager.app.util.MoneyFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetAllocationAdapter extends RecyclerView.Adapter<BudgetAllocationAdapter.VH> {

    public interface OnEditClick {
        void onEdit(Category category, long currentAmount);
    }

    private List<Category> categories = new ArrayList<>();
    private List<Budget> budgets = new ArrayList<>();
    private Map<String, Long> spentMap = new HashMap<>();
    private OnEditClick listener;

    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    public void setBudgets(List<Budget> budgets) {
        this.budgets = budgets;
        notifyDataSetChanged();
    }

    public void setSpentMap(Map<String, Long> spentMap) {
        this.spentMap = spentMap;
        notifyDataSetChanged();
    }

    public void setOnEditClick(OnEditClick listener) {
        this.listener = listener;
    }

    private long getBudgetAmountForCategory(String categoryId) {
        for (Budget b : budgets) {
            if (categoryId.equals(b.getCategoryId())) {
                return b.getLimitAmount();
            }
        }
        return 0L;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBudgetAllocationBinding b = ItemBudgetAllocationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Category cat = categories.get(position);
        long budgetAmount = getBudgetAmountForCategory(cat.getId());
        long spent = spentMap.containsKey(cat.getId()) ? spentMap.get(cat.getId()) : 0L;
        int progress = budgetAmount > 0 ? (int) (spent * 100 / budgetAmount) : 0;
        progress = Math.min(progress, 100);

        holder.binding.textCategoryName.setText(cat.getName());
        holder.binding.textAllocatedAmount.setText(MoneyFormat.formatLong(budgetAmount));
        holder.binding.textPercent.setText(progress + "%");
        holder.binding.progressBar.setProgress(progress);

        int color;
        try {
            color = Color.parseColor(cat.getColorHex());
        } catch (Exception e) {
            color = holder.itemView.getContext().getColor(
                    com.expensemanager.app.R.color.primary);
        }
        com.expensemanager.app.util.CategoryIcons.apply(
                holder.binding.textCategoryIcon, holder.binding.viewCategoryBg,
                cat.getIconKey(), color, cat.getType());

        holder.binding.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(cat, budgetAmount);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(cat, budgetAmount);
            }
        });
    }


    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemBudgetAllocationBinding binding;
        VH(ItemBudgetAllocationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
