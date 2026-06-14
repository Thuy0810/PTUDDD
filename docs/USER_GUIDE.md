# HƯỚNG DẪN SỬ DỤNG

Tài liệu hướng dẫn cho người dùng cuối sử dụng ứng dụng Quản lý Chi tiêu.

> **Lưu ý:** Tên các tab trong ứng dụng: Tổng quan, Ngân sách, Giao dịch, Báo cáo, Cá nhân. Mục lục bên dưới dùng "Cá nhân" thay vì "Hồ sơ" cho chính xác.

## 1. Bắt đầu

### 1.1. Đăng ký tài khoản
1. Mở app → màn hình **Đăng nhập** → bấm **"Đăng ký"**.
2. Nhập email, mật khẩu (tối thiểu 6 ký tự), tên hiển thị.
3. Bấm **"Đăng ký"** → quay về **Đăng nhập**.

### 1.2. Đăng nhập
1. Nhập email + mật khẩu.
2. Bấm **"Đăng nhập"**.
3. Nếu đã bật **PIN**, nhập PIN hoặc dùng vân tay/khuôn mặt để mở khoá.

## 2. Ví tiền

### 2.1. Tạo ví mới
1. Vào tab **Cá nhân** → **Ví của tôi** → nút **"+"**.
2. Nhập tên ví (ví dụ: "Tiền mặt", "Vietcombank", "MoMo").
3. Chọn **loại ví**: Tiền mặt / Ngân hàng / Ví điện tử / Tiết kiệm.
4. Nhập **số dư ban đầu** (có thể = 0).
5. Bấm **Lưu**.

### 2.2. Sửa / Lưu trữ / Xoá ví
- **Sửa:** bấm vào ví → chỉnh sửa thông tin → Lưu.
- **Lưu trữ (archive):** nếu ví có giao dịch, bạn chỉ có thể lưu trữ thay vì xoá. Ví lưu trữ không hiển thị khi tạo giao dịch mới.
- **Xoá:** chỉ xoá được khi ví **chưa có giao dịch nào**.

### 2.3. Chuyển tiền giữa 2 ví
1. Tab **Cá nhân** → **Ví của tôi** → nút **"Chuyển tiền"** (hoặc vào thẳng 1 ví → "Chuyển").
2. Chọn **ví nguồn**, **ví nhận**, nhập **số tiền**, **ghi chú**.
3. Bấm **Xác nhận**. Hệ thống kiểm tra số dư, cập nhật đồng thời 2 ví và ghi log giao dịch chuyển tiền.

## 3. Ghi giao dịch

### 3.1. Thêm khoản thu / chi
1. Màn hình chính → nút **"+"** (FAB).
2. Chọn **Thu** hoặc **Chi**.
3. Nhập **số tiền**, chọn **ví**, **danh mục**, **ngày**, **ghi chú** (tuỳ chọn).
4. Bấm **Lưu**. Số dư ví tự động cập nhật.

### 3.2. Sửa / Xoá giao dịch
1. Vào tab **Giao dịch** → bấm vào 1 giao dịch.
2. Chỉnh sửa hoặc bấm **Xoá**. Số dư ví tự động hoàn tác.

## 4. Danh mục

1. Tab **Cá nhân** → **Danh mục**.
2. Có sẵn 10 danh mục chi + 5 danh mục thu mặc định (không xoá được).
3. Bấm **"+"** để tạo danh mục riêng, chọn nhóm (Thiết yếu / Cần thiết / Mong muốn / Khác).

## 5. Ngân sách

### 5.1. Đặt ngân sách tổng tháng
1. Tab **Ngân sách** → bấm **"Đặt ngân sách tháng"**.
2. Nhập **hạn mức** (VD: 5.000.000đ/tháng).
3. Có thể thêm **ngưỡng cảnh báo** (mặc định 80% và 90%).

### 5.2. Đặt ngân sách theo danh mục
1. Tab **Ngân sách** → chọn tháng → bấm **"Phân bổ"**.
2. Chọn **danh mục** → nhập **hạn mức cho danh mục đó**.

### 5.3. Xem cảnh báo
- Khi chi tiêu vượt ngưỡng (80%, 90%, 100%), hệ thống sẽ hiển thị cảnh báo trên màn hình chính.

