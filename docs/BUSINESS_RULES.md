# QUY TẮC NGHIỆP VỤ

> **Cập nhật 17/06/2026 — đọc trước.** Một số mục bên dưới mô tả trạng thái cũ; những thay đổi đã áp dụng vào codebase:
> - **Bỏ tính năng Nhãn (Tag):** danh mục (`Category`) đảm nhận việc phân loại. Toàn bộ model/repository/UI/string của Tag đã được gỡ.
> - **Bỏ Chuyển tiền (Transfer):** `TransferActivity` và loại `TYPE_TRANSFER` không còn; giao dịch chỉ gồm `income`/`expense`. Mục 7 bên dưới chỉ còn giá trị lịch sử.
> - **Đã thêm lớp `domain/usecase/`:** `BudgetService`, `GoalService`, `RecurringService`, `WalletAdjustmentService`. UI **không còn** gọi `FirebaseFirestore.getInstance()` trực tiếp (các vi phạm TD-01…TD-09 đã xử lý).
> - **Migration tiền tệ `double → long` hoàn tất** cho mọi model; `Wallet` đã có `isArchived`/`updatedAt`.
> - **Danh mục cha/con:** `Category.parentId` hỗ trợ danh mục con.
> - Giao dịch định kỳ nằm ở `ui/recurring/RecurringListActivity` (tách khỏi `ChallengeListActivity`).

Tài liệu này mô tả chi tiết các module nghiệp vụ, dựa trên rà soát thực tế code trong repo. Mỗi module gồm: mục đích, màn hình, model, repository, collection Firestore, luồng xử lý, ràng buộc, lỗi, đề xuất, ưu tiên.

---

## 1. Đăng ký (`RegisterActivity`)

- **Mục đích:** Tạo tài khoản mới bằng email + mật khẩu.
- **Màn hình:** `ui/auth/RegisterActivity.java` (ViewBinding: `ActivityRegisterBinding`).
- **Model liên quan:** `data/model/UserProfile.java`.
- **Repository:** `data/repository/AuthRepository.java`.
- **Collection Firestore:** `users/{uid}` (tạo document UserProfile khi đăng ký thành công).
- **Luồng hiện tại:**
  1. Nhập email, mật khẩu, tên hiển thị.
  2. `AuthRepository.register(email, password, displayName)` → tạo user Firebase Auth + set `users/{uid}` với `UserProfile`.
  3. Chuyển sang `LoginActivity` (hoặc `MainActivity`).
- **Ràng buộc hiện tại:** Không có.
- **Lỗi / điểm chưa hợp lý:**
  - Không validate độ dài mật khẩu tối thiểu trước khi gọi Firebase.
  - Không kiểm tra email format trước khi gửi.
- **Đề xuất:** Thêm validate client trước khi gọi repo; trả lỗi thân thiện bằng tiếng Việt.
- **Ưu tiên:** Thấp.

---

## 2. Đăng nhập (`LoginActivity`)

- **Mục đích:** Đăng nhập bằng email + mật khẩu.
- **Màn hình:** `ui/auth/LoginActivity.java`.
- **Model liên quan:** `UserProfile`.
- **Repository:** `AuthRepository`, `RecurringRepository` (gọi `catchUp` sau khi đăng nhập).
- **Luồng hiện tại:**
  1. Nhập email + mật khẩu.
  2. `AuthRepository.login(...)` → đăng nhập Auth + gọi `SeedData.seedIfNeeded(uid)`.
  3. Gọi `new RecurringRepository().catchUp(uid)` trực tiếp trong Activity (vi phạm kiến trúc).
  4. Chuyển sang `MainActivity` hoặc `LockActivity` (nếu PIN enabled).
- **Ràng buộc hiện tại:** Không có.
- **Lỗi:** Activity gọi `RecurringRepository.catchUp` trực tiếp — cần tách ra service.
- **Đề xuất:** Tạo `domain/usecase/RecurringService.runOnLogin(uid)` để Activity chỉ gọi 1 method.
- **Ưu tiên:** Trung bình.

---

## 3. Splash & điều hướng (`SplashActivity`)

