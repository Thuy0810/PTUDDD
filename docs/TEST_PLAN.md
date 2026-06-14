# KẾ HOẠCH KIỂM THỬ

Tài liệu mô tả test cần viết cho từng module. Mục tiêu: đảm bảo rà soát và migration không phá vỡ dữ liệu, không làm sai số dư, không crash app.

## 1. Test `MoneyValueParser`

File: `app/src/test/java/com/expensemanager/app/util/MoneyValueParserTest.java`

### Test `toLong(Object)` — đọc từ Firestore

| Input | Expected |
|---|---|
| `null` | `null` |
| `Long.valueOf(1000)` | `1000L` |
| `Long.valueOf(0)` | `0L` |
| `Integer.valueOf(1000)` | `1000L` |
| `Double.valueOf(1000.0)` | `1000L` |
| `Double.valueOf(1000.5)` | `1001L` (làm tròn lên) |
| `Double.valueOf(1000.4)` | `1000L` (làm tròn xuống) |
| `Float.valueOf(1000.0f)` | `1000L` |
| `String.valueOf("1000")` | `1000L` |
| `String.valueOf("1.000.000")` | `1000000L` |
| `String.valueOf("1,000,000")` | `1000000L` |
| `String.valueOf("")` | `null` |
| `String.valueOf("abc")` | `null` |
| `Boolean.TRUE` | `null` |
| `List` (kiểu khác) | `null` |

### Test `tryParse(String, long)`

| Input | Default | Expected |
|---|---|---|
| `null` | `0` | `0` |
| `""` | `0` | `0` |
| `" "` | `0` | `0` |
| `"123"` | `0` | `123` |
| `"1.234.567"` | `0` | `1234567` |
| `"1,234,567"` | `0` | `1234567` |
| `"-100"` | `0` | `0` (âm không hợp lệ) |
| `"abc"` | `100` | `100` (fallback default) |
| `"  500  "` | `0` | `500` (trim) |

### Test `tryParseStrict(String)`

| Input | Expected |
|---|---|
| `null` | `null` |
| `""` | `null` |
| `"123"` | `123L` |
| `"-1"` | `null` (âm) |
| `"99999999999999999999"` (vượt Long.MAX) | `null` |
| `"1.5"` | `15L` (coi `.` là grouping) |

### Test `isValidAmount(long)`

| Input | Expected |
|---|---|
| `0` | `false` |
| `1` | `true` |
| `Long.MAX_VALUE` | `true` |
| `-1` | `false` |

---

## 2. Test `DateUtils`

File: `app/src/test/java/com/expensemanager/app/util/DateUtilsTest.java`

### Test `startOfMonth(String monthKey)`

| Input | Expected (Date VN) |
|---|---|
| `"2026-06"` | `2026-06-01 00:00:00 ICT` |
| `"2026-12"` | `2026-12-01 00:00:00 ICT` |
| `"invalid"` | throw hoặc trả về null (tùy thiết kế) |

### Test `startOfNextMonth(String monthKey)`

| Input | Expected |
|---|---|
| `"2026-06"` | `2026-07-01 00:00:00 ICT` |
| `"2026-12"` | `2027-01-01 00:00:00 ICT` |

### Test `monthKey(Date)` nhất quán với `currentMonthKey()`

- `monthKey(today)` == `currentMonthKey()` (trong cùng 1 giây).

### Test timezone

- Đặt device timezone sang `UTC`.
- `currentMonthKey()` vẫn trả về tháng theo ICT (không theo UTC).

### Test `isSameDay`

- 2 `Date` trong cùng 1 ngày ICT → `true`.
- 2 `Date` khác ngày ICT → `false`.
- 1 `Date` lúc 23:00 ICT và 1 `Date` lúc 01:00 ICT ngày hôm sau → `false`.

---

## 3. Test `BudgetChecker` / `BudgetService`

File: `app/src/test/java/com/expensemanager/app/util/BudgetServiceTest.java`

### Test `validate(Budget, List<Budget>)`

