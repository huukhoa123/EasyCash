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
