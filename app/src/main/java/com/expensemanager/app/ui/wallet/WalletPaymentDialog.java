package com.expensemanager.app.ui.wallet;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.expensemanager.app.R;
import com.expensemanager.app.data.model.Wallet;
import com.expensemanager.app.data.repository.WalletRepository;
import com.expensemanager.app.databinding.DialogWalletPaymentBinding;
import com.expensemanager.app.util.MoneyInputFormatter;
import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

public class WalletPaymentDialog extends Dialog {

    private final DialogWalletPaymentBinding binding;
    private final String uid;
    private final WalletRepository walletRepo = new WalletRepository();

    private String selectedType = "cash";
    private View selectedCard = null;

    // Type -> icon text mapping
    private static final Map<String, String> TYPE_ICONS = new HashMap<String, String>() {{
        put("viettel", "📱");
        put("momo", "💖");
        put("zalopay", "💚");
        put("vnpay", "💳");
        put("cash", "💵");
    }};

    // Type -> color hex
    private static final Map<String, String> TYPE_COLORS = new HashMap<String, String>() {{
        put("viettel", "#ED1B2E");
        put("momo", "#A61F69");
        put("zalopay", "#0068FF");
        put("vnpay", "#0057A8");
        put("cash", "#FFB84D");
    }};

    public WalletPaymentDialog(@NonNull Context context, String uid) {
        super(context);
        this.uid = uid;
        this.binding = DialogWalletPaymentBinding.inflate(LayoutInflater.from(context));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(binding.getRoot());

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        MoneyInputFormatter.attach(binding.editInitialBalance);
        setupTypeCards();
        binding.btnCreate.setOnClickListener(v -> createWallet());
    }

    private void setupTypeCards() {
        View[] cards = {
                binding.cardViettel,
                binding.cardMomo,
                binding.cardZaloPay,
                binding.cardVNPay,
                binding.cardCash
        };
        String[] types = {"viettel", "momo", "zalopay", "vnpay", "cash"};

        for (int i = 0; i < cards.length; i++) {
            final String type = types[i];
            final View card = cards[i];
            card.setOnClickListener(v -> selectCard(card, type, cards));
        }

        // Default: select cash
        selectCard(binding.cardCash, "cash", cards);
    }

    private void selectCard(View card, String type, View[] allCards) {
        selectedType = type;
        for (View c : allCards) {
            c.setSelected(false);
        }
        card.setSelected(true);
        selectedCard = card;
    }

    private void createWallet() {
        String name = binding.editWalletName.getText().toString().trim();
        String balanceStr = binding.editInitialBalance.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), getContext().getString(R.string.j2_enter_wallet_name), Toast.LENGTH_SHORT).show();
            return;
        }

        long balance = 0L;
        if (!balanceStr.isEmpty()) {
            long parsedBalance = MoneyInputFormatter.getRawValue(binding.editInitialBalance);
            if (parsedBalance <= 0) {
                Toast.makeText(getContext(), getContext().getString(R.string.error_invalid_amount), Toast.LENGTH_SHORT).show();
                return;
            }
            balance = parsedBalance;
        }

        Wallet wallet = new Wallet();
        wallet.setName(name);
        wallet.setType(selectedType);
        wallet.setInitialBalance(balance);
        wallet.setCurrentBalance(balance);
        wallet.setCreatedAt(Timestamp.now());
        wallet.setColor(TYPE_COLORS.get(selectedType));
        wallet.setIcon(TYPE_ICONS.get(selectedType));

        walletRepo.add(uid, wallet);
        Toast.makeText(getContext(), getContext().getString(R.string.j2_payment_wallet_created), Toast.LENGTH_SHORT).show();
        dismiss();
    }
}
