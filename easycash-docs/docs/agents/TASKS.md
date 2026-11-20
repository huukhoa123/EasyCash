# TASKS

Status: `TODO` · `IN_PROGRESS` · `REVIEW` · `DONE` · `BLOCKED`
Nhận việc: đổi Owner/Status trước khi bắt đầu. Không nhận task đang có chủ.

## Phase 1 — Foundation & Architecture

| ID | Task | Owner | Status | Phụ thuộc | Tiêu chí xong (verify) |
|----|------|-------|--------|-----------|------------------------|
| T-001 | Khởi tạo project Gradle Kotlin DSL với 4 module `:app :ui :domain :data`, version catalog, Hilt, Room skeleton. Manifest **không** có `INTERNET`. | codex | REVIEW | — | `./gradlew assembleDebug` pass; `:ui` không phụ thuộc `:data` (kiểm tra trong `build.gradle.kts`) |
| T-002 | Chốt contract v1: model + repository interface trong `:domain`, cập nhật mục Active của `CONTRACTS.md` | codex | TODO | — | `CONTRACTS.md` khớp code; Antigravity ghi `ACK` |
| T-003 | Design system: theme Material 3 Dark (mặc định) + Light theo token trong `docs/UI_UX.md`, typography, shapes, preview | antigravity | TODO | T-001 | Preview dark/light hiển thị đúng; không hard-code màu ngoài theme |
| T-004 | Navigation Compose + Bottom Navigation 5 tab (Home, Transactions, Reports, Accounts, Settings) với màn hình placeholder | antigravity | TODO | T-001 | Chạy được, chuyển tab không mất trạng thái |
| T-005 | Hilt wiring `:app/di` nối `:ui` ↔ `:domain` ↔ `:data` (binding tạm có thể là fake) | codex | TODO | T-001, T-002 | App khởi động, ViewModel mẫu nhận được repository qua Hilt |
| T-006 | Tổng kiểm Phase 1 theo tiêu chí trong spec mục 15 | codex + antigravity | TODO | T-001…T-005 | Mọi mục Phase 1 trong spec được tick; `HANDOFF.md` có báo cáo |

## Backlog (chưa nhận, chưa bắt đầu)

Phase 2 (Core Finance):
- [codex] Room entity + DAO + repository: accounts, categories, transactions (xem `docs/DATABASE.md`); seed danh mục mặc định; tính số dư ước tính; chuyển tiền nội bộ.
- [antigravity] Màn Accounts, Transactions (danh sách, thêm/sửa/xoá, tìm kiếm, lọc), Home cơ bản (số dư ước tính, tổng thu/chi, giao dịch gần đây).

Phase 3 (Automatic Detection): xem `docs/NOTIFICATION_PARSING.md`. **Bị chặn bởi việc Khoa cung cấp mẫu thông báo thật của MoMo/BIDV.**

Các phase sau: theo spec mục 15.