| Case | Expected |
|---|---|
| `month="2026-06"`, `limitAmount=5000000L`, `scope=monthly`, không có budget khác | OK |
| `month="2026-06"`, `limitAmount=0L` | Error "limitAmount phải > 0" |
| `month="2026-06"`, `limitAmount=-1000L` | Error |
| `month="invalid"`, `limitAmount=5000000L` | Error "month sai format" |
| `scope=monthly`, đã có 1 budget `monthly` cho `"2026-06"` | Error "Đã có budget tổng cho tháng này" |
| `scope=category`, `categoryId=null` | Error "Phải có danh mục" |
| `scope=category`, đã có budget cho `(2026-06, food)` | Error "Đã có budget cho danh mục này" |
| `alertAt=[0.8, 0.9]` | OK |
| `alertAt=[0.9, 0.8]` (không sort) | Error "Phải sắp xếp tăng dần" |
| `alertAt=[0.5, 0.5]` (trùng) | Error "Ngưỡng trùng" |
| `alertAt=[0, 0.5]` (có 0) | Error "Ngưỡng phải > 0" |
| `alertAt=[1.5]` (> 1) | Error "Ngưỡng phải <= 1" |

### Test `computeUsage(Budget, List<Transaction>)`

| Case | Expected `usedAmount` |
|---|---|
| 1 expense 100.000đ trong tháng 6/2026 | `100000L` |
| 1 income 200.000đ trong tháng 6/2026 | `0L` (không tính income) |
| 1 transfer 50.000đ trong tháng 6/2026 | `0L` (không tính transfer) |
| 1 expense 100.000đ ngoài tháng 6/2026 | `0L` |
| 3 expense: 100k + 200k + 50k trong tháng | `350000L` |
| `limitAmount=1000000L`, usedAmount=250000L | `usageRate=0.25`, `remainingAmount=750000L` |
| `limitAmount=1000000L`, usedAmount=1500000L | `usageRate=1.5`, `remainingAmount=-500000L` (vượt) |

### Test `checkAlerts(Budget, usedAmount)`

| Case | Expected alerts |
|---|---|
| `alertAt=[0.8, 0.9]`, used=70% | (không alert) |
| `alertAt=[0.8, 0.9]`, used=80% | "Đã đạt 80% ngân sách" |
| `alertAt=[0.8, 0.9]`, used=90% | "Đã đạt 80% và 90% ngân sách" |
| `alertAt=[0.8, 0.9]`, used=100% | "Đã đạt 80%, 90%, 100% ngân sách" |
| `alertAt=[0.8, 0.9]`, used=120% | "Đã vượt ngân sách 20%" |

---

## 4. Test `TransactionRepository` (atomic + transfer)

File: `app/src/test/java/com/expensemanager/app/data/repository/TransactionRepositoryTest.java`

Yêu cầu: dùng Firebase Local Emulator hoặc mock `FirebaseFirestore`.

### Test `addAtomic` cho income

- Ví có `currentBalance=0L`.
- Gọi `addAtomic(uid, transaction(income, 1000000L), walletId)`.
- Expected: transaction được tạo, `currentBalance=1000000L`.

### Test `addAtomic` cho expense vượt số dư

- Ví có `currentBalance=500000L`.
- Gọi `addAtomic(uid, transaction(expense, 1000000L), walletId)`.
- Expected: **tùy theo policy** — hiện tại cho phép số dư âm, nên `currentBalance=-500000L`.

### Test `addAtomic` cho transfer

- Ví A có `currentBalance=1000000L`, ví B có `currentBalance=0L`.
- Gọi service `transfer(uid, A, B, 300000L)`.
- Expected: transaction `transfer` được tạo; ví A = 700000L; ví B = 300000L.

### Test `addAtomic` với ví không tồn tại

- `walletId="nonexistent"`.
- Expected: **sẽ thay đổi trong giai đoạn 3.1**: throw lỗi "Ví không tồn tại" thay vì âm thầm ghi transaction.

### Test `addAtomic` với ví archived

- `walletId` thuộc ví có `isArchived=true`.
- Expected: throw lỗi "Ví đã lưu trữ".

### Test `updateAtomic` đổi số tiền

- transaction income 100k → sửa thành 200k.
- Ví `currentBalance` phải tăng thêm 100k (từ 100k → 200k).

### Test `updateAtomic` đổi expense → income

- transaction expense 100k → sửa thành income 100k.
- Ví `currentBalance` phải tăng 200k (đảo dấu + đổi loại).

### Test `updateAtomic` đổi ví

- transaction expense 100k ở ví A → sửa thành ví B.
- Ví A `currentBalance` phải cộng 100k (hoàn tác); ví B phải trừ 100k.

### Test `updateAtomic` đổi expense → transfer

- transaction expense 100k ở ví A → sửa thành transfer 100k từ A → B.
- Ví A: cộng 100k (hoàn tác expense) → trừ 100k (transfer out) = không đổi.
- Ví B: cộng 100k.

### Test `deleteAtomic` cho income

