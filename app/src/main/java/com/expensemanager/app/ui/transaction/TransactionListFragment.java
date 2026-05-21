package com.expensemanager.app.ui.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.expensemanager.app.databinding.FragmentTransactionListBinding;
import com.expensemanager.app.data.model.Category;
import com.expensemanager.app.data.model.Transaction;
import com.expensemanager.app.data.repository.AuthRepository;
import com.expensemanager.app.data.repository.CategoryRepository;
import com.expensemanager.app.data.repository.TransactionRepository;
import com.expensemanager.app.ui.adapter.TransactionAdapter;
import com.expensemanager.app.util.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransactionListFragment extends Fragment {
    private FragmentTransactionListBinding binding;
    private final AuthRepository authRepo = new AuthRepository();
    private final TransactionRepository txRepo = new TransactionRepository();
    private final CategoryRepository catRepo = new CategoryRepository();
    private TransactionAdapter adapter;
    private List<Transaction> allMonth = new ArrayList<>();
    private Map<String, Category> catMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String uid = authRepo.getUid();
        if (uid == null) return;

        adapter = new TransactionAdapter();
        binding.recyclerTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTransactions.setAdapter(adapter);

        adapter.setOnItemClick(t -> {
            Intent i = new Intent(requireContext(), AddTransactionActivity.class);
            i.putExtra(AddTransactionActivity.EXTRA_TX_ID, t.getId());
            startActivity(i);
        });

        catRepo.observeAll(uid).observe(getViewLifecycleOwner(), cats -> {
            catMap = CategoryRepository.toMap(cats);
            adapter.setCategoryMap(catMap);
        });

        txRepo.observeMonth(uid, DateUtils.currentMonthKey())
                .observe(getViewLifecycleOwner(), list -> {
                    allMonth = list != null ? list : new ArrayList<>();
                    applyFilter();
                });

        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilter() {
        String q = binding.editSearch.getText() != null
                ? binding.editSearch.getText().toString() : "";
        List<Transaction> filtered = TransactionRepository.filterBySearch(allMonth, q);
        adapter.setItems(filtered);
        binding.textEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
