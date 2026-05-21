package com.expensemanager.app.ui.category;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.RadioGroup;
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
import java.util.List;

public class CategoryListActivity extends AppCompatActivity {
    private final AuthRepository authRepo = new AuthRepository();
    private final CategoryRepository catRepo = new CategoryRepository();
    private CategoryAdapter adapter;
    private ActivityCategoryListBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCategoryListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.categories);
        }

        RecyclerView rv = findViewById(R.id.recyclerCategories);
        adapter = new CategoryAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        String uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        catRepo.observeAll(uid).observe(this, list -> {
            adapter.setItems(list != null ? list : new ArrayList<>());
        });

        adapter.setOnItemLongClick(c -> {
            if (c.isSystem()) {
                Toast.makeText(this, "Không xóa danh mục hệ thống", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("Xóa danh mục?")
                    .setMessage("Xóa \"" + c.getName() + "\"?")
                    .setPositiveButton("Xóa", (d, w) -> {
                        catRepo.delete(uid, c.getId());
                        Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        findViewById(R.id.fabAdd).setOnClickListener(v -> showAddDialog(uid));
    }

    private void showAddDialog(String uid) {
        EditText editName = new EditText(this);
        editName.setHint("Tên danh mục");
        RadioGroup radioType = new RadioGroup(this);
        radioType.setOrientation(RadioGroup.HORIZONTAL);
        android.widget.RadioButton rbExpense = new android.widget.RadioButton(this);
        rbExpense.setText("Chi tiêu");
        rbExpense.setChecked(true);
        android.widget.RadioButton rbIncome = new android.widget.RadioButton(this);
        rbIncome.setText("Thu nhập");
        radioType.addView(rbExpense);
        radioType.addView(rbIncome);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 16, 48, 0);
        layout.addView(editName);
        layout.addView(radioType);

        new AlertDialog.Builder(this)
                .setTitle("Danh mục mới")
                .setView(layout)
                .setPositiveButton("Tạo", (d, w) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Nhập tên danh mục", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String type = rbIncome.isChecked() ? Category.TYPE_INCOME : Category.TYPE_EXPENSE;
                    Category c = new Category(null, name, type, "other", "#6B7280", false);
                    catRepo.add(uid, c);
                    Toast.makeText(this, "Đã tạo danh mục", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