- transaction income 100k tồn tại.
- Xoá.
- Ví `currentBalance` phải giảm 100k.

### Test `deleteAtomic` cho transfer

- transaction transfer 100k từ A → B tồn tại.
- Xoá.
- Ví A: cộng 100k.
- Ví B: trừ 100k.

---

## 5. Test `TransferService`

File: `app/src/test/java/com/expensemanager/app/domain/usecase/TransferServiceTest.java`

### Validate

| Case | Expected |
|---|---|
| `fromWalletId == toWalletId` | Error "Hai ví phải khác nhau" |
| `fromWallet` không tồn tại | Error "Ví nguồn không tồn tại" |
| `toWallet` không tồn tại | Error "Ví nhận không tồn tại" |
| `fromWallet.isArchived` | Error "Ví nguồn đã lưu trữ" |
| `toWallet.isArchived` | Error "Ví nhận đã lưu trữ" |
| `amount <= 0` | Error "Số tiền phải > 0" |
| `fromWallet.currentBalance < amount` | Error "Số dư không đủ" |
| `amount > Long.MAX_VALUE` | Error "Số tiền quá lớn" |

### Happy path

- Ví A 1.000.000L, ví B 0L.
- Transfer 300.000L.
- Expected: A = 700.000L, B = 300.000L, transaction `transfer` được tạo với `fromWalletId=A, toWalletId=B, amount=300000L`.

### Atomicity

- 2 transfer cùng lúc A → B và A → C, mỗi cái 600.000L.
- A ban đầu 1.000.000L.
- Expected: chỉ 1 thành công (vì A không đủ cho 2 × 600k), transaction kia rollback.

---

## 6. Test `BalanceCalculator`

File: `app/src/test/java/com/expensemanager/app/util/BalanceCalculatorTest.java`

### Test `verify(wallet, transactions)`

| Case | Expected |
|---|---|
| Ví initial=0, có 1 income 100k, 1 expense 30k | `expected=70000L`, `actual=wallet.currentBalance`, `isMatch=true` |
| Ví initial=0, currentBalance=99999L (do dữ liệu cũ bị lệch) | `isMatch=false` (actual=99999, expected=70000) |
| Ví archived, có 5 transaction | Vẫn tính `expected`, không bỏ qua |

### Test `totalAssets(List<Wallet>, List<Transaction>)`

- 2 ví: A=500k, B=200k → `totalAssets=700000L`.
- Ví archived: vẫn tính trong total (chỉ ẩn khi tạo giao dịch mới).

---

## 7. Test tích hợp (Integration)

File: `app/src/androidTest/java/com/expensemanager/app/IntegrationTest.java`

### Scenario E2E: Tạo ví → ghi giao dịch → kiểm tra số dư

1. Login user test.
2. Tạo ví "Test Ví" với `initialBalance=0L`.
3. Thêm income 1.000.000L vào ví.
4. Thêm expense 300.000L từ ví.
5. Thêm transfer 200.000L từ ví → ví khác.
6. Verify: `currentBalance = 1.000.000 - 300.000 - 200.000 = 500.000L`.

### Scenario E2E: Đặt ngân sách → vượt ngân sách → cảnh báo

1. Đặt budget category "food" tháng 6/2026 = 1.000.000L, `alertAt=[0.8]`.
2. Thêm expense food 500k.
3. Thêm expense food 400k. Tổng = 900k (vượt 80%).
4. Verify: home hiển thị alert "Đã đạt 80% ngân sách food".

### Scenario E2E: Sao lưu → xoá app data → khôi phục

1. Export dữ liệu ra JSON.
2. Clear app data.
3. Import JSON.
4. Verify: tất cả ví, giao dịch, ngân sách được khôi phục đúng.

---

## 8. Test thủ công (Manual Checklist)

### Trước khi release

- [ ] Đăng ký tài khoản mới → login thành công.
- [ ] Tạo 3 ví với số dư ban đầu khác nhau.
- [ ] Ghi 5 income, 10 expense, 3 transfer.
- [ ] Đặt ngân sách tổng tháng + 2 ngân sách danh mục.
- [ ] Vượt ngưỡng 80% → kiểm tra cảnh báo hiển thị.
- [ ] Tạo mục tiêu tiết kiệm → đóng góp 50% → kiểm tra số dư ví giảm đúng.
- [ ] Đặt PIN → thoát app → mở lại → bị khoá → nhập PIN → mở được.
- [ ] Bật nhắc nhở 21:00 → đợi tới giờ → kiểm tra notification.
- [ ] Sao lưu dữ liệu ra file → xoá 1 ví → khôi phục → ví quay lại.
- [ ] Đổi mật khẩu → đăng xuất → đăng nhập với mật khẩu mới.
- [ ] Đăng xuất → đăng nhập lại → dữ liệu vẫn còn.

