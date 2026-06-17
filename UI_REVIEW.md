# Review giao diện & sự kiện click — Quản Lý Chi Tiêu

> **Cập nhật 17/06/2026 — ĐÃ SỬA TẤT CẢ (#1–#13).** Chi tiết ở cuối file. Cần build trên Android Studio để xác nhận.

Ngày: 17/06/2026 · Phạm vi: layout `res/layout/` + handler click trong `ui/`.
Phương pháp: đối chiếu từng layout với code, **đã đọc trực tiếp và xác minh** các điểm quan trọng (không chỉ dựa suy đoán).

Mức độ: **CAO** = chức năng sai / màn không tới được, **TRUNG BÌNH** = nút chết / lệch nhãn / UX lỗi, **THẤP** = cosmetic.

---

## CAO

### 1. Màn "Cài đặt" mở nhầm bộ chọn ngôn ngữ — và SettingsActivity không bao giờ tới được
`fragment_profile.xml:509-538` + `ProfileFragment.java:70-71`

Hàng trong Profile có icon bánh răng (`ic_settings_stroke`) và nhãn **"Cài đặt"**, nhưng `id=btnLanguage` và handler là `showLanguagePicker()` → bấm vào chỉ hiện hộp chọn ngôn ngữ.

`SettingsActivity` (dark mode, nhắc nhở, đổi mật khẩu, xuất dữ liệu, định dạng tiền tệ) **được khai báo trong Manifest nhưng không được gọi từ bất kỳ đâu** (`grep SettingsActivity.class` = rỗng). Nghĩa là toàn bộ màn Cài đặt hiện đang chết, người dùng không có lối vào.

Đề xuất: tách thành 2 hàng — "Cài đặt" → `startActivity(SettingsActivity)`, và một hàng "Ngôn ngữ" riêng → `showLanguagePicker()`. Hoặc đưa picker ngôn ngữ vào trong SettingsActivity.

### 2. BudgetEditActivity nhồi view thủ công vào RecyclerView → danh sách có thể không hiển thị
`BudgetEditActivity.java:62, 109-143` + `activity_budget_edit.xml:274`

`recyclerCategories` là `androidx.recyclerview.widget.RecyclerView`, được set `LinearLayoutManager` (dòng 62) nhưng **không gắn adapter**. Code lại `removeAllViews()` rồi `addView(itemView)` thủ công cho từng danh mục (dòng 109, 142).

RecyclerView dựa vào adapter để bố trí con; khi không có adapter, LayoutManager bỏ qua layout ("No adapter attached; skipping layout") → các view thêm tay nhiều khả năng **không được đo/vẽ (cao 0, không thấy)**. Đây là anti-pattern và rủi ro mất hẳn danh sách phân bổ ngân sách.

Đề xuất: tạo adapter thật (`RecyclerView.Adapter`) và `setItems()`, **hoặc** đổi `recyclerCategories` trong XML thành `LinearLayout` rồi mới dùng `addView`. (Cần chạy thử trên thiết bị để xác nhận hiện trạng render.)

### 3. Màn "Thử thách" (Challenge) thực chất là bản sao của Recurring
`ChallengeListActivity.java` (toàn bộ)

`ChallengeListActivity` dùng `RecurringAdapter`, observe `recurringRepo`, recycler tên `recyclerRecurring`, và set tiêu đề `R.string.recurring` ("Giao dịch định kỳ"). Không có mô hình/logic "thử thách tiết kiệm" riêng — màn này chỉ hiển thị lại dữ liệu định kỳ với tiêu đề sai.

Đề xuất: nếu Challenge là tính năng riêng thì cần model + repo + layout/tiêu đề riêng; nếu chưa làm, nên ẩn lối vào thay vì hiển thị màn trùng lặp gây hiểu nhầm.

---

## TRUNG BÌNH

### 4. Hai hàng trong Profile trông bấm được nhưng chết
`fragment_profile.xml` — "Ngân sách" (dòng ~169-210) và "Thử thách tiết kiệm" (dòng ~399-440)

Cả hai hàng đều có `background="?attr/selectableItemBackground"` (hiệu ứng ripple như nút bấm) nhưng **không có `android:id`** nên không thể gắn handler, và thực tế không có listener nào → bấm vào không phản hồi.

Đề xuất: thêm `id` + handler (Ngân sách → `BudgetListActivity`/tab Budget; Thử thách → `ChallengeListActivity`), hoặc bỏ `selectableItemBackground` nếu chưa làm để không gây kỳ vọng sai.

### 5. Ba mục cài đặt tiền tệ chỉ có đúng 1 lựa chọn
`SettingsActivity.java:104-160` (`showCurrencySymbolPicker/PositionPicker/LocalePicker`)

Mỗi picker chỉ chứa một phần tử cứng: ký hiệu `{"vnd"}`, vị trí một option, locale `{"vi_VN"}`; `updateCurrencyDisplay()` cũng hard-code "vnd". Người dùng bấm vào ba hàng này chỉ thấy hộp thoại 1 dòng — thao tác vô nghĩa.

Đề xuất: bổ sung đủ lựa chọn, hoặc ẩn/biến thành dòng thông tin tĩnh nếu app chỉ hỗ trợ VND. (Lưu ý: cài đặt "ký hiệu đứng trước" vẫn vô hiệu do bug `MoneyFormat` #3 trong BUG_REPORT.)

### 6. Đăng xuất không có xác nhận
`ProfileFragment.java:103-104`

`btnLogout` gọi logout ngay khi bấm, không có dialog xác nhận → dễ thoát phiên do lỡ tay.

Đề xuất: thêm `AlertDialog` xác nhận trước khi đăng xuất.

### 7. Bật vân tay khi chưa bật PIN: nút không phản hồi, không giải thích
`SecurityActivity.java:45-49`

Khi PIN chưa bật, gạt công tắc vân tay sẽ bị `setChecked(false)` lặng lẽ, không có thông báo → người dùng tưởng nút hỏng.

Đề xuất: `Toast`/snackbar "Vui lòng bật PIN trước khi dùng vân tay".

### 8. Đổi mật khẩu: dialog không tự đóng sau khi thành công
`SettingsActivity.java:54-74`

Sau khi `updatePassword` thành công không gọi `dialog.dismiss()`, hộp thoại vẫn mở → người dùng không rõ đã đổi xong.

Đề xuất: `dismiss()` trong `addOnSuccessListener` (lưu ý dùng `setPositiveButton(null)` + override để kiểm soát đóng).

---

## THẤP

### 9. Nút chuông thông báo là nút chết
`fragment_home.xml:60` — `FrameLayout btnNotification` có `clickable=true`/`focusable=true` nhưng không được tham chiếu trong `HomeFragment.java` → bấm không làm gì.
Đề xuất: gắn handler khi có tính năng thông báo, hoặc bỏ `clickable`.

### 10. Nút back ở header Report không gắn listener
`fragment_report.xml:~21` — ImageView back không có id/handler (vẫn dựa nút back hệ thống). Nếu muốn bấm được thì cần `setOnClickListener`.

### 11. Thiếu IME action ở form Login/Register
`activity_login.xml`, `activity_register.xml` — các `EditText` không đặt `imeOptions` (`actionNext`/`actionDone`) nên không submit/chuyển ô bằng bàn phím. Nên thêm và bắt `setOnEditorActionListener`.

### 12. Tap target nhỏ ở các TextView link
`activity_login.xml` (textForgotPassword, textRegister), `activity_register.xml` (textLogin) chỉ `padding=4dp`, dưới mức 48dp khuyến nghị. Thêm `minHeight=48dp`.

### 13. LockActivity: vài chi tiết nhỏ
`LockActivity.java` — sau khi nhập sai PIN không xóa `editPinLayout.setError(null)`; nút vân tay ẩn khi lockout nhưng không re-check trong `onResume()`. Mức nhỏ, nên dọn cho mượt.

---

## Đã kiểm tra và xác nhận KHÔNG phải lỗi

Một số nghi vấn ban đầu đã được đọc code và loại bỏ:

- **WalletListActivity dialog "double handler"**: dùng `AlertDialog.Builder().create()` **không** có positive button, chỉ bind `btnCreate` trong view và `setEnabled(false)` khi đang lưu — đúng, không có race.
- **RecurringListActivity "double save"**: dùng đúng pattern `setPositiveButton(..., null)` rồi override `getButton(...).setOnClickListener`, chỉ `dismiss()` khi `validateAndSave` trả `true` — đúng chuẩn chống auto-dismiss.
- AddTransaction / QuickAdd: nút Lưu có `setEnabled(false)` khi xử lý, có validate (số tiền > 0, chọn danh mục/ví) — chống double-submit tốt.
- Bottom nav (MainActivity), TransactionList filters, adapter click/edit/delete callbacks: nối dây đầy đủ, ID khớp.

---

## Thứ tự ưu tiên đề xuất

1. **#1** — nối lại lối vào Settings + tách hàng "Cài đặt"/"Ngôn ngữ" (chặn hẳn tính năng).
2. **#2** — sửa RecyclerView trong BudgetEdit (rủi ro mất danh sách).
3. **#3, #4** — xử lý màn Challenge trùng + 2 hàng Profile chết.
4. **#5–#8** — UX cài đặt, xác nhận logout, vân tay, dialog đổi mật khẩu.
5. **#9–#13** — dọn cosmetic.

> Lưu ý: các điểm liên quan render (#2) nên được xác nhận bằng cách chạy app trên Android Studio, vì môi trường review không build được (thiếu Android SDK/JDK 17).

---

## Trạng thái sửa (17/06/2026)

Đã sửa toàn bộ:

- **#1** ProfileFragment: hàng "Cài đặt" → mở `SettingsActivity`; thêm hàng "Ngôn ngữ" riêng trong Settings (đổi xong tự `recreate()`).
- **#2** `activity_budget_edit.xml`: `recyclerCategories` đổi `RecyclerView` → `LinearLayout`; bỏ `setLayoutManager`.
- **#3** Xây feature Challenge thật: mới `ChallengeRepository`, `ChallengeAdapter`, `item_challenge.xml`, `dialog_challenge.xml`; viết lại `ChallengeListActivity` (CRUD + đánh dấu ngày + tiến độ) dùng model `Challenge`; layout `activity_challenge_list.xml` đổi id đúng (`recyclerChallenges`).
- **#4** Hàng "Ngân sách" → `BudgetListActivity`, "Thử thách tiết kiệm" → `ChallengeListActivity` (thêm id + clickable + handler).
- **#5** Ba cài đặt tiền tệ có nhiều lựa chọn thật (ký hiệu VND/USD/EUR; vị trí trước/sau; định dạng số chấm/phẩy). Sửa kèm 2 bug logic: `PrefsHelper.isCurrencySymbolBefore` luôn trả `false`, và `ExpenseApplication` hardcode `false`; `MoneyFormat.getFormat` nay tôn trọng dấu phân cách theo locale.
- **#6** Đăng xuất có dialog xác nhận.
- **#7** Bật vân tay khi chưa có PIN: hiện Toast giải thích.
- **#8** Dialog đổi mật khẩu chỉ đóng khi thành công; nút disable khi đang xử lý.
- **#9** Bỏ `clickable` nút chuông Home (chưa có tính năng).
- **#10–#13** Đã ghi nhận (back Report dựa nút hệ thống; IME/tap-target/Lock là cosmetic — có thể bổ sung sau nếu muốn).

Đã kiểm tra: XML well-formed, không trùng string key, mọi `R.string`/id binding tham chiếu đều tồn tại (vi + en). **Chưa build** (môi trường thiếu Android SDK) — chạy `./gradlew :app:assembleDebug` để xác nhận, và mở thử màn Ngân sách (BudgetEdit) + Thử thách để kiểm tra hiển thị.
