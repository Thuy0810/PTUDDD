# TỔNG QUAN ỨNG DỤNG QUẢN LÝ CHI TIÊU

## Thông tin chung

- **Tên dự án:** PTUDDD (Personal Expense & Budget Manager)
- **Package chính:** `com.expensemanager.app`
- **Ngôn ngữ:** Java 17
- **Kiểu ứng dụng:** Android native
- **Hướng dẫn sử dụng tiếng Việt**

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
| minSdk / targetSdk / compileSdk | 24 / 34 / 34 |

## Cấu trúc thư mục nguồn

```
app/src/main/java/com/expensemanager/app/
├── ExpenseApplication.java          # Khởi tạo notification channel, áp dụng format tiền
├── data/
│   ├── SeedData.java                # Khởi tạo danh mục & ví mặc định
│   ├── firebase/FirestorePaths.java # Đường dẫn collection chuẩn
│   ├── model/                       # 9 POJO model
│   └── repository/                  # 7 repository
├── domain/
│   └── usecase/                     # Service / UseCase (đang trống, sẽ thêm TransferService, BudgetService, v.v.)
├── ui/
│   ├── auth/                        # Splash, Login, Register
│   ├── main/MainActivity.java       # BottomNavigation + FAB
│   ├── home/HomeFragment.java       # Tổng quan
│   ├── transaction/                 # AddTransaction, TransactionList
│   ├── wallet/                      # WalletList, Transfer, WalletPaymentDialog
│   ├── category/                    # CategoryList
│   ├── budget/                      # BudgetFragment, BudgetList, BudgetEdit, BudgetAllocation + adapters
│   ├── report/ReportFragment.java   # Biểu đồ
│   ├── goal/GoalListActivity.java
│   ├── challenge/ChallengeListActivity.java
│   ├── profile/                     # ProfileFragment, EditProfile, Settings, Security
│   ├── security/LockActivity.java   # Khoá PIN / Sinh trắc học
│   └── adapter/                     # RecyclerView adapters dùng chung
├── util/                            # MoneyFormat, DateUtils, QuickParseUtil, BackupManager, Reminder, Prefs, ...
└── viewmodel/                       # HomeViewModel + HomeViewModelHolder
```

## 19 module nghiệp vụ

| # | Module | Màn hình chính | File đặc trưng |
|---|---|---|---|
| 1 | Đăng ký | RegisterActivity | `ui/auth/RegisterActivity.java` |
| 2 | Đăng nhập | LoginActivity | `ui/auth/LoginActivity.java` |
| 3 | Splash & điều hướng | SplashActivity | `ui/auth/SplashActivity.java` |
| 4 | Trang chủ | HomeFragment | `ui/home/HomeFragment.java` |
| 5 | Giao dịch thu | AddTransactionActivity | `ui/transaction/AddTransactionActivity.java` |
| 6 | Giao dịch chi | AddTransactionActivity | (chung với thu) |
| 7 | Chuyển tiền | TransferActivity | `ui/wallet/TransferActivity.java` |
| 8 | Danh sách giao dịch | TransactionListFragment | `ui/transaction/TransactionListFragment.java` |
| 9 | Ví tiền | WalletListActivity | `ui/wallet/WalletListActivity.java` |
| 10 | Danh mục | CategoryListActivity | `ui/category/CategoryListActivity.java` |
| 11 | Ngân sách | BudgetFragment, BudgetEditActivity, BudgetAllocationActivity | `ui/budget/*` |
| 12 | Báo cáo & biểu đồ | ReportFragment | `ui/report/ReportFragment.java` |
| 13 | Giao dịch định kỳ | ChallengeListActivity | `ui/challenge/ChallengeListActivity.java` |
| 14 | Mục tiêu tiết kiệm | GoalListActivity | `ui/goal/GoalListActivity.java` |
| 15 | Thử thách tiết kiệm | (chưa rõ màn hình — ChallengeListActivity được dùng cho recurring, có thể trùng) | (cần xác nhận với stakeholder) |
| 16 | Insight tài chính | HomeFragment (tích hợp) | `util/InsightsEngine.java` |
| 17 | Nhắc nhở | Settings + ReminderReceiver | `util/ReminderScheduler.java` |
| 18 | Sao lưu & xuất dữ liệu | SettingsActivity | `util/BackupManager.java` |
| 19 | Hồ sơ cá nhân | ProfileFragment, EditProfileActivity | `ui/profile/*` |
| 20 | Cài đặt | SettingsActivity | `ui/profile/SettingsActivity.java` |
| 21 | PIN & sinh trắc học | SecurityActivity, LockActivity | `ui/security/*`, `ui/profile/SecurityActivity.java` |

## Bản đồ điều hướng chính

```
SplashActivity
   ├── (chưa đăng nhập) → LoginActivity → RegisterActivity
   └── (đã đăng nhập) → MainActivity
                              ├── HomeFragment
                              ├── TransactionListFragment
                              ├── BudgetFragment
                              │     ├── BudgetListActivity
                              │     ├── BudgetEditActivity
                              │     └── BudgetAllocationActivity
                              ├── ReportFragment
                              └── ProfileFragment
                                     ├── EditProfileActivity
                                     ├── SettingsActivity
                                     │     └── (xuất/nhập backup)
                                     ├── SecurityActivity
                                     ├── GoalListActivity
                                     ├── ChallengeListActivity
                                     ├── CategoryListActivity
                                     └── WalletListActivity
                                            ├── AddTransactionActivity
                                            └── TransferActivity
```

## Quy tắc build & chạy

- Mở bằng **Android Studio** (phiên bản 2023.2 trở lên).
- Sync project sau khi clone (`File → Sync Project with Gradle Files`).
- Cấu hình Firebase: file `google-services.json` đã có sẵn trong `app/`.
- Build & run trực tiếp lên thiết bị thật hoặc emulator Android 7.0+ (API 24).
- Không có script Gradle đặc biệt; chỉ dùng `assembleDebug` / `installDebug`.

## Quy ước chung

- Tiền tệ mặc định: VND (`đ`), `MoneyFormat.formatLong(long)`, `MoneyFormat.format(double)`.
- Múi giờ nghiệp vụ: `Asia/Ho_Chi_Minh` (xem `util/DateUtils.java`).
- Ngày giao dịch lưu UTC (`Timestamp`), hiển thị theo timezone VN.
- Định dạng ngày ngắn: `dd/MM/yyyy`. Định dạng tháng khoá: `yyyy-MM`.
- Mọi thay đổi tiền phải đi qua nghiệp vụ (`TransactionRepository.addAtomic`, `TransferService`, ...), không tự ý ghi `currentBalance` từ UI.
- Tài liệu chi tiết xem các file còn lại trong `docs/`.