### Edge cases

- [ ] Nhập số tiền = 0 → bị từ chối.
- [ ] Nhập số tiền âm → bị từ chối.
- [ ] Nhập chuỗi `"abc"` → bị từ chối.
- [ ] Nhập chuỗi `"1.000.000"` → parse thành 1.000.000.
- [ ] Chọn 2 ví giống nhau cho transfer → bị từ chối.
- [ ] Transfer vượt số dư → bị từ chối.
- [ ] Tạo 2 budget monthly cùng tháng → bị từ chối.
- [ ] Xoá ví có giao dịch → bị từ chối (hoặc ép archive).
- [ ] Đổi múi giờ thiết bị → ngày tháng vẫn theo ICT.

### P0 — PIN & Lock (14 test cases)

> Áp dụng sau khi đã triển khai P0 fixes.

- [ ] **P0-1.** Tạo PIN → đóng app → mở lại → bị chuyển sang LockActivity → nhập PIN đúng → mở được MainActivity
- [ ] **P0-2.** LockActivity: nhập PIN đúng → mở được
- [ ] **P0-3.** LockActivity: nhập PIN sai 1-4 lần → hiển thị "Còn X lần thử", vẫn mở được khi đúng
- [ ] **P0-4.** LockActivity: nhập PIN sai đủ 5 lần → hiển thị thông báo khóa 5 phút, không nhập được
- [ ] **P0-5.** LockActivity: mở lại app trong thời gian khóa → vẫn ở LockActivity
- [ ] **P0-6.** LockActivity: mở lại app sau khi hết thời gian khóa → nhập PIN đúng → mở được
- [ ] **P0-7.** Tắt PIN trong SecurityActivity (confirm dialog) → đóng app → mở lại → vào thẳng MainActivity
- [ ] **P0-8.** Thiết bị có biometric: bật biometric → đóng app → mở lại → biometric prompt tự hiện → xác thực → mở được
- [ ] **P0-9.** Biometric: hủy biometric → quay về nhập PIN bình thường
- [ ] **P0-10.** Biometric thất bại → hiển thị lỗi, không crash
- [ ] **P0-11.** Thiết bị không hỗ trợ biometric → nút biometric ẩn
- [ ] **P0-12.** Xoay màn hình khi ở LockActivity → không crash
- [ ] **P0-13.** Đưa app xuống background rồi mở lại (biometric đang hiện) → biometric prompt bị hủy, không crash
- [ ] **P0-14.** Đăng xuất khi PIN đang bật → quay về LoginActivity, không yêu cầu mở khóa

### P0 — EditProfile (13 test cases)

> Áp dụng sau khi đã triển khai P0 fixes.

- [ ] **P0-15.** Mở sửa hồ sơ từ nút header → màn hình đúng (KHÔNG phải giao diện đăng ký)
- [ ] **P0-16.** Mở sửa hồ sơ từ mục tài khoản → màn hình đúng
- [ ] **P0-17.** Dữ liệu hiện tại được điền sẵn trong form
- [ ] **P0-18.** Email hiển thị đúng, read-only
- [ ] **P0-19.** KHÔNG xuất hiện trường mật khẩu
- [ ] **P0-20.** KHÔNG xuất hiện giao diện đăng ký (không có tiêu đề "Tạo tài khoản mới", nút "Đăng ký")
- [ ] **P0-21.** Tên rỗng → bị từ chối, hiển thị lỗi
- [ ] **P0-22.** Tên chỉ có khoảng trắng → bị từ chối, hiển thị lỗi
- [ ] **P0-23.** Tên hợp lệ → lưu thành công, quay về ProfileFragment
- [ ] **P0-24.** Tên sau khi sửa cập nhật ngay trên ProfileFragment (không cần reload)
- [ ] **P0-25.** Mất mạng khi lưu → hiển thị lỗi, KHÔNG đóng màn hình
- [ ] **P0-26.** Bấm nút lưu liên tục nhiều lần → chỉ gửi 1 request
- [ ] **P0-27.** Biến mất khi tên không thay đổi → không gọi Firebase

### Lịch sử giao dịch (Transaction Display)

