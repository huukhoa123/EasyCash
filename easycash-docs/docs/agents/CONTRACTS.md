# CONTRACTS

Hợp đồng giữa backend (`:domain`, `:data`) và frontend (`:ui`).
Nếu tài liệu và code lệch nhau, xem quy trình đổi trong `AGENTS.md` mục 5.2.

## Active

### v1 · T-002

- Codex: ACK.
- Antigravity: ACK (2026-11-20). Đã xác nhận: TimeRange [start, end) nửa mở và WriteResult với 4 nhóm lỗi hoàn toàn phù hợp cho Compose UI và ViewModel.
- Model, enum và kiểu lọc nằm trong [Models.kt](../../../domain/src/main/kotlin/com/easycash/domain/Models.kt). Chữ ký của cả sáu repository và `WriteResult` nằm trong [Repositories.kt](../../../domain/src/main/kotlin/com/easycash/domain/Repositories.kt).
- Tiền là `Long` theo đơn vị đồng VND. `amount > 0` với mọi loại giao dịch trừ `ADJUSTMENT`, có thể âm và phải khác 0. `estimatedBalance` là số dư ước tính.
- Thời gian dùng epoch milliseconds UTC. UI định dạng theo múi giờ máy. `TimeRange` bao gồm đầu mốc và loại trừ cuối mốc. Tháng của ngân sách dùng `yyyyMM`.
- Các trường `null` trong `TransactionFilter` bỏ qua tiêu chí tương ứng. Tiêu chí gồm khoảng thời gian, loại giao dịch, tài khoản, danh mục, trạng thái và từ khoá.
- Repository quan sát qua `Flow`. `observeById` trả về `null` khi thiếu giao dịch. Để gỡ liên kết nguồn thông báo, gọi `linkAccount` với `accountId = null`.
- Thao tác ghi là `suspend`. `WriteResult.Success` trả về ID đã lưu cho `add` và `upsert`, hoặc `Unit` cho thao tác ghi còn lại. `WriteResult.Failure` biểu diễn dữ liệu sai, bản ghi thiếu, xung đột hoặc lỗi lưu trữ.
- `CategorySpending.categoryId = null` biểu thị chi chưa gán danh mục.

### Quản lý dữ liệu v1 · P-002 đã ACK
- Interface trong domain/DataManagementRepository.kt: exportBackup, restoreBackup, exportCsv, clearAll, đều suspend và trả WriteResult.
- Chữ ký exportBackup(password: CharArray): WriteResult<ByteArray>, restoreBackup(bytes: ByteArray, password: CharArray): WriteResult<Unit>, exportCsv(): WriteResult<String>, clearAll(): WriteResult<Unit>.
- Mật khẩu 8..1024 ký tự; file backup tối đa 32MiB dữ liệu rõ cộng 48 byte header/tag. Restore hỗ trợ schema hiện tại và nâng backup schema 5 lên 6, thay dữ liệu nguyên tử và tắt các nguồn thông báo sau restore.
- Sai mật khẩu/file/schema/dữ liệu nhập trả INVALID_INPUT. Lỗi SQLite vận hành trả STORAGE_ERROR. Cancellation không bị nuốt.
- CSV có UTF-8 BOM, CRLF, escaping dấu ngoặc kép và bảo vệ công thức ở note. clearAll đặt lại dữ liệu mặc định và nguồn BIDV tắt.
- Codex ACK. Antigravity ACK 2026-11-21 theo P-002.

### Nhắc định kỳ v1 · P-003 đã ACK
- Model Reminder(id: Long, name: String, accountId: Long, categoryId: Long?, type: CategoryType, amount: Long?, frequency: ReminderFrequency, dayRule: Int, nextDueAtMillis: Long, remindBeforeDays: Int, isActive: Boolean).
- ReminderFrequency gồm WEEKLY, MONTHLY, YEARLY.
- Repository interface ReminderRepository trong domain: observeAll(): Flow<List<Reminder>>, suspend upsert(Reminder): WriteResult<Long>, delete(id): WriteResult<Unit>.
- Chỉ nhắc, không tự tạo giao dịch/không thanh toán. Giữ ngày gốc khi lùi ngày cuối tháng. Thời gian theo timezone máy, WorkManager có thể trễ do OS.
- Frontend quản lý UI CRUD nhắc định kỳ và xin POST_NOTIFICATIONS khi người dùng chủ động bật (Android 13+). Không xin quyền khi khởi động.
- Codex ACK 2026-11-22. Antigravity ACK 2026-11-22.

## Proposals

*(Chưa có đề xuất nào.)*
