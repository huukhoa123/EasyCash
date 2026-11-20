# UI / UX

Tài liệu cho **Antigravity**. Chưa phải code.
Mã màu, cỡ chữ, khoảng cách là **[ĐỀ XUẤT]**, Khoa có thể đổi. Khi chốt thì đưa vào theme (T-003), **không hard-code màu ở nơi khác**.

## 1. Định hướng

Modern Premium Fintech: hiện đại, sạch, dễ đọc con số. Material 3.

- **Dark Premium là mặc định.** Light Mode là tuỳ chọn.
- Màu chủ đạo: **Emerald Green**, **Deep Navy**, **Teal**.
- Con số tiền là nhân vật chính: lớn, rõ, căn chỉnh nhất quán.
- Không làm UI "nói" quá mức về độ chính xác: số dư luôn gắn nhãn **ước tính**.

## 2. Token màu [ĐỀ XUẤT]

| Vai trò | Dark | Light |
|---|---|---|
| Nền (background) | `#0B1220` (Deep Navy) | `#F6F8FA` |
| Bề mặt (surface) | `#111B2E` | `#FFFFFF` |
| Bề mặt nâng (surface variant) | `#18253D` | `#E9EEF3` |
| Primary (Emerald) | `#10B981` | `#059669` |
| Secondary (Teal) | `#14B8A6` | `#0D9488` |
| Chữ chính | `#E6EDF7` | `#0F1B2D` |
| Chữ phụ | `#9FB0C8` | `#4A5B73` |
| Thu nhập | Emerald | Emerald đậm |
| Chi tiêu | `#F87171` | `#DC2626` |
| Cảnh báo / cần xem lại | `#FBBF24` | `#B45309` |

Quy tắc:
- Tối thiểu độ tương phản WCAG AA cho chữ. Kiểm tra cả hai chế độ.
- **Không dùng màu làm tín hiệu duy nhất** cho thu/chi: kèm dấu `+`/`−` hoặc icon.
- Map về `ColorScheme` của Material 3 qua theme. Component chỉ dùng token theme.

## 3. Điều hướng

Bottom Navigation, 5 tab theo thứ tự:

`Home` · `Transactions` · `Reports` · `Accounts` · `Settings`

Chuyển tab giữ nguyên trạng thái từng tab (vị trí cuộn, bộ lọc). Màn chi tiết/thêm/sửa là màn con của tab, mở trên tab đó.

## 4. Danh mục màn hình

| Tab | Màn hình | Phase | Nội dung chính |
|---|---|---|---|
| Home | Tổng quan | 2 (cơ bản), 4 (biểu đồ) | Số dư ước tính, tổng thu/chi theo ngày/tuần/tháng, giao dịch gần đây, biểu đồ thu chi (Vico, Phase 4) |
| Transactions | Danh sách giao dịch | 2 | Danh sách theo ngày, tìm kiếm, bộ lọc (thời gian, loại, tài khoản, danh mục, trạng thái) |
| | Thêm/Sửa giao dịch | 2 | Cả 5 loại: thu, chi, chuyển tiền, hoàn tiền, điều chỉnh |
| | Chi tiết giao dịch | 2 | Hiển thị nguồn (thủ công/tự động), trạng thái |
| | Hàng chờ xem lại | 3 | Giao dịch `NEEDS_REVIEW` và `AUTO_RECORDED`: xác nhận, sửa, từ chối |
| Reports | Báo cáo | 4 | Thu/chi theo kỳ, phân bổ chi tiêu theo danh mục, so sánh tháng, thống kê thói quen tối thiểu |
| | Ngân sách | 4 | Theo tháng + danh mục, mức sử dụng, cảnh báo |
| | Nhắc định kỳ | 4 | Danh sách khoản định kỳ, nhắc thanh toán |
| Accounts | Danh sách tài khoản | 2 | Ngân hàng, ví, tiền mặt, số dư ước tính |
| | Thêm/Sửa tài khoản | 2 | Số dư ban đầu, liên kết nguồn thông báo |
| | Chuyển tiền nội bộ | 2 | Giữa hai tài khoản |
| Settings | Nguồn thông báo | 3 | Bật/tắt từng nguồn, trạng thái quyền, liên kết tài khoản |
| | Danh mục | 2 | Danh mục mặc định và tuỳ chỉnh |
| | Giao diện | 1 | Dark/Light |
| | Bảo mật | 5 | PIN, sinh trắc học |
| | Dữ liệu | 5 | Xuất CSV, sao lưu/khôi phục, xoá toàn bộ dữ liệu |
| (luồng) | Onboarding | 3 | Giải thích quyền đọc thông báo, hướng dẫn cấp quyền |
| (luồng) | Khoá ứng dụng | 5 | PIN / sinh trắc học |