- [ ] **TX-1.** Income hiển thị đúng danh mục, ví, ngày giờ và số tiền với dấu `+`
- [ ] **TX-2.** Expense hiển thị đúng danh mục, ví, ngày giờ và số tiền với dấu `-`
- [ ] **TX-3.** Transfer hiển thị đúng ví nguồn → ví nhận, không dấu cộng/trừ
- [ ] **TX-4.** Danh mục bị xóa → hiển thị "Danh mục đã xóa", KHÔNG hiển thị ID
- [ ] **TX-5.** Ví bị xóa → hiển thị "Ví đã xóa", KHÔNG hiển thị ID
- [ ] **TX-6.** Ghi chú rỗng → không tạo dòng trống
- [ ] **TX-7.** Ghi chú dài → được rút gọn bằng ellipsis, không phá layout
- [ ] **TX-8.** Số tiền lớn → không bị xuống dòng
- [ ] **TX-9.** Icon hiển thị đúng theo danh mục, không phải cố định một emoji
- [ ] **TX-10.** Item có trạng thái pressed/ripple khi bấm
- [ ] **TX-11.** Ngày giờ hiển thị đúng định dạng `dd/MM/yyyy HH:mm`
- [ ] **TX-12.** Click item → mở bottom sheet chi tiết đầy đủ

### Đa ngôn ngữ (i18n)

- [ ] **I18N-1.** Chọn tiếng Việt → toàn bộ ứng dụng hiển thị tiếng Việt
- [ ] **I18N-2.** Chọn English → toàn bộ ứng dụng hiển thị tiếng Anh
- [ ] **I18N-3.** Đóng app, mở lại → vẫn giữ ngôn ngữ đã chọn
- [ ] **I18N-4.** Toast và dialog đổi ngôn ngữ theo lựa chọn
- [ ] **I18N-5.** Notification đổi ngôn ngữ theo lựa chọn
- [ ] **I18N-6.** KHÔNG dịch dữ liệu người dùng tự nhập (tên ví, ghi chú, tên danh mục)

### Sức khỏe tài chính (Financial Health)

- [ ] **FH-1.** Điểm sức khỏe luôn nằm trong khoảng 0–100
- [ ] **FH-2.** Không chia cho 0 khi thu nhập = 0 → hiển thị `--`
- [ ] **FH-3.** Không chia cho 0 khi ngân sách = 0 → hiển thị `--`
- [ ] **FH-4.** Điểm 85–100 → hiển thị nhãn "Xuất sắc"
- [ ] **FH-5.** Điểm 70–84 → hiển thị nhãn "Tốt"
- [ ] **FH-6.** Điểm 50–69 → hiển thị nhãn "Cần chú ý"
- [ ] **FH-7.** Điểm 0–49 → hiển thị nhãn "Nguy hiểm"
- [ ] **FH-8.** Chuyển tiền KHÔNG được tính là thu hoặc chi
- [ ] **FH-9.** Card sức khỏe không hiển thị nhiều đoạn văn dài
- [ ] **FH-10.** Chỉ hiển thị 1 cảnh báo quan trọng nhất

### Tiền tệ (Currency Format)

- [ ] **CF-1.** `500000` → `500,000 VND`
- [ ] **CF-2.** Income `500000` → `+500,000 VND`
- [ ] **CF-3.** Expense `500000` → `-500,000 VND`
- [ ] **CF-4.** Transfer `500000` → `500,000 VND` (không dấu)
- [ ] **CF-5.** KHÔNG còn ký hiệu đứng trước số tiền
- [ ] **CF-6.** KHÔNG còn format `500.000 đ`
- [ ] **CF-7.** Mọi màn hình dùng cùng một định dạng qua `MoneyFormat`
- [ ] **CF-8.** Số tiền không bị làm tròn sai do `double`

---

## 9. Công cụ

- **Unit test:** JUnit 4 (mặc định Android).
- **Mock:** Mockito 5.x.
- **Coroutine/Async test:** dùng `Task.addOnSuccessListener` trong test (đồng bộ) hoặc `CountDownLatch`.
- **Firebase Local Emulator:** cho integration test.
- **Coverage:** ≥ 70% cho `util/`, `domain/usecase/`, `data/repository/`.

## 10. Tham chiếu

- Ràng buộc 5.3: nhập số tiền validate `> 0`, không âm, không chỉ dấu.
- Ràng buộc 7.3: transfer phải atomic.
- Ràng buộc 8: add/update/delete transaction phải xử lý đủ trường hợp.
- Ràng buộc 9: budget chỉ tính expense.
