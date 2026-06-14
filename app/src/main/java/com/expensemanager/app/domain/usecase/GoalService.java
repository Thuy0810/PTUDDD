package com.expensemanager.app.domain.usecase;

import androidx.annotation.NonNull;

import com.expensemanager.app.data.repository.GoalRepository;
import com.google.android.gms.tasks.Task;

/**
 * Nghiệp vụ mục tiêu tiết kiệm.
 *
 * <p>Bao gồm {@link #contributeToGoal(String, String, long, String)} dùng
 * {@link GoalRepository#addContribution(String, String, long, String)} để đảm bảo atomic:
 * trừ ví + cộng savedAmount của Goal + ghi log.
 *
 * <p>UI không được tự tính & cập nhật currentBalance (ràng buộc 4.1, 8).
 */
public final class GoalService {

    private final GoalRepository goalRepo;

    public GoalService() {
        this(new GoalRepository());
    }

    public GoalService(@NonNull GoalRepository goalRepo) {
        this.goalRepo = goalRepo;
    }

    @NonNull
    public Task<Void> contributeToGoal(@NonNull String uid, @NonNull String goalId,
                                         long amount, @NonNull String walletId) {
        return goalRepo.addContribution(uid, goalId, amount, walletId);
    }
}
