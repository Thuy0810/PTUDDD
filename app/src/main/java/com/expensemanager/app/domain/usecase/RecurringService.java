package com.expensemanager.app.domain.usecase;

import androidx.annotation.NonNull;

import com.expensemanager.app.data.repository.RecurringRepository;

/**
 * Service chạy các nghiệp vụ nền khi user vừa đăng nhập.
 *
 * <p>Tách riêng để Activity không gọi thẳng {@link RecurringRepository} (ràng buộc 4.1).
 */
public final class RecurringService {

    private final RecurringRepository repo;

    public RecurringService() {
        this(new RecurringRepository());
    }

    public RecurringService(@NonNull RecurringRepository repo) {
        this.repo = repo;
    }

    /**
     * Chạy tất cả rule định kỳ đến hạn cho user.
     */
    public void runOnLogin(@NonNull String uid) {
        repo.catchUp(uid);
    }
}
