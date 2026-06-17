# MÔ HÌNH DỮ LIỆU

> Cập nhật theo codebase ngày 17/06/2026. Tất cả số tiền đã dùng `long` (migration `double → long` đã hoàn tất). Đã **bỏ model `Tag`** và **bỏ loại giao dịch `transfer`**.

## Tổng quan Collection Firestore

| Collection | Model / Ghi chú |
|---|---|
| `users/{uid}` | `UserProfile` |
| `users/{uid}/wallets/{walletId}` | `Wallet` |
| `users/{uid}/categories/{categoryId}` | `Category` (hỗ trợ danh mục cha/con qua `parentId`) |
| `users/{uid}/transactions/{txId}` | `Transaction` (chỉ `income` / `expense`) |
| `users/{uid}/budgets/{budgetId}` | `Budget` |
| `users/{uid}/budget_reallocations/{id}` | `BudgetReallocation` (lịch sử phân bổ lại) |
| `users/{uid}/savings_goals/{goalId}` | `SavingsGoal` |
| `users/{uid}/savings_history/{id}` | Log khi đóng góp vào mục tiêu |
| `users/{uid}/recurring/{ruleId}` | `RecurringRule` |
| `users/{uid}/recurring_occurrences/{occurrenceId}` | Chống trùng kỳ recurring |
| `users/{uid}/challenges/{challengeId}` | `Challenge` |
| `users/{uid}/wallet_adjustments/{id}` | Log điều chỉnh số dư ví (audit) |

## Danh sách model (`data/model/`)

POJO: `UserProfile`, `Wallet`, `Category`, `Transaction`, `Budget`, `BudgetReallocation`, `SavingsGoal`, `RecurringRule`, `Challenge`, `FinancialInsights`, `HomeSummary`.
Enum: `FinancialHealthStatus`, `FinancialAlertType`.

---

## 1. `Transaction`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | String | `@DocumentId` |
| `type` | String | `income` / `expense` (KHÔNG còn `transfer`) |
| `amount` | long | |
| `categoryId` | String | |
| `walletId` | String | Ví của giao dịch |
| `note` | String | |
| `date` | Timestamp | Ngày giao dịch (UTC) |
| `mood` | String | (tuỳ chọn) |
| `regretFlag` | String | (tuỳ chọn) |
| `recurringRuleId` | String | Nếu sinh từ giao dịch định kỳ |
| `createdAt` / `updatedAt` | Timestamp | Audit |

> Đã bỏ các trường `fromWalletId`, `toWalletId`, `tagIds`.

---

## 2. `Wallet`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | String | `@DocumentId` |
| `name` | String | |
| `type` | String | `cash` / `bank` / `ewallet` / `savings` |
| `initialBalance` | long | |
| `currentBalance` | long | |
| `icon` | String | |
| `color` | String | |
| `createdAt` / `updatedAt` | Timestamp | |
| `isArchived` | boolean | Ví đã lưu trữ (không xoá cứng nếu còn giao dịch) |

---

## 3. `Category`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | String | `@DocumentId` |
| `name` | String | |
| `type` | String | `income` / `expense` |
| `iconKey` | String | |
| `colorHex` | String | |
| `isSystem` | boolean | |
| `group` | String | `essential` / `need` / `want` / `other` |
| `parentId` | String | Nếu khác rỗng → là danh mục con (`isSubcategory()`) |

---

## 4. `Budget`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | String | `@DocumentId` |
| `scope` | String | `monthlyTotal` / `category` |
| `categoryId` | String | null khi `scope=monthlyTotal` |
| `month` | String | `yyyy-MM` |
| `limitAmount` | long | |
| `alertAt` | List\<Double\> | Tỷ lệ cảnh báo `(0, 1]` |
| `createdAt` / `updatedAt` | Timestamp | |
| `isArchived` | boolean | |

---

## 5. `BudgetReallocation`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | String | `@DocumentId` |
| `month` | String | `yyyy-MM` |
| `sourceType` | String | `UNALLOCATED` / `OTHER_BUDGET` |
| `sourceBudgetId` | String | null nếu từ phần chưa phân bổ |
| `targetBudgetId` | String | Ngân sách đích |
| `amount` | long | |
| `reason` | String | |
| `createdAt` | Timestamp | |

Cập nhật trong Firestore Transaction (atomic), không cập nhật một phần.

---

## 6. `SavingsGoal`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | String | `@DocumentId` |
| `title` | String | |
| `targetAmount` | long | |
| `savedAmount` | long | |
| `walletId` | String | Ví nguồn để trừ khi đóng góp |
| `completed` | boolean | |
| `deadline` | Timestamp | |
| `createdAt` / `updatedAt` | Timestamp | |
| `isArchived` | boolean | |

Đóng góp đi qua `GoalService` (atomic: trừ ví + tăng `savedAmount` + ghi `savings_history`).

---

## 7. `RecurringRule`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | String | `@DocumentId` |
| `type` | String | `income` / `expense` (không hỗ trợ transfer) |
| `amount` | long | |
| `categoryId` / `walletId` / `note` | String | |
| `cycleType` | String | `daily` / `weekly` / `monthly` / `yearly` |
| `dayOfMonth` | int | 1–31 |
| `dayOfWeek` | int | 1=CN … 7=T7 |
| `monthOfYear` | int | 1–12 |
| `useLastDayOfMonth` | boolean | Thay magic number 32 |
| `dateStart` / `dateEnd` | Timestamp | |
| `enabled` | boolean | |
| `nextRun` / `lastRun` | Timestamp | |

Mỗi kỳ có `occurrenceId = ruleId_yyyy-MM-dd` làm document ID chống trùng. Thực thi atomic + idempotent qua `RecurringService` / `RecurringTransactionWorker`. Catch-up tối đa 50 kỳ/lần.

---

## 8. `Challenge`

| Trường | Kiểu | Ghi chú |
|---|---|---|
| `id` | String | `@DocumentId` |
| `title` / `description` | String | |
| `targetSavings` | long | |
| `totalDays` / `completedDays` | int | |
| `startDate` | Timestamp | |
| `active` | boolean | |
| `createdAt` / `updatedAt` | Timestamp | |
| `isArchived` | boolean | |

---

## 9. `FinancialInsights` + enum

`FinancialInsights` chứa dữ liệu số/enum (KHÔNG chứa câu tiếng Việt đã dịch): `healthScore` (int 0–100), `savingRate`, `budgetUsageRate`, `expenseChangeRate`, `dailyAllowance`, `monthPrediction`… cùng `FinancialHealthStatus` (EXCELLENT/GOOD/WARNING/CRITICAL) và `FinancialAlertType`. UI chịu trách nhiệm format hiển thị.

`HomeSummary`: gói số liệu tổng hợp cho trang chủ (income, expense, balance…).

---

## 10. `UserProfile`

| Trường | Kiểu |
|---|---|
| `displayName`, `email`, `currency` | String |
| `theme` | String (`system`/`light`/`dark`) |
| `notificationsEnabled`, `pinEnabled` | boolean |

---

## Tình trạng migration tiền tệ

Hoàn tất: `Transaction.amount`, `Wallet.initialBalance/currentBalance`, `Budget.limitAmount`, `SavingsGoal.targetAmount/savedAmount`, `Challenge.targetSavings`, `RecurringRule.amount`, `BudgetReallocation.amount` — tất cả là `long`.

**Nguyên tắc:** chỉ parse khi đọc (`MoneyValueParser.toLong`); không cập nhật hàng loạt document Firestore cũ.
