package com.expensemanager.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.expensemanager.app.databinding.FragmentProfileBinding;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.ui.auth.LoginActivity;
import com.expensemanager.app.ui.budget.BudgetListActivity;
import com.expensemanager.app.ui.category.CategoryListActivity;
import com.expensemanager.app.ui.challenge.ChallengeListActivity;
import com.expensemanager.app.ui.goal.GoalListActivity;
import com.expensemanager.app.ui.wallet.WalletListActivity;
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
            if (p != null) {
                binding.textUserName.setText(p.getDisplayName() != null ? p.getDisplayName() : "User");
                binding.textUserEmail.setText(p.getEmail());
                if (p.getDisplayName() != null && !p.getDisplayName().isEmpty()) {
                    binding.textAvatarInitial.setText(
                            p.getDisplayName().substring(0, 1).toUpperCase());
                }
            }
        });

        binding.btnWallets.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), WalletListActivity.class)));
        binding.btnCategories.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CategoryListActivity.class)));
        binding.btnBudgets.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), BudgetListActivity.class)));
        binding.btnGoals.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), GoalListActivity.class)));
        binding.btnChallenges.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChallengeListActivity.class)));
        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));
        binding.textUserName.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        binding.btnLogout.setOnClickListener(v -> {
            HomeViewModelHolder.clear();
            authRepo.logout();
            Intent i = new Intent(requireContext(), LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
