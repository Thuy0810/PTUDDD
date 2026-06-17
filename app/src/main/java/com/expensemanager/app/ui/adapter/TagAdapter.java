package com.expensemanager.app.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.data.model.Tag;
import com.expensemanager.app.databinding.ItemTagBinding;

import java.util.ArrayList;
import java.util.List;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.VH> {
    public interface OnTagAction {
        void onEdit(Tag t);
        void onDelete(Tag t);
    }

    private List<Tag> items = new ArrayList<>();
    private OnTagAction listener;

    public void setItems(List<Tag> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnTagAction(OnTagAction listener) { this.listener = listener; }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemTagBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Tag t = items.get(position);
        holder.binding.textTagName.setText(t.getName());

        try {
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(Color.parseColor(t.getColorHex()));
            holder.binding.viewTagColor.setBackground(bg);
        } catch (Exception ignored) {}

        holder.binding.btnEditTag.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(t);
        });
        holder.binding.btnDeleteTag.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(t);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemTagBinding binding;
        VH(ItemTagBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
