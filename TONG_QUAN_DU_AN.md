# Tổng Quan Dự Án — Expense Manager

> Ứng dụng Android quản lý chi tiêu cá nhân, xây dựng bằng **Java** theo kiến trúc **MVVM + Clean Architecture**, dữ liệu lưu trên **Firebase**.

---

## 1. Thông tin chung

| Mục | Giá trị |
|---|---|
| Tên ứng dụng | ExpenseManager (`com.expensemanager.app`) |
| Ngôn ngữ | Java 17 (92 file `.java`, không dùng Kotlin) |
| Kiến trúc | MVVM + Clean Architecture (UI – ViewModel – Domain – Data) |
| Backend / CSDL | Firebase Firestore + Firebase Auth |
| compileSdk / targetSdk | 34 |
| minSdk | 24 (Android 7.0 trở lên) |
| Version | 1.0 |
| Giao diện | XML Layout + Material Components (chưa dùng Jetpack Compose) |

---

## 2. Kiến trúc tổng thể

Ứng dụng chia thành 4 tầng rõ ràng, dữ liệu chảy một chiều và Model không phụ thuộc View:

```
┌─────────────────────────────────────────────┐
│  UI Layer (ui/)                              │
│  Activity / Fragment / Adapter / Dialog      │
│  - Chỉ hiển thị & observe LiveData           │
└───────────────┬─────────────────────────────┘
                │ observe LiveData
┌───────────────▼─────────────────────────────┐
│  ViewModel Layer (viewmodel/)                │
│  HomeViewModel, SecurityViewModel            │
│  - Giữ state, kết hợp dữ liệu nhiều nguồn    │
└───────────────┬─────────────────────────────┘
                │ gọi
┌───────────────▼─────────────────────────────┐
│  Domain Layer (domain/usecase/)              │
│  BudgetService, GoalService,                 │
│  RecurringService, WalletAdjustmentService   │
│  - Business logic thuần                      │
└───────────────┬─────────────────────────────┘
                │ gọi
┌───────────────▼─────────────────────────────┐
│  Data Layer (data/)                          │
│  Repository (Transaction, Wallet, Budget...) │
│  Model · FirestorePaths                      │
│  - Truy vấn Firestore, trả về LiveData       │
└───────────────┬─────────────────────────────┘
                │
        ┌───────▼────────┐
        │  Firebase      │
        │  Firestore/Auth│
        └────────────────┘
```

**Nguyên tắc:** View quan sát ViewModel → ViewModel gọi Repository/Service → Repository truy vấn Firestore và trả về `LiveData`. Khi xoay màn hình, ViewModel sống lâu hơn View nên không mất dữ liệu.

---

## 3. Cấu trúc thư mục source

