# DECISIONS

Trạng thái: `ĐÃ CHỐT` (từ yêu cầu gốc của Khoa) · `ĐỀ XUẤT` (chờ Khoa duyệt, agent làm theo mặc định nhưng không coi là cố định) · `CẦN QUYẾT ĐỊNH` (chưa có mặc định, chặn phase liên quan).

| ID | Quyết định | Trạng thái | Ghi chú |
|----|-----------|-----------|---------|
| D-001 | Android native: Kotlin, Jetpack Compose, Material 3, Clean Architecture + MVVM, Room, Hilt, Coroutines/Flow, Navigation Compose, WorkManager, DataStore, Vico, Gradle KTS. | ĐÃ CHỐT | Lý do chính: cần `NotificationListenerService` (API gốc Android). |
| D-002 | Tách **4 Gradle module** `:app :ui :domain :data` (thay vì một module chia package). | ĐỀ XUẤT | Để `:ui` không thể import `:data`, ép ranh giới Codex/Antigravity bằng trình biên dịch. Đổi lại: tốn công dựng ban đầu (T-001). Khoa có thể đổi về một module trước khi T-001 bắt đầu. |
| D-003 | **Mục tiêu tiết kiệm (Savings Goals)** là tính năng đề xuất, thuộc Phase 4, không phải yêu cầu bắt buộc cho tới khi Khoa xác nhận chi tiết. | ĐỀ XUẤT | Yêu cầu gốc có "tiết kiệm cho một mục đích" nhưng spec chỉ có ngân sách theo danh mục. Hai thứ khác nhau. Khoa cần nói rõ: một hay nhiều mục tiêu, tiến độ tính theo số dư tài khoản hay theo khoản đóng góp. |
| D-004 | Nhật ký sự kiện chỉ lưu metadata/hash, tự xoá sau 30 ngày. OTP không lưu gì. Khoá chống trùng được giữ để tránh tái nhập sau xoá giao dịch. | ĐÃ CHỐT | Khoa xác nhận 2026-11-20 trong chat. |
| D-005 | Số dư ước tính tính từ giao dịch có trạng thái `AUTO_RECORDED` và `USER_CONFIRMED`. `NEEDS_REVIEW` và `REJECTED` **không** tính. | ĐÃ CHỐT | Khoa xác nhận 2026-11-20: chỉ AUTO_RECORDED và USER_CONFIRMED. |
| D-006 | Cài đặt người dùng lưu bằng **DataStore**. Bảng `app_settings` trong spec gốc bị trùng chức năng nên **không** tạo ở v1. | ĐỀ XUẤT | Spec gốc liệt kê cả hai. Nếu cần dữ liệu cài đặt đi theo file backup thì xem lại. |
| D-007 | Mã hóa backup bằng mật khẩu người dùng, AES-GCM. Không yêu cầu mã hóa toàn DB trong v1. PIN/sinh trắc được bỏ theo D-011. | ĐÃ CHỐT | Khoa xác nhận 2026-11-21. Giữ khả năng chuyển dữ liệu từ bản thử. |
| D-008 | Phát hiện chuyển tiền nội bộ giữa hai nguồn: **chỉ gợi ý** ghép cặp (cùng số tiền, ngược chiều, hai tài khoản, trong cửa sổ thời gian ngắn), người dùng xác nhận. Không tự động gộp. | ĐỀ XUẤT | Tránh tính đôi thu/chi. Cửa sổ thời gian cần xác minh với mẫu thật. |
| D-009 | Mục tiêu hệ điều hành: `minSdk 26` (Android 8.0); `compileSdk`/`targetSdk` là bản ổn định mới nhất tại Phase 1. | ĐỀ XUẤT / CẦN XÁC MINH | Xác minh thêm nhu cầu thiết bị thực tế của Khoa. |
| D-010 | Cảnh báo ngân sách và nhắc định kỳ trong app, đồng thời gửi thông báo Android khi người dùng cấp quyền. | ĐÃ CHỐT | Khoa xác nhận 2026-11-21. Cần xử lý quyền POST_NOTIFICATIONS trên Android 13+. |

## Mẫu ghi quyết định mới

```
| D-0xx | <quyết định> | <trạng thái> | <lý do, ngày, ai duyệt> |
```

| D-011 | Bỏ khóa ứng dụng bằng PIN và sinh trắc khỏi phạm vi v1 dùng cá nhân. Giữ backup mã hóa và chuyển dữ liệu. | ĐÃ CHỐT | Khoa xác nhận trong chat 2026-11-21, thay phần khóa app của D-007 và FR-ST-04. |
