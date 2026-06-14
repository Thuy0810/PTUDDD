# KẾ HOẠCH MIGRATION DỮ LIỆU

## Mục tiêu

Chuyển các trường tiền tệ từ `double` sang `long` để đảm bảo độ chính xác tuyệt đối cho tiền VND (tránh floating-point error). Đồng thời thêm các trường audit (`createdAt`, `updatedAt`) và `isArchived` cho các model còn thiếu.

## Nguyên tắc bất khả xâm phạm

1. **KHÔNG cập nhật hàng loạt** dữ liệu Firestore thật.
2. **KHÔNG yêu cầu rollback** vì không có thay đổi dữ liệu nguồn.
3. **KHÔNG làm crash app** nếu Firestore trả về kiểu bất ngờ (Long, Double, Integer, String, null).
4. **KHÔNG phá vỡ dữ liệu cũ** — phải đọc được cả `double` lẫn `long`.

## Các trường cần migration

| Model | Trường | Từ | Sang |
|---|---|---|---|
| `Wallet` | `initialBalance` | `double` | `long` |
| `Wallet` | `currentBalance` | `double` | `long` |
| `Budget` | `limitAmount` | `double` | `long` |
| `SavingsGoal` | `targetAmount` | `double` | `long` |
| `SavingsGoal` | `savedAmount` | `double` | `long` |
| `Challenge` | `targetSavings` | `double` | `long` |

## Trường cần bổ sung (không migration, chỉ thêm mặc định)

| Model | Trường mới | Mặc định |
|---|---|---|
| `Wallet` | `updatedAt: Timestamp` | `null` (sẽ set khi update) |
| `Wallet` | `isArchived: boolean` | `false` |
| `Budget` | `createdAt: Timestamp` | `Timestamp.now()` khi tạo |
| `Budget` | `updatedAt: Timestamp` | `null` |
| `Budget` | `isArchived: boolean` | `false` |
| `SavingsGoal` | `createdAt: Timestamp` | `Timestamp.now()` |
| `SavingsGoal` | `updatedAt: Timestamp` | `null` |
| `SavingsGoal` | `isArchived: boolean` | `false` |
| `Challenge` | `createdAt: Timestamp` | `Timestamp.now()` |
| `Challenge` | `updatedAt: Timestamp` | `null` |
| `Challenge` | `isArchived: boolean` | `false` |
| `RecurringRule` | `lastRun: Timestamp` | `null` (idempotent catchUp) |

**Cách xử lý khi đọc document cũ không có trường mới:**
- `isArchived`: mặc định `false` (coi như chưa lưu trữ).
- `updatedAt`: nếu `null` thì fallback về `createdAt` hoặc `null` tuỳ ngữ cảnh.
- `createdAt`: nếu `null` thì fallback về `Timestamp.now()` khi hiển thị (có thể sai nhưng chỉ 1 lần).

## Công cụ: `util/MoneyValueParser.java`

Class tiện ích dùng chung, đặt tại `app/src/main/java/com/expensemanager/app/util/MoneyValueParser.java`.

### API chính

```java
public final class MoneyValueParser {
    /**
     * Chuyển đổi an toàn từ Object (Firestore) sang Long.
     * - Long → trả về Long
     * - Integer → trả về Long
     * - Double → Math.round
     * - Float → Math.round
     * - String → parse an toàn (chấp nhận "1.000.000", "1,000,000", "1000000")
     * - null → null
     * - khác → null
     */
    public static Long toLong(Object value);

    /**
     * Parse input người dùng (EditText) sang long với giá trị mặc định.
     * - Chuẩn hoá: bỏ dấu phẩy, dấu chấm, khoảng trắng
     * - Nếu parse lỗi hoặc empty → trả về defaultValue
     */
    public static long tryParse(String input, long defaultValue);

    /**
     * Parse với validate: trả về null nếu < 0 hoặc > Long.MAX_VALUE
     */
    public static Long tryParseStrict(String input);

    /**
     * Validate: amount > 0 và <= Long.MAX_VALUE
     */
    public static boolean isValidAmount(long amount);
}
```

### Quy tắc parse chuỗi

| Input | Kết quả `tryParse(input, 0)` |
|---|---|
| `""` (empty) | `0` |
| `" "` (whitespace) | `0` |
| `"123"` | `123` |
| `"1.234"` | `1234` (bỏ dấu chấm grouping) |
| `"1,234"` | `1234` (bỏ dấu phẩy grouping) |
| `"1.234.567"` | `1234567` |
| `"1,234,567"` | `1234567` |
| `"1.5"` | `15` (coi `.` là grouping, không phải decimal — vì tiền VND không có decimal) |
| `"abc"` | `0` (parse fail) |
| `"-100"` | `0` (âm → fail) |
| `null` | `0` |

