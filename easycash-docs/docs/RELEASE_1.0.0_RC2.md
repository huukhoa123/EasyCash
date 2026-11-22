# EasyCash 1.0.0-rc.2 — sửa thông báo BIDV

Pre-release. Sửa lỗi parser bỏ qua push BIDV thực tế vì bắt buộc câu mở đầu và mã giao dịch không có trong thông báo. Parser v2 hỗ trợ mẫu đã quan sát, vẫn giữ hỗ trợ mẫu cũ và lọc OTP trước khi xử lý. Đọc nội dung textLines khi không có bigText.

Khi listener kết nối lại, app kiểm tra các thông báo BIDV còn trên thanh thông báo; nguồn vẫn phải bật và liên kết tài khoản. Không đọc lịch sử đã xóa khỏi thanh thông báo. Giao dịch nhận diện đưa vào **Chờ xem lại**, chưa làm đổi số dư ước tính cho đến khi xác nhận.

## Chọn đúng APK

- Đang dùng APK thử/debug hoặc backup-bridge: cài đè **EasyCash-backup-bridge-1.0.0-rc.2.apk**, không gỡ app, giữ dữ liệu.
- Đang dùng APK release ký riêng rc.1 hoặc cài mới: dùng **EasyCash-1.0.0-rc.2.apk**.
- Không đổi từ bridge sang release bằng cách gỡ app khi chưa kiểm tra backup. Hai dòng APK khác khóa ký.

Để thử lại khoản vừa rồi, giữ thông báo BIDV trong thanh thông báo, cập nhật đúng APK, kiểm tra nguồn BIDV bật và đã gán tài khoản, rồi tắt/bật lại quyền **đọc thông báo** EasyCash trong Android để listener kết nối lại. Xem bộ lọc **Chờ xem lại**. Không cần chuyển tiền thêm.

## Giới hạn

Push không có mã giao dịch: chống trùng dùng hash của tài khoản, thời gian giao dịch, số tiền có dấu, số dư cuối và nội dung đã chuẩn hóa. Không lưu nội dung thô. Hai thông báo có toàn bộ các trường này giống hệt được coi là trùng; cần kiểm tra thủ công nếu ngân hàng gửi các giao dịch không phân biệt được bằng thông báo.

Không tự ghép chuyển tiền nội bộ: chuyển giữa hai tài khoản của chính mình cần xem lại để tránh ghi thành chi tiêu. MoMo chưa có parser. Nhận diện trên thiết bị người dùng và quyền/lịch Android13+ vẫn cần nghiệm thu. Có SHA256SUMS.txt và SIGNING-CERTIFICATES.txt kèm release; không chứa khóa riêng.
