package com.expensemanager.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.RecurringRule;
import com.expensemanager.app.databinding.ItemRecurringBinding;
import com.expensemanager.app.util.MoneyFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecurringAdapter extends RecyclerView.Adapter<RecurringAdapter.VH> {
    public interface OnToggle { void onToggle(RecurringRule r, boolean enabled); }
    public interface OnItemClick { void onClick(RecurringRule r); }

    private List<RecurringRule> items = new ArrayList<>();
    private Map<String, Category> categoryMap = new HashMap<>();
    private OnToggle toggleListener;
    private OnItemClick clickListener;

    public void setItems(List<RecurringRule> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCategoryMap(Map<String, Category> map) {
        this.categoryMap = map != null ? map : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setOnToggle(OnToggle listener) { this.toggleListener = listener; }
    public void setOnItemClick(OnItemClick listener) { this.clickListener = listener; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemRecurringBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        RecurringRule r = items.get(position);
        holder.binding.textName.setText(r.getNote() != null && !r.getNote().isEmpty() ? r.getNote() : "Khoản định kỳ");
        holder.binding.textAmount.setText("- " + MoneyFormat.format(r.getAmount()));

        Category cat = categoryMap.get(r.getCategoryId());
        holder.binding.textCategory.setText(cat != null ? cat.getName() : "");

        String repeatLabel = buildRepeatLabel(r);
        holder.binding.textRepeat.setText(repeatLabel);
        holder.binding.switchEnabled.setChecked(r.isEnabled());

        holder.binding.switchEnabled.setOnCheckedChangeListener((b, checked) -> {
            if (toggleListener != null) toggleListener.onToggle(r, checked);
        });

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(r);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    private String buildRepeatLabel(RecurringRule r) {
        String cycle = r.getCycleType();
        if (cycle == null) cycle = RecurringRule.CYCLE_MONTHLY;

        String dayPart;
        switch (cycle) {
            case RecurringRule.CYCLE_DAILY:
                return "Hàng ngày";
            case RecurringRule.CYCLE_WEEKLY:
                dayPart = getDayOfWeekName(r.getDayOfWeek());
                return "Mỗi " + dayPart;
            case RecurringRule.CYCLE_YEARLY:
                dayPart = "ngày " + r.getDayOfMonth() + " tháng " + r.getMonthOfYear();
                return dayPart;
            case RecurringRule.CYCLE_MONTHLY:
            default:
                if (r.getDayOfMonth() == 0) {
                    return "Hàng tháng";
                } else if (r.getDayOfMonth() == 31) {
                    return "Ngày cuối mỗi tháng";
                } else {
                    return "Ngày " + r.getDayOfMonth() + " mỗi tháng";
                }
        }
    }

    private String getDayOfWeekName(int day) {
        String[] days = {"", "Chủ nhật", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"};
        if (day >= 1 && day <= 7) return days[day];
        return "";
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemRecurringBinding binding;
        VH(ItemRecurringBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
