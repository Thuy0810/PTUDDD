package com.expensemanager.app.ui.tag;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Tag;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.TagRepository;
import com.expensemanager.app.databinding.ActivityTagListBinding;
import com.expensemanager.app.ui.adapter.TagAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TagListActivity extends AppCompatActivity {
    private final AuthRepository authRepo = new AuthRepository();
    private final TagRepository tagRepo = new TagRepository();
    private ActivityTagListBinding binding;
    private TagAdapter adapter;
    private String uid;

    private static final String[] COLOR_HEX = {
            "#F97316", "#3B82F6", "#EC4899", "#8B5CF6", "#14B8A6",
            "#22C55E", "#EAB308", "#EF4444", "#6366F1", "#6B7280"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTagListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.manage_tags);
        }

        uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        adapter = new TagAdapter();
        binding.recyclerTags.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerTags.setAdapter(adapter);

        tagRepo.observeAll(uid).observe(this, list -> {
            List<Tag> tags = list != null ? list : new ArrayList<>();
            adapter.setItems(tags);
            binding.textEmptyTags.setVisibility(tags.isEmpty() ? View.VISIBLE : View.GONE);
        });

        adapter.setOnTagAction(new TagAdapter.OnTagAction() {
            @Override public void onEdit(Tag t) { showEditDialog(t); }
            @Override public void onDelete(Tag t) { confirmDelete(t); }
        });

        binding.fabAddTag.setOnClickListener(v -> showEditDialog(null));
    }

    private void showEditDialog(Tag existing) {
        EditText editName = new EditText(this);
        editName.setHint(getString(R.string.tag_name));
        if (existing != null) editName.setText(existing.getName());

        TextView labelColor = new TextView(this);
        labelColor.setText(getString(R.string.tag_color));
        labelColor.setPadding(0, 24, 0, 0);

        Spinner spinnerColor = new Spinner(this);
        List<String> colorLabels = new ArrayList<>(Arrays.asList(COLOR_HEX));
        ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, colorLabels);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerColor.setAdapter(ad);
        if (existing != null) {
            int idx = colorLabels.indexOf(existing.getColorHex());
            if (idx >= 0) spinnerColor.setSelection(idx);
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 0);
        layout.addView(editName);
        layout.addView(labelColor);
        layout.addView(spinnerColor);

        int titleRes = existing == null ? R.string.add_tag : R.string.edit_tag;
        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setView(layout)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, getString(R.string.tag_enter_name), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String color = COLOR_HEX[spinnerColor.getSelectedItemPosition()];
                    if (existing == null) {
                        tagRepo.add(uid, new Tag(null, name, color));
                    } else {
                        existing.setName(name);
                        existing.setColorHex(color);
                        tagRepo.update(uid, existing);
                    }
                    Toast.makeText(this, getString(R.string.success_saved), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmDelete(Tag t) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_tag_title)
                .setMessage(getString(R.string.j2_delete_quoted, t.getName()))
                .setPositiveButton(R.string.delete, (d, w) -> {
                    tagRepo.delete(uid, t.getId());
                    Toast.makeText(this, getString(R.string.success_delete), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