**Lưu ý quan trọng:** VND không có phần thập phân, nên dấu `.` luôn là grouping. Nếu sau này có thêm đơn vị có decimal thì cần điều chỉnh.

## Quy trình áp dụng

### Bước 1 — Thêm `MoneyValueParser` (Giai đoạn 1)

Tạo file `util/MoneyValueParser.java` với các method ở trên. Thêm unit test `MoneyValueParserTest`.

### Bước 2 — Cập nhật từng Repository

Trong mỗi repository có đọc trường tiền, thay:
```java
Double balance = walletSnap.getDouble("currentBalance"); // CŨ
```
bằng:
```java
Long balance = MoneyValueParser.toLong(walletSnap.get("currentBalance")); // MỚI
```

Áp dụng cho:
- `TransactionRepository.addAtomic/updateAtomic/deleteAtomic`
- `BudgetRepository.observeMonth` (nếu cần — observe thường qua POJO)
- `WalletRepository` (qua POJO)
- `GoalRepository`
- `BackupManager.deserializeJsonValue`

### Bước 3 — Cập nhật từng POJO Model

Khi đổi `double` → `long` trong model:
1. Cập nhật kiểu field.
2. Cập nhật getter/setter.
3. Cập nhật constructor (nếu có).
4. Cập nhật `toMap()` — KHÔNG cần thay đổi `Map.put` vì `long` auto-boxing thành `Long` cho Firestore.
5. Thêm trường `updatedAt`, `isArchived` (nếu thiếu).
6. Thêm logic migration: nếu đọc từ Firestore mà field là `Double` thì `MoneyValueParser.toLong()` trước khi gán.

**Cách Firestore xử lý:** Firestore lưu `long` thành **Integer** (32-bit) hoặc **Long** (64-bit) tuỳ giá trị. Khi đọc bằng `getLong()` thì trả về `Long`; khi đọc bằng `getDouble()` thì có thể trả về `Double`. Vì vậy `MoneyValueParser.toLong(Object)` là cách an toàn nhất.

### Bước 4 — Từng bước theo module

| Module | Trường đổi | Giai đoạn |
|---|---|---|
| `Budget` | `limitAmount` | 2 |
| `Wallet` | `initialBalance`, `currentBalance` | 3.2 |
| `SavingsGoal` | `targetAmount`, `savedAmount` | 3.4 |
| `Challenge` | `targetSavings` | 3.4 |

Mỗi giai đoạn phải build + test thành công trước khi qua giai đoạn sau.

### Bước 5 — Kiểm thử hồi quy

Test case bắt buộc:
1. Đọc document cũ `currentBalance: 500000.0` → trả về `currentBalance = 500000L`.
2. Đọc document mới `currentBalance: 500000` → trả về `currentBalance = 500000L`.
3. Đọc document cũ `currentBalance: 1234567.89` → trả về `currentBalance = 1234568L` (làm tròn).
4. Đọc document cũ `currentBalance: null` → trả về `currentBalance = 0L`.
5. Tính `currentBalance = 1000000 - 100000 = 900000` (không mất precision).

## Rủi ro & Giảm thiểu

| Rủi ro | Giảm thiểu |
|---|---|
| `MoneyValueParser` parse sai input đặc biệt | Unit test toàn diện các case (xem TEST_PLAN) |
| Firestore trả về `BigDecimal` hoặc kiểu khác | `toLong` trả về `null` thay vì throw; caller xử lý null |
| Code cũ gọi `getDouble("currentBalance")` mà giờ field là `Long` | Repository wrapper sẽ dùng `MoneyValueParser.toLong(snap.get("currentBalance"))` chấp nhận cả 2 |
| Người dùng nhập `"1.5"` cho 1.500.000đ | Hiển thị cảnh báo "Vui lòng nhập số nguyên" (VND không có decimal) |
| App cũ đang chạy thì cập nhật app mới | App cũ vẫn dùng được vì chỉ có field cũ; field mới sẽ được set khi user thao tác lần đầu |

## Tham chiếu

- Ràng buộc 5.1: tất cả số tiền VND phải dùng `long`.
- Ràng buộc 5.2: phải có migration an toàn, không crash khi đọc dữ liệu cũ.
- Ràng buộc 5.3: nhập số tiền phải chuẩn hoá, không dùng `Double.parseDouble`.
- Ràng buộc 6: ví phải có `updatedAt`, `isArchived`.
- Ràng buộc 9: ngân sách chỉ tính `expense`, không tính `income`/`transfer`.