- **Mục đích:** Kiểm tra trạng thái đăng nhập + PIN, chuyển hướng.
- **Màn hình:** `ui/auth/SplashActivity.java`.
- **Luồng hiện tại:**
  1. Nếu đã đăng nhập + PIN enabled → `LockActivity` → `MainActivity`.
  2. Nếu đã đăng nhập + PIN disabled → `MainActivity`.
  3. Nếu chưa đăng nhập → `LoginActivity`.
- **Lỗi:** Chưa dùng ViewBinding.
- **Đề xuất:** Thêm ViewBinding.
- **Ưu tiên:** Thấp.

---

## 4. Trang chủ (`HomeFragment`)

- **Mục đích:** Tổng quan tài chính, insight, gợi ý.
- **Màn hình:** `ui/home/HomeFragment.java` (dùng `HomeViewModel` qua `HomeViewModelHolder`).
- **ViewModel:** `viewmodel/HomeViewModel.java`.
- **Model liên quan:** `Transaction`, `Wallet`, `Category`, `Budget`, `FinancialInsights`.
- **Repository:** tất cả repo.
- **Luồng hiện tại:**
  1. ViewModel `observeMonth`, `observeAll` cho 5 nguồn.
  2. `recompute()` khi bất kỳ nguồn nào đổi → tính income, expense, balance, insights, alerts, giao dịch gần nhất.
  3. UI hiển thị qua các MediatorLiveData.
- **Lỗi:**
  - `recompute()` không debounce → compute nhiều lần không cần thiết.
  - `Calendar.getInstance()` không timezone.
- **Đề xuất:** Dùng `Transformations.switchMap` hoặc debounce; thay `Calendar.getInstance()` bằng `DateUtils.nowVietnam()`.
- **Ưu tiên:** Trung bình.

---

## 5. Giao dịch thu (`AddTransactionActivity` — type=income)

- **Mục đích:** Ghi nhận khoản thu.
- **Màn hình:** `ui/transaction/AddTransactionActivity.java`.
- **Model:** `Transaction` (type=`TYPE_INCOME`).
- **Repository:** `TransactionRepository.addAtomic(uid, t, walletId)`.
- **Collection:** `users/{uid}/transactions`.
- **Luồng hiện tại:**
  1. Nhập số tiền, ví, danh mục, ngày, ghi chú.
  2. `txRepo.addAtomic` → Firestore transaction: read ví → cộng `amount` → set transaction doc → update ví.
- **Lỗi:**
  - `loadTransaction()` (khi edit) gọi `FirebaseFirestore.getInstance().collection(...).document(txId).get()` trực tiếp — vi phạm.
  - Không kiểm tra ví tồn tại / archived.
  - Cast `(long) t.getAmount()` thừa vì `Transaction.amount` đã là `long`.
- **Đề xuất:** Thêm `txRepo.getTransactionById(uid, id)`; bỏ cast thừa; thêm check archive.
- **Ưu tiên:** Cao.

---

## 6. Giao dịch chi (`AddTransactionActivity` — type=expense)

- Tương tự module 5 nhưng ảnh hưởng `currentBalance = currentBalance - amount`.
- **Ràng buộc bổ sung (ràng buộc 7.2):** Phiên bản hiện tại cho phép số dư âm; cần cảnh báo trước khi lưu.
- **Lỗi:** Chưa hiển thị cảnh báo chi vượt số dư.
- **Đề xuất:** Trước khi gọi `addAtomic`, nếu `wallet.currentBalance < amount` → dialog cảnh báo.
- **Ưu tiên:** Trung bình.

---

## 7. Chuyển tiền (`TransferActivity`) — ĐÃ GỠ KHỎI PHIÊN BẢN HIỆN TẠI

> Mục này chỉ còn giá trị lịch sử. `TransferActivity` và loại `TYPE_TRANSFER` đã bị bỏ. Nếu cần lại tính năng này trong tương lai, hãy hiện thực qua `domain/usecase/TransferService` (atomic, đọc/ghi 2 ví trong cùng Firestore Transaction) thay vì đặt logic trong Activity. Để điều chỉnh/nạp số dư ví hiện dùng `WalletAdjustmentService` + `WalletPaymentDialog`.

