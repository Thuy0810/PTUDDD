package com.expensemanager.app.ui.budget;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.databinding.ItemBudgetEssentialBinding;
import com.expensemanager.app.databinding.ItemBudgetSectionBinding;
import com.expensemanager.app.util.MoneyFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetSectionAdapter extends RecyclerView.Adapter<BudgetSectionAdapter.VH> {

    public static class Section {
        public String title;
        public List<CategoryItem> categories = new ArrayList<>();
        public boolean isExpanded = true;
        public boolean isOtherSection = false;
        public long totalBalance = 0L;

        public Section(String title) {
            this.title = title;
        }
    }

    public static class CategoryItem {
        public Category category;
        public long allocated = 0L;
        public long spent = 0L;

        public long getRemaining() {
            return allocated - spent;
        }

        public int getProgressPercent() {
            if (allocated <= 0) return 0;
            return (int) Math.min((spent * 100) / allocated, 100L);
        }
    }

    public interface OnCategoryEdit {
        void onEdit(Category category, long currentAmount);
    }

    private List<Section> sections = new ArrayList<>();
    private Map<String, Long> allocatedMap = new HashMap<>();
    private Map<String, Long> spentMap = new HashMap<>();
    private OnCategoryEdit editListener;
    private long totalBalance = 0L;

    public void setSections(List<Section> sections) {
        this.sections = sections;
        notifyDataSetChanged();
    }

    public void setAllocatedMap(Map<String, Long> map) {
        this.allocatedMap = map;
        notifyDataSetChanged();
    }

    public void setSpentMap(Map<String, Long> map) {
        this.spentMap = map;
        notifyDataSetChanged();
    }

    public void setOnCategoryEdit(OnCategoryEdit listener) {
        this.editListener = listener;
    }

    public void setTotalBalance(long balance) {
        this.totalBalance = balance;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBudgetSectionBinding b = ItemBudgetSectionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Section section = sections.get(position);
        holder.bind(section);
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    class VH extends RecyclerView.ViewHolder {
        final ItemBudgetSectionBinding binding;

        VH(ItemBudgetSectionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Section section) {
            binding.textSectionTitle.setText(section.title);

            // Calculate total progress
            long totalAllocated = 0L, totalSpent = 0L;
            for (CategoryItem item : section.categories) {
                String catId = item.category.getId();
                item.allocated = allocatedMap.containsKey(catId) ? allocatedMap.get(catId) : 0L;
                item.spent = spentMap.containsKey(catId) ? spentMap.get(catId) : 0L;
                totalAllocated += item.allocated;
                totalSpent += item.spent;
            }
            int progress = totalAllocated > 0 ? (int) (totalSpent * 100 / totalAllocated) : 0;
            binding.textProgress.setText(progress + "%");

            binding.layoutCards.removeAllViews();

            if (section.isExpanded) {
                for (CategoryItem item : section.categories) {
                    addCategoryCard(binding.layoutCards, item);
                }

                // Add balance card for "Khác" section
                if ("Khác".equals(section.title) && totalBalance > 0) {
                    addBalanceCard(binding.layoutCards);
                }
            }

            binding.getRoot().setOnClickListener(v -> {
                section.isExpanded = !section.isExpanded;
                notifyItemChanged(getAdapterPosition());
            });
        }

        private void addCategoryCard(LinearLayout container, CategoryItem item) {
            ItemBudgetEssentialBinding cardBinding = ItemBudgetEssentialBinding.inflate(
                    LayoutInflater.from(itemView.getContext()), container, false);

            Category cat = item.category;
            cardBinding.textCategoryName.setText(cat.getName());
            cardBinding.textCategoryIcon.setText(getCategoryEmoji(cat.getIconKey()));
            cardBinding.textAllocated.setText(MoneyFormat.formatLong(item.allocated));
            cardBinding.textSpent.setText(MoneyFormat.formatLong(item.spent));
            cardBinding.textRemaining.setText(
                    MoneyFormat.formatLong(Math.max(item.getRemaining(), 0L)));

            int progress = item.getProgressPercent();
            cardBinding.progressBar.setProgress(progress);

            if (item.getRemaining() < 0) {
                cardBinding.textRemaining.setTextColor(
                        itemView.getContext().getColor(R.color.expense_red));
            }

            if (cat.getColorHex() != null) {
                try {
                    int color = Color.parseColor(cat.getColorHex());
                    GradientDrawable bg = new GradientDrawable();
                    bg.setShape(GradientDrawable.OVAL);
                    bg.setColor(color);
                    cardBinding.viewCategoryBg.setBackground(bg);
                } catch (Exception ignored) {}
            }

            // Toggle expand/collapse
            final boolean[] expanded = {false};
            cardBinding.layoutHeader.setOnClickListener(v -> {
                expanded[0] = !expanded[0];
                cardBinding.layoutExpanded.setVisibility(expanded[0] ? View.VISIBLE : View.GONE);
                cardBinding.iconExpand.setRotation(expanded[0] ? 180f : 0f);
            });

            cardBinding.btnEdit.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onEdit(cat, item.allocated);
                }
            });

            container.addView(cardBinding.getRoot());
        }

        private void addBalanceCard(LinearLayout container) {
            View balanceView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.item_budget_balance, container, false);

            TextView textBalance = balanceView.findViewById(R.id.textBalance);
            TextView textBalanceLabel = balanceView.findViewById(R.id.textBalanceLabel);

            textBalance.setText(MoneyFormat.formatLong(totalBalance));
            textBalance.setTextColor(itemView.getContext().getColor(R.color.income_green));

            if (totalBalance >= 0) {
                textBalanceLabel.setText("Số dư khả dụng");
                textBalance.setTextColor(itemView.getContext().getColor(R.color.income_green));
            } else {
                textBalanceLabel.setText("Số dư âm");
                textBalance.setTextColor(itemView.getContext().getColor(R.color.expense_red));
            }

            container.addView(balanceView);
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
                case "home": return "🏠";
                case "rent": return "🏠";
                default: return "📦";
            }
        }
    }
}
