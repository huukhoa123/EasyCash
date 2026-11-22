# Timo — nhận diện thông báo

Khoa yêu cầu thêm Timo và cung cấp ảnh push mở rộng ngày 2026-11-22. Package `io.lifestyle.plus` được xác minh từ [Google Play của Timo by BVBank](https://play.google.com/store/apps/details?id=io.lifestyle.plus). Không đọc email “Timo Support” trong Gmail, không đưa Gmail/Zalo vào whitelist.

Mẫu đã xác minh cấu trúc: “Số dư tài khoản vừa tăng … vào dd/MM/yyyy HH:mm VN”, số dư hiện tại, mô tả và số tài khoản bị che. Tiền dùng dấu chấm phân cách nghìn; giờ VN được đổi theo Asia/Ho_Chi_Minh, không theo timezone thiết bị. Fixture trong data/src/test/resources/notifications/timo/credit.txt dùng toàn bộ thông tin giả.

Chỉ hỗ trợ **nhận tiền theo mẫu này**. Chưa có mẫu trừ tiền/thất bại đã xác minh; parser không đoán các trường hợp đó. Thiếu số tiền, thời gian, số dư hoặc tài khoản/mô tả thì không tự tạo giao dịch.

Nguồn Timo được thêm mặc định ở trạng thái tắt; người dùng bật và gán tài khoản riêng. Không đổi schema/contract. ClearAll thêm lại BIDV/Timo đều tắt; restore giữ liên kết nhưng tắt nguồn như quy tắc hiện tại.

Không có mã tham chiếu trong mẫu: chống trùng dùng hash nội dung đầy đủ đã chuẩn hóa khoảng trắng. Hai giao dịch có mọi trường giống hệt không thể phân biệt từ thông báo. Không lưu nội dung ngân hàng thô. OTP bị bỏ trước lưu, nguồn tắt/ngoài whitelist không đọc nội dung.

Timo và BIDV có thể cùng thông báo cho chuyển tiền giữa hai tài khoản người dùng. Chưa tự ghép chuyển nội bộ; cần xem lại để không coi hai chiều là thu/chi tiêu dùng. T-025 tự ghi nhận/học danh mục là yêu cầu riêng đang chờ triển khai; thay đổi này giữ hành vi NEEDS_REVIEW hiện có.
