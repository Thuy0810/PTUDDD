# Quản Lý Chi Tiêu (Android Java + Firebase)

Ứng dụng quản lý thu chi cá nhân: đăng ký/đăng nhập, ví, danh mục, giao dịch, ngân sách, báo cáo biểu đồ, mục tiêu tiết kiệm, insight tài chính.

## Yêu cầu

- Android Studio Hedgehog trở lên
- JDK 17
- Tài khoản Firebase

## Cấu hình Firebase

1. Vào [Firebase Console](https://console.firebase.google.com/) → tạo project.
2. **Authentication** → bật **Email/Password**.
3. **Firestore Database** → tạo database.
4. Deploy rules từ thư mục [`firebase/firestore.rules`](firebase/firestore.rules).
5. **Project settings** → thêm app Android, package: `com.expensemanager.app`.
6. Tải `google-services.json` → thay file [`app/google-services.json`](app/google-services.json) (file hiện tại chỉ là placeholder).

## Chạy app

1. Mở thư mục `PTUDDD` bằng Android Studio.
2. **Gradle JDK = 17** (bắt buộc nếu máy đang dùng Java 21/25):
   - `File` → `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Gradle`
   - **Gradle JDK** → chọn `jbr-17` / `Embedded JDK 17` hoặc tải **JDK 17**
   - Lỗi `Incompatible Gradle JVM` thường do Gradle chạy bằng Java 25 trong khi AGP 8.x cần **JDK 17**
3. `File` → `Sync Project with Gradle Files`
4. Chạy trên emulator/thiết bị (cần Google Play Services)

## Cấu trúc chính

```
app/src/main/java/com/expensemanager/app/
├── data/model/          # Transaction, Wallet, Category, Budget...
├── data/repository/     # Auth, Firestore repositories
├── viewmodel/           # HomeViewModel
├── ui/                  # Activities & Fragments
└── util/                # InsightsEngine, MoneyFormat...
```

## Tính năng

| Mức | Nội dung |
|-----|----------|
| 1 | Auth, CRUD giao dịch, danh mục seed, tổng thu/chi/số dư |
| 2 | Ví, ngân sách, báo cáo MPAndroidChart, mục tiêu, recurring, nhắc nhở |
| 3 | Điểm sức khỏe tài chính, gợi ý danh mục, nhập nhanh, PIN/vân tay, export |

## Bottom navigation

Trang chủ | Ngân sách | Giao dịch | Báo cáo | Cá nhân

## Lưu ý

- Firestore query theo tháng cần index `date` — deploy `firebase/firestore.indexes.json` nếu báo lỗi index.
- Đổi `google-services.json` thật trước khi demo đăng nhập Firebase.