- **Mục đích (cũ):** Chuyển tiền giữa 2 ví.
- **Màn hình:** `ui/wallet/TransferActivity.java` (ViewBinding: `ActivityTransferBinding`).
- **Model:** `Transaction` (type=`TYPE_TRANSFER`).
- **Repository:** **Hiện KHÔNG có** — toàn bộ logic nằm trong Activity.
- **Luồng hiện tại:**
  1. Chọn ví nguồn, ví đích, số tiền, ghi chú.
  2. Activity gọi `db.runTransaction(...)` đọc 2 ví, kiểm tra số dư, tạo transaction, cập nhật 2 ví.
- **Vi phạm NGHIÊM TRỌNG:**
  - `FirebaseFirestore.getInstance()` trực tiếp trong Activity (line 34).
  - `db.runTransaction()` trong Activity (line 111-153).
  - Đọc `currentBalance` bằng `getLong()` (line 117-118) — không khớp với model `double`.
  - Không kiểm tra ví archived.
  - Không có `TransferService` riêng.
- **Đề xuất:** Tạo `domain/usecase/TransferService.performTransfer(...)`; sửa Activity chỉ gọi service.
- **Ưu tiên:** **CAO — sửa trong Giai đoạn 3.1.**

---

## 8. Danh sách giao dịch (`TransactionListFragment`)

- **Mục đích:** Xem, lọc, tìm giao dịch.
- **Màn hình:** `ui/transaction/TransactionListFragment.java`.
- **Model:** `Transaction`.
- **Repository:** `TransactionRepository` (observe, filter).
- **Luồng:** Lọc theo category, search theo note/amount, lọc theo amount range.
- **Lỗi:** Filter amount dùng `Double.parseDouble` (line 90-91) — không nhất quán với `Transaction.amount` là `long`.
- **Đề xuất:** Dùng `MoneyValueParser.tryParse(...)` trả về `long`.
- **Ưu tiên:** Thấp.

---

## 9. Ví tiền (`WalletListActivity`)

- **Mục đích:** Quản lý ví: tạo, sửa, xoá, archive.
- **Màn hình:** `ui/wallet/WalletListActivity.java`.
- **Model:** `Wallet`.
- **Repository:** `WalletRepository`.
- **Luồng:** CRUD ví + hiển thị tổng số dư (tính trong Activity).
- **Lỗi:**
  - Chưa dùng ViewBinding.
  - Tính `totalBalance` trong Activity (có thể chuyển vào `BalanceCalculator`).
  - Cho phép xoá ví đang có giao dịch (vi phạm ràng buộc 6).
  - `Wallet` chưa có `isArchived`, `updatedAt` (ràng buộc 6 yêu cầu).
- **Đề xuất:** Thêm 2 trường vào `Wallet`; nút xoá phải archive thay vì xoá nếu ví có giao dịch.
- **Ưu tiên:** Cao.

---

## 10. Danh mục (`CategoryListActivity`)

- **Mục đích:** Quản lý danh mục thu/chi.
- **Màn hình:** `ui/category/CategoryListActivity.java`.
- **Model:** `Category`.
- **Repository:** `CategoryRepository`.
- **Luồng:** CRUD danh mục + gán nhóm (essential/need/want/other).
- **Lỗi:** Không có.
- **Đề xuất:** Thêm ViewModel riêng nếu phức tạp hơn.
- **Ưu tiên:** Thấp.

---

## 11. Ngân sách (`BudgetFragment`, `BudgetListActivity`, `BudgetEditActivity`, `BudgetAllocationActivity`)

- **Mục đích:** Đặt hạn mức chi tiêu theo tháng + theo danh mục.
- **Màn hình:**
  - `BudgetFragment`: tổng quan.
  - `BudgetListActivity`: danh sách.
  - `BudgetEditActivity`: sửa theo danh mục.
  - `BudgetAllocationActivity`: phân bổ theo tháng.
  - `BudgetSettingsDialog`: cài đặt (ngôn ngữ, tiền tệ).
