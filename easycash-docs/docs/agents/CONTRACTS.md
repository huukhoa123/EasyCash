# CONTRACTS

Hợp đồng giữa backend (`:domain`, `:data`) và frontend (`:ui`).
Nếu tài liệu và code lệch nhau, xem quy trình đổi trong `AGENTS.md` mục 5.2.

## Active

### v1 · T-002

- Codex: ACK.
- Antigravity: chờ ACK.
- Model, enum và kiểu lọc nằm trong [Models.kt](../../../domain/src/main/kotlin/com/easycash/domain/Models.kt). Chữ ký của cả sáu repository và `WriteResult` nằm trong [Repositories.kt](../../../domain/src/main/kotlin/com/easycash/domain/Repositories.kt).
- Tiền là `Long` theo đơn vị đồng VND. `amount > 0` với mọi loại giao dịch trừ `ADJUSTMENT`, có thể âm và phải khác 0. `estimatedBalance` là số dư ước tính.
- Thời gian dùng epoch milliseconds UTC. UI định dạng theo múi giờ máy. `TimeRange` bao gồm đầu mốc và loại trừ cuối mốc. Tháng của ngân sách dùng `yyyyMM`.
- Các trường `null` trong `TransactionFilter` bỏ qua tiêu chí tương ứng. Tiêu chí gồm khoảng thời gian, loại giao dịch, tài khoản, danh mục, trạng thái và từ khoá.
- Repository quan sát qua `Flow`. `observeById` trả về `null` khi thiếu giao dịch. Để gỡ liên kết nguồn thông báo, gọi `linkAccount` với `accountId = null`.
- Thao tác ghi là `suspend`. `WriteResult.Success` trả về ID đã lưu cho `add` và `upsert`, hoặc `Unit` cho thao tác ghi còn lại. `WriteResult.Failure` biểu diễn dữ liệu sai, bản ghi thiếu, xung đột hoặc lỗi lưu trữ.
- `CategorySpending.categoryId = null` biểu thị chi chưa gán danh mục.

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
