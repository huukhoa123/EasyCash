# EasyCash 1.0.0-rc.3 — khôi phục ghi BIDV và chẩn đoán trên thiết bị

Pre-release. Sửa lỗi thông báo nhận trước lúc gán tài khoản bị chặn ghi lại: nhật ký sự kiện không còn bị coi là giao dịch đã ghi; chỉ receipt/giao dịch thực sự mới chặn trùng. Test tái hiện lỗi trước sửa và kiểm chứng sau sửa.

## Cài đúng APK, không gỡ app

- Đang dùng backup-bridge/bản thử: **EasyCash-backup-bridge-1.0.0-rc.3.apk**.
- Đang dùng release ký riêng: **EasyCash-1.0.0-rc.3.apk**.

Giữ thông báo BIDV còn trong thanh thông báo. Cập nhật APK, kiểm tra nguồn BIDV bật và đã gán tài khoản. Bật quyền **gửi thông báo** cho EasyCash để xem chẩn đoán, sau đó tắt/bật lại quyền **đọc thông báo** để kết nối lại listener.

Tìm thông báo **EasyCash — kiểm tra BIDV**. Nội dung cho biết bộ đọc đã kết nối, nguồn tắt, chưa gán tài khoản, chưa khớp định dạng, đã ghi hoặc đã xử lý trước đó. Nếu chỉ thấy “Bộ đọc đã kết nối”, chưa có kết quả xử lý BIDV sau lần kết nối đó. Nếu không có thông báo chẩn đoán, kiểm tra quyền gửi/kênh “Kiểm tra nhận BIDV”; chưa thể kết luận listener hỏng chỉ từ việc không thấy thông báo.

Chẩn đoán chỉ hiện trạng thái cố định, không chứa số tiền/số tài khoản/nội dung ngân hàng/OTP, không ghi Logcat hoặc gửi mạng. Nội dung bị ẩn trên màn hình khóa. Có thể tắt kênh chẩn đoán trong Android sau khi nghiệm thu; không ảnh hưởng đọc giao dịch. Đây là chẩn đoán tạm thời cho Pre-release.

Giao dịch nhận được nằm ở **Giao dịch → Chờ xem lại**; số dư chỉ đổi khi xác nhận. Không cần chuyển tiền thêm. App không phục hồi được thông báo đã xóa khỏi thanh thông báo.

Vẫn cần nghiệm thu trên máy người dùng. MoMo chưa hỗ trợ parser. Thông báo không mã tham chiếu có giới hạn chống trùng như rc.2; không tự ghép chuyển tiền nội bộ.
