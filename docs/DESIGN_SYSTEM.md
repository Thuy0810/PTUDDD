# HỆ THỐNG THIẾT KẾ (Design System)

Tài liệu mô tả các quy ước UI đang dùng trong ứng dụng. Mục tiêu: thống nhất cách sử dụng giữa các màn hình và chuẩn bị cho giai đoạn refactor.

## 1. Bảng màu (file `res/values/colors.xml`)

| Tên màu | Mã hex | Công dụng |
|---|---|---|
| `primary` | (xem colors.xml) | Màu chính, nút bấm, link |
| `income_green` | (xem colors.xml) | Hiển thị khoản thu |
| `expense_red` | (xem colors.xml) | Hiển thị khoản chi |
| `text_primary` | (xem colors.xml) | Chữ chính |
| `text_secondary` | (xem colors.xml) | Chữ phụ |
| `background` | (xem colors.xml) | Nền chính |
| `card_bg` | (xem colors.xml) | Nền thẻ |
| `divider` | (xem colors.xml) | Đường phân chia |
| `white` | `#FFFFFF` | Nền trắng |
| `#1B5E20` | ~~hardcode trong layout~~ | Thay bằng `@color/budget_safe` |
| `#F9A825` | ~~hardcode trong layout~~ | Thay bằng `@color/warning` |
| `#B3FFFFFF` | ~~hardcode trong layout~~ | Thay bằng `@color/text_on_dark` |
| `#B3000000` | ~~hardcode trong layout~~ | Thay bằng `@color/text_secondary` |

**Đã sửa P0:** Các hardcode màu trong 8 file layout đã được thay bằng `@color/...` tokens (xem Phase 5, task 14).

## 2. Typography

| Cỡ | Công dụng |
|---|---|
| 18sp | Tiêu đề màn hình, đậm |
| 16sp | Số tiền lớn, đậm |
| 15sp | Tiêu đề section, đậm |
| 14sp | Nút bấm, label quan trọng |
| 13sp | Label phụ |
| 12sp | Mô tả, ghi chú, thời gian |
| 11sp | Hint, gợi ý |

**Style chữ:** Hầu hết dùng `textStyle="bold"` cho tiêu đề/số tiền; thường cho mô tả. Chưa dùng `TextAppearance.Material3.*` xuyên suốt.

## 3. Spacing & Padding

- `paddingHorizontal="16dp"` — nội dung chính của màn hình
- `padding="12dp"` hoặc `14dp` — bên trong card
- `padding="8dp"` — icon, nút nhỏ
- `layout_marginTop` từ 4-20dp cho khoảng cách giữa các block
- `cornerRadius` 10-14dp cho card, 10dp cho button

## 4. Component Style

### Button
- `style="@style/Widget.Material3.Button.TextButton"` — nút text (dùng cho "LƯU", "HỦY")
- `style="@style/Widget.ExpenseManager.Button.Outlined"` — nút viền (dùng cho "+ Thêm", "+ Tạo nhóm")
- `app:cornerRadius="10dp"` — bo góc
- `app:strokeColor="@color/primary"` hoặc `@color/divider`

### Card
- `com.google.android.material.card.MaterialCardView`
- `app:cardCornerRadius="12dp"` hoặc `14dp`
- `app:cardElevation="0dp"` — phẳng
- `app:cardBackgroundColor` — tuỳ chỉnh

### Tab
- `bg_tabs_container` (LinearLayout nền xám, padding 4dp)
- `bg_tab_active` (TextView nền trắng, text đậm)

### Icon
- `ic_back`, `ic_edit`, `ic_settings`, `ic_close` — vector drawable trong `res/drawable/`
- Tint bằng `app:tint="@color/text_primary"` hoặc `@color/text_secondary`

## 5. Format tiền (`util/MoneyFormat`)

- Mặc định: `đ 1.500.000` (symbol trước, dấu chấm ngăn hàng nghìn, không có phần thập phân)
- Locale: `vi_VN`
- Có thể tuỳ chỉnh qua `PrefsHelper` (SettingsActivity)
- **`format(double)`** — method hiện tại, nhận `double`
- **`formatLong(long)`** — method mới (sẽ thêm trong Giai đoạn 1.3), nhận `long` để tránh cast
- `formatCompact(double)` — viết tắt `1.5M`, `200K`

## 6. Quy tắc chung khi thêm màn hình mới

1. Dùng **ViewBinding** (không tìm View bằng `findViewById`).
2. Dùng **Material Components** (không dùng View gốc Android).
3. Background màn hình: `@color/background`.
4. Status bar: tuỳ theme, hiện không ép.
5. ActionBar: chỉ bật khi màn hình đứng độc lập (không phải fragment trong MainActivity).
6. Icon dùng vector drawable, tint qua `app:tint`, không tint qua Java code.
7. Format tiền dùng `MoneyFormat.formatLong(long)` (sau khi migration xong).
8. Màu dùng từ `colors.xml`, không hardcode hex trong layout.

## 7. Launcher Icon

### Resource files

|| File | Loại |
|---|---|
| `mipmap-anydpi-v26/ic_launcher.xml` | Adaptive icon XML (Android 8+) |
| `mipmap-anydpi-v26/ic_launcher_round.xml` | Adaptive icon XML round |
| `mipmap-*/ic_launcher.xml` | Layer-list fallback (Android 7) |
| `mipmap-*/ic_launcher_round.xml` | Layer-list fallback round |
| `drawable/ic_launcher_foreground.xml` | Vector foreground (ví + coin) |
| `values/ic_launcher_background.xml` | Color resource `#FFB84D` |

### Colors

- Background: `@color/ic_launcher_background` = `#FFB84D` (primary orange)
- Foreground: white `#FFFFFF` + accent `#FFD080`
- Safe zone: foreground nằm trong vùng center-crop của adaptive icon

### Design

- Wallet body (white rounded shape)
- Coin stack top-right (orange accent)
- Growth arrow bottom-left (orange accent)
- Không chứa text nhỏ, tối giản, dễ nhận diện

## 8. Vấn đề cần xử lý

- Layout `activity_budget_allocation.xml` (280 dòng) hardcode 4-5 màu hex; cần thay bằng tên trong `colors.xml`.
- Một số màn hình chưa ViewBinding: `SplashActivity`, `WalletListActivity`, `BudgetListActivity`, `ChallengeListActivity`.
- Một số adapter còn dùng `findViewById` bên trong `onBindViewHolder` (sẽ rà soát thêm).
