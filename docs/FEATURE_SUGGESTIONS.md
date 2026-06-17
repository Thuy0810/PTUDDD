# ĐỀ XUẤT TÍNH NĂNG / NGHIỆP VỤ BỔ SUNG

> Mục đích: liệt kê các tính năng có thể thêm để **bạn duyệt**. Tick `[x]` vào ô của tính năng muốn làm, mình sẽ bắt tay vào hiện thực (model + repository/usecase + màn hình).
>
> Trạng thái nền tảng hiện tại: chỉ giao dịch `income`/`expense` (đã bỏ transfer), danh mục cha/con, ngân sách theo tháng + danh mục + phân bổ lại, mục tiêu tiết kiệm, giao dịch định kỳ, thử thách, insight/sức khoẻ tài chính, nhắc nhở, backup JSON, PIN/sinh trắc học, đa ngôn ngữ.

Quy ước cột:
- **Giá trị**: lợi ích cho người dùng.
- **Phức tạp**: Thấp / TB / Cao (ước lượng công sức).
- **Tận dụng sẵn**: phần hạ tầng đã có giúp làm nhanh hơn.

---

## A. Tận dụng dữ liệu sẵn có (khuyến nghị làm trước)

- [ ] **A1. Khoản nợ & cho vay (Debt/Loan)**
  - Mô tả: theo dõi "ai nợ mình / mình nợ ai", ngày hẹn trả, đánh dấu đã trả, nhắc hạn.
  - Giá trị: Cao · Phức tạp: TB · Tận dụng: model gần giống `SavingsGoal`, dùng `ReminderScheduler`.
  - Cần thêm: model `Debt`, `DebtRepository`/`DebtService` (atomic khi tất toán trừ/cộng ví), màn hình danh sách + thêm/sửa.

- [ ] **A2. Hoá đơn định kỳ & nhắc thanh toán (Bills)**
  - Mô tả: hoá đơn lặp lại (điện, nước, thuê nhà) → nhắc trước hạn N ngày, đánh dấu đã trả → sinh giao dịch chi.
  - Giá trị: Cao · Phức tạp: TB · Tận dụng: mở rộng từ `RecurringRule` + notification có sẵn.

- [ ] **A3. Ngân sách cuốn chiếu (Rollover budget)**
  - Mô tả: phần dư/âm của ngân sách tháng này tự cộng/trừ sang tháng sau.
  - Giá trị: TB · Phức tạp: TB · Tận dụng: đã có `BudgetReallocation` + `BudgetService`.

- [ ] **A4. Cảnh báo chi vượt số dư khi lưu giao dịch**
  - Mô tả: khi khoản chi > số dư ví → hiện dialog cảnh báo trước khi lưu (vẫn cho phép xác nhận).
  - Giá trị: TB · Phức tạp: Thấp · Tận dụng: ngay trong `AddTransactionActivity` + `addAtomic`.

---

## B. Phân tích & thông minh

- [ ] **B1. Dự báo dòng tiền nhiều tháng**
  - Mô tả: dựa trên giao dịch định kỳ + xu hướng chi tiêu, dự báo số dư các tháng tới, cảnh báo "có thể âm ví vào ngày X".
  - Giá trị: Cao · Phức tạp: Cao · Tận dụng: `InsightsEngine`, `RecurringRule`.

- [ ] **B2. Gợi ý danh mục tự học theo lịch sử**
  - Mô tả: khi nhập ghi chú, gợi ý danh mục dựa trên các lần nhập trước (không chỉ từ khoá cứng).
  - Giá trị: TB · Phức tạp: TB · Tận dụng: đã có `CategorySuggester`.

- [ ] **B3. Báo cáo so sánh nhiều kỳ + xuất PDF/Excel**
  - Mô tả: so sánh chi tiêu theo danh mục giữa các tháng/quý; xuất báo cáo (ngoài backup JSON hiện có).
  - Giá trị: Cao · Phức tạp: TB · Tận dụng: `ReportFragment`, MPAndroidChart.

---

## C. Nhập liệu & trải nghiệm

- [ ] **C1. Widget màn hình chính + nhập nhanh**
  - Mô tả: widget hiển thị số dư/chi hôm nay; chạm để thêm giao dịch nhanh.
  - Giá trị: Cao · Phức tạp: TB.

- [ ] **C2. Đọc thông báo ngân hàng/SMS để tạo giao dịch tự động**
  - Mô tả: bắt notification/SMS biến động số dư → đề xuất tạo giao dịch (cần quyền + lưu ý quyền riêng tư).
  - Giá trị: Cao · Phức tạp: Cao.

- [ ] **C3. Đính kèm ảnh hoá đơn cho giao dịch**
  - Mô tả: chụp/đính ảnh biên lai, lưu lên Firebase Storage, xem trong chi tiết giao dịch.
  - Giá trị: TB · Phức tạp: TB · Tận dụng: `TransactionDetailBottomSheet`.

- [ ] **C4. Tìm kiếm & lọc nâng cao trong danh sách giao dịch**
  - Mô tả: lọc kết hợp (danh mục + ví + khoảng tiền + khoảng ngày) và lưu bộ lọc yêu thích.
  - Giá trị: TB · Phức tạp: Thấp · Tận dụng: `TransactionListFragment` đã có sẵn nhiều bộ lọc.

---

## D. Mở rộng phạm vi

- [ ] **D1. Đa tiền tệ + tỷ giá**
  - Mô tả: ví theo nhiều loại tiền, quy đổi khi báo cáo.
  - Giá trị: TB · Phức tạp: Cao (ảnh hưởng toàn bộ format tiền hiện đang VND/long).

- [ ] **D2. Ví nhóm / chia sẻ gia đình**
  - Mô tả: nhiều người dùng cùng truy cập một bộ dữ liệu (đồng sở hữu).
  - Giá trị: Cao · Phức tạp: Cao (phải đổi Firestore rules & mô hình quyền).

- [ ] **D3. Đăng nhập Google/Apple + Quên mật khẩu + Validate đăng ký**
  - Mô tả: bổ sung phương thức đăng nhập, luồng quên mật khẩu, validate email/độ dài mật khẩu phía client.
  - Giá trị: TB · Phức tạp: Thấp–TB · Tận dụng: `AuthRepository`.

- [ ] **D4. (Tuỳ chọn) Khôi phục lại Chuyển tiền giữa 2 ví**
  - Mô tả: nếu cần lại, hiện thực đúng chuẩn qua `TransferService` (atomic 2 ví trong 1 Firestore Transaction), không tính vào thu/chi.
  - Giá trị: TB · Phức tạp: TB. *(Đã bị gỡ theo yêu cầu — chỉ làm nếu bạn muốn dùng lại.)*

---

## E. Dọn nợ kỹ thuật còn lại (không phải tính năng, nhưng nên cân nhắc)

- [ ] **E1. Chuẩn hoá timezone**: thay `Calendar.getInstance()`/`new Date()` rải rác bằng `DateUtils` (Asia/Ho_Chi_Minh).
- [ ] **E2. ViewBinding cho các màn hình còn thiếu** (Splash, WalletList, BudgetList, Challenge...).
- [ ] **E3. Rà lại `TransactionRepository.updateAtomic/deleteAtomic`** sau khi bỏ transfer (đơn giản hoá logic, thêm unit test).

---

### Cách duyệt
Sửa file này, đổi `[ ]` → `[x]` ở các mục muốn làm, rồi báo mình. Mình sẽ ưu tiên theo thứ tự bạn chọn và lên thiết kế chi tiết trước khi code.
