# TASKS

Status: TODO · IN_PROGRESS · REVIEW · DONE · BLOCKED
Nhận việc: đổi Owner/Status trước khi bắt đầu. Không nhận task đang có chủ.

## Phase 1 — Foundation & Architecture

| ID | Task | Owner | Status | Phụ thuộc | Tiêu chí xong (verify) |
|---|---|---|---|---|---|
| T-001 | Khởi tạo project Gradle Kotlin DSL với 4 module :app :ui :domain :data, version catalog, Hilt, Room skeleton. Manifest **không** có INTERNET. | codex | DONE | — | ./gradlew assembleDebug pass; :ui không phụ thuộc :data (kiểm tra trong  uild.gradle.kts) |
| T-002 | Chốt contract v1: model + repository interface trong :domain, cập nhật mục Active của CONTRACTS.md | codex | DONE | — | CONTRACTS.md khớp code; Antigravity ghi ACK |
| T-003 | Design system: theme Material 3 Dark (mặc định) + Light theo token trong docs/UI_UX.md, typography, shapes, preview | antigravity | DONE | T-001 | Preview dark/light hiển thị đúng; không hard-code màu ngoài theme |
| T-004 | Navigation Compose + Bottom Navigation 5 tab (Home, Transactions, Reports, Accounts, Settings) với màn hình placeholder | antigravity | DONE | T-001 | Chạy được, chuyển tab không mất trạng thái |
| T-005 | Hilt wiring :app/di nối :ui ↔ :domain ↔ :data (binding tạm có thể là fake) | codex | DONE | T-001, T-002 | App khởi động, ViewModel mẫu nhận được repository qua Hilt |
| T-006 | Tổng kiểm Phase 1 theo tiêu chí trong spec mục 15 | codex + antigravity | DONE | T-001…T-005 | Mọi mục Phase 1 trong spec được tick; HANDOFF.md có báo cáo |

## Backlog (chưa nhận, chưa bắt đầu)

Phase 2 (Core Finance):
- [codex] Room entity + DAO + repository: accounts, categories, transactions (xem docs/DATABASE.md); seed danh mục mặc định; tính số dư ước tính; chuyển tiền nội bộ.
- [antigravity] Màn Accounts, Transactions (danh sách, thêm/sửa/xoá, tìm kiếm, lọc), Home cơ bản (số dư ước tính, tổng thu/chi, giao dịch gần đây).

Phase 3 (Automatic Detection): xem docs/NOTIFICATION_PARSING.md. **Bị chặn bởi việc Khoa cung cấp mẫu thông báo thật của MoMo/BIDV.**

Các phase sau: theo spec mục 15.

## Phase 2 — Core Finance (Đã hoàn thành)

| ID | Task | Owner | Status | Verify |
|---|---|---|---|---|
| T-007 | Room schema và DAO danh mục, sau đó repository theo contract v1 | codex | DONE | Room compiler kiểm tra query, build và unit test |
| T-008 | Fake repository (:ui): FakeAccountRepository, FakeCategoryRepository, FakeTransactionRepository để dựng UI độc lập | antigravity | DONE | Fake cung cấp đủ Flow và thao tác CRUD, unit test pass |
| T-009 | Màn hình Accounts: danh sách tài khoản, thêm/sửa tài khoản, số dư ước tính | antigravity | DONE | Preview hoạt động, tương tác CRUD với Fake repo |
| T-010 | Màn hình Transactions: danh sách theo ngày, bộ lọc, thêm/sửa giao dịch | antigravity | DONE | Thêm/sửa/xoá được trên Fake repo, unit test pass |
| T-011 | Màn hình Home cơ bản: thẻ số dư ước tính, tổng thu/chi, giao dịch gần đây | antigravity | DONE | Hiển thị đúng số liệu từ repository, unit test pass |
| T-012 | Room accounts/transactions/stats, số dư D-005, migration v1-v2 và binding debug | codex | DONE | Test tiền/chuyển tiền/migration, assembleDebug, testDebugUnitTest |

## Phase 3 — Automatic Transaction Detection (Đang triển khai)

| ID | Task | Owner | Status | Verify |
|---|---|---|---|---|
| T-013 | Fixture mẫu thông báo BIDV & MoMo + Parser BIDV Kotlin thuần với unit test | codex | IN_PROGRESS | Test fixture trích xuất đúng số tiền, chiều, thời gian, dedup_key |
| T-014 | Database migration v3: thêm bảng notification_sources & notification_events, RoomNotificationSourceRepository | codex | DONE | Test migration v2-v3, CRUD nguồn và sự kiện |
| T-015 | NotificationListenerService + lọc OTP + dedup_key + dispatch sự kiện vào DB | codex | REVIEW | Test huỷ OTP, không lưu/log nhạy cảm, chặn trùng lặp |
| T-016 | UI Hàng chờ xem lại (Review Queue) & Lọc giao dịch tự động trên Home & Transactions | antigravity | DONE | Filter chip Chờ xem lại, nút Xác nhận/Từ chối, cập nhật số dư D-005 |
| T-017 | UI Quản lý nguồn thông báo & Onboarding cấp quyền Android 13+ | antigravity | DONE | Onboarding banner trên Home, hướng dẫn chi tiết 2 bước trong Settings |
## GitHub CI

| ID | Task | Owner | Status | Verify |
|---|---|---|---|---|
| T-018 | Android GitHub Actions, APK/test artifacts và required check main | codex | REVIEW | GitHub run pass, required Verify debug |

## Hoàn thiện v1 và thiết kế lại theo Khoa 2026-11-21

| ID | Task | Owner | Status | Verify |
|---|---|---|---|---|
| T-019 | Ngân sách Room thật, migration v5, binding release, test tháng/trạng thái | codex | REVIEW | Debug/release build và test storage ngân sách |
| T-020 | Thiết kế lại toàn bộ UX/UI cao cấp, nhất quán, tối ưu dùng cá nhân | antigravity | DONE | Kiểm tra 5 tab, dialogs, Dark/Light, SAF P-002, ảnh emulator, unit tests pass 100% |
| T-021 | Backup mã hóa, restore nguyên tử, CSV, clearAll và validation dữ liệu nhập | codex | REVIEW | Test restore sạch/sai mật khẩu/file lỗi/rollback, build debug/release |
| T-022 | Backend & UI nhắc định kỳ + cảnh báo ngân sách Android, migration v6, WorkManager | codex + antigravity | DONE | Test lịch/tháng/dedup/quyền, UI CRUD nhắc định kỳ, WorkManager, build debug/release pass |

| T-023 | APK ký riêng, bản cập nhật chuyển dữ liệu và GitHub Pre-release theo Khoa | codex | DONE | Build/test, chữ ký/checksum, CI 45039121174 pass; Pre-release công khai. Emulator không kết nối, nghiệm thu máy thật còn chờ |

| T-024 | Sổ tiền cá nhân hiện đại theo UI_LEDGER_DIRECTION.md | antigravity | REVIEW | 5 tab ledger palette, sửa 3 điểm review, ảnh workspace, build/test pass |
| T-025 | Tự ghi nhận thông báo rõ ràng và học danh mục từ giao dịch đã phân loại | codex | TODO | Không xác nhận lại mẫu rõ, chống trùng, phân biệt giao dịch mới tương tự |

| T-026 | Nguồn Timo, parser nhận tiền từ mẫu thật, whitelist/backup/test | codex | REVIEW | Parser fixture, OTP, chống trùng, nguồn tắt và restore; chưa nghiệm thu điện thoại |
