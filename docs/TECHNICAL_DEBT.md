# NỢ KỸ THUẬT (Technical Debt)

Danh sách các vi phạm ràng buộc, lỗi kỹ thuật, và điểm chưa hợp lý đã phát hiện khi rà soát codebase. Sắp xếp theo mức độ ưu tiên.

## CAO — Ưu tiên sửa ngay

| # | File:Line | Mô tả | Ảnh hưởng |
|---|---|---|---|
| TD-01 | `ui/wallet/TransferActivity.java:34` | `FirebaseFirestore.getInstance()` trực tiếp trong Activity | Vi phạm kiến trúc |
| TD-02 | `ui/wallet/TransferActivity.java:111-153` | `db.runTransaction()` chạy trong Activity | Vi phạm kiến trúc |
| TD-03 | `ui/wallet/TransferActivity.java:117-118` | `getLong("currentBalance")` không khớp với `Wallet.currentBalance: double` → có thể trả về null và crash | Runtime error |
| TD-04 | `ui/transaction/AddTransactionActivity.java:193` | `FirebaseFirestore.getInstance().collection(...).document(txId).get()` trực tiếp | Vi phạm kiến trúc |
| TD-05 | `ui/goal/GoalListActivity.java:244-257` | `deductFromWallet()` gọi Firestore trực tiếp + tự cập nhật số dư trong Activity | Mất atomic, sai dữ liệu |
| TD-06 | `ui/goal/GoalListActivity.java:248-249` | Không kiểm tra `wallet.currentBalance >= amount` trước khi trừ | Số dư âm không kiểm soát |
| TD-07 | `ui/budget/BudgetFragment.java:237-240` | `FirebaseFirestore.getInstance().update(...)` trong Fragment | Vi phạm kiến trúc |
| TD-08 | `ui/budget/BudgetEditActivity.java:183-186` | `FirebaseFirestore.getInstance().update(...)` trong Activity | Vi phạm kiến trúc |
| TD-09 | `ui/budget/BudgetSettingsDialog.java:141` | `FirebaseFirestore.getInstance()` lưu settings (chỉ cần SharedPreferences) | Dư thừa, vi phạm |
| TD-10 | `data/repository/TransactionRepository.java:103-117` (`addAtomic`) | Đọc `getDouble("currentBalance")` không nhất quán; không kiểm tra ví tồn tại, không kiểm tra `isArchived`; TRANSFER bị xử lý như expense (trừ tiền) | Sai logic nghiệp vụ |
| TD-11 | `data/repository/TransactionRepository.java:120-174` (`updateAtomic`) | Không đọc lại transaction cũ từ Firestore (tin dữ liệu từ UI); không xử lý đổi thường↔transfer đúng | Sai logic nghiệp vụ |
| TD-12 | `data/repository/TransactionRepository.java:177-207` (`deleteAtomic`) | Transfer không được hoàn tác đúng (không cộng ví nhận, không trừ ví nguồn) | Sai số dư |
| TD-13 | `data/model/Wallet.java` | Thiếu `updatedAt`, `isArchived` (ràng buộc 6 yêu cầu) | Không đối soát được ví cũ |
| TD-14 | `data/model/Budget.java` | `limitAmount: double` (ràng buộc 5.1 yêu cầu `long`); thiếu `createdAt`, `updatedAt`, `isArchived` | Precision + audit |
| TD-15 | `data/model/SavingsGoal.java` | `targetAmount`, `savedAmount: double`; thiếu `updatedAt`, `isArchived` | Precision + audit |
| TD-16 | `data/model/Challenge.java` | `targetSavings: double`; thiếu `updatedAt`, `isArchived` | Precision + audit |
| TD-17 | Toàn bộ | Chưa có `domain/usecase/` cho `TransferService`, `BudgetService`, `GoalService` | Thiếu tách lớp nghiệp vụ |

## TRUNG BÌNH — Sửa trong giai đoạn tiếp theo

