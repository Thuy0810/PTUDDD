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
import com.expensemanager.app.data.model.RecurringRule;
import com.expensemanager.app.databinding.ItemRecurringBinding;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecurringAdapter extends RecyclerView.Adapter<RecurringAdapter.VH> {
    public interface OnToggle { void onToggle(RecurringRule r, boolean enabled); }
    public interface OnItemClick { void onClick(RecurringRule r); }

    private List<RecurringRule> items = new ArrayList<>();
    private Map<String, Category> categoryMap = new HashMap<>();
    private Map<String, Category> incomeCategoryMap = new HashMap<>();
    private OnToggle toggleListener;
    private OnItemClick clickListener;

    public void setItems(List<RecurringRule> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCategoryMap(Map<String, Category> map) {
        this.categoryMap = map != null ? map : new HashMap<>();
        this.incomeCategoryMap = new HashMap<>();
        for (Map.Entry<String, Category> e : this.categoryMap.entrySet()) {
            if ("income".equals(e.getValue().getType())) {
                incomeCategoryMap.put(e.getKey(), e.getValue());
            }
        }
        notifyDataSetChanged();
    }

    public void setOnToggle(OnToggle listener) { this.toggleListener = listener; }
    public void setOnItemClick(OnItemClick listener) { this.clickListener = listener; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemRecurringBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        RecurringRule r = items.get(position);
        holder.bind(r, categoryMap, incomeCategoryMap, toggleListener, clickListener);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemRecurringBinding binding;

        VH(ItemRecurringBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(RecurringRule r, Map<String, Category> categoryMap,
                  Map<String, Category> incomeCategoryMap,
                  OnToggle toggleListener, OnItemClick clickListener) {

            boolean isIncome = r.isIncome();

            // Type icon
            if (isIncome) {
                binding.textTypeIcon.setText("\uD83D\uDCB0");
                binding.textTypeIcon.setTextColor(Color.WHITE);
                try {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setShape(GradientDrawable.OVAL);
                    bg.setColor(ContextCompat.getColor(
                            binding.getRoot().getContext(), R.color.income_green));
                    binding.viewIconBg.setBackground(bg);
                } catch (Exception ignored) {}
                binding.textAmount.setTextColor(ContextCompat.getColor(
                        binding.getRoot().getContext(), R.color.income_green));
            } else {
                binding.textTypeIcon.setText("\uD83D\uDCB8");
                binding.textTypeIcon.setTextColor(Color.WHITE);
                try {
                    GradientDrawable bg = new GradientDrawable();
                    bg.setShape(GradientDrawable.OVAL);
                    bg.setColor(ContextCompat.getColor(
                            binding.getRoot().getContext(), R.color.expense_red));
                    binding.viewIconBg.setBackground(bg);
                } catch (Exception ignored) {}
                binding.textAmount.setTextColor(ContextCompat.getColor(
                        binding.getRoot().getContext(), R.color.expense_red));
            }

            // Name
            String name = r.getNote();
            if (TextUtils.isEmpty(name)) {
                name = isIncome
                        ? binding.getRoot().getContext().getString(R.string.recurring_income_default)
                        : binding.getRoot().getContext().getString(R.string.recurring_expense_default);
            }
            binding.textName.setText(name);

            // Amount with sign
            String amountStr = MoneyFormat.formatSigned(r.getAmount(),
                    isIncome ? "income" : "expense");
            binding.textAmount.setText(amountStr);

            // Category + Wallet
            Map<String, Category> catMap = isIncome ? incomeCategoryMap : categoryMap;
            Category cat = catMap.get(r.getCategoryId());
            String catName = cat != null ? cat.getName()
                    : binding.getRoot().getContext().getString(R.string.category_deleted);
            String walletId = r.getWalletId();
            // Wallet name from the same map (we need wallet names but don't have walletMap here)
            // Show category info
            binding.textCategoryWallet.setText(catName);

            // Cycle
            String cycleLabel = buildCycleLabel(r);
            binding.textCycle.setText(cycleLabel);

            // Next run
            String nextRunLabel = buildNextRunLabel(r);
            binding.textNextRun.setText(nextRunLabel);

            // Toggle
            binding.switchEnabled.setChecked(r.isEnabled());
            binding.switchEnabled.setOnCheckedChangeListener((btn, checked) -> {
                if (toggleListener != null) toggleListener.onToggle(r, checked);
            });

            // Click
            binding.getRoot().setOnClickListener(v -> {
                if (clickListener != null) clickListener.onClick(r);
            });
        }

        private String buildCycleLabel(RecurringRule r) {
            String cycle = r.getCycleType();
            if (cycle == null) cycle = RecurringRule.CYCLE_MONTHLY;
            String ctx = binding.getRoot().getContext().getString(R.string.recurring_every);

            switch (cycle) {
                case RecurringRule.CYCLE_DAILY:
                    return binding.getRoot().getContext().getString(R.string.recurring_daily);
                case RecurringRule.CYCLE_WEEKLY:
                    return ctx + " " + getDayOfWeekName(r.getDayOfWeek());
                case RecurringRule.CYCLE_YEARLY:
                    return ctx + " " + getDayOfYearLabel(r.getDayOfMonth(), r.getMonthOfYear());
                case RecurringRule.CYCLE_MONTHLY:
                default:
                    if (r.isUseLastDayOfMonth()) {
                        return binding.getRoot().getContext().getString(
                                R.string.recurring_last_day_of_month);
                    }
                    return binding.getRoot().getContext().getString(
                            R.string.recurring_day_of_month, r.getDayOfMonth());
            }
        }

        private String buildNextRunLabel(RecurringRule r) {
            if (r.getNextRun() == null) return "";

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String nextDate = sdf.format(r.getNextRun().toDate());
            String label = binding.getRoot().getContext().getString(
                    R.string.recurring_next_run, nextDate);

            if (r.getDateEnd() != null) {
                String endDate = sdf.format(r.getDateEnd().toDate());
                label += " • " + binding.getRoot().getContext().getString(
                        R.string.recurring_until, endDate);
            }
            return label;
        }

        private String getDayOfWeekName(int day) {
            String[] days = binding.getRoot().getContext().getResources()
                    .getStringArray(R.array.day_of_week_labels);
            if (day >= 1 && day <= 7) return days[day - 1];
            return "";
        }

        private String getDayOfYearLabel(int day, int month) {
            String[] months = binding.getRoot().getContext().getResources()
                    .getStringArray(R.array.month_labels);
            String monthStr = (month >= 1 && month <= 12)
                    ? months[month - 1] : String.valueOf(month);
            return binding.getRoot().getContext().getString(
                    R.string.recurring_day_month, day, monthStr);
        }
    }
}
