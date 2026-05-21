package com.expensemanager.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.data.model.SavingsGoal;
import com.expensemanager.app.databinding.ItemGoalBinding;
import com.expensemanager.app.util.DateUtils;
import com.expensemanager.app.util.MoneyFormat;

import java.util.ArrayList;
import java.util.List;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.VH> {
    public interface OnItemClick { void onClick(SavingsGoal g); }

    private List<SavingsGoal> items = new ArrayList<>();
    private OnItemClick listener;

    public void setItems(List<SavingsGoal> items) {
        this.items = items != null ? items : new ArrayList<>();
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
        holder.binding.textTitle.setText(g.getTitle());
        holder.binding.textSaved.setText(MoneyFormat.format(g.getSavedAmount()));
        holder.binding.textTarget.setText(MoneyFormat.format(g.getTargetAmount()));
        int pct = (int) (g.getProgress() * 100);
        holder.binding.textPercent.setText(pct + "%");
        holder.binding.progressBar.setProgress(pct);

        if (g.getDeadline() != null) {
            holder.binding.textDeadline.setText("Hạn: " + DateUtils.formatDisplay(g.getDeadline().toDate()));
        } else {
            holder.binding.textDeadline.setText("Không có hạn");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(g);
        });
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