- **Model:** `Budget` (đang `limitAmount: double`).
- **Repository:** `BudgetRepository`.
- **Vi phạm NGHIÊM TRỌNG:**
  - `FirebaseFirestore.getInstance()` trong `BudgetFragment` (line 237-240), `BudgetEditActivity` (line 183-186), `BudgetSettingsDialog` (line 141).
  - `Budget.limitAmount` là `double` (ràng buộc 5.1 yêu cầu `long`).
  - `BudgetSettingsDialog` lưu settings lên Firestore dư thừa.
- **Đề xuất:** Sửa ngay trong Giai đoạn 2.
- **Ưu tiên:** **CAO — ưu tiên #1 theo lựa chọn người dùng.**

---

## 12. Báo cáo & biểu đồ (`ReportFragment`)

- **Mục đích:** Biểu đồ Pie/Bar theo thời gian, ví, danh mục.
- **Màn hình:** `ui/report/ReportFragment.java`.
- **Model:** `Transaction`, `Wallet`, `Category`.
- **Repository:** nhiều repo.
- **Luồng:** Tính tổng income/expense theo filter, vẽ chart.
- **Lỗi:** Tính `totalBalance` trong Fragment; logic `getDateRange` phức tạp trong Fragment.
- **Đề xuất:** Tách `util/DateRangeUtils`; dùng `BalanceCalculator.totalAssets`.
- **Ưu tiên:** Trung bình.

---

## 13. Giao dịch định kỳ (`RecurringListActivity`)

- **Mục đích:** Tạo rule định kỳ (daily/weekly/monthly/yearly), hệ thống tự tạo giao dịch.
- **Màn hình:** `ui/recurring/RecurringListActivity.java` (ViewBinding).
- **Model:** `RecurringRule` (đã thêm `useLastDayOfMonth`, `makeOccurrenceId`).
- **Repository:** `RecurringRepository` (đã viết lại với atomic execution).
- **Worker:** `RecurringTransactionWorker` (WorkManager periodic).

### Luồng thực thi

1. `catchUp(uid)` load tất cả rule đang bật.
2. Mỗi rule: kiểm tra `dateStart`, `dateEnd`, `nextRun`.
3. Chạy bù (catch-up) tất cả kỳ bị bỏ lỡ, max 50/lần.
4. `executeOccurrence` trong Firestore Transaction:
   - Check `occurrenceId` chưa tồn tại.
   - Tạo transaction + cập nhật số dư ví.
   - Cập nhật `lastRun` và `nextRun`.
5. **Không bao giờ tạo trùng** — dù occurrenceId làm document ID.

### Chu kỳ

- **Daily:** Không cần ngày/thứ.
- **Weekly:** Bắt buộc chọn thứ (1=CN ... 7=T7).
- **Monthly:** Ngày 1-31 HOẶC "Ngày cuối tháng" (`useLastDayOfMonth=true`).
- **Yearly:** Bắt buộc chọn tháng + ngày.

### Loại giao dịch

- Hỗ trợ INCOME và EXPENSE.
- Khoản thu: bắt buộc chọn danh mục thu.
- Khoản chi: bắt buộc chọn danh mục chi.
- Không hỗ trợ TRANSFER trong phiên bản hiện tại.

### Nút "Thêm"

- Trên Bottom Navigation 5 mục (Tổng quan / Ngân sách / **Thêm** / Báo cáo / Cá nhân).
- FAB đã loại bỏ khỏi MainActivity.
- Mục Thêm không giữ trạng thái selected.

### Lỗi đã sửa

- `ChallengeListActivity` → `RecurringListActivity` (tách đúng module).
- `RecurringRule.useLastDayOfMonth` thay thế `dayOfMonth=32`.
- `RecurringRepository` dùng Firestore Transaction + occurrenceId.
- Thêm RadioGroup loại giao dịch (INCOME/EXPENSE).
- Category spinner lọc theo loại giao dịch.
- MoneyInputFormatter tự động thêm dấu phẩy.
- Adapter hiển thị đầy đủ: icon theo type, cycle, nextRun, endDate.

### Ưu tiên: Đã hoàn thành.

---

## 14. Mục tiêu tiết kiệm (`GoalListActivity`)

