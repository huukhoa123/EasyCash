# HANDOFF (chỉ ghi thêm, không sửa mục cũ)

Mẫu mục: `ngày giờ · agent · đã xong · agent kia cần làm gì · điều chưa chắc`
Mục mới nhất nằm cuối file.

---

### 2026-11-20 · claude (lập kế hoạch cho Khoa)

**Đã xong:** tạo bộ tài liệu khởi đầu: `AGENTS.md`, `GEMINI.md`, `docs/PROJECT_SPECIFICATION.md`, `docs/DATABASE.md`, `docs/NOTIFICATION_PARSING.md`, `docs/UI_UX.md`, và bốn file trong `docs/agents/`. Chưa có dòng code nào.

**Codex cần làm tiếp:** bắt đầu từ T-001 và T-002 trong `TASKS.md`. Đọc `docs/DATABASE.md` và `docs/agents/DECISIONS.md` (D-002, D-004, D-005) trước khi dựng module và contract.

**Antigravity cần làm tiếp:** T-003 và T-004 sau khi T-001 xong. Đọc `docs/UI_UX.md`. Các mã màu trong đó là **[ĐỀ XUẤT]**, có thể đổi theo Khoa.

**Điều chưa chắc, cần Khoa trả lời:**
1. Mục tiêu tiết kiệm (D-003): một hay nhiều mục tiêu? Tiến độ tính thế nào?
2. Xác nhận 4 module (D-002) trước khi T-001 bắt đầu.
3. **Mẫu thông báo thật của MoMo và BIDV** (đã che số tài khoản, tên). Không có thì Phase 3 không thể bắt đầu. Xem `docs/NOTIFICATION_PARSING.md` mục 8.
4. Quy tắc tính số dư (D-005) và phạm vi mã hoá (D-007).
5. Các hành vi cần thử trên máy thật: cấp quyền đọc thông báo cho APK cài ngoài Store trên Android 13+ (spec mục 16).

### 2026-11-20 · codex · T-001

**Đã xong:** Gradle Kotlin DSL, wrapper, version catalog, bốn module, Hilt Application và cấu hình Room/KSP để xuất schema khi có database. `assembleDebug testDebugUnitTest` chạy thành công với Android SDK 37.0 và JDK 21, bytecode đích Java 17. Tác vụ unit test chưa có nguồn test. APK debug không yêu cầu quyền. `:ui` chỉ phụ thuộc `:domain`.

**Antigravity cần làm tiếp:** T-003 và T-004 trên cấu trúc module mới. Entry point màn hình thuộc T-004.

**Điều chưa chắc:** `minSdk 26` là cấu hình khởi đầu theo D-009. Chưa chạy trên thiết bị.

### 2026-11-20 · codex · T-002

**Đã xong:** contract v1 trong mục Active và khai báo Kotlin thuần trong module domain. Build, tác vụ unit test Android và tác vụ domain test chạy thành công. Các tác vụ test báo NO-SOURCE. APK debug không yêu cầu quyền.

**Antigravity cần làm tiếp:** đọc chữ ký trong Models.kt và Repositories.kt, rồi ghi ACK hoặc OBJECT vào CONTRACTS.md. T-005 sẽ nối repository với ViewModel sau khi contract được duyệt và có ViewModel mẫu.

**Điều chưa chắc:** contract dùng khoảng thời gian bao gồm đầu mốc và loại trừ cuối mốc, cùng kiểu kết quả ghi có bốn nhóm lỗi. Chờ Antigravity xác nhận hai quy ước này.
