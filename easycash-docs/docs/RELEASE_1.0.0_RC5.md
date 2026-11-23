# EasyCash 1.0.0-rc.5 — phục hồi bộ đọc thông báo

Pre-release, versionCode 6. Bổ sung yêu cầu Android kết nối lại listener khi nhận callback mất kết nối; kiểm tra lại lúc tiến trình app khởi động và trong worker định kỳ hiện có. Không đặt service dừng theo task màn hình. Chỉ yêu cầu kết nối khi quyền đọc còn được cấp và listener chưa kết nối; không thay đổi hay tự cấp lại quyền.

Khi kết nối lại, app kiểm tra thông báo ngân hàng còn hiện và chống trùng như trước. Worker là dự phòng, có chu kỳ tối thiểu 15 phút và có thể trễ do Android; không phải cơ chế ghi nhận realtime khi listener đang bị hệ thống chặn. Không dùng foreground service giả để giữ app sống.

## Cài đặt

- Bản thử/backup-bridge đang có: cài đè **EasyCash-backup-bridge-1.0.0-rc.5.apk**.
- Bản release ký riêng đang có hoặc cài mới: **EasyCash-1.0.0-rc.5.apk**.

Không gỡ app để cập nhật. Cài xong mở app một lần; quyền đọc đã được cấp sẽ được kiểm tra để yêu cầu kết nối lại, không cần tắt/bật quyền thủ công. Nếu chưa cấp quyền, vẫn cần người dùng cấp trong Android.

## Cách nghiệm thu

1. Kiểm tra nguồn ngân hàng bật và đã gán tài khoản, giữ thông báo ngân hàng trên thanh thông báo.
2. Mở EasyCash, về Home hoặc khóa màn hình; không tắt/bật quyền. Sau giao dịch tự nhiên kiểm tra hàng chờ và thông báo chẩn đoán nếu đã cấp quyền gửi.
3. Thử vuốt khỏi danh sách gần đây và mở lại app. Không chuyển tiền chỉ để test. Kiểm tra không ghi trùng khi kết nối lại.

“Buộc dừng” trong cài đặt Android là trường hợp khác: hệ điều hành giữ app trong trạng thái dừng đến khi người dùng tương tác/mở lại. Không cam kết vượt giới hạn này. Một số thiết bị có chính sách chạy nền riêng; cần nghiệm thu trên điện thoại thực tế trước khi coi hoàn tất.

Tham khảo chính thức: [NotificationListenerService/requestRebind](https://developer.android.com/reference/android/service/notification/NotificationListenerService#requestRebind(android.content.ComponentName)), [trạng thái package stopped](https://developer.android.com/about/versions/15/behavior-changes-all#stopped-state).

## Các thay đổi khác và giới hạn

Có nguồn Timo với mẫu **nhận tiền** đã được cung cấp, mặc định tắt và phải gán tài khoản riêng. Chưa có mẫu Timo trừ tiền đã xác minh. BIDV/Timo vẫn vào NEEDS_REVIEW trong bản này; T-025 tự ghi nhận/học danh mục còn riêng. Chưa tự ghép chuyển tiền nội bộ.

APK này được build từ source đã commit để tránh đóng gói UI ledger còn chưa nghiệm thu trong workspace. Giao diện ledger mới chưa nằm trong bản cập nhật này. Giữ logo và giao diện của dòng rc.4. Không thay schema/khóa ký.