- **Mục đích:** Đặt mục tiêu tiết kiệm, đóng góp tiền vào mục tiêu.
- **Màn hình:** `ui/goal/GoalListActivity.java`.
- **Model:** `SavingsGoal` (đang `targetAmount`, `savedAmount` là `double`).
- **Repository:** `GoalRepository`.
- **Vi phạm NGHIÊM TRỌNG:**
  - `FirebaseFirestore.getInstance()` trong `deductFromWallet()` (line 244-257).
  - Tự tính & cập nhật `currentBalance` trong Activity.
  - Không kiểm tra số dư ví.
  - `addContribution` không atomic.
  - `SavingsGoal` chưa có `isArchived`, `updatedAt`.
- **Đề xuất:** Tạo `domain/usecase/GoalService.contributeToGoal(...)` atomic.
- **Ưu tiên:** Cao.

---

## 15. Thử thách tiết kiệm (`Challenge` model)

- **Mục đích:** Đặt thử thách tiết kiệm theo số ngày.
- **Màn hình:** Có thể nằm trong `ChallengeListActivity` (trùng với recurring) — **CẦN XÁC NHẬN** với stakeholder.
- **Model:** `Challenge` (đang `targetSavings: double`).
- **Repository:** Chưa có (dùng chung repository nào đó?).
- **Lỗi:** Chưa có repository riêng; `Challenge.targetSavings` là `double`; chưa có `isArchived`, `updatedAt`.
- **Đề xuất:** Xác nhận màn hình, tạo `ChallengeRepository` nếu cần.
- **Ưu tiên:** Trung bình (cần làm rõ trước).

---

## 16. Insight tài chính (`InsightsEngine`)

- **Mục đích:** Tính health score, daily allowance, dự đoán chi tháng, so sánh tháng.
- **File:** `util/InsightsEngine.java`.
- **Model:** `FinancialInsights` (chứa String đã format sẵn).
- **Luồng:** `compute(uid, transactions, wallets, budgets, prevMonth)` → `FinancialInsights`.
- **Lỗi:**
  - `new Date()` không timezone (line 114).
  - Dùng `double` cho tiền.
  - Các field là public mutable — nên đóng gói.
- **Đề xuất:** Đổi sang `long` cho tiền, dùng `DateUtils.nowVietnam()`.
- **Ưu tiên:** Trung bình.

---

## 17. Nhắc nhở (`ReminderScheduler`, `ReminderReceiver`)

- **Mục đích:** Hằng ngày thông báo nhắc ghi chi tiêu.
- **File:** `util/ReminderScheduler.java`, `util/ReminderReceiver.java`, `util/BootReceiver.java`.
- **Luồng:** `scheduleDaily(ctx)` đặt `AlarmManager.setRepeating`; `BootReceiver` re-schedule sau reboot.
- **Lỗi:** `Calendar.getInstance()` không timezone.
- **Đề xuất:** Dùng `TimeZone.getTimeZone("Asia/Ho_Chi_Minh")`.
- **Ưu tiên:** Thấp.

---

## 18. Sao lưu & xuất dữ liệu (`BackupManager`)

- **Mục đích:** Export/Import toàn bộ dữ liệu người dùng ra JSON.
- **File:** `util/BackupManager.java`.
- **Luồng:** Đọc 7 collection → serialize Timestamp → share qua FileProvider; restore thì đọc JSON → `WriteBatch` 400/batch.
- **Lỗi:** Dùng `Double` cho số tiền khi serialize.
- **Đề xuất:** Khi đọc serialize, dùng `MoneyValueParser.toLong` để chuẩn hoá.
- **Ưu tiên:** Thấp.

---

## 19. Hồ sơ cá nhân (`ProfileFragment`, `EditProfileActivity`)

- **Mục đích:** Xem thông tin, sửa tên hiển thị.
- **Màn hình:** `ui/profile/ProfileFragment.java`, `ui/profile/EditProfileActivity.java`.
- **Layout:** `activity_edit_profile.xml` (layout riêng, KHÔNG dùng `activity_register.xml`).
- **Binding:** `ActivityEditProfileBinding` (ViewBinding).
- **Validation:**
  - Tên: trim whitespace, `length >= 2 && <= 50`, không cho lưu nếu không thay đổi.
  - Email: read-only, lấy từ `FirebaseAuth.getInstance().getCurrentUser().getEmail()`.