*Mục tiêu tiết kiệm*: **[ĐỀ XUẤT]**, chưa thiết kế màn hình cho tới khi Khoa xác nhận (D-003).

## 5. Thành phần và hành vi cần chú ý

- **Hàng giao dịch:** icon danh mục, ghi chú, số tiền (có dấu + màu), tài khoản, giờ. Giao dịch tự động có nhãn nhỏ "Tự động". Giao dịch `NEEDS_REVIEW` có chip "Cần xem lại" (màu cảnh báo).
- **Định dạng tiền:** Việt Nam, ví dụ `1.250.000 ₫`. Dùng một hàm format duy nhất cho toàn app. Không format thủ công ở từng màn hình.
- **Nhãn "ước tính":** gắn cạnh mọi số dư. Giải thích ngắn khi chạm vào: số dư tính từ các giao dịch EasyCash đã ghi nhận, không phải số dư ngân hàng.
- **Hàng chờ xem lại:** thao tác nhanh một chạm (xác nhận / từ chối), vuốt hoặc nút. Có huy hiệu số lượng chờ trên Home (**[ĐỀ XUẤT]**).
- **Xoá giao dịch:** hỏi xác nhận; hoàn tác ngắn hạn bằng Snackbar **[ĐỀ XUẤT]**.
- **Biểu đồ:** dùng Vico. Có mô tả `contentDescription` tóm tắt cho TalkBack.
- **Ẩn số dư nhanh** (che số trên Home) **[ĐỀ XUẤT]**, hữu ích khi dùng nơi công cộng.

### Mọi màn hình có đủ 4 trạng thái

Đang tải · Rỗng (kèm gợi ý hành động) · Lỗi (kèm cách thử lại) · Có dữ liệu.

## 6. Luồng cấp quyền đọc thông báo (Onboarding)

Quan trọng nhất của app. Nói rõ, trung thực, không doạ, không ép.

1. Giải thích: EasyCash đọc thông báo **chỉ của các app bạn chọn**, xử lý **ngay trên máy**, không gửi đi đâu, **không bao giờ cần mật khẩu hay OTP**.
2. Cho chọn nguồn (MoMo, BIDV).
3. Dẫn tới màn cấp quyền truy cập thông báo của hệ thống.
4. **Với APK cài ngoài Store trên Android 13+ có thể bị chặn "cài đặt bị hạn chế"** (cần bật thủ công trong thông tin ứng dụng). Cần hướng dẫn bằng hình/bước rõ ràng. **[CẦN XÁC MINH]** hành vi chính xác và đường dẫn trên máy thật, đừng viết hướng dẫn theo trí nhớ.
5. Quay lại app, hiển thị trạng thái quyền (đã cấp / chưa cấp). Không cấp cũng dùng được ở chế độ nhập tay.
6. Màn Settings → Nguồn thông báo luôn hiển thị trạng thái quyền hiện tại và cách thu hồi.

## 7. Trợ năng và bản địa hoá

- Vùng chạm tối thiểu 48dp. Mọi icon tương tác có nhãn cho TalkBack.
- Hỗ trợ cỡ chữ lớn của hệ thống mà không vỡ bố cục.
- Ngôn ngữ v1: **tiếng Việt** **[ĐỀ XUẤT]**. Mọi chuỗi đặt trong tài nguyên chuỗi, không viết cứng trong code, để mở rộng sau.

## 8. Làm việc với backend

- Chỉ dùng interface trong `:domain` (xem `docs/agents/CONTRACTS.md`). Không import `:data`.
- Mỗi màn hình có `UiState` riêng, ViewModel biến dữ liệu từ `Flow` thành `StateFlow<UiState>`.
- Dùng **fake repository** để dựng và preview màn hình khi backend chưa xong. Fake không đưa vào bản release.
- Cần thêm dữ liệu/thao tác mà contract chưa có: đề xuất trong `CONTRACTS.md`, không tự suy diễn.
