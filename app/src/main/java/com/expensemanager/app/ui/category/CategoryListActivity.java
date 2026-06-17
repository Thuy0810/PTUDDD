package com.expensemanager.app.ui.category;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.databinding.ActivityCategoryListBinding;
import com.expensemanager.app.ui.adapter.CategoryAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryListActivity extends AppCompatActivity {
    private final AuthRepository authRepo = new AuthRepository();
    private final CategoryRepository catRepo = new CategoryRepository();
    private CategoryAdapter adapter;
    private ActivityCategoryListBinding binding;
    private List<Category> allCategories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ((android.widget.TextView) findViewById(R.id.textHeaderTitle)).setText(R.string.categories);
        findViewById(R.id.btnHeaderBack).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.recyclerCategories);
        adapter = new CategoryAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        String uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        catRepo.observeAll(uid).observe(this, list -> {
            allCategories = list != null ? list : new ArrayList<>();
            Map<String, String> parentNames = new HashMap<>();
            for (Category c : allCategories) {
                if (c.getId() != null) parentNames.put(c.getId(), c.getName());
            }
            adapter.setParentNames(parentNames);
            adapter.setItems(allCategories);
        });

        adapter.setOnItemLongClick(c -> {
            if (c.isSystem()) {
                Toast.makeText(this, getString(R.string.j2_cannot_delete_system_category), Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.j2_delete_category_title))
                    .setMessage(getString(R.string.j2_delete_quoted, c.getName()))
                    .setPositiveButton(getString(R.string.delete), (d, w) -> {
                        catRepo.delete(uid, c.getId());
                        Toast.makeText(this, getString(R.string.success_delete), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        });

        adapter.setOnItemClick(c -> showEditDialog(uid, c));

        findViewById(R.id.fabAdd).setOnClickListener(v -> showAddDialog(uid));
    }

    /** Sửa danh mục: đổi tên + icon (giữ nguyên loại, nhóm, danh mục cha). */
    private void showEditDialog(String uid, Category c) {
        EditText editName = new EditText(this);
        editName.setHint(getString(R.string.category_name));
        editName.setText(c.getName());

        TextView labelIcon = new TextView(this);
        labelIcon.setText(getString(R.string.category_icon));
        labelIcon.setPadding(0, 24, 0, 0);
        final String[] iconHolder = { c.getIconKey() != null ? c.getIconKey() : "other" };
        View iconPicker = com.expensemanager.app.util.CategoryIconPicker.createScroller(this, iconHolder);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 0);
        layout.addView(editName);
        layout.addView(labelIcon);
        layout.addView(iconPicker);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.edit_category))
                .setView(layout)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, getString(R.string.j2_enter_category_name),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    c.setName(name);
                    c.setIconKey(iconHolder[0]);
                    catRepo.update(uid, c);
                    Toast.makeText(this, getString(R.string.j3_updated), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showAddDialog(String uid) {
        EditText editName = new EditText(this);
        editName.setHint(getString(R.string.category_name));
        RadioGroup radioType = new RadioGroup(this);
        radioType.setOrientation(RadioGroup.HORIZONTAL);
        android.widget.RadioButton rbExpense = new android.widget.RadioButton(this);
        rbExpense.setText(getString(R.string.expense));
        rbExpense.setChecked(true);
        android.widget.RadioButton rbIncome = new android.widget.RadioButton(this);
        rbIncome.setText(getString(R.string.income));
        radioType.addView(rbExpense);
        radioType.addView(rbIncome);

        TextView labelParent = new TextView(this);
        labelParent.setText(getString(R.string.parent_category));
        labelParent.setPadding(0, 24, 0, 0);

        Spinner spinnerParent = new Spinner(this);
        // Danh sách cha song song với spinner: phần tử 0 = "không có cha" (null).
        final List<Category> parentCandidates = new ArrayList<>();
        Runnable refreshParents = () -> {
            String type = rbIncome.isChecked() ? Category.TYPE_INCOME : Category.TYPE_EXPENSE;
            parentCandidates.clear();
            parentCandidates.add(null);
            List<String> labels = new ArrayList<>();
            labels.add(getString(R.string.no_parent_category));
            for (Category c : allCategories) {
                // Chỉ danh mục gốc cùng loại mới được làm cha (cây 2 cấp).
                if (type.equals(c.getType()) && !c.isSubcategory()) {
                    parentCandidates.add(c);
                    labels.add(c.getName());
                }
            }
            ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, labels);
            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerParent.setAdapter(ad);
        };
        refreshParents.run();
        radioType.setOnCheckedChangeListener((g, id) -> refreshParents.run());

        TextView labelIcon = new TextView(this);
        labelIcon.setText(getString(R.string.category_icon));
        labelIcon.setPadding(0, 24, 0, 0);
        final String[] iconHolder = {"other"};
        View iconPicker = com.expensemanager.app.util.CategoryIconPicker.createScroller(this, iconHolder);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 0);
        layout.addView(editName);
        layout.addView(radioType);
        layout.addView(labelIcon);
        layout.addView(iconPicker);
        layout.addView(labelParent);
        layout.addView(spinnerParent);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j2_new_category_title))
                .setView(layout)
                .setPositiveButton(getString(R.string.create), (d, w) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, getString(R.string.j2_enter_category_name), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String type = rbIncome.isChecked() ? Category.TYPE_INCOME : Category.TYPE_EXPENSE;
                    Category c = new Category(null, name, type, iconHolder[0], "#6B7280", false);
                    int pos = spinnerParent.getSelectedItemPosition();
                    if (pos > 0 && pos < parentCandidates.size()) {
                        Category parent = parentCandidates.get(pos);
                        if (parent != null) c.setParentId(parent.getId());
                    }
                    catRepo.add(uid, c);
                    Toast.makeText(this, getString(R.string.j2_category_created), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