- **Trạng thái lưu:** `isSaving` flag prevent double-tap, button disable + ProgressBar khi đang gửi.
- **Ràng buộc:**
  - Email không được thay đổi (chỉ đọc).
  - KHÔNG hiển thị layout đăng ký.
- **Lỗi đã sửa:** Layout dùng `ActivityRegisterBinding` → giờ dùng `ActivityEditProfileBinding`.
- **Ưu tiên:** Đã sửa trong P0.

---

## 20. Cài đặt (`SettingsActivity`)

- **Mục đích:** Dark mode, reminder, currency, backup/restore, đổi mật khẩu.
- **Màn hình:** `ui/profile/SettingsActivity.java`.
- **Repository:** `AuthRepository`.
- **Lỗi:** Chưa có ViewModel riêng.
- **Đề xuất:** Thêm ViewModel nếu logic phức tạp.
- **Ưu tiên:** Thấp.

---

## 21. PIN & sinh trắc học (`SecurityActivity`, `LockActivity`, `PrefsHelper`)

- **Mục đích:** Khoá app bằng PIN, mở bằng sinh trắc học.
- **Màn hình:** `ui/security/LockActivity.java`, `ui/profile/SecurityActivity.java`.
- **File:** `util/PrefsHelper.java` (PBKDF2 hash, EncryptedSharedPreferences, lockout 5 phút sau 5 lần sai).
- **Lỗi đã sửa:**
  - `LockActivity`: field initializer dùng `ContextCompat.getMainExecutor(this)` → giờ khai báo `Executor executor` rỗng, khởi tạo trong `onCreate()` sau `super.onCreate()`.
  - `LockActivity`: biometric không protected → thêm `isFinishing()`, `isDestroyed` flag, `currentPrompt` tracking, `cancelAuthentication()` trong `onDestroy()`.
  - `LockActivity`: `openMain()` không clear back stack → thêm `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`.
  - `PrefsHelper`: tạo `EncryptedSharedPreferences` mới mỗi lần gọi → giờ dùng `volatile SharedPreferences cachedPrefs` lazy-init.
  - `PrefsHelper`: thiếu `disablePin()` → đã thêm xóa toàn bộ key PIN + biometric.
  - `SecurityActivity`: tắt PIN bằng `setPinEnabled(false, null)` → giờ dùng `disablePin()`.
  - `SecurityActivity`: biometric không bị disable khi tắt PIN → giờ gọi `setBiometricEnabled(false)` trong `disablePin()`.
  - `SecurityActivity`: không có confirm dialog khi tắt PIN → thêm confirm dialog.
- **Ràng buộc:**
  - PIN: chỉ chữ số, 4-6 ký tự, dùng `EncryptedSharedPreferences`.
  - Không ghi PIN/hash ra logcat.
  - `disablePin()` dùng `.remove()` không phải `.putString(null)`.
- **Đề xuất:** Tạo `SecurityViewModel` xử lý PIN/biometric.
- **Ưu tiên:** Đã sửa trong P0.

---

## 22. Hiển thị lịch sử giao dịch

- **Mục đích:** Item giao dịch hiển thị đầy đủ thông tin, không hardcode text.
- **Adapter:** `ui/adapter/TransactionAdapter.java`.
- **Layout:** `layout/item_transaction.xml`.
- **Quy tắc:**
  - Mỗi item: icon (từ danh mục hoặc loại giao dịch), tên danh mục/loại, số tiền, ví, ngày giờ, ghi chú.
  - Thu nhập: `+500,000 VND`, màu xanh.
  - Chi tiêu: `-85,000 VND`, màu đỏ.
  - Chuyển tiền: `500,000 VND` (không dấu), hiển thị `Ví A → Ví B`.
  - Icon: lấy từ danh mục (emoji hoặc drawable), màu nền từ danh mục.
  - Nếu danh mục/ví bị xóa → hiển thị "Danh mục đã xóa" / "Ví đã xóa".
  - KHÔNG hiển thị ID thay cho tên.
