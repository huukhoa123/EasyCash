# Nghiệm thu và chuyển dữ liệu v1

Chưa phát hành stable v1. Khoa đã duyệt phát hành Pre-release có ký trước khi nghiệm thu máy thật; xem RELEASE_1.0.0_RC1.md. CI không có khóa riêng vẫn build unsigned và không dùng APK đó để cài.

## Điều kiện trước khi phát hành

- [x] Build debug/release và unit test của bản tích hợp nhắc định kỳ đã pass.
- [x] Sửa phép tính tổng tiền nhắc định kỳ bị tràn Long; giữ nguyên số tiền từng khoản.
- [x] Cập nhật trạng thái quyền thông báo khi quay lại app.
- [ ] Nghiệm thu trên Android 13+: từ chối/cấp/thu hồi quyền gửi thông báo; nhập tay vẫn dùng được.
- [ ] Nghiệm thu thông báo BIDV thật: nhận đúng chiều/số tiền, thông báo lặp không tạo thêm giao dịch, số dư chỉ đổi sau xác nhận.
- [ ] Có mẫu MoMo đã che dữ liệu, parser và test tương ứng. Không đoán định dạng thông báo.
- [x] Tạo khóa ký riêng cùng mật khẩu trong .signing/ bị Git bỏ qua, giới hạn quyền truy cập; cấu hình ký và tăng version 1.0.0-rc.1 (2). Chủ máy cần sao lưu khóa riêng an toàn.
- [ ] Kiểm tra chuyển dữ liệu từ bản thử trên điện thoại theo quy trình dưới đây.
- [ ] CI của đúng commit phát hành thành công; ghi commit, checksum và chứng chỉ ký của APK trong release notes.

## Giữ dữ liệu từ bản thử

1. Cài đè bản thử mới có chức năng backup bằng cùng khóa ký của bản thử hiện tại. Không dùng APK debug từ CI vì khóa ký có thể khác.
2. Trong Cài đặt, xuất backup mã hóa ra nơi người dùng chọn và giữ mật khẩu. Xuất thêm CSV nếu muốn đối chiếu; CSV không thay thế backup.
3. Đối chiếu số tài khoản, số giao dịch và số dư ước tính. Chỉ gỡ bản thử sau khi đã kiểm tra khôi phục file backup thành công trên thiết bị thử khác và vẫn giữ file gốc.
4. Cài bản release có khóa riêng, khôi phục backup và đối chiếu lại các số liệu ở bước 3.
5. Bật lại nguồn thông báo và cấp các quyền mong muốn. Restore chủ động tắt nguồn để tránh nhập ngoài ý muốn.

Nếu chưa kiểm tra khôi phục được, tiếp tục giữ bản thử và dữ liệu trên máy. Không yêu cầu chuyển tiền để tạo mẫu; có thể nghiệm thu khi phát sinh giao dịch tự nhiên.

## Kiểm tra thiết bị thật

Ghi phiên bản Android, phiên bản EasyCash, nguồn thông báo, bước thực hiện và kết quả. Không đưa nội dung thông báo thật, OTP, số tài khoản hoặc mật khẩu vào Git/log.

- Tạo/sửa/xóa khoản nhắc; lưu trữ tài khoản phải tắt khoản nhắc liên quan.
- Đến hạn nhắc không tự tạo giao dịch. WorkManager có thể chạy trễ theo hệ điều hành.
- Cảnh báo ngân sách không lặp lại cùng mức trong cùng tháng.
- Đổi quyền gửi thông báo ở Cài đặt hệ thống rồi quay lại app; banner phải phản ánh quyền mới.
- Sao lưu/khôi phục; sai mật khẩu và file lỗi phải giữ nguyên dữ liệu hiện tại.
