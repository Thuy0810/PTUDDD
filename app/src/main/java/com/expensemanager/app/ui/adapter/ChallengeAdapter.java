package com.expensemanager.app.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Challenge;
import com.expensemanager.app.databinding.ItemChallengeBinding;
import com.expensemanager.app.util.MoneyFormat;

import java.util.ArrayList;
import java.util.List;

public class ChallengeAdapter extends RecyclerView.Adapter<ChallengeAdapter.VH> {
    public interface OnItemClick { void onClick(Challenge c); }
    public interface OnMarkDay { void onMark(Challenge c); }

    private List<Challenge> items = new ArrayList<>();
    private OnItemClick listener;
    private OnMarkDay markListener;

    public void setItems(List<Challenge> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnItemClick(OnItemClick listener) { this.listener = listener; }
    public void setOnMarkDay(OnMarkDay listener) { this.markListener = listener; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemChallengeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Challenge c = items.get(position);
        Context ctx = holder.itemView.getContext();

        holder.binding.textTitle.setText(c.getTitle());
        String desc = c.getDescription();
        holder.binding.textSubtitle.setText(
                desc != null && !desc.isEmpty() ? desc : ctx.getString(R.string.c1_no_description));

        int pct = (int) (c.getProgress() * 100);
        holder.binding.textPercent.setText(pct + "%");
        holder.binding.progressBar.setProgress(pct);

        holder.binding.textDays.setText(ctx.getString(
                R.string.c1_days_progress, c.getCompletedDays(), c.getTotalDays()));
        holder.binding.textTarget.setText(MoneyFormat.formatLong(c.getTargetSavings()));

        boolean done = c.getTotalDays() > 0 && c.getCompletedDays() >= c.getTotalDays();
        holder.binding.btnMarkDay.setEnabled(!done);
        holder.binding.btnMarkDay.setText(done
                ? ctx.getString(R.string.c1_completed)
                : ctx.getString(R.string.c1_mark_day_done));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(c);
        });
        holder.binding.btnMarkDay.setOnClickListener(v -> {
            if (markListener != null && !done) markListener.onMark(c);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemChallengeBinding binding;
        VH(ItemChallengeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
