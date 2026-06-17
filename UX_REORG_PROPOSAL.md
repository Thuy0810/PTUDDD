# Đề xuất tổ chức lại trang & thao tác tiện tay — Quản Lý Chi Tiêu

> Ngày: 17/06/2026 · Trạng thái: **ĐỀ XUẤT — chưa sửa code.** Cần bạn duyệt trước khi triển khai.
> Phạm vi: kiến trúc thông tin (gom trang/chức năng) + ergonomics (thao tác tiện tay).

---

## 1. Vấn đề chính: Profile là "ngăn kéo chứa tất cả"

Toàn bộ **9 chức năng** chỉ vào được từ tab Profile (`ProfileFragment.java`):

```
Ví · Danh mục · Mục tiêu · Định kỳ · Thử thách · Ngân sách
· Bảo mật · Cài đặt · Sửa hồ sơ
```

Hệ quả:

- Người dùng hiểu **"Cá nhân" = tài khoản & cài đặt**, nhưng Ví / Mục tiêu / Định kỳ / Thử thách là **chức năng tài chính cốt lõi**, dùng hằng ngày — lại bị chôn 2 lớp sâu trong tab ít ai mở.
- Tab Profile dài 13 hàng bấm được → quá tải, khó quét mắt.
- Home chỉ có 3 shortcut (`HomeFragment.java:62–69`): "Xem tất cả" → Report, header → Add, card → Budget. Không có lối tới Ví / Mục tiêu / Định kỳ.

Bằng chứng (entry point hiện tại — tất cả từ `ProfileFragment`):

| Chức năng | Mở từ |
|---|---|
| WalletListActivity | ProfileFragment |
| CategoryListActivity | ProfileFragment |
| GoalListActivity | ProfileFragment |
| RecurringListActivity | ProfileFragment |
| ChallengeListActivity | ProfileFragment |
| BudgetListActivity | ProfileFragment |
| SecurityActivity | ProfileFragment, SettingsActivity |
| SettingsActivity | ProfileFragment |
| EditProfileActivity | ProfileFragment |

---

## 2. Sơ đồ tổ chức: TRƯỚC → SAU

### TRƯỚC (hiện tại)

```
┌─ Home ──────┐  ┌─ Budget ─┐  ┌─ Report ─┐  ┌─ Profile ───────────────┐
│ tổng quan   │  │ ngân sách│  │ biểu đồ  │  │ Sửa hồ sơ               │
│ + 3 shortcut│  │          │  │          │  │ Ngân sách   ← lặp Budget│
└─────────────┘  └──────────┘  └──────────┘  │ Ví                      │
                                              │ Danh mục                │
        [FAB +] Thêm giao dịch (giữa)         │ Mục tiêu                │
                                              │ Định kỳ                 │
                                              │ Thử thách               │
                                              │ Bảo mật                 │
                                              │ Ngôn ngữ / Cài đặt      │
                                              │ Trợ giúp · Liên hệ      │
                                              │ Đăng xuất               │
                                              └─────────────────────────┘
                                                 (13 hàng — quá tải)
```

### SAU (đề xuất)

```
┌─ Home ───────────────┐  ┌─ Kế hoạch (Budget) ─┐  ┌─ Report ─┐  ┌─ Cá nhân ───────┐
│ tổng quan            │  │ Ngân sách           │  │ biểu đồ  │  │ Sửa hồ sơ       │
│                      │  │ Mục tiêu tiết kiệm  │  │          │  │ Bảo mật         │
│ ⚡ Shortcut nhanh:    │  │ Thử thách           │  │          │  │ Cài đặt         │
│  [Ví][Mục tiêu]      │  │                     │  │          │  │  (ngôn ngữ,     │
│  [Định kỳ][Danh mục] │  │ (cùng mục đích:     │  │          │  │   dark mode,    │
│                      │  │  đặt & theo dõi     │  │          │  │   nhắc nhở)     │
│                      │  │  mục tiêu tiền)     │  │          │  │ Trợ giúp·Liên hệ│
└──────────────────────┘  └─────────────────────┘  └──────────┘  │ Đăng xuất       │
                                                                  └─────────────────┘
            [FAB +] Thêm giao dịch (giữa — giữ nguyên)              (~6 hàng — gọn)
```

