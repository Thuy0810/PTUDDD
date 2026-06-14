# TÀI LIỆU DỰ ÁN

Thư mục này chứa tài liệu kỹ thuật và nghiệp vụ cho dự án **Quản lý Chi tiêu** (PTUDDD).

## Danh sách file

| File | Mục đích |
|---|---|
| `APP_OVERVIEW.md` | Tổng quan ứng dụng, stack, cấu trúc thư mục, 21 module nghiệp vụ, navigation map. |
| `BUSINESS_RULES.md` | Mô tả chi tiết 21 module nghiệp vụ: mục đích, màn hình, model, repository, collection Firestore, luồng, ràng buộc, lỗi, đề xuất, ưu tiên. |
| `DATA_MODEL.md` | Mô tả 9 model dữ liệu, kiểu trường, collection path, trạng thái migration. |
| `DESIGN_SYSTEM.md` | Bảng màu, typography, spacing, component style, format tiền. |
| `USER_GUIDE.md` | Hướng dẫn sử dụng cho người dùng cuối (đăng ký, ví, giao dịch, ngân sách, mục tiêu, sao lưu, bảo mật). |
| `TECHNICAL_DEBT.md` | Danh sách 50 vi phạm / lỗi kỹ thuật đã phát hiện, phân loại mức độ. |
| `MIGRATION_PLAN.md` | Kế hoạch chuyển `double` → `long` cho tất cả trường tiền, dùng `MoneyValueParser` để tương thích dữ liệu cũ. |
| `TEST_PLAN.md` | Test cho `MoneyValueParser`, `DateUtils`, `BudgetService`, `TransferService`, `TransactionRepository`, `BalanceCalculator`, manual checklist. |

## Tài liệu kế hoạch

File kế hoạch tổng thể nằm tại `c:\Users\nguye\.cursor\plans\r_C3_A0_so_C3_A1t__26_s_E1_BB_ADa_l_E1_BB_97i__E1_BB_A9ng_d_E1_BB_A5ng_qu_E1_BA_A3n_l_C3_BD_chi_ti_C3_AAu_0e6dd7e4.plan.md`.

## Quy ước cốt lõi

1. **Tiền tệ:** tất cả VND lưu trong code là `long`. `MoneyValueParser.toLong(Object)` đọc an toàn cả `Double` cũ.
2. **Múi giờ:** luôn `Asia/Ho_Chi_Minh` qua `DateUtils.VIETNAM` / `DateUtils.newCalendar()`.
3. **Kiến trúc:** UI không gọi `FirebaseFirestore.getInstance()`. Mọi truy cập Firestore qua Repository, nghiệp vụ phức tạp trong `domain/usecase/`.
4. **Tiền atomic:** thêm/sửa/xoá transaction phải là Firestore transaction; transfer phải cập nhật 2 ví cùng lúc.
5. **Ví:** có `isArchived`, không xoá ví đang có giao dịch (chỉ archive).
6. **Ngân sách:** chỉ tính `expense`, bỏ qua `income` và `transfer`.
7. **Migrate không phá dữ liệu:** KHÔNG cập nhật hàng loạt Firestore, chỉ parse tương thích khi đọc.