| # | File:Line | Mô tả |
|---|---|---|
| TD-18 | `util/DateUtils.java:29, 35, 46, 56, 65, 71, 86` | Dùng `Calendar.getInstance()` không có TimeZone |
| TD-19 | `util/QuickParseUtil.java:128` | `Double.parseDouble(m.group(1).replace(',', '.'))` → parse `1.500.000` thành `1.5` |
| TD-20 | `util/QuickParseUtil.java:165, 179, 252, 255` | `Calendar.getInstance()` không TimeZone |
| TD-21 | `util/ReminderScheduler.java:25, 30` | `Calendar.getInstance()` không TimeZone |
| TD-22 | `util/InsightsEngine.java:77, 114` | `new Date()` không TimeZone |
| TD-23 | `viewmodel/HomeViewModel.java:107, 110` | `Calendar.getInstance()` không TimeZone |
| TD-24 | `data/model/SavingsGoal.java:49, 54` | `isOverdue`, `getRemainingDays` dùng `new Date()` không TimeZone |
| TD-25 | `data/repository/GoalRepository.java` (`addContribution`) | Không atomic; chỉ ghi history, không cập nhật `savedAmount` trong cùng transaction |
| TD-26 | `data/repository/RecurringRepository.java` (`catchUp`) | Không idempotent; không dùng `txRepo.addAtomic`; không kiểm tra ví archived |
| TD-27 | `ui/profile/EditProfileActivity.java` | ~~Dùng sai `ActivityRegisterBinding` thay vì binding riêng → dễ crash nếu 2 layout khác nhau~~ | **ĐÃ SỬA P0** — Tạo `activity_edit_profile.xml`, dùng `ActivityEditProfileBinding`, validate, `isSaving` flag |
| TD-28 | `ui/report/ReportFragment.java` | `getDateRange`, `getLastPeriodRange` phức tạp trong Fragment; tính `totalBalance` trong Fragment |
| TD-29 | `ui/wallet/WalletListActivity.java` | Chưa ViewBinding; tự tính `totalBalance` trong Activity; cho phép xoá ví có giao dịch |
| TD-30 | `ui/profile/SecurityActivity.java` | ~~Logic PIN/biometric/reminder phức tạp trong Activity — cần tách ViewModel~~ | **ĐÃ SỬA P0** — Thêm `disablePin()`, confirm dialog, biometric guard, validate PIN |
| TD-31 | `ui/profile/SettingsActivity.java` | Chưa có ViewModel riêng |
| TD-32 | `ui/auth/LoginActivity.java` | Gọi `RecurringRepository.catchUp(uid)` trực tiếp trong Activity |
| TD-33 | `util/MoneyFormat.java:46` | Chỉ có `format(double)` — cần thêm `formatLong(long)` |

## THẤP — Sửa khi có thời gian

| # | File:Line | Mô tả |
|---|---|---|
| TD-34 | `ui/auth/SplashActivity.java` | Chưa dùng ViewBinding |
| TD-35 | `ui/budget/BudgetListActivity.java` | Chưa dùng ViewBinding; `onClick` trên `findViewById(android.R.id.content)` sai |
| TD-36 | `ui/challenge/ChallengeListActivity.java` | Chưa dùng ViewBinding; `showAddDialog()` 180 dòng nên tách |
| TD-37 | `ui/wallet/WalletPaymentDialog.java` | Không có error callback cho `walletRepo.add()` |
| TD-38 | `ui/transaction/TransactionListFragment.java:90-91` | Filter amount dùng `Double.parseDouble` không khất quán với `Transaction.amount: long` |
| TD-39 | `ui/transaction/AddTransactionActivity.java` | Edit mode cast `(long) t.getAmount()` thừa |
| TD-40 | `ui/goal/GoalAdapter.java:61-73` | `buildTimeProgress` ghi đè text deadline 2 lần |
| TD-41 | `ui/adapter/WalletAdapter.java` | `setBalances(List<Transaction>)` nhận transactions nhưng không dùng |
| TD-42 | `ui/adapter/TransactionAdapter.java` | `getTransferLabel()` trả về empty khi cả 2 wallet null |
| TD-43 | `data/model/FinancialInsights.java` | Tất cả field là public mutable, không encapsulation |
| TD-44 | `data/model/UserProfile.java` | Không có `@DocumentId`, không có `id` |
| TD-45 | `viewmodel/HomeViewModelHolder.java` | Singleton thủ công có thể leak |
| TD-46 | `util/BackupManager.java` | Dùng `Double` cho số tiền khi serialize |
| TD-47 | `ui/wallet/TransferActivity.java:97` | Đọc `fromWallet.getCurrentBalance()` (double) từ cache trong UI — có thể lệch với Firestore |
| TD-48 | `res/layout/activity_budget_allocation.xml:112, 126, 147, 161, 171` | Hardcode màu hex trong layout |
| TD-49 | `ui/main/MainActivity.java` | `checkOverdueGoals()` tính `shortfall` trong Activity |
| TD-50 | `ui/security/LockActivity.java` | ~~Logic lockout phức tạp trong Activity, có thể race condition~~ | **ĐÃ SỬA P0** — Fix field init crash, `isDestroyed` flag, biometric protection, `FLAG_ACTIVITY_CLEAR_TASK`, handler cleanup |
| TD-51 | `util/PrefsHelper.java` | ~~Tạo `EncryptedSharedPreferences` mới mỗi lần gọi prefs()~~ | **ĐÃ SỬA P0** — `volatile SharedPreferences cachedPrefs` lazy-init |

## Thống kê

- Tổng số vi phạm: **51 mục** (thêm TD-51)
- Mức cao: **17** (chưa sửa)
- Mức trung bình: **14** (-2 đã sửa P0: TD-27, TD-30)
- Mức thấp: **16** (-1 đã sửa P0: TD-50)
- **Đã sửa P0: 3 mục** (TD-27, TD-30, TD-50)
- **Mới: 1 mục** (TD-51)

## Nguyên tắc sửa

1. Sửa hết mức **CAO** trước khi thêm tính năng mới.
2. Sửa mức **TRUNG BÌNH** theo từng module, ưu tiên module đang chạm vào.
3. Mức **THẤP** dồn vào các giai đoạn dọn dẹp.
4. Mỗi sửa chữa phải kèm test nếu là business logic.
5. Không được thêm dependency mới (theo ràng buộc).
