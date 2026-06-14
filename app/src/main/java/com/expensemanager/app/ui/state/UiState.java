package com.expensemanager.app.ui.state;

/**
 * Wrapper trạng thái UI cho mỗi màn hình tải dữ liệu.
 *
 * <p>Thay thế việc trả về {@code null} hoặc {@code empty List} khi có lỗi.
 * Mỗi state là một trong 4 loại:
 * <ol>
 *   <li>{@link #loading()} — đang tải, hiển thị ProgressBar.</li>
 *   <li>{@link #success(Object)} — tải thành công.</li>
 *   <li>{@link #empty()} — tải thành công nhưng không có dữ liệu.</li>
 *   <li>{@link #error(String)} — tải thất bại, hiển thị thông báo lỗi.</li>
 * </ol>
 *
 * <p>Usage trong ViewModel:
 * <pre>{@code
 * private final MutableLiveData<UiState<List<Transaction>>> transactions =
 *     new MutableLiveData<>(UiState.loading());
 * }</pre>
 *
 * <p>Usage trong Activity/Fragment:
 * <pre>{@code
 * vm.getTransactions().observe(this, state -> {
 *     if (state.isLoading()) { showLoading(); return; }
 *     if (state.isError()) { showError(state.getError()); return; }
 *     if (state.isEmpty()) { showEmpty(); return; }
 *     showData(state.getData());
 * });
 * }</pre>
 */
public abstract class UiState<T> {

    private UiState() {}

    public static <T> UiState<T> loading() {
        return new Loading<>();
    }

    public static <T> UiState<T> success(T data) {
        return new Success<>(data);
    }

    public static <T> UiState<T> empty() {
        return new Empty<>();
    }

    public static <T> UiState<T> error(String message) {
        return new Error<>(message);
    }

    // ---- type checks ----
    public boolean isLoading() { return this instanceof Loading; }
    public boolean isSuccess() { return this instanceof Success; }
    public boolean isEmpty()   { return this instanceof Empty; }
    public boolean isError()   { return this instanceof Error; }

    // ---- data access ----
    public T getData() {
        if (this instanceof Success) return ((Success<T>) this).data;
        return null;
    }
    public String getError() {
        if (this instanceof Error) return ((Error<?>) this).message;
        return null;
    }

    // ---- concrete subclasses ----
    private static final class Loading<T> extends UiState<T> {}

    private static final class Success<T> extends UiState<T> {
        private final T data;
        Success(T data) { this.data = data; }
    }

    private static final class Empty<T> extends UiState<T> {}

    private static final class Error<T> extends UiState<T> {
        private final String message;
        Error(String message) { this.message = message; }
    }
}
