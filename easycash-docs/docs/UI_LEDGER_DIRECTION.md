# EasyCash — sổ tiền cá nhân hiện đại

## Yêu cầu mới của Khoa

Ngày 2026-11-23, Khoa cung cấp ảnh tham khảo kiểu sổ thu chi: nền giấy sáng, số dư vàng nhạt, thu xanh mint, chi hồng, ví dạng lưới, giao dịch như cuống hóa đơn. Yêu cầu là sáng tạo từ cấu trúc đó để giao diện **khác rõ bản hiện tại**, không sao chép nguyên mẫu. Hướng này thay phần thị giác/bố cục của UI_REFRESH_LIGHT.md; giữ các yêu cầu chức năng, an toàn dữ liệu và accessibility của brief cũ.

## Hướng thị giác đã chọn để triển khai

“Sổ tiền cá nhân hiện đại”: ấm, rõ số liệu, có cá tính nhưng vẫn là ứng dụng tài chính dễ dùng. Tránh chỉ thay màu xanh thành vàng trên bộ card cũ.

- Canvas ivory #FAF7EF, chữ mực #202B27, chữ phụ #5D665E. Emerald #08664F giữ làm màu nhận diện và CTA chính.
- Bảng màu chức năng: số dư butter #FFF0B5, thu mint #D8F2E4, chi rose #F7DFE5, chuyển nội bộ slate #E8EBF0. Chữ đậm có tương phản tốt, luôn kèm nhãn/dấu chứ không chỉ dùng màu.
- Phân biệt ba hình thức: vùng số dư lớn dạng trang sổ, ví dạng ô nhỏ, giao dịch thành danh sách phẳng. Không áp cùng Surface/outline/bo góc cho mọi thành phần.
- Nét viền 1dp màu mực nhạt ở những chỗ cần chia nhóm; không viền đen dày toàn màn. Bo 12–18dp, không pill khổng lồ. Chỉ dùng một motif mép hóa đơn ở đầu/đuôi nhóm giao dịch, không lặp răng cưa từng hàng.
- Có thể dùng một dải nhấn nhỏ như tab giấy ở tiêu đề. Không thêm texture bitmap phủ toàn màn, ghim đỏ/emoji/trang trí chuyển động hoặc ảnh nền nặng.
- Font hệ thống có tiếng Việt, nhãn ngắn. Số tiền chính 34–38sp, thông tin phụ 13–15sp. Tiền dài phải xem đầy đủ được, không âm thầm cắt mất chữ số.

## Home: xây lại thứ tự thông tin

1. Header thấp: logo EasyCash + tên màn “Sổ tiền”, chọn tháng dạng nút gọn khi đã có state/logic hỗ trợ; không dựng nút giả. Nút ẩn số tiền có nhãn rõ.
2. Vùng “Số dư ước tính” màu butter, con số lớn, dòng số tài khoản nhỏ. Không gọi “số dư ngân hàng” hay “khả dụng” vì app không truy vấn ngân hàng.
3. Hai ô Thu/Chi tháng cùng hàng, mint/rose, nhãn kèm dấu +/−, số tiền đọc nhanh. Chiều cao gọn khoảng 80–96dp ở font thường; cho phép tăng chiều cao khi font lớn.
4. “Ví của bạn”: tối đa hai tài khoản đầu dạng lưới hai cột và nút “Xem tất cả”. Hiển thị tên/loại/số dư, dùng dữ liệu thật. Có một ví thì một ô cùng CTA tạo ví khi hợp lý; không tạo ví demo cho đủ lưới.
5. “Dòng tiền gần đây”: danh sách phẳng chia ngày, có đầu nhóm gợi trang hóa đơn. Hàng gồm icon danh mục, nội dung, tài khoản và số tiền căn phải; divider nhẹ. Không bao từng giao dịch bằng một card viền riêng.
6. Banner cần xem lại/cấp quyền xuất hiện có điều kiện, dùng hàng thông báo gọn. Không chiếm nửa màn hoặc che các vùng chính.

Ở 360×800dp, font thường và có dữ liệu: thấy số dư, thu/chi và ít nhất tiêu đề vùng ví trong viewport đầu; cuộn ít để tới giao dịch. Không cố nhét tất cả bằng chữ nhỏ. Trạng thái trống vẫn theo cùng bố cục, một CTA rõ để tạo tài khoản.

## Các tab khác phải cùng ngôn ngữ mới

- Giao dịch: dòng tiêu đề kiểu sổ, tháng/bộ lọc gọn, danh sách chia ngày. Thông tin giao dịch là nội dung chính. Sửa lỗi nhãn tìm kiếm hứa tìm số tiền khi backend chỉ tìm ghi chú. Giữ sửa danh mục, xác nhận/từ chối theo contract hiện tại.
- Tài khoản: lưới ví hai cột cho màn đủ rộng, tên dài/font lớn tự chuyển một cột; số dư không bị cắt. Chuyển nội bộ có sắc thái và ký hiệu riêng.
- Báo cáo: một vùng tổng kết tháng nhẹ cùng chart thật, phân bổ danh mục dạng thanh và nhãn rõ. Không trang trí kiểu hóa đơn lấn át biểu đồ. Nhắc định kỳ giữ luồng đang dùng.
- Cài đặt: các nhóm hàng phẳng, không biến mỗi mục thành một thẻ lớn. Nguồn BIDV/Timo từ repository, CTA gán tài khoản rõ. Không hiển thị MoMo như nguồn đang hỗ trợ nếu backend chưa có.
- Navigation: giữ 5 đích hiện có và trạng thái điều hướng. Nếu dùng nút “+” nhấn mạnh, phải nối luồng thêm giao dịch thật và có nhãn; không thay mất tab hoặc tạo hai CTA cùng vai trò. Không đưa thanh địa chỉ, notch giả, banner cài PWA hay khung điện thoại vào UI native.

## Tiêu chí khác biệt và nghiệm thu

Antigravity làm Home trước như một lát cắt hoàn chỉnh, sau đó áp cùng hệ thống cho các tab. Không chỉ sửa Color.kt/Type.kt rồi gọi hoàn tất.

Phải có ảnh trước/sau cùng thiết bị, cùng dữ liệu cho Home, Giao dịch, Tài khoản và Cài đặt; cung cấp **đường dẫn file có thật**. Bộ ảnh gồm light/dark, dữ liệu rỗng/có dữ liệu, màn360dp và font1.3. Với dark, giữ tương phản và vai trò màu, không đảo màu đơn giản. Giữ theme đã lưu của người dùng.

Kiểm tra tên tài khoản dài không đẩy mất ngày, tiền lớn không biến mất, vùng chạm ít nhất48dp, bàn phím/safe area, backup/restore và cấp quyền không mất thao tác. Các lỗi review trong HANDOFF.md vẫn phải sửa. Build/test đạt mới bàn giao; Codex nghiệm thu và ký APK, chưa phát hành chỉ vì hình nhìn khác.

Không thay logic tự ghi nhận/học danh mục trong UI: đó là T-025 của Codex. Không tự ghép BIDV/Timo thành chuyển nội bộ hoặc thêm thông tin ngân hàng giả để minh họa khi chạy release.
