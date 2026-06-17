# Báo cáo bug tiềm ẩn — Dự án Quản Lý Chi Tiêu

Ngày rà soát: 17/06/2026
Phạm vi: rà soát tĩnh toàn bộ source `app/src/main/java`.

## Trạng thái sửa (cập nhật 17/06/2026)

Đã sửa: #1, #2, #3, #4, #5, #6, #7, #8, #9, #10, #13, #14, và lỗi giao dịch mồ côi ở #11.
Cố ý GIỮ NGUYÊN (rủi ro hồi quy / cần đổi mô hình dữ liệu, mức thấp):
- #12 `BalanceCalculator.walletBalance` — heuristic `currentBalance == 0`. Vì `currentBalance`
  nay đã được duy trì atomic, đổi sang cờ "đã khởi tạo" là thay đổi mô hình; để lại ghi chú.
- Bộ đếm `AtomicInteger pending` trong `RecurringRepository` — mã chết vô hại, không ảnh hưởng.

File mới: `data/repository/FirestoreQueryLiveData.java` (LiveData lifecycle-aware, sửa #5).
Vẫn chưa build/test được trong môi trường này (thiếu Android SDK + JDK 17); đã kiểm tra cú pháp
thủ công. **Cần chạy `./gradlew :app:testDebugUnitTest` + `lint` trên Android Studio để xác nhận.**

---

## Về việc kiểm chứng bằng build/test

Không build/test được trong môi trường hiện tại vì sandbox **không có Android SDK**, **không có mạng để tải Gradle 8.11**, và chỉ có **JRE 11 (không có `javac`, dự án cần JDK 17)**. Vì vậy các phát hiện dưới đây dựa trên đọc code. Khuyến nghị chạy `./gradlew :app:testDebugUnitTest` và `./gradlew lint` trên máy có Android Studio + JDK 17 để xác nhận, đặc biệt cho các lỗi đánh dấu CAO.

Mức độ: **CAO** = sai chức năng / crash, **TRUNG BÌNH** = sai lệch dữ liệu / rò rỉ tài nguyên / vi phạm quy ước, **THẤP** = cosmetic / phòng thủ.

---

## CAO

### 1. Giao dịch định kỳ KHÔNG bao giờ chạy được (đọc sau khi ghi trong Firestore transaction)
`data/repository/RecurringRepository.java` — `executeOccurrence()`, dòng ~205–245.

Trong một Firestore `runTransaction`, **mọi lệnh đọc phải đứng trước mọi lệnh ghi**. Hàm này ghi trước rồi mới đọc:

- dòng 216: `transaction.set(occRef, occData)` (ghi)
- dòng 232: `transaction.set(txRef, t.toMap())` (ghi)
- dòng 237: `transaction.get(walletRef)` (**đọc sau khi đã ghi**)

Hậu quả: transaction luôn ném `IllegalStateException: Firestore transactions require all reads to be executed before all writes` → mọi kỳ định kỳ (recurring) đều thất bại, số dư ví không được cập nhật, giao dịch định kỳ không được tạo.

Đề xuất: chuyển `transaction.get(walletRef)` lên ngay sau `transaction.get(occRef)` (đọc cả `occSnap` và `walletSnap` trước), rồi mới `set`/`update`.

### 2. `observeMonth` tính biên tháng theo múi giờ máy thay vì ICT
`data/repository/TransactionRepository.java` — dòng 39–44 (`Calendar.getInstance()`).

Cả app quy ước lọc theo múi giờ ICT (xem `DateUtils`, ràng buộc 10), và `monthKey` được sinh bằng `DateUtils` (ICT). Nhưng `observeMonth` dựng `start`/`end` bằng `Calendar.getInstance()` (múi giờ thiết bị). Trên máy không ở ICT (hoặc đổi múi giờ), biên truy vấn lệch vài giờ so với `monthKey` → giao dịch sát đầu/cuối tháng bị lọt sang tháng khác hoặc mất khỏi danh sách tháng.

Đề xuất: dùng `DateUtils.startOfMonth(monthKey)` và `DateUtils.startOfNextMonth(monthKey)` (đã chuẩn ICT, nửa mở `[start, end)`).

### 3. Cài đặt "ký hiệu tiền tệ đứng trước số" không bao giờ có tác dụng
`util/MoneyFormat.java` — `applySettings()`, dòng 30: `symbolBefore = false;`

Tham số `before` bị bỏ qua, gán cứng `false`. Mọi định dạng (`format`, `formatCompact`) luôn đặt ký hiệu sau số, kể cả khi người dùng chọn "đứng trước". Tính năng vị trí ký hiệu hỏng hoàn toàn.

Đề xuất: `symbolBefore = before;`

---

## TRUNG BÌNH

### 4. `updateAtomic` dùng `double` cho tiền — phá vỡ migration sang `long`
`data/repository/TransactionRepository.java` — `updateAtomic()`, dòng 160, 166, 191, 196–198.

Khác với `addAtomic`/`deleteAtomic` (dùng `MoneyValueParser.toLong` + `long`), hàm này dùng `getDouble("currentBalance")`, tính `double totalChange`, rồi ghi `balance + totalChange` (kiểu `double`) ngược lại Firestore. Hậu quả:
- Ghi lại `currentBalance` dưới dạng số thực → tái xuất hiện sai số dấu phẩy động cho tiền VND (lẽ ra luôn `long`).
- **Không cập nhật `updatedAt`** và **không kiểm tra ví tồn tại/đã archive** (addAtomic có kiểm tra).

Đề xuất: dùng `MoneyValueParser.toLong(...)`, tính bằng `long`, thêm `updatedAt = serverTimestamp()`, và kiểm tra `exists()`/`isArchived` như `addAtomic`.

### 5. Rò rỉ Firestore snapshot listener (toàn hệ thống)
Tất cả repository (`TransactionRepository`, `WalletRepository`, `CategoryRepository`, `BudgetRepository`, `GoalRepository`, `RecurringRepository`, `AuthRepository`) gọi `addSnapshotListener(...)` nhưng **không giữ `ListenerRegistration`** và **không bao giờ `remove()`**. LiveData dùng `MutableLiveData` thuần (không gắn `onActive`/`onInactive`).

Hậu quả: listener sống suốt vòng đời tiến trình, tích lũy mỗi lần gọi lại (ví dụ tạo lại fragment/observe nhiều lần) → rò rỉ bộ nhớ, đọc Firestore liên tục (tốn chi phí), và có thể `setValue` từ callback của dữ liệu đã không còn dùng. Kết hợp với `HomeViewModelHolder` (singleton tĩnh, chỉ `clear()` khi logout ở `ProfileFragment`) làm vấn đề nghiêm trọng hơn — nếu đổi user qua luồng khác, listener của uid cũ vẫn sống.

Đề xuất: chuyển sang dùng pattern LiveData lifecycle-aware (đăng ký listener trong `onActive`, gọi `registration.remove()` trong `onInactive`), hoặc giữ `ListenerRegistration` và hủy trong `ViewModel.onCleared()` / khi logout.

### 6. `parseDateRangeEnd` dùng `23:59:59` làm biên cuối + múi giờ máy
`ui/transaction/TransactionListFragment.java` — dòng 148–160 (và `parseDateRangeStart` dòng 134–146).

Đặt `end = ngày cuối 23:59:59` rồi query `whereLessThan("date", end)`. Vi phạm quy ước nửa mở `[start, end)` của `DateUtils` ("Không bao giờ dùng 23:59:59 làm ranh giới cuối"): các giao dịch trong khoảng `23:59:59.001–23:59:59.999` và đúng giây cuối của ngày cuối bị loại. Ngoài ra dùng `Calendar.getInstance()` (múi giờ máy) thay vì ICT.

Đề xuất: đặt `end = startOfNextDay(ngày cuối)` theo ICT (`DateUtils.newCalendar()`).

### 7. `makeOccurrenceId` không cố định múi giờ → khóa chống trùng có thể lệch
`data/model/RecurringRule.java` — `makeOccurrenceId()`, dòng 107–112.

`SimpleDateFormat("yyyy-MM-dd")` không set timezone → dùng múi giờ thiết bị, trong khi lịch chạy định kỳ tính theo ICT (`DateUtils`). Trên máy lệch múi giờ/qua mốc nửa đêm, `occurrenceId` (yyyy-MM-dd) có thể khác ngày ICT → idempotency không nhất quán (bỏ sót hoặc tạo trùng kỳ).

Đề xuất: set `sdf.setTimeZone(DateUtils.VIETNAM)`.

### 8. `MoneyValueParser.toLong` xử lý chuỗi âm không nhất quán
`util/MoneyValueParser.java` — `parseString()`, dòng ~118–135.

`toLong("-5")` trả về `-5` (nhánh `Long.parseLong` đầu tiên thành công, **không** kiểm tra `< 0`), nhưng `toLong("-5,000")` trả về `null` (nhánh chuẩn hóa có kiểm tra `v < 0`). Số tiền âm lẽ ra nên bị từ chối nhất quán. Nếu Firestore lỡ chứa số âm, ví có thể nhận số dư âm bất ngờ.

Đề xuất: kiểm tra `< 0` ở cả nhánh parse trực tiếp (trả `null` nếu âm), thống nhất quy tắc.

---

## THẤP

### 9. `BudgetService.checkAlerts` dừng ở ngưỡng thấp nhất, trái với comment "ngưỡng cao nhất"
`domain/usecase/BudgetService.java` — dòng 193–202. `alertAt` sắp tăng dần, vòng lặp `break` ở ngưỡng khớp đầu tiên (thấp nhất). Comment ghi "chỉ thông báo ngưỡng cao nhất". Tác động chỉ là cosmetic (thông báo vẫn hiển thị % thực tế), nhưng logic lệch ý định. Nếu muốn đúng comment, duyệt ngược danh sách.

### 10. So sánh hash PIN không hằng-thời-gian
`util/PrefsHelper.java` — `verifyPin()`: `computedHash.equals(storedHash)`. Về lý thuyết có timing side-channel (rủi ro thấp với PIN cục bộ). Cân nhắc `MessageDigest.isEqual(...)`. Ngoài ra hash lưu kèm tiền tố `iterations:` nhưng verify luôn tính lại bằng hằng `120_000`; nếu sau này đổi số vòng, hash cũ sẽ không verify được.

### 11. Mã chết / giao dịch mồ côi trong recurring
`data/repository/RecurringRepository.java`: `AtomicInteger pending` được `decrementAndGet()` nhưng không bao giờ đọc → không có tác dụng. Trong `executeOccurrence`, nếu ví không tồn tại vẫn `set(txRef, ...)` tạo giao dịch (mồ côi) mà không cập nhật số dư. Nên kiểm tra ví tồn tại trước khi tạo giao dịch (và đây cũng là chỗ cần sửa cùng lỗi #1).

### 12. `BalanceCalculator.walletBalance` coi `currentBalance == 0` là "chưa set"
`util/BalanceCalculator.java` — dòng 38–40. Ví có số dư đúng bằng 0 (đã tiêu hết) sẽ kích hoạt `recomputeBalance` từ lịch sử. Thường ra cùng kết quả nên ít gây sai, nhưng heuristic dễ vỡ nếu `currentBalance` mới là nguồn đúng. Cân nhắc dùng cờ "đã khởi tạo" rõ ràng thay vì so sánh với 0.

### 13. `QuickParseUtil` dùng múi giờ máy và kiểu `Double` cho tiền
`util/QuickParseUtil.java`: dùng `Calendar.getInstance()` (múi giờ máy, dòng 165/179/252/255) trong khi chuẩn app là ICT; `parseAmount` trả `Double` vào hệ thống dùng `long`; với đơn vị "tr"/"t" còn sót ký tự thừa trong note ("5tr" → note còn "r"). Tác động nhỏ nhưng nên đồng bộ ICT + `long`.

### 14. `HomeViewModel.recompute` dùng `double` cộng dồn tiền + `subList` là view
`viewmodel/HomeViewModel.java`: cộng `income/expense` bằng `double` (an toàn tới 2^53 nhưng lệch quy ước `long`); `sorted.subList(0, 5)` trả về view dựa trên list gốc khi `setValue` (nên dùng `new ArrayList<>(sorted.subList(...))`).

---

## Gợi ý thứ tự sửa

1. Lỗi #1 (recurring read-after-write) — chặn hoàn toàn tính năng, sửa trước.
2. Lỗi #2, #6, #7, #13 — gom thành một đợt "chuẩn hóa múi giờ ICT".
3. Lỗi #4 + #8 — gom thành đợt "tiền luôn là `long`".
4. Lỗi #3 — sửa nhanh 1 dòng.
5. Lỗi #5 — refactor listener (công sức lớn nhất, nên lên kế hoạch riêng).
6. Còn lại (#9–#12, #14) — dọn dẹp khi có thời gian.

Sau khi sửa, chạy `./gradlew :app:testDebugUnitTest` (đã có sẵn `BudgetServiceTest`, `MoneyValueParserTest`) và bổ sung test cho `RecurringRepository`/`DateUtils` để chốt các lỗi múi giờ và recurring.
