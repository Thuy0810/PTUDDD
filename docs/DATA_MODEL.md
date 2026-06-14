# MÔ HÌNH DỮ LIỆU

Tài liệu mô tả cấu trúc 9 model đang dùng, collection Firestore, kiểu dữ liệu, và các trường cần migration sang `long`.

## Tổng quan Collection Firestore

| Collection | Model | Ghi chú |
|---|---|---|
| `users/{uid}` | `UserProfile` | Tài liệu user (không có subcollection con) |
| `users/{uid}/wallets/{walletId}` | `Wallet` | |
| `users/{uid}/categories/{categoryId}` | `Category` | |
| `users/{uid}/transactions/{txId}` | `Transaction` | Bao gồm income, expense, transfer |
| `users/{uid}/budgets/{budgetId}` | `Budget` | |
| `users/{uid}/savings_goals/{goalId}` | `SavingsGoal` | |
| `users/{uid}/recurring/{ruleId}` | `RecurringRule` | |
| `users/{uid}/recurring_occurrences/{occurrenceId}` | (ghi phụ) | Chống trùng kỳ recurring |
| `users/{uid}/budget_reallocations/{id}` | `BudgetReallocation` | Lịch sử phân bổ lại ngân sách |
| `users/{uid}/challenges/{challengeId}` | `Challenge` | |
| `users/{uid}/savings_history/{historyId}` | (ghi phụ) | Log khi đóng góp vào SavingsGoal |

---

## 1. `Transaction`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | String | `@DocumentId` |
| `type` | String | `income` / `expense` / `transfer` |
| `amount` | **long** | ĐÃ dùng `long` ✅ |
| `categoryId` | String | (chỉ cho income/expense) |
| `walletId` | String | Ví chính (cho transfer = fromWallet) |
| `fromWalletId` | String | Chỉ cho `transfer` |
| `toWalletId` | String | Chỉ cho `transfer` |
| `note` | String | |
| `date` | Timestamp | Ngày giao dịch (UTC) |
| `mood` | String | |
| `regretFlag` | String | |
| `recurringRuleId` | String | |
| `createdAt` | Timestamp | Audit |
| `updatedAt` | Timestamp | Audit |

---

## 2. `Wallet` — **CẦN MIGRATION**

| Trường | Kiểu hiện tại | Kiểu mục tiêu | Ghi chú |
|---|---|---|---|
| `id` | String | String | `@DocumentId` |
| `name` | String | String | |
| `type` | String | String | `cash` / `bank` / `ewallet` / `savings` |
| `initialBalance` | **double** | **long** | Migration sang `long` |
| `currentBalance` | **double** | **long** | Migration sang `long` |
| `icon` | String | String | |
| `color` | String | String | |
| `createdAt` | Timestamp | Timestamp | |
| `updatedAt` | — | Timestamp | **BỔ SUNG** |
| `isArchived` | — | boolean | **BỔ SUNG** (ràng buộc 6) |

---

## 3. `Budget`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | String | `@DocumentId` |
| `scope` | String | `monthlyTotal` / `category` |
| `categoryId` | String | null khi scope=monthlyTotal |
| `month` | String | Format `yyyy-MM` |
| `limitAmount` | **long** | ✅ |
| `alertAt` | List\<Double\> | Tỷ lệ `(0, 1]` |
| `createdAt` | Timestamp | |
| `updatedAt` | Timestamp | |
| `isArchived` | boolean | |

---

## 4. `Category`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | String | `@DocumentId` |
| `name` | String | |
| `type` | String | `income` / `expense` |
| `iconKey` | String | |
| `colorHex` | String | |
| `isSystem` | boolean | |
| `group` | String | `essential` / `need` / `want` / `other` |

---

