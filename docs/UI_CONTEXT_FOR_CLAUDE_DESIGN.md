# CONTEXT THIẾT KẾ LẠI GIAO DIỆN — App "Quản Lý Chi Tiêu"

> Đính kèm file này khi nhờ Claude vẽ mockup. Mục tiêu: vẽ ĐÚNG chức năng thật của app
> hiện tại, được phép tinh chỉnh nhẹ để UX/UI chuẩn hơn, KHÔNG bỏ bớt tính năng.

## 0. App là gì
Ứng dụng quản lý thu chi cá nhân (Android, tiếng Việt). Người dùng đăng nhập, ghi giao dịch
thu/chi theo ví & danh mục, đặt ngân sách, xem báo cáo biểu đồ, đặt mục tiêu tiết kiệm,
giao dịch định kỳ, và theo dõi "sức khỏe tài chính".

## 1. Ràng buộc quan trọng (đừng vẽ vượt khả năng)
- **Phải code được bằng Java chuẩn + Android Studio** (Java 17, KHÔNG Kotlin, KHÔNG Jetpack
  Compose). Layout XML + Material Components + ViewBinding. Mockup chỉ để xem trước, nhưng
  mọi bố cục/thành phần đề xuất phải là thứ dựng lại được bằng XML Android tiêu chuẩn
  → tránh hiệu ứng/layout chỉ web mới làm được (vd grid phức tạp, animation CSS nâng cao).
- **Chỉ có 2 loại giao dịch: Thu (income) và Chi (expense). KHÔNG có chuyển tiền giữa ví. KHÔNG có tag/nhãn.**
- Định dạng tiền: ký hiệu nằm SAU số, ngăn cách hàng nghìn bằng dấu chấm, không thập phân.
  Ví dụ: `1.500.000 VND`. (Số âm cho khoản chi: `-250.000 VND`.)
- Tiếng Việt toàn bộ.

## 2. Phong cách mong muốn: XANH DƯƠNG + CAM, hiện đại
- Nền app `#F4F7FE`; thẻ trắng `#FFFFFF`, bo góc 18–24px, đổ bóng nhẹ.
- Header gradient xanh: `#1D4ED8 → #2563EB → #1E3A8A`.
- Xanh chính `#2563EB`, xanh đậm `#1D4ED8`, xanh nhạt `#DBEAFE`, xanh soft `#EFF4FF`.
- Cam `#FB8C2E`, cam đậm `#EA7317`, nền cam nhạt `#FFF1E3` (dùng cho nhãn pill, nút phụ).
- Thu = xanh lá `#4CAF50`; Chi = đỏ `#FF6B6B`.
- Chữ chính `#1F2937`, chữ phụ `#6B7280`. Font sans-serif.

## 3. Điều hướng (Bottom Navigation — TỐI ĐA 5 mục, gồm cả nút ＋)
Thanh menu dưới chỉ có đúng 5 mục theo thứ tự:
**Tổng quan | Ngân sách | ＋ Thêm | Báo cáo | Cá nhân**
- Nút **＋ Thêm** nằm GIỮA thanh nav (nổi bật, tô cam), mở màn Thêm giao dịch.
- Tab đang chọn tô xanh `#2563EB`.
- **"Giao dịch" KHÔNG nằm trên thanh menu** (vì giới hạn 5 mục). Truy cập danh sách giao dịch
  qua nút **"Xem tất cả"** ở mục Giao dịch gần đây trên màn **Tổng quan**.

## 4. Các màn hình chính (4 tab bấm chuyển được + màn Giao dịch)
> 4 tab trên thanh menu: Tổng quan, Ngân sách, Báo cáo, Cá nhân. Màn Giao dịch (4.3)
> KHÔNG phải tab — mở từ nút "Xem tất cả" ở Tổng quan.

### 4.1. TỔNG QUAN (Trang chủ)
- Header gradient xanh: nhãn "Số dư", **tổng số dư** lớn (trắng), icon ví.
- Chip "Còn lại trong ngân sách: X VND" (ẩn nếu chưa đặt ngân sách).
- 2 thẻ cạnh nhau: **Thu tháng này** (xanh lá), **Chi tháng này** (đỏ).
- Dòng phụ: "Hôm nay đã chi ..." và "Danh mục chi nhiều nhất ...".
- Thẻ cảnh báo (đỏ) hiện khi vượt ngưỡng ngân sách (chỉ hiện khi có cảnh báo).
- Thẻ **Sức khỏe tài chính**: điểm số (nhãn pill cam) + trạng thái chữ + 3 chỉ số:
  *Tỉ lệ tiết kiệm* (xanh lá), *% dùng ngân sách* (đỏ), *So với tháng trước* (xanh dương);
  khối dự báo nền xanh nhạt: *Ngân sách/ngày* và *Dự báo chi cuối tháng*.
- Mục **Giao dịch gần đây** + nút "Xem tất cả" (pill cam) mở màn Giao dịch (4.3)
  + danh sách vài giao dịch.

