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
        public long rollover = 0L; // cuốn chiếu từ tháng trước (có thể âm)

        /** Tiền khả dụng của phong bì (ZBB) = phân bổ + cuốn chiếu. */
        public long getAvailable() {
            return allocated + rollover;
        }

        public long getRemaining() {
            return getAvailable() - spent;
        }

        public int getProgressPercent() {
            long avail = getAvailable();
            if (avail <= 0) return spent > 0 ? 100 : 0;
            return (int) Math.min((spent * 100) / avail, 100L);
        }
    }

    public interface OnCategoryEdit {
        void onEdit(Category category, long currentAmount);
    }

    private List<Section> sections = new ArrayList<>();
    private Map<String, Long> allocatedMap = new HashMap<>();
    private Map<String, Long> spentMap = new HashMap<>();
    private Map<String, Long> rolloverMap = new HashMap<>();
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

    public void setRolloverMap(Map<String, Long> map) {
        this.rolloverMap = map != null ? map : new HashMap<>();
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
                item.rollover = rolloverMap.containsKey(catId) ? rolloverMap.get(catId) : 0L;
                totalAllocated += item.getAvailable();
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
            // ZBB: hiển thị tiền khả dụng của phong bì (phân bổ + cuốn chiếu).
            cardBinding.textAllocated.setText(MoneyFormat.formatLong(item.getAvailable()));
            cardBinding.textSpent.setText(MoneyFormat.formatLong(item.spent));
            cardBinding.textRemaining.setText(
                    MoneyFormat.formatLong(Math.max(item.getRemaining(), 0L)));

            int progress = item.getProgressPercent();
            cardBinding.progressBar.setProgress(progress);

            // Highlight + nhắc phân bổ khi danh mục vượt ngân sách.
            android.content.Context ctx = itemView.getContext();
            com.google.android.material.card.MaterialCardView card =
                    (com.google.android.material.card.MaterialCardView) cardBinding.getRoot();
            boolean over = item.getRemaining() < 0;
            if (over) {
                long deficit = -item.getRemaining();
                cardBinding.textRemaining.setTextColor(ctx.getColor(R.color.expense_red));
                cardBinding.textOverBadge.setText(ctx.getString(
                        R.string.budget_exceeded_amount, MoneyFormat.formatLong(deficit)));
                cardBinding.textOverBadge.setBackgroundColor(ctx.getColor(R.color.budget_danger));
                cardBinding.textOverBadge.setVisibility(View.VISIBLE);
                card.setStrokeColor(ctx.getColor(R.color.budget_danger));
                card.setStrokeWidth((int) (1.5f * ctx.getResources().getDisplayMetrics().density));
            } else {
                // Reset trạng thái (do RecyclerView tái dùng view).
                cardBinding.textRemaining.setTextColor(ctx.getColor(R.color.income_green));
                cardBinding.textOverBadge.setVisibility(View.GONE);
                card.setStrokeWidth(0);
            }

            // Icon danh muc: icon vector (kieu net) theo thiet ke
            int catColor = itemView.getContext().getColor(R.color.primary);
            if (cat.getColorHex() != null) {
                try { catColor = Color.parseColor(cat.getColorHex()); } catch (Exception ignored) {}
            }
            if (com.expensemanager.app.util.CategoryIcons.isEmoji(cat.getIconKey())) {
                cardBinding.imageCategoryIcon.setVisibility(View.GONE);
                cardBinding.textCategoryIcon.setVisibility(View.VISIBLE);
                cardBinding.textCategoryIcon.setText(getCategoryEmoji(cat.getIconKey()));
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(com.expensemanager.app.util.CategoryIcons.softTint(catColor));
                cardBinding.viewCategoryBg.setBackground(bg);
            } else {
                cardBinding.textCategoryIcon.setVisibility(View.GONE);
                cardBinding.imageCategoryIcon.setVisibility(View.VISIBLE);
                com.expensemanager.app.util.CategoryIcons.apply(
                        cardBinding.imageCategoryIcon, cardBinding.viewCategoryBg,
                        cat.getIconKey(), catColor, cat.getType());
            }

            // Toggle expand/collapse
            final boolean[] expanded = {false};
            cardBinding.layoutHeader.setOnClickListener(v -> {
                expanded[0] = !expanded[0];
                cardBinding.layoutExpanded.setVisibility(expanded[0] ? View.VISIBLE : View.GONE);
                cardBinding.iconExpand.setRotation(expanded[0] ? 180f : 0f);
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
                textBalanceLabel.setText(itemView.getContext().getString(R.string.j1_available_balance));
                textBalance.setTextColor(itemView.getContext().getColor(R.color.income_green));
            } else {
                textBalanceLabel.setText(itemView.getContext().getString(R.string.j1_negative_balance));
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