```
app/src/main/java/com/expensemanager/app/
│
├── ExpenseApplication.java        # Application: tạo NotificationChannel, áp dụng cấu hình tiền tệ
│
├── data/                          # ===== TẦNG DỮ LIỆU =====
│   ├── SeedData.java              # Dữ liệu mẫu khởi tạo (danh mục mặc định...)
│   ├── firebase/
│   │   └── FirestorePaths.java    # Đường dẫn collection Firestore tập trung
│   ├── model/                     # Các lớp dữ liệu (POJO)
│   │   ├── Transaction.java       # Giao dịch (thu/chi, số tiền, danh mục, ví, mood...)
│   │   ├── Wallet.java            # Ví / nguồn tiền
│   │   ├── Category.java          # Danh mục thu chi
│   │   ├── Budget.java            # Ngân sách theo tháng
│   │   ├── BudgetReallocation.java# Tái phân bổ ngân sách
│   │   ├── SavingsGoal.java       # Mục tiêu tiết kiệm
│   │   ├── RecurringRule.java     # Quy tắc giao dịch định kỳ
│   │   ├── Challenge.java         # Thử thách tiết kiệm
│   │   ├── UserProfile.java       # Hồ sơ người dùng
│   │   ├── HomeSummary.java       # Dữ liệu tổng hợp màn hình chính
│   │   ├── FinancialInsights.java # Phân tích tài chính
│   │   ├── FinancialHealthStatus.java / FinancialAlertType.java
│   └── repository/                # Repository (cầu nối Firestore)
│       ├── AuthRepository.java
│       ├── TransactionRepository.java
│       ├── WalletRepository.java
│       ├── CategoryRepository.java
│       ├── BudgetRepository.java
│       ├── GoalRepository.java
│       ├── RecurringRepository.java
│       ├── ChallengeRepository.java
│       └── FirestoreQueryLiveData.java  # Bọc truy vấn Firestore thành LiveData
│
├── domain/usecase/                # ===== TẦNG NGHIỆP VỤ =====
│   ├── BudgetService.java         # Logic ngân sách & phân bổ
│   ├── GoalService.java           # Logic mục tiêu tiết kiệm
│   ├── RecurringService.java      # Sinh giao dịch định kỳ
│   └── WalletAdjustmentService.java # Điều chỉnh số dư ví
│
├── viewmodel/                     # ===== TẦNG VIEWMODEL =====
│   ├── HomeViewModel.java         # Tổng hợp dữ liệu trang chủ (5 repository)
│   ├── HomeViewModelHolder.java
│   └── SecurityViewModel.java     # Logic khoá ứng dụng / sinh trắc học
│
├── ui/                            # ===== TẦNG GIAO DIỆN =====
│   ├── auth/        # Splash, Onboarding, Login, Register
│   ├── main/        # MainActivity (điều hướng chính)
│   ├── home/        # HomeFragment (màn hình tổng quan)
│   ├── transaction/ # Thêm/sửa, thêm nhanh, chi tiết, danh sách giao dịch
│   ├── wallet/      # Danh sách ví, thanh toán
│   ├── category/    # Quản lý danh mục
│   ├── budget/      # Ngân sách: danh sách, phân bổ, chỉnh sửa, tổng quan
│   ├── goal/        # Mục tiêu tiết kiệm
│   ├── recurring/   # Giao dịch định kỳ
│   ├── challenge/   # Thử thách tiết kiệm
│   ├── report/      # Báo cáo & biểu đồ
│   ├── profile/     # Hồ sơ, cài đặt, bảo mật
│   ├── security/    # LockActivity (màn hình khoá)
│   ├── adapter/     # RecyclerView Adapter dùng chung
│   └── state/       # UiState, ResourceLiveData (quản lý trạng thái loading/error)
│
├── worker/
│   └── RecurringTransactionWorker.java  # WorkManager: tự sinh giao dịch định kỳ đến hạn
│
└── util/                          # ===== TIỆN ÍCH =====
    ├── MoneyFormat / MoneyInputFormatter / MoneyValueParser  # Định dạng & xử lý tiền tệ
    ├── DateUtils / DateRangeUtils                            # Xử lý ngày tháng (múi giờ ICT)
    ├── BalanceCalculator                                     # Tính số dư
    ├── InsightsEngine                                        # Sinh phân tích tài chính
    ├── BudgetChecker                                         # Kiểm tra vượt ngân sách & cảnh báo
    ├── CategorySuggester / CategoryIconPicker / CategoryIcons
    ├── WalletIcons / GoalIcons
    ├── ReminderScheduler / ReminderReceiver                  # Lập lịch nhắc nhở (AlarmManager)
    ├── BootReceiver                                          # Đặt lại lịch sau khi khởi động máy
    ├── BackupManager                                         # Sao lưu / phục hồi dữ liệu
    ├── PrefsHelper                                           # SharedPreferences
    ├── LocaleHelper                                          # Đa ngôn ngữ (vi / en)
    └── QuickParseUtil                                        # Phân tích nhập liệu nhanh
```

---

## 4. Mô hình dữ liệu Firestore

Dữ liệu được tổ chức theo từng người dùng (per-user), định nghĩa tập trung trong `FirestorePaths.java`:

```
users/{uid}
├── wallets/{walletId}          # Ví / nguồn tiền
├── categories/{categoryId}     # Danh mục thu chi
├── transactions/{txId}         # Giao dịch
├── budgets/{budgetId}          # Ngân sách theo tháng
├── savings_goals/{goalId}      # Mục tiêu tiết kiệm
├── recurring/{ruleId}          # Quy tắc định kỳ
└── challenges/{challengeId}    # Thử thách tiết kiệm
```