## 5. `RecurringRule`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | String | `@DocumentId` |
| `type` | String | `income` / `expense` |
| `amount` | **long** | ✅ |
| `categoryId` | String | |
| `walletId` | String | |
| `note` | String | |
| `cycleType` | String | `daily` / `weekly` / `monthly` / `yearly` |
| `dayOfMonth` | int | Ngày 1-31 |
| `dayOfWeek` | int | 1=CN, 2=T2, ..., 7=T7 |
| `monthOfYear` | int | 1-12 |
| `useLastDayOfMonth` | boolean | **Mới** — thay magic number 32 |
| `dateStart` | Timestamp | |
| `dateEnd` | Timestamp | |
| `enabled` | boolean | |
| `nextRun` | Timestamp | Nguồn sự thật cho lần chạy tiếp theo |
| `lastRun` | Timestamp | Thời điểm chạy lần cuối |

**Nghiệp vụ:**
- Hỗ trợ INCOME và EXPENSE. Không hỗ trợ TRANSFER.
- Ngày cuối tháng: dùng `useLastDayOfMonth=true`, KHÔNG dùng `dayOfMonth=32`.
- Mỗi kỳ có `occurrenceId = ruleId_yyyy-MM-dd` — dùng làm document ID chống trùng.
- Thực thi trong Firestore Transaction để đảm bảo atomic và idempotent.
- Catch-up: chạy bù nhiều kỳ bị bỏ lỡ (max 50/lần).

---

## 6. `BudgetReallocation`

Collection: `users/{uid}/budget_reallocations/{id}`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | String | `@DocumentId` |
| `month` | String | Format `yyyy-MM` |
| `sourceType` | String | `UNALLOCATED` / `OTHER_BUDGET` |
| `sourceBudgetId` | String | null nếu từ unallocated |
| `targetBudgetId` | String | Ngân sách đích |
| `amount` | **long** | |
| `createdAt` | Timestamp | |
| `reason` | String | |

**Nghiệp vụ:**
- Nguồn 1: Phần chưa phân bổ của tháng (`effectiveIncome - totalAllocated`).
- Nguồn 2: Phần dư của ngân sách khác (`allocated - spent`).
- Tất cả cập nhật trong Firestore Transaction — không cập nhật một phần.
- Lịch sử dùng để đối soát và hỗ trợ hoàn tác.

---

## 7. `SavingsGoal`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | String | `@DocumentId` |
| `title` | String | |
| `targetAmount` | **long** | ✅ |
| `savedAmount` | **long** | ✅ |
| `walletId` | String | |
| `completed` | boolean | |
| `deadline` | Timestamp | |
| `createdAt` | Timestamp | |
| `updatedAt` | Timestamp | |
| `isArchived` | boolean | |

---

## 8. `Challenge`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | String | `@DocumentId` |
| `title` | String | |
| `description` | String | |
| `targetSavings` | **long** | ✅ |
| `totalDays` | int | |
| `completedDays` | int | |
| `startDate` | Timestamp | |
| `active` | boolean | |
| `createdAt` | Timestamp | |
| `updatedAt` | Timestamp | |
| `isArchived` | boolean | |

---

## 9. `FinancialInsights`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `healthScore` | int | 0-100 |
| `healthMessage` | String | |
| `alerts` | List\<String\> | |
| `dailyAllowance` | String | Tiền nên tiêu hôm nay |
| `monthPrediction` | String | |
| `monthComparison` | String | |
| `feedMessage` | String | |

---

## 10. `UserProfile`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `displayName` | String | |
| `email` | String | |
| `currency` | String | Mặc định `VND` |
| `theme` | String | `system` / `light` / `dark` |
| `notificationsEnabled` | boolean | |
| `pinEnabled` | boolean | |

---

## Tổng kết migration `double` → `long`

| Model | Trường | Trạng thái |
|---|---|---|
| `Wallet` | `initialBalance`, `currentBalance` | Cần migration |
| `Budget` | `limitAmount` | ✅ Đã là `long` |
| `SavingsGoal` | `targetAmount`, `savedAmount` | ✅ Đã là `long` |
| `Challenge` | `targetSavings` | ✅ Đã là `long` |
| `Transaction` | `amount` | ✅ Đã là `long` |
| `RecurringRule` | `amount` | ✅ Đã là `long` |

**Nguyên tắc:** Chỉ parse khi đọc (`MoneyValueParser.toLong`). KHÔNG cập nhật hàng loạt document Firestore.