## 6. Báo cáo

1. Tab **Báo cáo** → chọn khoảng thời gian (tuần/tháng/quý/năm), chọn ví, chọn danh mục.
2. Xem biểu đồ **tròn** (tỉ lệ chi theo danh mục) và **cột** (so sánh theo thời gian).

## 7. Giao dịch định kỳ

1. Tab **Cá nhân** → **Giao dịch định kỳ**.
2. Bấm **"+"** → chọn loại (thu/chi), số tiền, ví, danh mục, chu kỳ (hằng ngày/tuần/tháng/năm).
3. Thiết lập ngày bắt đầu và ngày kết thúc (tuỳ chọn).
4. Hệ thống tự động tạo giao dịch khi đến hạn, mỗi ngày chỉ tạo tối đa 1 giao dịch cho mỗi rule.
5. Hệ thống tự động tắt rule nếu đã qua ngày kết thúc.

## 8. Mục tiêu tiết kiệm

1. Tab **Cá nhân** → **Mục tiêu tiết kiệm** → **"+"**.
2. Nhập tên mục tiêu, số tiền cần đạt, deadline, ví nguồn.
3. Bấm vào mục tiêu → nhập số tiền muốn đóng góp → **Lưu**. Số tiền sẽ được trừ khỏi ví nguồn và cộng vào mục tiêu một cách tự động.
4. Khi số tiền đã đạt mục tiêu, trạng thái mục tiêu sẽ tự động chuyển sang **Hoàn thành**.
5. Mục tiêu quá hạn nhưng chưa đủ tiền sẽ được đánh dấu **Quá hạn**.

## 9. Cài đặt

1. Tab **Cá nhân** → **Cài đặt**.
2. Các tuỳ chọn:
   - **Dark mode**: Sáng / Tối / Theo hệ thống.
   - **Nhắc nhở**: Bật/tắt, đặt giờ nhắc (mặc định 21:00). Nhắc nhở sử dụng báo thức chính xác nếu thiết bị cho phép.
   - **Tiền tệ**: Ký hiệu (đ, $, ...), vị trí, số thập phân, locale.
   - **Sao lưu**: Xuất dữ liệu ra file JSON / Nhập từ file JSON.
   - **Đổi mật khẩu**.

## 10. Bảo mật

### 10.1. Đặt PIN
1. Tab **Cá nhân** → **Bảo mật** → **Đặt PIN**.
2. Nhập PIN 4-6 số → xác nhận.
3. PIN sẽ được hash bằng PBKDF2 và lưu trong EncryptedSharedPreferences.

### 10.2. Bật sinh trắc học (vân tay / khuôn mặt)
1. Tab **Cá nhân** → **Bảo mật** → **Sinh trắc học** → Bật.
2. Khi mở app sẽ ưu tiên dùng sinh trắc học (nếu thiết bị hỗ trợ).

### 10.3. Quên PIN
- Liên hệ hỗ trợ để được reset (hiện chưa có cơ chế tự reset vì lý do bảo mật).

## 11. Câu hỏi thường gặp

**Hỏi:** Tôi có thể xoá ví đang có giao dịch không?
**Đáp:** Không. Bạn chỉ có thể **lưu trữ** ví. Ví lưu trữ sẽ ẩn khỏi danh sách chọn khi tạo giao dịch mới nhưng vẫn giữ trong lịch sử.

**Hỏi:** Chuyển tiền có ảnh hưởng đến thu/chi không?
**Đáp:** Không. Chuyển tiền chỉ thay đổi số dư giữa 2 ví, không tính vào thu nhập/chi tiêu và không ảnh hưởng ngân sách.

**Hỏi:** Số dư ví bị lệch với lịch sử giao dịch?
**Đáp:** Vào **Ví của tôi** → chọn ví → **Đối soát** (tính năng sẽ được thêm trong giai đoạn sau). Hiện tại hãy liên hệ hỗ trợ.

**Hỏi:** Khi nào tôi nên sao lưu dữ liệu?
**Đáp:** Nên sao lưu **mỗi tuần** hoặc sau khi nhập nhiều giao dịch. File backup lưu ở bộ nhớ trong và có thể chia sẻ qua email/cloud.