### 4.2. NGÂN SÁCH (tab trên thanh menu)
- Bộ chọn tháng (yyyy-MM).
- Thẻ tổng quan tháng: hạn mức tổng, đã chi, còn lại, % dùng + thanh tiến độ.
- Nút "Đặt ngân sách tháng" và "Phân bổ" (đặt ngân sách theo từng danh mục).
- Danh sách ngân sách theo danh mục: tên danh mục + icon, đã chi / hạn mức,
  thanh tiến độ đổi màu: **xanh an toàn / cam ≥80% / đỏ ≥100%**.
- Ngưỡng cảnh báo mặc định 80% và 90%.

### 4.3. GIAO DỊCH (không phải tab — mở từ nút "Xem tất cả" ở Tổng quan)
- Thanh lọc: theo loại (Tất cả/Thu/Chi), ví, ngày, số tiền, ô tìm kiếm.
- Danh sách giao dịch nhóm theo ngày: icon danh mục (tròn), tên danh mục + ghi chú,
  tên ví, số tiền (thu xanh lá `+`, chi đỏ `-`).
- Bấm 1 giao dịch → bottom sheet **Chi tiết** (xem/sửa/xoá). Xoá thì số dư ví tự hoàn tác.
- Nút ＋ thêm giao dịch.
- Màn **Thêm/Sửa giao dịch** (mở từ nút ＋): chọn Thu/Chi, nhập số tiền (bàn phím số lớn),
  chọn ví, chọn danh mục, chọn ngày, ghi chú (tuỳ chọn), nút Lưu.

### 4.4. BÁO CÁO
- Bộ chọn khoảng thời gian: Tuần / Tháng / Quý / Năm; lọc theo ví, danh mục.
- **Biểu đồ tròn**: tỉ lệ chi theo danh mục (có chú thích + %).
- **Biểu đồ cột**: so sánh Thu vs Chi theo thời gian (vài kỳ gần nhất).
- Tóm tắt: tổng thu, tổng chi, chênh lệch trong kỳ.

### 4.5. CÁ NHÂN
- Đầu trang: avatar + tên người dùng + email (nút sửa hồ sơ).
- Danh sách mục (mỗi mục 1 hàng, icon trái + mũi tên phải):
  **Ví của tôi, Danh mục, Mục tiêu tiết kiệm, Giao dịch định kỳ,
  Thử thách tiết kiệm, Bảo mật (PIN/vân tay), Cài đặt, Đăng xuất.**

## 5. Màn phụ (mở từ Cá nhân / các tab) — vẽ nếu còn thời gian
- **Ví của tôi:** danh sách ví (tên, loại: Tiền mặt/Ngân hàng/Ví điện tử/Tiết kiệm, số dư),
  nút ＋ tạo ví; bấm ví để sửa / lưu trữ / điều chỉnh số dư (nhập số dư mới + lý do).
- **Danh mục:** 10 danh mục chi + 5 danh mục thu mặc định, nhóm Thiết yếu/Cần thiết/Mong muốn/Khác;
  hỗ trợ danh mục cha–con; nút ＋ tạo danh mục.
- **Mục tiêu tiết kiệm:** thẻ mục tiêu (tên, đã đạt / cần đạt, deadline, thanh tiến độ,
  trạng thái Đang chạy / Hoàn thành / Quá hạn); bấm để đóng góp tiền (trừ ví nguồn).
- **Giao dịch định kỳ:** danh sách rule (loại, số tiền, ví, danh mục, chu kỳ ngày/tuần/tháng/năm,
  ngày bắt đầu–kết thúc, bật/tắt).
- **Cài đặt:** Dark mode (Sáng/Tối/Theo hệ thống), Nhắc nhở (bật/tắt + giờ), Tiền tệ
  (ký hiệu, vị trí, thập phân, locale), Sao lưu (xuất/nhập JSON), Đổi mật khẩu.
- **Bảo mật:** Đặt/đổi PIN (4–6 số), bật sinh trắc học.
- **Đăng nhập / Đăng ký:** email, mật khẩu (≥6 ký tự), tên hiển thị.
- **Khoá app (Lock):** nhập PIN hoặc vân tay khi mở app.

## 6. Yêu cầu khi vẽ
1. Vẽ artifact HTML interactive, khung điện thoại ~390px. Thanh menu dưới đúng 5 mục
   (Tổng quan, Ngân sách, ＋, Báo cáo, Cá nhân) — 4 tab bấm chuyển được + nút ＋ ở giữa.
   Vẽ thêm màn Giao dịch (mở từ nút "Xem tất cả" ở Tổng quan).
2. Bám sát chức năng & dữ liệu mô tả ở trên; dùng dữ liệu mẫu tiếng Việt hợp lý.
3. **Được phép tinh chỉnh nhẹ UX/UI cho chuẩn hơn** (khoảng cách, thứ bậc thông tin,
   gom nhóm, trạng thái rỗng, vi tương tác) — NHƯNG không được bỏ tính năng nào ở trên.
4. Nếu bạn đề xuất cải tiến UX, hãy liệt kê riêng cuối câu trả lời (mục "Gợi ý cải tiến")
   để tôi quyết định trước khi code.
```
