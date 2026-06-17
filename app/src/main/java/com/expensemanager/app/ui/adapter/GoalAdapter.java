package com.expensemanager.app.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.SavingsGoal;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.databinding.ItemGoalBinding;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.GoalIcons;
import com.expensemanager.app.util.MoneyFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.VH> {
    public interface OnItemClick { void onClick(SavingsGoal g); }

    private List<SavingsGoal> items = new ArrayList<>();
    private Map<String, Wallet> walletMap = new HashMap<>();
    private OnItemClick listener;

    public void setItems(List<SavingsGoal> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setWalletMap(Map<String, Wallet> map) {
        this.walletMap = map != null ? map : new HashMap<>();
        notifyDataSetChanged();
    }

    public void setOnItemClick(OnItemClick listener) { this.listener = listener; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemGoalBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        SavingsGoal g = items.get(position);
        GoalIcons.apply(holder.binding.imgIcon, g.getIconKey());
        holder.binding.textTitle.setText(g.getTitle());
        holder.binding.textSaved.setText(MoneyFormat.format(g.getSavedAmount()));
        holder.binding.textTarget.setText(MoneyFormat.format(g.getTargetAmount()));
        int pct = (int) (g.getProgress() * 100);
        holder.binding.textPercent.setText(pct + "%");
        holder.binding.progressBar.setProgress(pct);

        android.content.Context ctx = holder.itemView.getContext();
        boolean overdue = g.isOverdue();
        if (overdue) {
            holder.binding.textDeadline.setTextColor(0xFFE53935);
            holder.binding.textDeadline.setText(ctx.getString(R.string.j3_overdue_with_date,
                    DateUtils.formatDisplay(g.getDeadline().toDate())));
        } else if (g.getDeadline() != null) {
            holder.binding.textDeadline.setTextColor(
                    holder.itemView.getContext().getColor(R.color.text_secondary));
            holder.binding.textDeadline.setText(ctx.getString(R.string.j3_deadline_with_date,
                    DateUtils.formatDisplay(g.getDeadline().toDate())));
        } else {
            holder.binding.textDeadline.setTextColor(
                    holder.itemView.getContext().getColor(R.color.text_secondary));
            holder.binding.textDeadline.setText(ctx.getString(R.string.j3_no_deadline));
        }

        String progressLine = buildTimeProgress(ctx, g);
        holder.binding.textDeadline.setText(progressLine);

        if (overdue && !g.isCompleted()) {
            holder.binding.cardGoal.setCardBackgroundColor(0x1AFF5252);
        } else {
            holder.binding.cardGoal.setCardBackgroundColor(
                    holder.itemView.getContext().getColor(R.color.card_bg));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(g);
        });
    }

    private String buildTimeProgress(android.content.Context ctx, SavingsGoal g) {
        if (g.isCompleted()) {
            return ctx.getString(R.string.j3_completed);
        }

        StringBuilder sb = new StringBuilder();

        if (g.getDeadline() != null) {
            long daysLeft = g.getRemainingDays();
            String dateStr = DateUtils.formatDisplay(g.getDeadline().toDate());
            if (daysLeft < 0) {
                sb.append(ctx.getString(R.string.j3_overdue_with_date, dateStr));
            } else if (daysLeft == 0) {
                sb.append(ctx.getString(R.string.j3_due_today));
            } else if (daysLeft <= 7) {
                sb.append(ctx.getString(R.string.j3_days_left_with_date, daysLeft, dateStr));
            } else {
                sb.append(ctx.getString(R.string.j3_deadline_with_date, dateStr));
            }
        } else {
            sb.append(ctx.getString(R.string.j3_no_deadline));
        }

        if (!g.isCompleted() && g.getTargetAmount() > g.getSavedAmount()) {
            double monthlyRequired = g.getMonthlyRequired();
            if (monthlyRequired > 0) {
                sb.append(ctx.getString(R.string.j3_required_per_month,
                        MoneyFormat.format(monthlyRequired)));
            }
        }

        return sb.toString();
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemGoalBinding binding;
        VH(ItemGoalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
