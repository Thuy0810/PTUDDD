package com.expensemanager.app.ui.state;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

/**
 * LiveData wrapper chuyển đổi Firestore {@code Task} thành {@link UiState}.
 *
 * <p>Usage:
 * <pre>{@code
 * ResourceLiveData.of(task)
 *     .observeForever(state -> { ... });
 * }</pre>
 *
 * <p>Hoặc kết hợp với repository:
 * <pre>{@code
 * MediatorLiveData<UiState<List<T>>> merged = new MediatorLiveData<>();
 * merged.addSource(source1, data -> merged.setValue(UiState.success(data)));
 * merged.addSource(source2, error -> merged.setValue(UiState.error(error)));
 * return merged;
 * }</pre>
 *
 * <p>Phân biệt loại lỗi Firestore cho user:
 * <ul>
 *   <li>{@code PERMISSION_DENIED} → "Không có quyền truy cập dữ liệu"</li>
 *   <li>{@code UNAVAILABLE} → "Không có kết nối mạng"</li>
 *   <li>{@code INVALID_ARGUMENT} → "Dữ liệu không hợp lệ"</li>
 *   <li>{@code FAILED_PRECONDITION} → "Thao tác không hợp lệ"</li>
 *   <li>{@code UNKNOWN} hoặc khác → "Đã xảy ra lỗi không xác định"</li>
 * </ul>
 */
public final class ResourceLiveData<T> extends MediatorLiveData<UiState<T>> {

    private ResourceLiveData() {}

    /**
     * Tạo LiveData từ một Firestore Task.
     * Chuyển đổi các loại lỗi Firebase thành thông báo thân thiện với người dùng.
     */
    @NonNull
    public static <T> LiveData<UiState<T>> of(@NonNull com.google.android.gms.tasks.Task<T> task) {
        ResourceLiveData<T> live = new ResourceLiveData<>();
        live.setValue(UiState.loading());
        task.addOnSuccessListener(data -> live.postValue(UiState.success(data)));
        task.addOnFailureListener(e -> live.postValue(UiState.error(mapError(e))));
        return live;
    }

    /**
     * Chuyển Firestore exception thành thông báo người dùng.
     */
    @NonNull
    public static String mapError(@NonNull Exception e) {
        if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
            com.google.firebase.firestore.FirebaseFirestoreException.Code code =
                    ((com.google.firebase.firestore.FirebaseFirestoreException) e).getCode();
            switch (code) {
                case PERMISSION_DENIED:
                    return "Không có quyền truy cập dữ liệu. Vui lòng đăng nhập lại.";
                case UNAVAILABLE:
                    return "Không có kết nối mạng. Kiểm tra Wi-Fi hoặc dữ liệu di động.";
                case INVALID_ARGUMENT:
                    return "Dữ liệu không hợp lệ. Vui lòng thử lại.";
                case FAILED_PRECONDITION:
                    return "Thao tác không hợp lệ. Vui lòng kiểm tra lại thông tin.";
                case DEADLINE_EXCEEDED:
                    return "Yêu cầu hết thời gian. Vui lòng thử lại.";
                case NOT_FOUND:
                    return "Dữ liệu không tồn tại.";
                case ALREADY_EXISTS:
                    return "Dữ liệu đã tồn tại. Không thể tạo trùng.";
                case ABORTED:
                    return "Thao tác bị hủy do xung đột dữ liệu.";
                default:
                    return "Đã xảy ra lỗi: " + e.getMessage();
            }
        }
        if (e instanceof com.google.firebase.auth.FirebaseAuthException) {
            return "Xác thực thất bại. Vui lòng đăng nhập lại.";
        }
        return "Đã xảy ra lỗi không xác định. Vui lòng thử lại.";
    }
}
