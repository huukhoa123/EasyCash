# DECISIONS

Trạng thái: `ĐÃ CHỐT` (từ yêu cầu gốc của Khoa) · `ĐỀ XUẤT` (chờ Khoa duyệt, agent làm theo mặc định nhưng không coi là cố định) · `CẦN QUYẾT ĐỊNH` (chưa có mặc định, chặn phase liên quan).

| ID | Quyết định | Trạng thái | Ghi chú |
|----|-----------|-----------|---------|
| D-001 | Android native: Kotlin, Jetpack Compose, Material 3, Clean Architecture + MVVM, Room, Hilt, Coroutines/Flow, Navigation Compose, WorkManager, DataStore, Vico, Gradle KTS. | ĐÃ CHỐT | Lý do chính: cần `NotificationListenerService` (API gốc Android). |
| D-002 | Tách **4 Gradle module** `:app :ui :domain :data` (thay vì một module chia package). | ĐỀ XUẤT | Để `:ui` không thể import `:data`, ép ranh giới Codex/Antigravity bằng trình biên dịch. Đổi lại: tốn công dựng ban đầu (T-001). Khoa có thể đổi về một module trước khi T-001 bắt đầu. |
| D-003 | **Mục tiêu tiết kiệm (Savings Goals)** là tính năng đề xuất, thuộc Phase 4, không phải yêu cầu bắt buộc cho tới khi Khoa xác nhận chi tiết. | ĐỀ XUẤT | Yêu cầu gốc có "tiết kiệm cho một mục đích" nhưng spec chỉ có ngân sách theo danh mục. Hai thứ khác nhau. Khoa cần nói rõ: một hay nhiều mục tiêu, tiến độ tính theo số dư tài khoản hay theo khoản đóng góp. |
| D-004 | Bảng `notification_events` **không lưu nội dung gốc**. Chỉ lưu `fingerprint` (hash) và bản đã che dữ liệu nhạy cảm, tự xoá sau 30 ngày. Thông báo OTP không lưu gì. | ĐỀ XUẤT | Tránh phá luật riêng tư. Con số 30 ngày là mặc định, đổi được. |
| D-005 | Số dư ước tính tính từ giao dịch có trạng thái `AUTO_RECORDED` và `USER_CONFIRMED`. `NEEDS_REVIEW` và `REJECTED` **không** tính. | ĐỀ XUẤT | Tránh số dư sai khi giao dịch chưa được xác nhận. Cần Khoa xác nhận. |
| D-006 | Cài đặt người dùng lưu bằng **DataStore**. Bảng `app_settings` trong spec gốc bị trùng chức năng nên **không** tạo ở v1. | ĐỀ XUẤT | Spec gốc liệt kê cả hai. Nếu cần dữ liệu cài đặt đi theo file backup thì xem lại. |
| D-007 | Phạm vi mã hoá: chỉ file backup (khoá suy ra từ mật khẩu người dùng, AES-GCM) + khoá cục bộ trong Android Keystore. Mã hoá toàn DB hoặc từng trường là **CẦN QUYẾT ĐỊNH**. | CẦN QUYẾT ĐỊNH | Chặn Phase 5. Lưu ý: mã hoá trường `note` sẽ làm tìm kiếm SQL theo ghi chú không dùng được. Khoá Keystore không xuất được ra máy khác nên backup không thể dựa vào nó. |
| D-008 | Phát hiện chuyển tiền nội bộ giữa hai nguồn: **chỉ gợi ý** ghép cặp (cùng số tiền, ngược chiều, hai tài khoản, trong cửa sổ thời gian ngắn), người dùng xác nhận. Không tự động gộp. | ĐỀ XUẤT | Tránh tính đôi thu/chi. Cửa sổ thời gian cần xác minh với mẫu thật. |
| D-009 | Mục tiêu hệ điều hành: `minSdk 26` (Android 8.0); `compileSdk`/`targetSdk` là bản ổn định mới nhất tại Phase 1. | ĐỀ XUẤT / CẦN XÁC MINH | Xác minh thêm nhu cầu thiết bị thực tế của Khoa. |
| D-010 | Quyền `POST_NOTIFICATIONS` (Android 13+) cho cảnh báo ngân sách/nhắc định kỳ: cần xác minh cách cảnh báo (trong app hay thông báo hệ thống) trước khi làm Phase 4. | CẦN XÁC MINH | |

## Mẫu ghi quyết định mới

```
| D-0xx | <quyết định> | <trạng thái> | <lý do, ngày, ai duyệt> |
```
