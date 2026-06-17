package com.expensemanager.app.ui.transaction;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.ActivityQuickAddBinding;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.util.CategorySuggester;
import com.expensemanager.app.util.MoneyInputFormatter;
import com.expensemanager.app.util.QuickParseUtil;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Màn hình "Nhập nhanh": người dùng gõ một câu (vd "cà phê 25k hôm qua"),
 * hệ thống tự tách số tiền / ngày / ghi chú và phân loại danh mục bằng
 * {@link CategorySuggester}. Mọi thứ hiển thị ở khung xem trước để người dùng
 * kiểm tra & chỉnh trước khi lưu.
 */
public class QuickAddActivity extends AppCompatActivity {

    private ActivityQuickAddBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private final TransactionRepository txRepo = new TransactionRepository();

    private List<Category> categories = new ArrayList<>();
    private List<Wallet> wallets = new ArrayList<>();
    private boolean categoriesLoaded = false;
    private boolean walletsLoaded = false;

    private Timestamp parsedDate = null;
    /** Cho phép người dùng đổi loại thủ công mà không bị AI ghi đè ngay. */
    private boolean userChangedType = false;
    /** True khi đang đổi loại bằng code (AI), để không tính là người dùng đổi. */
    private boolean programmaticTypeChange = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuickAddBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ((android.widget.TextView) findViewById(R.id.textHeaderTitle)).setText(R.string.quick_add_title);
        findViewById(R.id.btnHeaderBack).setOnClickListener(v -> finish());

        MoneyInputFormatter.attach(binding.editAmount);
        binding.textDate.setText(QuickParseUtil.formatDate(new Date()));

        String uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        new CategoryRepository().observeAll(uid).observe(this, list -> {
            categories = list != null ? list : new ArrayList<>();
            categoriesLoaded = true;
            setupCategorySpinner(currentType());
            reanalyzeIfNeeded();
        });
        new WalletRepository().observeAll(uid).observe(this, list -> {
            wallets = list != null ? list : new ArrayList<>();
            walletsLoaded = true;
            setupWalletSpinner();
            reanalyzeIfNeeded();
        });

        binding.radioType.setOnCheckedChangeListener((g, id) -> {
            if (!programmaticTypeChange) {
                userChangedType = true;
            }
            setupCategorySpinner(currentType());
        });

