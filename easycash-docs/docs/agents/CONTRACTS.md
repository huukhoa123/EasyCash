# CONTRACTS

Hợp đồng giữa backend (`:domain`, `:data`) và frontend (`:ui`).
Nếu file này và code lệch nhau, **file này là chuẩn**, trừ khi đã thống nhất đổi qua mục Proposals.
Quy trình đổi: xem `AGENTS.md` mục 5.2.

## Active

*(Chưa có. Codex chốt ở T-002 rồi chuyển bản nháp bên dưới sang đây.)*

## Draft v1 (Codex hoàn thiện ở T-002)

Quy ước chung:
- Tiền: `Long`, đơn vị đồng VND. Với mọi loại giao dịch trừ `ADJUSTMENT`, `amount > 0`. `ADJUSTMENT` có thể âm.
- Thời gian: epoch milliseconds (UTC), UI tự format theo múi giờ máy.
- `:domain` không phụ thuộc Android.
- Dữ liệu quan sát được trả về `Flow`. Thao tác ghi là `suspend`.
- Lỗi nghiệp vụ trả về qua kiểu kết quả rõ ràng, không ném exception chung chung ra UI. *(Hình dạng cụ thể: Codex đề xuất ở T-002.)*

### Enum

```kotlin
enum class TransactionType   { INCOME, EXPENSE, TRANSFER, REFUND, ADJUSTMENT }
enum class TransactionStatus { AUTO_RECORDED, USER_CONFIRMED, NEEDS_REVIEW, REJECTED }
enum class TransactionOrigin { MANUAL, NOTIFICATION }
enum class AccountType       { CASH, BANK, EWALLET }
enum class CategoryType      { INCOME, EXPENSE }
```

### Model (rút gọn, field chính)

```kotlin
data class Account(id: Long, name: String, type: AccountType,
                   initialBalance: Long, estimatedBalance: Long, isArchived: Boolean)

data class Category(id: Long, name: String, type: CategoryType,
                    iconKey: String, colorKey: String, isSystem: Boolean)

data class Transaction(id: Long, accountId: Long, toAccountId: Long?, categoryId: Long?,
                       type: TransactionType, amount: Long, note: String,
                       occurredAtMillis: Long, status: TransactionStatus,
                       origin: TransactionOrigin)

data class Budget(id: Long, categoryId: Long, month: Int /* yyyyMM */, limitAmount: Long,
                  warnThresholdPercent: Int)
data class BudgetUsage(budget: Budget, spent: Long)

data class NotificationSource(id: Long, packageName: String, displayName: String,
                              isEnabled: Boolean, linkedAccountId: Long?)
```

### Repository interface (tên + việc chính)

| Interface | Quan sát (`Flow`) | Ghi (`suspend`) |
|---|---|---|
| `TransactionRepository` | `observe(filter)`, `observeById(id)`, `observeNeedsReviewCount()` | `add`, `update`, `delete`, `confirm(id)`, `reject(id)` |
| `AccountRepository` | `observeAll()` | `add`, `update`, `archive` |
| `CategoryRepository` | `observeAll(type)` | `add`, `update`, `archive` |
| `BudgetRepository` | `observeUsage(month)` | `upsert`, `delete` |
| `NotificationSourceRepository` | `observeAll()`, `observeAccessGranted()` | `setEnabled`, `linkAccount` |
| `StatsRepository` | `observeSummary(range)`, `observeSpendingByCategory(range)` | — |

`TransactionFilter` gồm: khoảng thời gian, loại, tài khoản, danh mục, trạng thái, từ khoá. *(Danh sách chính xác: chốt ở T-002.)*

## Proposals

*(Chưa có đề xuất nào.)*

Mẫu:

```
### P-001 · <tiêu đề> · <agent> · <ngày>
- Đổi gì:
- Vì sao:
- Ảnh hưởng:
- Phản hồi: <ACK | OBJECT> · <agent> · <lý do>
```
