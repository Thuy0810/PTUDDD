# TỔNG QUAN ỨNG DỤNG QUẢN LÝ CHI TIÊU

> Cập nhật theo codebase ngày 17/06/2026. Đã **bỏ tính năng Tag/Nhãn** (danh mục đảm nhận vai trò phân loại). Tính năng **Chuyển tiền (transfer) không còn** trong phiên bản hiện tại — giao dịch chỉ gồm `income` và `expense`.

## Thông tin chung

- **Tên dự án:** PTUDDD (Personal Expense & Budget Manager)
- **Package chính:** `com.expensemanager.app`
- **Ngôn ngữ:** Java 17
- **Kiểu ứng dụng:** Android native
- **Ngôn ngữ giao diện:** Tiếng Việt + Tiếng Anh (chọn trong app)

## Stack công nghệ (bắt buộc giữ nguyên)

| Hạng mục | Công nghệ |
|---|---|
| Ngôn ngữ | Java 17 (không Kotlin) |
| UI Layout | XML (không Jetpack Compose) |
| Backend | Firebase Authentication + Cloud Firestore |
| View binding | `ViewBinding` (bật trong `buildFeatures`) |
| Reactive | LiveData + ViewModel (Lifecycle 2.7.0) |
| Điều hướng | Navigation Component 2.7.7 |
| UI Components | Material Components 1.12.0 |
| Biểu đồ | MPAndroidChart v3.1.0 |
| Bảo mật | Biometric 1.1.0, Security Crypto 1.1.0-alpha06 (EncryptedSharedPreferences) |
| Tác vụ nền | WorkManager 2.9.0 + AlarmManager |
| minSdk / targetSdk / compileSdk | 24 / 34 / 34 |

## Cấu trúc thư mục nguồn

```
app/src/main/java/com/expensemanager/app/
├── ExpenseApplication.java          # Khởi tạo notification channel, áp dụng locale & format tiền
├── data/
│   ├── SeedData.java                # Khởi tạo danh mục & ví mặc định
│   ├── firebase/FirestorePaths.java # Đường dẫn collection chuẩn
│   ├── model/                       # 13 model (POJO + enum)
│   └── repository/                  # 7 repository
├── domain/
│   └── usecase/                     # BudgetService, GoalService, RecurringService, WalletAdjustmentService
├── ui/
│   ├── auth/                        # Splash, Login, Register
│   ├── main/MainActivity.java       # BottomNavigation + nút Thêm
│   ├── home/HomeFragment.java       # Tổng quan
│   ├── transaction/                 # AddTransaction, TransactionList, TransactionDetailBottomSheet
│   ├── wallet/                      # WalletList, WalletPaymentDialog
│   ├── category/                    # CategoryList (hỗ trợ danh mục cha/con)
│   ├── budget/                      # BudgetFragment, BudgetList, BudgetEdit, BudgetAllocation + adapters
│   ├── report/ReportFragment.java   # Biểu đồ
│   ├── goal/GoalListActivity.java   # Mục tiêu tiết kiệm
│   ├── recurring/RecurringListActivity.java   # Giao dịch định kỳ
│   ├── challenge/ChallengeListActivity.java   # Thử thách tiết kiệm
│   ├── profile/                     # ProfileFragment, EditProfile, Settings, Security
│   ├── security/LockActivity.java   # Khoá PIN / Sinh trắc học
│   ├── state/                       # UiState (loading/success/error)
│   └── adapter/                     # RecyclerView adapters dùng chung
├── util/                            # MoneyFormat, DateUtils, DateRangeUtils, QuickParseUtil,
│                                    # BalanceCalculator, BudgetChecker, CategorySuggester,
│                                    # InsightsEngine, BackupManager, Reminder*, LocaleHelper, Prefs...
└── viewmodel/                       # HomeViewModel + HomeViewModelHolder
```

## Các module nghiệp vụ hiện có

