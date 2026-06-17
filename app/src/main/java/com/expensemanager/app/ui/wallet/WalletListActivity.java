package com.expensemanager.app.ui.wallet;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.ui.adapter.WalletAdapter;
import com.expensemanager.app.util.MoneyFormat;
import com.expensemanager.app.util.MoneyInputFormatter;
import com.expensemanager.app.util.WalletIcons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalletListActivity extends AppCompatActivity {
    private final AuthRepository authRepo = new AuthRepository();
    private final WalletRepository walletRepo = new WalletRepository();
    private WalletAdapter adapter;
    private List<Transaction> allTx = new ArrayList<>();
    private List<Wallet> wallets = new ArrayList<>();
    private TextView textTotalBalance;

    // Loai vi: chi gom Vi thanh toan + The ghi no
    private static final String[] TYPE_KEYS = {"payment", "debit"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_list);
        ((android.widget.TextView) findViewById(R.id.textHeaderTitle)).setText(R.string.wallets);
        findViewById(R.id.btnHeaderBack).setOnClickListener(v -> finish());

        textTotalBalance = findViewById(R.id.textTotalBalance);
        RecyclerView rv = findViewById(R.id.recyclerWallets);
        adapter = new WalletAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        String uid = authRepo.getUid();
        if (uid == null) { finish(); return; }

        new TransactionRepository().observeAll(uid).observe(this, txs -> {
            allTx = txs != null ? txs : new ArrayList<>();
            adapter.setBalances(allTx);
            updateTotalBalance();
        });

        walletRepo.observeAll(uid).observe(this, ws -> {
            wallets = ws != null ? ws : new ArrayList<>();
            adapter.setWallets(wallets);
            updateTotalBalance();
        });

        adapter.setOnItemClick(w -> showEditDialog(uid, w));
        rv.setOnLongClickListener(v -> {
            showAddDialog(uid);
            return true;
        });

        findViewById(R.id.fabAdd).setOnClickListener(v -> showAddDialog(uid));
    }

    /** Bộ chọn icon ví (vector từ thư viện); lưu KHÓA icon đang chọn vào selectedHolder. */
    private void setupIconPicker(LinearLayout container, String[] selectedHolder) {
        container.removeAllViews();
        float d = getResources().getDisplayMetrics().density;
        int sizePx = (int) (44 * d);
        int padPx = (int) (10 * d);
        int marginPx = (int) (6 * d);
        if (selectedHolder[0] == null || WalletIcons.drawableFor(selectedHolder[0]) == 0) {
            selectedHolder[0] = WalletIcons.PICKER_KEYS[0];
        }
        final int selColor = getColor(R.color.brand_blue);
        final int normColor = getColor(R.color.text_secondary);
        for (String key : WalletIcons.PICKER_KEYS) {
            ImageView iv = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
            lp.setMargins(0, 0, marginPx, 0);
            iv.setLayoutParams(lp);
            iv.setPadding(padPx, padPx, padPx, padPx);
            iv.setImageResource(WalletIcons.drawableFor(key));
            iv.setBackgroundResource(R.drawable.bg_icon_choice);
            boolean sel = key.equals(selectedHolder[0]);
            iv.setSelected(sel);
            iv.setColorFilter(sel ? selColor : normColor);
            iv.setOnClickListener(v -> {
                selectedHolder[0] = key;
                for (int i = 0; i < container.getChildCount(); i++) {
                    View c = container.getChildAt(i);
                    c.setSelected(false);
                    if (c instanceof ImageView) ((ImageView) c).setColorFilter(normColor);
                }
                v.setSelected(true);
                ((ImageView) v).setColorFilter(selColor);
            });
            container.addView(iv);
        }
    }

    private void updateTotalBalance() {
        long total = 0L;
        for (Wallet w : wallets) {
            total += w.getCurrentBalance();
        }
        textTotalBalance.setText(MoneyFormat.formatLong(total));
    }

    private void showAddDialog(String uid) {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.dialog_wallet, null);
        EditText editName = view.findViewById(R.id.editWalletName);
        EditText editBalance = view.findViewById(R.id.editWalletBalance);
        Spinner spinnerType = view.findViewById(R.id.spinnerWalletType);
        LinearLayout iconPicker = view.findViewById(R.id.layoutIconPicker);
        MoneyInputFormatter.attach(editBalance);

        String[] types = getResources().getStringArray(R.array.wallet_type_labels);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        final String[] selectedIcon = {WalletIcons.PICKER_KEYS[0]};
        setupIconPicker(iconPicker, selectedIcon);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
                .create();

        view.findViewById(R.id.btnCreate).setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.j2_enter_wallet_name), Toast.LENGTH_SHORT).show();
                return;
            }
            int pos = spinnerType.getSelectedItemPosition();
            String typeKey = TYPE_KEYS[pos];
            long balance = MoneyInputFormatter.getRawValue(editBalance);
            Wallet wallet = new Wallet(null, name, typeKey, balance);
            wallet.setCurrentBalance(balance);
            wallet.setIcon(selectedIcon[0]);
            view.findViewById(R.id.btnCreate).setEnabled(false);
            walletRepo.add(uid, wallet)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, getString(R.string.j2_wallet_added), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        view.findViewById(R.id.btnCreate).setEnabled(true);
                        Toast.makeText(this, getString(R.string.j2_cannot_add, e.getMessage()), Toast.LENGTH_LONG).show();
                    });
        });
        dialog.show();
    }

    private void showEditDialog(String uid, Wallet wallet) {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.dialog_wallet, null);
        EditText editName = view.findViewById(R.id.editWalletName);
        EditText editBalance = view.findViewById(R.id.editWalletBalance);
        Spinner spinnerType = view.findViewById(R.id.spinnerWalletType);
        LinearLayout iconPicker = view.findViewById(R.id.layoutIconPicker);
        MoneyInputFormatter.attach(editBalance);

        editName.setText(wallet.getName());
        // Cho phép sửa thẳng số dư hiện tại (không ghi log điều chỉnh).
        editBalance.setText(MoneyInputFormatter.format(wallet.getCurrentBalance()));
        editBalance.setVisibility(View.VISIBLE);
        try {
            ((com.google.android.material.textfield.TextInputLayout)
                    editBalance.getParent().getParent())
                    .setHint(getString(R.string.current_balance));
        } catch (Exception ignored) { }
        view.<com.google.android.material.button.MaterialButton>findViewById(R.id.btnCreate).setText(getString(R.string.save));

        String[] types = getResources().getStringArray(R.array.wallet_type_labels);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        for (int i = 0; i < TYPE_KEYS.length; i++) {
            if (TYPE_KEYS[i].equals(wallet.getType())) {
                spinnerType.setSelection(i);
                break;
            }
        }

        final String[] selectedIcon = {wallet.getIcon()};
        setupIconPicker(iconPicker, selectedIcon);

        final AlertDialog[] dialogRef = new AlertDialog[1];
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.edit_wallet)
                .setView(view)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    // Nếu ví đang có giao dịch thì chỉ cho phép archive thay vì xoá (ràng buộc 6).
                    walletRepo.countTransactions(uid, wallet.getId())
                            .addOnSuccessListener(count -> {
                                if (count > 0L) {
                                    showArchiveConfirm(uid, wallet, dialogRef[0]);
                                } else {
                                    showDeleteConfirm(uid, wallet);
                                }
                            })
                            .addOnFailureListener(e -> showDeleteConfirm(uid, wallet));
                })
                .setNeutralButton(R.string.transfer_money, (d, w) -> showTransferDialog(uid, wallet))
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialogRef[0] = dialog;

        view.findViewById(R.id.btnCreate).setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.j2_enter_wallet_name), Toast.LENGTH_SHORT).show();
                return;
            }
            int pos = spinnerType.getSelectedItemPosition();
            wallet.setName(name);
            wallet.setType(TYPE_KEYS[pos]);
            wallet.setIcon(selectedIcon[0]);
            wallet.setCurrentBalance(MoneyInputFormatter.getRawValue(editBalance));
            view.findViewById(R.id.btnCreate).setEnabled(false);
            walletRepo.update(uid, wallet)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, getString(R.string.j2_wallet_updated), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        view.findViewById(R.id.btnCreate).setEnabled(true);
                        Toast.makeText(this, getString(R.string.j2_cannot_save, e.getMessage()), Toast.LENGTH_LONG).show();
                    });
        });

        dialog.show();
    }

    /** Chuyển tiền giữa hai ví (atomic qua WalletRepository.transfer). */
    private void showTransferDialog(String uid, Wallet fromWallet) {
        // Chỉ chuyển giữa các ví chưa lưu trữ.
        List<Wallet> active = new ArrayList<>();
        for (Wallet w : wallets) {
            if (!w.isArchived()) active.add(w);
        }
        if (active.size() < 2) {
            Toast.makeText(this, getString(R.string.need_two_wallets), Toast.LENGTH_SHORT).show();
            return;
        }

        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, pad / 2, pad, 0);

        List<String> names = new ArrayList<>();
        for (Wallet w : active) {
            names.add(w.getName() + " (" + MoneyFormat.formatLong(w.getCurrentBalance()) + ")");
        }

        TextView lblFrom = new TextView(this);
        lblFrom.setText(getString(R.string.transfer_from));
        lblFrom.setTextColor(getColor(R.color.text_secondary));
        lblFrom.setTextSize(13);
        container.addView(lblFrom);
        Spinner spFrom = new Spinner(this);
        ArrayAdapter<String> aFrom = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        aFrom.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spFrom.setAdapter(aFrom);
        container.addView(spFrom);

        TextView lblTo = new TextView(this);
        lblTo.setText(getString(R.string.transfer_to));
        lblTo.setTextColor(getColor(R.color.text_secondary));
        lblTo.setTextSize(13);
        lblTo.setPadding(0, (int) (12 * d), 0, 0);
        container.addView(lblTo);
        Spinner spTo = new Spinner(this);
        ArrayAdapter<String> aTo = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<>(names));
        aTo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTo.setAdapter(aTo);
        container.addView(spTo);

        TextView lblAmt = new TextView(this);
        lblAmt.setText(getString(R.string.transfer_amount));
        lblAmt.setTextColor(getColor(R.color.text_secondary));
        lblAmt.setTextSize(13);
        lblAmt.setPadding(0, (int) (12 * d), 0, 0);
        container.addView(lblAmt);
        EditText editAmount = new EditText(this);
        editAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        MoneyInputFormatter.attach(editAmount);
        container.addView(editAmount);

        // Mặc định: ví nguồn = ví đang sửa, ví đích = ví khác đầu tiên.
        int fromIdx = 0;
        for (int i = 0; i < active.size(); i++) {
            if (active.get(i).getId() != null && active.get(i).getId().equals(fromWallet.getId())) {
                fromIdx = i;
                break;
            }
        }
        spFrom.setSelection(fromIdx);
        spTo.setSelection(fromIdx == 0 ? 1 : 0);

        new AlertDialog.Builder(this)
                .setTitle(R.string.transfer_between_wallets)
                .setView(container)
                .setPositiveButton(R.string.transfer_money, (dl, wl) -> {
                    int fp = spFrom.getSelectedItemPosition();
                    int tp = spTo.getSelectedItemPosition();
                    Wallet from = active.get(fp);
                    Wallet to = active.get(tp);
                    if (from.getId() != null && from.getId().equals(to.getId())) {
                        Toast.makeText(this, getString(R.string.j2_select_two_diff_wallets),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    long amount = MoneyInputFormatter.getRawValue(editAmount);
                    if (amount <= 0) {
                        Toast.makeText(this, getString(R.string.j2_enter_amount_gt_zero),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (from.getCurrentBalance() < amount) {
                        Toast.makeText(this,
                                getString(R.string.j2_warn_insufficient_balance,
                                        MoneyFormat.formatLong(from.getCurrentBalance())),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    walletRepo.transfer(uid, from.getId(), to.getId(), amount)
                            .addOnSuccessListener(unused -> Toast.makeText(this,
                                    getString(R.string.transfer_success, MoneyFormat.formatLong(amount)),
                                    Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    getString(R.string.transfer_failed, e.getMessage()),
                                    Toast.LENGTH_LONG).show());
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showArchiveConfirm(String uid, Wallet wallet, AlertDialog parent) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j2_archive_wallet_title))
                .setMessage(getString(R.string.j2_archive_wallet_message, wallet.getName()))
                .setPositiveButton(getString(R.string.j2_archive), (dl, wl) -> {
                    walletRepo.archive(uid, wallet.getId())
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, getString(R.string.j2_wallet_archived), Toast.LENGTH_SHORT).show();
                                parent.dismiss();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            getString(R.string.j2_error_generic, e.getMessage()),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showDeleteConfirm(String uid, Wallet wallet) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.j2_delete_wallet_title))
                .setMessage(getString(R.string.j2_delete_wallet_message, wallet.getName()))
                .setPositiveButton(getString(R.string.delete), (dl, wl) -> {
                    walletRepo.delete(uid, wallet.getId())
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this, getString(R.string.j2_wallet_deleted), Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            getString(R.string.j2_cannot_delete, e.getMessage()),
                                            Toast.LENGTH_LONG).show());
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