        // Phân tích khi rời ô nhập (tránh can thiệp lúc gõ tiếng Việt có dấu).
        binding.editQuick.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) analyze();
        });

        binding.btnSave.setOnClickListener(v -> save());
        binding.btnCancel.setOnClickListener(v -> finish());
    }

    private String currentType() {
        return binding.radioIncome.isChecked()
                ? Transaction.TYPE_INCOME : Transaction.TYPE_EXPENSE;
    }

    private void reanalyzeIfNeeded() {
        // Không phân tích khi người dùng đang gõ (ô đang focus). Listener realtime
        // của Firestore có thể nổ giữa lúc gõ, làm analyze() rebuild spinner/đổi
        // radio và reset vùng "compose" của bộ gõ -> mất chữ khi gõ tiếng Việt có dấu.
        // Khi rời ô, onFocusChange sẽ tự gọi analyze() nên không bỏ sót.
        if (binding.editQuick.hasFocus()) return;
        if (binding.editQuick.getText() != null
                && binding.editQuick.getText().length() > 0) {
            analyze();
        }
    }

    /** Phân tích câu nhập nhanh và đổ vào khung xem trước. */
    private void analyze() {
        String text = binding.editQuick.getText() != null
                ? binding.editQuick.getText().toString() : "";
        if (text.trim().isEmpty()) {
            binding.textDetected.setText(getString(R.string.quick_add_detected));
            return;
        }

        QuickParseUtil.ParseResult r = QuickParseUtil.parse(text);
        CategorySuggester.Suggestion s = CategorySuggester.classify(text);

        // Loại giao dịch (chỉ tự đặt khi người dùng chưa tự đổi)
        if (!userChangedType && s.isConfident()) {
            programmaticTypeChange = true;
            if (Transaction.TYPE_INCOME.equals(s.type) && !binding.radioIncome.isChecked()) {
                binding.radioIncome.setChecked(true); // kéo theo rebuild spinner
            } else if (Transaction.TYPE_EXPENSE.equals(s.type) && !binding.radioExpense.isChecked()) {
                binding.radioExpense.setChecked(true);
            }
            programmaticTypeChange = false;
        }

        // Số tiền
        if (r.amount != null) {
            binding.editAmount.setText(MoneyInputFormatter.format(r.amount.longValue()));
        }

        // Ghi chú
        if (r.note != null && !r.note.isEmpty()) {
            binding.editNote.setText(r.note);
        }

        // Ngày
        if (r.date != null && !r.date.after(new Date())) {
            parsedDate = new Timestamp(r.date);
            binding.textDate.setText(QuickParseUtil.formatDate(r.date));
        } else {
            parsedDate = null;
            binding.textDate.setText(QuickParseUtil.formatDate(new Date()));
        }

        // Danh mục + thông báo nhận diện
        if (s.isConfident()) {
            selectCategory(s.categoryId);
            String name = categoryName(s.categoryId);
            binding.textDetected.setText(getString(R.string.quick_add_detected_value,
                    name != null ? name : s.categoryId));
        } else {
            binding.textDetected.setText(getString(R.string.quick_add_not_detected));
        }
    }

    private String categoryName(String catId) {
        for (Category c : categories) {
            if (catId != null && catId.equals(c.getId())) return c.getName();
        }
        return null;
    }

    private void setupCategorySpinner(String type) {
        List<Category> filtered = new ArrayList<>();
        for (Category c : categories) {
            if (type.equals(c.getType())) filtered.add(c);
        }
        ArrayAdapter<Category> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filtered);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(adapter);
    }

    private void setupWalletSpinner() {
        ArrayAdapter<Wallet> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, wallets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerWallet.setAdapter(adapter);
    }

    private void selectCategory(String catId) {
        ArrayAdapter adapter = (ArrayAdapter) binding.spinnerCategory.getAdapter();
        if (adapter == null || catId == null) return;
        for (int i = 0; i < adapter.getCount(); i++) {
            Category c = (Category) adapter.getItem(i);
            if (c != null && catId.equals(c.getId())) {
                binding.spinnerCategory.setSelection(i);
                break;
            }
        }
    }

    private void save() {
        String uid = authRepo.getUid();
        if (uid == null) return;

        // Nếu người dùng gõ câu nhưng chưa rời ô (chưa phân tích), phân tích trước khi lưu.
        if (MoneyInputFormatter.getRawValue(binding.editAmount) <= 0
                && binding.editQuick.getText() != null
                && binding.editQuick.getText().toString().trim().length() > 0) {
            analyze();
        }

        long amount = MoneyInputFormatter.getRawValue(binding.editAmount);
        if (amount <= 0) {
            Toast.makeText(this, getString(R.string.j2_enter_amount_gt_zero), Toast.LENGTH_SHORT).show();
            return;
        }

        Category cat = (Category) binding.spinnerCategory.getSelectedItem();
        Wallet wallet = (Wallet) binding.spinnerWallet.getSelectedItem();
        if (cat == null) {
            Toast.makeText(this, getString(R.string.error_select_category), Toast.LENGTH_SHORT).show();
            return;
        }
        if (wallet == null) {
            Toast.makeText(this, getString(R.string.error_select_wallet), Toast.LENGTH_SHORT).show();
            return;
        }

        String type = currentType();
        Transaction t = new Transaction();
        t.setType(type);
        t.setAmount(amount);
        t.setCategoryId(cat.getId());
        t.setWalletId(wallet.getId());
        t.setNote(binding.editNote.getText() != null
                ? binding.editNote.getText().toString() : "");
        t.setDate(parsedDate != null ? parsedDate : Timestamp.now());

        binding.btnSave.setEnabled(false);
        txRepo.addAtomic(uid, t, wallet.getId())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, getString(R.string.success_saved), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnSave.setEnabled(true);
                    Toast.makeText(this, getString(R.string.j2_cannot_save, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