| # | Module | Màn hình chính | Ghi chú |
|---|---|---|---|
| 1 | Đăng ký | `ui/auth/RegisterActivity` | |
| 2 | Đăng nhập | `ui/auth/LoginActivity` | Gọi catch-up giao dịch định kỳ sau đăng nhập |
| 3 | Splash & điều hướng | `ui/auth/SplashActivity` | |
| 4 | Trang chủ / Tổng quan | `ui/home/HomeFragment` | Tổng thu/chi/số dư, insight, sức khoẻ tài chính |
| 5 | Thêm/sửa giao dịch thu | `ui/transaction/AddTransactionActivity` | type=income |
| 6 | Thêm/sửa giao dịch chi | `ui/transaction/AddTransactionActivity` | type=expense |
| 7 | Danh sách giao dịch | `ui/transaction/TransactionListFragment` | Lọc theo loại, ví, số tiền, ngày, tìm kiếm |
| 8 | Chi tiết giao dịch | `ui/transaction/TransactionDetailBottomSheet` | |
| 9 | Ví tiền | `ui/wallet/WalletListActivity` | CRUD + archive |
| 10 | Điều chỉnh / nạp ví | `ui/wallet/WalletPaymentDialog` + `WalletAdjustmentService` | Chỉnh số dư có ghi log audit |
| 11 | Danh mục (cha/con) | `ui/category/CategoryListActivity` | Nhóm essential/need/want/other |
| 12 | Ngân sách | `ui/budget/*` | Theo tháng + theo danh mục + phân bổ lại |
| 13 | Báo cáo & biểu đồ | `ui/report/ReportFragment` | |
| 14 | Giao dịch định kỳ | `ui/recurring/RecurringListActivity` + `RecurringService` | Catch-up idempotent |
| 15 | Mục tiêu tiết kiệm | `ui/goal/GoalListActivity` + `GoalService` | Đóng góp atomic, trừ ví |
| 16 | Thử thách tiết kiệm | `ui/challenge/ChallengeListActivity` | |
| 17 | Insight / Sức khoẻ tài chính | `util/InsightsEngine` + `HomeFragment` | |
| 18 | Nhắc nhở | `util/ReminderScheduler` + `ReminderReceiver` + `BootReceiver` | |
| 19 | Sao lưu & xuất/nhập dữ liệu | `util/BackupManager` (trong Settings) | JSON |
| 20 | Hồ sơ cá nhân | `ui/profile/ProfileFragment`, `EditProfileActivity` | |
| 21 | Cài đặt | `ui/profile/SettingsActivity` | Dark mode, ngôn ngữ, tiền tệ, nhắc nhở, backup, đổi mật khẩu |
| 22 | PIN & sinh trắc học | `ui/security/LockActivity`, `ui/profile/SecurityActivity` | |

> **Không còn:** Chuyển tiền giữa ví (transfer) và Nhãn (tag).

## Bản đồ điều hướng chính

```
SplashActivity
   ├── (chưa đăng nhập) → LoginActivity → RegisterActivity
   └── (đã đăng nhập) → (PIN bật) LockActivity → MainActivity
                          (PIN tắt)            → MainActivity
                              ├── HomeFragment
                              ├── BudgetFragment
                              │     ├── BudgetListActivity
                              │     ├── BudgetEditActivity
                              │     └── BudgetAllocationActivity
                              ├── (nút Thêm) → AddTransactionActivity
                              ├── TransactionListFragment
                              ├── ReportFragment
                              └── ProfileFragment
                                     ├── EditProfileActivity
                                     ├── SettingsActivity (xuất/nhập backup)
                                     ├── SecurityActivity
                                     ├── GoalListActivity
                                     ├── RecurringListActivity
                                     ├── ChallengeListActivity
                                     ├── CategoryListActivity
                                     └── WalletListActivity → WalletPaymentDialog
```

## Quy ước chung

- Tiền tệ mặc định: VND. Hiển thị qua `MoneyFormat.format(long)` / `formatSigned(long, type)` — ký hiệu `VND` nằm SAU số tiền. **Không dùng** `format(double)`.
- Mọi số tiền trong model đã là `long` (không thập phân).
- Múi giờ nghiệp vụ: `Asia/Ho_Chi_Minh` (xem `util/DateUtils.java`).
- Ngày giao dịch lưu UTC (`Timestamp`), hiển thị theo timezone VN. Ngày ngắn `dd/MM/yyyy`, tháng khoá `yyyy-MM`.
- Mọi thay đổi số dư đi qua repository/usecase atomic (`TransactionRepository.addAtomic/updateAtomic/deleteAtomic`, `GoalService`, `WalletAdjustmentService`, `RecurringService`); **UI không tự ghi `currentBalance`** và **không gọi `FirebaseFirestore.getInstance()` trực tiếp**.
- Không hardcode chuỗi hiển thị trong Java/XML — dùng `@string/`.

## Quy tắc build & chạy

- Mở bằng **Android Studio Hedgehog** trở lên, **Gradle JDK = 17**.
- `File → Sync Project with Gradle Files` sau khi clone.
- Cấu hình Firebase: thay `app/google-services.json` thật trước khi demo đăng nhập.
- Build & run trên thiết bị thật/emulator Android 7.0+ (API 24), cần Google Play Services.
