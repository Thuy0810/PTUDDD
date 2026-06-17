package com.expensemanager.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.expensemanager.app.R;
import com.expensemanager.app.databinding.FragmentProfileBinding;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.ui.auth.LoginActivity;
import com.expensemanager.app.ui.recurring.RecurringListActivity;
import com.expensemanager.app.ui.wallet.WalletListActivity;
import com.expensemanager.app.ui.category.CategoryListActivity;
import com.expensemanager.app.ui.goal.GoalListActivity;
import com.expensemanager.app.util.PrefsHelper;
import com.expensemanager.app.viewmodel.HomeViewModelHolder;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private final AuthRepository authRepo = new AuthRepository();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepo.observeProfile().observe(getViewLifecycleOwner(), p -> {
            if (p != null && binding != null) {
                String name = p.getDisplayName();
                binding.textUserName.setText(name != null && !name.isEmpty() ? name : getString(R.string.user));
                String email = p.getEmail();
                binding.textUserEmail.setText(email != null ? email : "");
                if (name != null && !name.isEmpty()) {
                    binding.textAvatarInitial.setText(name.substring(0, 1).toUpperCase());
                }
            }
        });

        // Header edit profile button
        if (binding.btnEditProfile != null) {
            binding.btnEditProfile.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), EditProfileActivity.class)));
        }

        // Tài khoản
        if (binding.btnUser != null) {
            binding.btnUser.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), EditProfileActivity.class)));
        }
        if (binding.btnSecurity != null) {
            binding.btnSecurity.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), SecurityActivity.class)));
        }
        // Hàng "Cài đặt" -> mở màn Cài đặt (đổi ngôn ngữ nằm trong đó)
        if (binding.btnLanguage != null) {
            binding.btnLanguage.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), SettingsActivity.class)));
        }

        // Quản lý: Ngân sách & Thử thách tiết kiệm
        if (binding.btnBudgetShortcut != null) {
            binding.btnBudgetShortcut.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), com.expensemanager.app.ui.budget.BudgetListActivity.class)));
        }
        if (binding.btnChallenge != null) {
            binding.btnChallenge.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), com.expensemanager.app.ui.challenge.ChallengeListActivity.class)));
        }

        // Quản lý
        if (binding.btnManageWallets != null) {
            binding.btnManageWallets.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), WalletListActivity.class)));
        }
        if (binding.btnManageCategories != null) {
            binding.btnManageCategories.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), CategoryListActivity.class)));
        }
        if (binding.btnManageGoals != null) {
            binding.btnManageGoals.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), GoalListActivity.class)));
        }
        if (binding.btnManageRecurring != null) {
            binding.btnManageRecurring.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), RecurringListActivity.class)));
        }

        // Hỗ trợ
        if (binding.btnHelp != null) {
            binding.btnHelp.setOnClickListener(v ->
                    Toast.makeText(requireContext(), getString(R.string.help_center), Toast.LENGTH_SHORT).show());
        }
        if (binding.btnContact != null) {
            binding.btnContact.setOnClickListener(v ->
                    Toast.makeText(requireContext(), getString(R.string.j3_contact_support), Toast.LENGTH_SHORT).show());
        }

        // Đăng xuất (có xác nhận)
        if (binding.btnLogout != null) {
            binding.btnLogout.setOnClickListener(v ->
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.logout)
                            .setMessage(R.string.success_logout_confirm)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.logout, (d, w) -> doLogout())
                            .show());
        }
    }

    private void doLogout() {
        try {
            HomeViewModelHolder.clear();
            PrefsHelper.clearPendingLogout(requireContext());
            authRepo.logout();
            Intent i = new Intent(requireContext(), LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(requireContext(), getString(R.string.j3_logout_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
