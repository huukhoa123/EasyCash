# EasyCash 1.0.0-rc.1 — bản thử nghiệm có ký

Đây là **Pre-release**, chưa nghiệm thu thông báo ngân hàng trên điện thoại thật.

## Chọn APK

- **EasyCash-1.0.0-rc.1.apk**: bản release ký bằng khóa riêng, dùng cho cài mới và các bản cập nhật release tiếp theo.
- **EasyCash-backup-bridge-1.0.0-rc.1.apk**: bản debug có chức năng backup, ký cùng khóa bản thử ngày 2026-11-21. Dùng để cập nhật bản thử cũ, giữ dữ liệu và xuất backup trước khi đổi khóa ký.
- **SHA256SUMS.txt**: checksum của hai APK. **SIGNING-CERTIFICATES.txt**: dấu vân tay chứng chỉ ký, không chứa khóa riêng.

Nếu máy đang có dữ liệu, **không gỡ app cũ ngay**. Hai APK dùng cùng applicationId nhưng khác khóa ký nên bản release không cài đè bản thử được.

## Đang dùng bản thử cũ

1. Tải và cài đè APK `backup-bridge`, giữ dữ liệu hiện có.
2. Trong Cài đặt → quản lý dữ liệu, xuất backup mã hóa ra file và giữ mật khẩu. Có thể xuất thêm CSV để đối chiếu.
3. Kiểm tra khôi phục backup trên thiết bị thử khác, đối chiếu tài khoản/giao dịch/số dư trước khi gỡ app cũ. Nếu chưa có thiết bị khác hoặc chưa kiểm tra được, **tiếp tục dùng bản bridge** để nghiệm thu; không cần chuyển sang release ngay.
4. Khi đã xác nhận backup dùng được, gỡ bản thử, cài APK release rồi khôi phục backup. Restore thay toàn bộ dữ liệu hiện tại.
5. Bật lại nguồn thông báo và cấp quyền mong muốn. Sau restore, các nguồn chủ động được tắt.

## Có trong bản này

Quản lý tài khoản, thu/chi/chuyển tiền, số dư ước tính, danh mục, báo cáo, ngân sách, nhắc định kỳ, backup mã hóa và CSV. Giao diện đã được thiết kế lại; sửa tràn tổng tiền nhắc định kỳ và đồng bộ quyền thông báo khi quay lại app. Lưu trữ tài khoản sẽ tắt các khoản nhắc liên quan.

App hoạt động offline, không có quyền INTERNET. Nhắc định kỳ không tự thanh toán hoặc tạo giao dịch; hệ điều hành có thể trì hoãn lịch chạy.

## Còn nghiệm thu

- BIDV: parser hiện có fixture nhưng chưa nghiệm thu push thật. Giao dịch nhận diện được đưa vào hàng chờ xem lại; chỉ trạng thái được chấp nhận mới ảnh hưởng số dư ước tính.
- MoMo: chưa có parser/mẫu thật đã xác minh, chưa hỗ trợ nhận diện tự động trong bản này.
- Quyền gửi thông báo và lịch chạy nền trên điện thoại Android 13+, chuyển backup trên điện thoại người dùng.

Bạn có thể dùng nhập tay ngay và thử thông báo khi phát sinh giao dịch tự nhiên. Không cần chuyển tiền riêng để test. Không đưa OTP, mật khẩu, số tài khoản hoặc nội dung thông báo chưa che dữ liệu lên GitHub.

## Lưu khóa cho lần cập nhật sau (máy build)

Khóa riêng và mật khẩu chỉ lưu trong thư mục `.signing/` bị Git bỏ qua. Chủ máy cần sao lưu cả thư mục sang nơi riêng an toàn; không gửi lên GitHub hoặc đính kèm release. Mất khóa ký sẽ không thể cài đè các bản cập nhật cùng ứng dụng.

Trên máy có `.signing/release.properties`, `assembleRelease` tạo APK ký riêng. CI không có file này chỉ kiểm tra build unsigned, không tạo APK phát hành có ký.