- **DateUtils:** Dùng `formatDateTime(Date)` → `dd/MM/yyyy • HH:mm`.
- **Date grouping:** Dùng `getRelativeDate(Date)` → "Hôm nay", "Hôm qua", hoặc `dd/MM/yyyy`.
- **MoneyFormat:** Dùng `formatSigned(long, type)` cho số tiền có dấu.
- **Chi tiết giao dịch:** `TransactionDetailBottomSheet.java` — hiển thị toàn bộ trường.
- **Lỗi đã sửa:**
  - Icon cố định `🍔` → icon động từ danh mục.
  - Không hiển thị ví → thêm dòng ví.
  - Không hiển thị giờ → thêm `formatDateTime`.
  - Hardcode "Chuyển tiền" → dùng `@string/transfer`.
  - Số tiền dùng `MoneyFormat.format(double)` → dùng `format(long)`.
- **Ưu tiên:** Đã sửa trong giai đoạn này.

---

## 23. Đa ngôn ngữ

- **Mục đích:** Hỗ trợ tiếng Việt và tiếng Anh, người dùng chọn được ngôn ngữ.
- **Resource:** `values/strings.xml` (VN), `values-en/strings.xml` (EN).
- **Nguyên tắc:**
  - KHÔNG hardcode text trong Java/XML → dùng `@string/`.
  - KHÔNG lưu câu đã dịch trong model/engine → dùng enum/code.
  - Dùng `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))`.
  - Định dạng ngày theo Locale thiết bị.
- **FinancialInsights:** KHÔNG chứa String câu tiếng Việt → chỉ dùng enum + data số.
- **Cài đặt:** Language dialog trong Settings/Profile → áp dụng ngay, không cần restart app.
- **Lưu trữ:** Ngôn ngữ được lưu trong SharedPreferences, áp dụng lại khi mở app.
- **Ưu tiên:** Đã sửa trong giai đoạn này.

---

## 24. Sức khỏe tài chính

- **Mục đích:** Trang chủ hiển thị sức khỏe tài chính gọn gàng, ưu tiên số liệu.
- **Model:** `FinancialInsights.java` — chỉ dùng `long`, `double`, enum, không String câu tiếng Việt.
- **Engine:** `InsightsEngine.java` — trả về dữ liệu số, UI format và hiển thị.
- **Enums:** `FinancialHealthStatus` (EXCELLENT/GOOD/WARNING/CRITICAL), `FinancialAlertType`.
- **Công thức:**
  - `savingRate = netCashFlow / incomeAmount`
  - `budgetUsageRate = expenseAmount / budgetLimit`
  - `expenseChangeRate = (current - previous) / previous`
- **Xếp loại:**
  - 85–100: Xuất sắc
  - 70–84: Tốt
  - 50–69: Cần chú ý
  - 0–49: Nguy hiểm
- **Card trang chủ:** Hiển thị score, trạng thái, 3 chỉ số chính (tiết kiệm, ngân sách, so với tháng trước), dự đoán.
- **Cảnh báo:** Chỉ hiển thị 1 cảnh báo quan trọng nhất trên trang chủ.
- **Ưu tiên:** Đã sửa trong giai đoạn này.

---

## 25. Chuẩn hóa tiền tệ

- **Mục đích:** Mọi số tiền hiển thị nhất quán: `500,000 VND`.
- **MoneyFormat.java:**
  - `format(long)` → `500,000 VND` (mặc định, ký hiệu SAU)
  - `formatSigned(long, type)` → `+500,000 VND` (income), `-500,000 VND` (expense), `500,000 VND` (transfer)
  - `formatCompactLong(long)` → compact format
  - `currencyCode = "VND"` (đổi từ `currencySymbol = "đ"`)
- **Ràng buộc:**
  - KHÔNG dùng `MoneyFormat.format(double)` — dùng `format(long)`.
  - Tiền tệ luôn nằm SAU số tiền trong hiển thị.
  - Không tự nối chuỗi `"đ" + amount` tại các màn hình.
- **Ưu tiên:** Đã sửa trong giai đoạn này.