**Nguyên tắc gom:**

- **Home** = thao tác hằng ngày → thêm hàng shortcut grid (Ví · Mục tiêu · Định kỳ · Danh mục) ngay tầm ngón cái.
- **Budget → "Kế hoạch"** = mọi thứ "đặt mục tiêu & theo dõi tiền": Ngân sách + Mục tiêu + Thử thách (3 cái cùng mục đích, đang bị tách rời).
- **Profile → "Cá nhân"** = chỉ tài khoản/hệ thống: Sửa hồ sơ, Bảo mật, Cài đặt, Trợ giúp, Liên hệ, Đăng xuất. Gọn còn ~6 hàng.
- Bỏ hàng "Ngân sách" trong Profile (trùng với tab Budget).

---

## 3. Thao tác tiện tay (ergonomics)

Phần lớn **đã tốt**, giữ nguyên:

- Bottom nav 4 tab + FAB cam nổi giữa (`activity_main.xml`) — đúng vùng ngón cái.
- Các màn danh sách (Ví, Danh mục, Mục tiêu, Định kỳ) dùng FAB góc dưới phải.
- Nút "Lưu giao dịch" full-width ở đáy form.

Hai điểm nên chỉnh:

### 3a. Nút Lưu bị cuộn mất
`activity_add_transaction.xml:376` — `btnSave` nằm *trong* `ScrollView` (có spacer `weight=1`). Form dài → nút cuộn khuất, phải vuốt xuống mới bấm được.
**Đề xuất:** tách `btnSave` ra ngoài ScrollView, ghim cố định ở đáy (sticky footer) để luôn trong tầm tay.

### 3b. Action ở màn chi tiết nằm trên toolbar
Các activity dùng theme `Theme.ExpenseManager.WithActionBar` (`AndroidManifest.xml`) → back và một số action ở **đỉnh màn**, khó với một tay trên máy lớn.
**Đề xuất:** với màn nhập liệu/sửa, đưa action chính (Lưu/Xóa) xuống đáy như form Thêm giao dịch đã làm. Back hệ thống vẫn giữ.

---

## 4. Checklist triển khai (khi bạn duyệt)

**Gom trang:**

- [ ] `fragment_home.xml`: thêm 1 hàng/grid 4 shortcut (Ví · Mục tiêu · Định kỳ · Danh mục).
- [ ] `HomeFragment.java`: gắn 4 handler `startActivity(...)` tương ứng.
- [ ] `fragment_budget.xml` + `BudgetFragment.java`: thêm lối vào Mục tiêu (`GoalListActivity`) và Thử thách (`ChallengeListActivity`).
- [ ] `fragment_profile.xml`: xóa các hàng Ví/Danh mục/Mục tiêu/Định kỳ/Thử thách/Ngân sách; giữ Sửa hồ sơ, Bảo mật, Cài đặt, Trợ giúp, Liên hệ, Đăng xuất.
- [ ] `ProfileFragment.java`: bỏ các handler đã chuyển đi.
- [ ] (Tùy chọn) Đổi nhãn tab: "Ngân sách" → "Kế hoạch", "Cá nhân" giữ nguyên — cập nhật `strings.xml` (vi + en).

**Ergonomics:**

- [ ] `activity_add_transaction.xml`: chuyển `btnSave`/`btnDelete` ra footer cố định ngoài ScrollView.
- [ ] Rà các màn chi tiết: cân nhắc đưa action chính xuống đáy.

**Kiểm tra sau khi sửa:**

- [ ] Mọi entry point cũ trong Profile đã có lối vào mới (không mất chức năng).
- [ ] `R.string` / id binding tồn tại đủ (vi + en).
- [ ] Build `./gradlew :app:assembleDebug` + mở thử từng tab.

---

## 5. Lưu ý

Đề xuất này **độc lập** với các lỗi UX đã nêu trước đó (dark mode gãy, thiếu `imeOptions`, chưa có Snackbar/undo, thiếu empty state Ví/Danh mục). Nên xử lý các lỗi đó song song hoặc sau khi gom trang xong.