Ví dụ cấu trúc một **Transaction**: `type` (thu/chi), `amount`, `categoryId`, `walletId`, `note`, `date`, `mood`, `regretFlag` (đánh dấu chi tiêu hối tiếc), `recurringRuleId`, `createdAt`, `updatedAt`.

---

## 5. Chức năng chính

**Xác thực & onboarding** — Đăng ký, đăng nhập qua Firebase Auth; màn hình Splash và Onboarding cho người dùng mới.

**Quản lý giao dịch** — Thêm/sửa/xoá giao dịch thu–chi; thêm nhanh (QuickAdd); xem chi tiết qua bottom sheet; ghi nhận cảm xúc (mood) và đánh dấu chi tiêu hối tiếc.

**Quản lý ví** — Nhiều ví/nguồn tiền, tính số dư tự động, hỗ trợ thanh toán.

**Danh mục** — Danh mục thu/chi có biểu tượng, gợi ý danh mục tự động (`CategorySuggester`).

**Ngân sách** — Lập ngân sách theo tháng, phân bổ và tái phân bổ, cảnh báo khi vượt ngân sách (`BudgetChecker`).

**Mục tiêu tiết kiệm** — Đặt và theo dõi tiến độ các mục tiêu tài chính.

**Giao dịch định kỳ** — Quy tắc định kỳ (ngày/tuần/tháng) tự động sinh giao dịch qua `RecurringTransactionWorker` (WorkManager).

**Thử thách tiết kiệm** — Các challenge giúp tạo thói quen tiết kiệm.

**Báo cáo & phân tích** — Biểu đồ (MPAndroidChart) và phân tích sức khoẻ tài chính (`InsightsEngine`, `FinancialInsights`).

**Nhắc nhở** — Thông báo nhắc ghi chép chi tiêu qua AlarmManager + NotificationChannel, đặt lại lịch sau khi khởi động máy (`BootReceiver`).

**Bảo mật** — Khoá ứng dụng bằng PIN/sinh trắc học (Biometric + Security-Crypto, `LockActivity`, `SecurityViewModel`).

**Cá nhân hoá** — Đa ngôn ngữ (vi/en), tuỳ chỉnh định dạng tiền tệ, sao lưu/phục hồi dữ liệu.

---

## 6. Công nghệ & thư viện

| Nhóm | Thư viện |
|---|---|
| Kiến trúc | AndroidX Lifecycle (ViewModel, LiveData) 2.7.0, Navigation Component |
| Backend | Firebase BoM 33.7.0 (Auth, Firestore) |
| Giao diện | Material Components 1.12, ConstraintLayout, RecyclerView, CardView, Fragment |
| Biểu đồ | MPAndroidChart 3.1.0 |
| Tác vụ nền | WorkManager 2.9.0 |
| Bảo mật | Biometric 1.1.0, Security-Crypto |
| Icon | Mikepenz Iconics + Google Material Typeface |
| Test | JUnit 4.13.2 (unit test cho domain/usecase và util) |

---

## 7. Điểm cần lưu ý (technical debt)

- **Chưa dùng Dependency Injection** (Hilt/Dagger): Repository được khởi tạo trực tiếp bằng `new` trong ViewModel → khó test và thay thế.
- **Toàn bộ viết bằng Java**, chưa tận dụng Kotlin/Coroutines/Flow vốn được Google khuyến nghị (Kotlin-first từ 2019).
- **Chưa có CSDL local** (Room): phụ thuộc hoàn toàn Firestore, hạn chế khả năng dùng offline.
- Thư mục `docs/` đã có sẵn nhiều tài liệu tham khảo: `APP_OVERVIEW.md`, `DATA_MODEL.md`, `BUSINESS_RULES.md`, `TECHNICAL_DEBT.md`, `TEST_PLAN.md`, `MIGRATION_PLAN.md`...

---

*Tài liệu được tạo tự động từ việc phân tích source code.*
