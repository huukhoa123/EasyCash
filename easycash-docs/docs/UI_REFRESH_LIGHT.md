# EasyCash — giao diện sáng, tinh gọn và cao cấp

Khoa chọn hướng này trong chat ngày 2026-11-22, thay định hướng dark mặc định trước đây. Antigravity triển khai :ui và MainActivity; Codex nghiệm thu tích hợp. Không đổi contract để làm phần trình bày.

## Vấn đề cần giải quyết

Ảnh máy thật cho thấy nhiều thẻ lớn, khoảng trống lớn bên trong thẻ, tiêu đề và nút cùng tranh sự chú ý. Home chưa cho thấy giao dịch và số liệu đủ sớm. Settings dùng diện tích quá lớn cho quyền đọc thông báo, còn bước gán tài khoản lại khó nhận biết. Đổi bảng màu đơn thuần chưa đủ.

## Hệ thống thị giác

- Nền trắng ấm #F7F8F5, surface #FFFFFF, chữ #152A25, chữ phụ #53655F, primary #08664F, màu nhấn nhạt #DDF4E9. Đây là palette thiết kế theo sản phẩm, chưa phải kết quả search tự động. Map về token Material, kiểm tra tương phản trước dùng.
- Số dư ước tính 34–38sp semibold, số tiền trong hàng giao dịch 16–18sp semibold, tiêu đề màn 26–28sp, nội dung 14–16sp. Dùng font có tiếng Việt từ hệ thống, không tải font qua mạng.
- Lề màn 20dp; nhịp 4/8/12/16/24/32dp. Bo góc 16–20dp cho card chính, 12dp cho input. Không bọc mọi nhóm dữ liệu trong card hoặc viền.
- Icon vector cùng nét, một bộ thống nhất; logo EasyCash chỉ ở header nhỏ hoặc nhận diện ứng dụng. Không dùng emoji làm icon cấu trúc.
- Một hành động chính mỗi vùng. Touch target ít nhất 48dp; chữ lớn không cắt nội dung; không dùng màu làm tín hiệu duy nhất.
- Chế độ sáng là mặc định cho người chưa chọn theme. Giữ lựa chọn tối đã lưu của người dùng và hoàn thiện dark tương ứng, không ép reset tùy chọn.

## Bố cục theo màn

### Tổng quan

Header gọn: logo/tên app bên trái, nút ẩn số tiền có nhãn accessibility bên phải. Chỉ một vùng số dư: nhãn “Số dư ước tính”, con số lớn, dòng số tài khoản nhỏ. Không có khối màu tối chiếm nửa màn.

Thu và chi tháng này đặt cùng một hàng nhẹ, dùng dấu +/−, không làm hai card khổng lồ. Chọn tháng gọn và rõ. Hàng thao tác thêm thu/chi/chuyển tiền dùng đúng luồng hiện có, không thêm chức năng giả.

Ngay sau đó là giao dịch gần đây, mỗi hàng có icon danh mục, tên/nội dung, tài khoản và số tiền căn phải. Header ngày nhỏ, divider mảnh. Với chiều cao màn 800dp, ít nhất thấy được đầu danh sách giao dịch khi có dữ liệu. Chưa có dữ liệu: một empty state gọn, CTA tạo tài khoản; không nhân đôi nút Tài khoản/Giao dịch vốn đã có ở bottom nav.

### Giao dịch và phân loại

Ô tìm kiếm, chọn tháng và bộ lọc ngắn; danh sách là nhân vật chính. Hàng 68–80dp, nội dung dài tối đa hai dòng, số tiền không bị đẩy ra ngoài. Chạm hàng mở chi tiết với thao tác sửa danh mục rõ ràng. Chuyển nội bộ có dấu hiệu riêng, không biểu diễn như chi tiêu.

Giữ hành vi backend hiện tại khi chưa có contract/bản triển khai mới. Không tự đổi trạng thái hoặc tính số dư từ UI để mô phỏng yêu cầu tự ghi nhận. “Chờ xem lại” chỉ xuất hiện khi có dữ liệu tương ứng; không làm thành hành động mặc định cho mọi giao dịch mới.

### Tài khoản

Danh sách gọn, mỗi hàng có tên/loại và số dư ước tính. Tổng hợp ở đầu nhỏ hơn Home, thao tác tạo tài khoản nổi bật. Cho thấy trạng thái gán nguồn thông báo nếu contract hiện tại cung cấp được. Không yêu cầu đăng nhập hoặc mật khẩu ngân hàng.

### Báo cáo và ngân sách

Chọn tháng rõ; chart dùng nhãn/dữ liệu thực, không fake. Thống kê chính rồi danh mục và ngân sách, không nhiều lớp card lồng nhau. Empty state giải thích khi chưa đủ dữ liệu. Khoản định kỳ có điều hướng rõ, giữ CRUD và permission flow đang hoạt động.

### Cài đặt

Chuyển thành các hàng nhóm gọn: Giao diện / Tự động ghi nhận / Dữ liệu. Trạng thái quyền đọc thông báo là một hàng, phần giải thích mở rộng khi cần. Thẻ nguồn BIDV đặt tên tài khoản đích và trạng thái cạnh nhau; nếu chưa gán phải hiện “Chưa thể ghi giao dịch — chọn tài khoản đích” cùng CTA rõ. Không để công tắc bật tạo cảm giác đã thiết lập xong.

Backup/restore và xóa dữ liệu giữ xác nhận, trạng thái đang làm và xử lý lỗi hiện có. Các luồng nhạy cảm không được mất khi thiết kế lại.

## Nghiệm thu

Antigravity bàn giao ảnh 5 tab có dữ liệu và ít nhất Home/Settings chưa có dữ liệu; light/dark, màn nhỏ 360dp và font scale 1.3. Kiểm tra tiền lớn, nội dung tiếng Việt dài, bàn phím, safe area và bottom navigation. Cung cấp ảnh đối chiếu trước/sau, không chỉ báo “đẹp hơn”.

Chạy assembleDebug và testDebugUnitTest; test logic ViewModel chỉ khi có đổi logic. Không đưa fake repository/data vào release. Không đổi data/domain/app-di. Codex kiểm tra build release, chữ ký và đóng gói APK sau bàn giao. Không tự phát hành trước kiểm tra.

## Các yêu cầu chức năng đang chờ, tách khỏi thay đổi trình bày

Khoa đã yêu cầu tự ghi nhận thu/chi từ thông báo đủ rõ, không phải xác nhận lại; học danh mục từ giao dịch đã phân loại để áp dụng giao dịch mới tương tự. Thông báo lặp của cùng giao dịch vẫn chống trùng; giao dịch mới tương tự vẫn là bản ghi mới. Codex cần triển khai backend và đề xuất contract nếu cần; chưa được coi là đã có trong rc.4. Không tự suy diễn ghép chuyển nội bộ.
