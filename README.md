<p align="center">
  <img src="assets/branding/easycash-logo-v1.png" alt="Logo EasyCash" width="112" />
</p>

<h1 align="center">EasyCash</h1>

<p align="center">Quản lý thu chi cá nhân trên Android · Offline · Dữ liệu trên thiết bị</p>

<p align="center">
  <a href="https://github.com/huukhoa123/EasyCash/actions/workflows/android.yml"><img src="https://github.com/huukhoa123/EasyCash/actions/workflows/android.yml/badge.svg" alt="Android CI" /></a>
  <a href="https://github.com/huukhoa123/EasyCash/releases"><img src="https://img.shields.io/badge/trạng_thái-Pre--release-08664F" alt="Pre-release" /></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-08664F" alt="Android 8.0 trở lên" />
</p>

EasyCash giúp theo dõi nhiều tài khoản, ghi thu/chi, chuyển tiền nội bộ, xem báo cáo và quản lý ngân sách. App có thể nhận diện giao dịch từ thông báo BIDV khi người dùng chủ động cấp quyền và bật nguồn tương ứng.

**Số dư trong EasyCash là số dư ước tính**, tính từ số dư ban đầu và giao dịch đã ghi nhận. App không truy vấn số dư trực tiếp từ ngân hàng, không yêu cầu tài khoản đăng nhập ngân hàng hay OTP.

## Tải và cài đặt

Bản có logo ứng dụng: [**v1.0.0-rc.4**](https://github.com/huukhoa123/EasyCash/releases/tag/v1.0.0-rc.4). Xem các bản cập nhật tại [Releases](https://github.com/huukhoa123/EasyCash/releases). Hiện dự án ở giai đoạn **thử nghiệm**, chưa chốt v1 ổn định.

| Trường hợp | APK cần tải |
|---|---|
| Đang dùng bản thử/debug hoặc backup-bridge | [EasyCash-backup-bridge-1.0.0-rc.4.apk](https://github.com/huukhoa123/EasyCash/releases/download/v1.0.0-rc.4/EasyCash-backup-bridge-1.0.0-rc.4.apk) |
| Cài mới hoặc đang dùng release ký riêng | [EasyCash-1.0.0-rc.4.apk](https://github.com/huukhoa123/EasyCash/releases/download/v1.0.0-rc.4/EasyCash-1.0.0-rc.4.apk) |

**Cập nhật bằng cùng dòng APK, không gỡ app để cập nhật.** Bridge và release ký bằng hai khóa khác nhau nên không cài đè chéo được. Muốn đổi dòng APK, cần xuất backup mã hóa và kiểm tra khôi phục trước; xem [hướng dẫn chuyển dữ liệu](easycash-docs/docs/RELEASE_1.0.0_RC1.md). Không dùng APK debug từ CI để thay bản thử hiện có vì chứng chỉ ký có thể khác.

Mỗi release có `SHA256SUMS.txt` và `SIGNING-CERTIFICATES.txt` để đối chiếu file tải và chứng chỉ ký.

## Chức năng hiện có

- Quản lý tài khoản, số dư ban đầu và số dư ước tính.
- Thêm/sửa/xóa thu, chi, chuyển nội bộ; tìm kiếm và lọc giao dịch.
- Danh mục, báo cáo thu chi, ngân sách theo danh mục và cảnh báo ngân sách.
- Khoản nhắc định kỳ theo tuần/tháng/năm; nhắc không tự thanh toán hay tạo giao dịch.
- Nhận diện thông báo BIDV, hàng chờ xem lại và chống ghi trùng.
- Backup mã hóa bằng mật khẩu, khôi phục dữ liệu, xuất CSV.
- Giao diện sáng/tối và biểu tượng EasyCash riêng.

### Trạng thái đang hoàn thiện

Chủ dự án đã xác nhận đọc được thông báo BIDV trên điện thoại. Chưa nghiệm thu đầy đủ mọi loại thông báo, thiết bị và tình huống giao dịch.

Trong **rc.4**, giao dịch nhận diện từ thông báo vẫn vào **Chờ xem lại**. Số dư tính các trạng thái `AUTO_RECORDED` và `USER_CONFIRMED`; không tính `NEEDS_REVIEW` hoặc `REJECTED`.

Các yêu cầu tiếp theo đang được theo dõi trong [TASKS.md](easycash-docs/docs/agents/TASKS.md):

- Thiết kế lại giao diện theo hướng sáng, tinh gọn và cao cấp.
- Tự ghi nhận thông báo đủ rõ và học danh mục từ giao dịch đã được người dùng phân loại; chưa có trong rc.4.
- Parser MoMo chưa được hỗ trợ; chờ mẫu thông báo thật đã che dữ liệu để triển khai và kiểm thử.

## Thiết lập nhận thông báo BIDV

1. Tạo một tài khoản trong tab **Tài khoản**, nhập số dư ban đầu phù hợp.
2. Trong **Cài đặt**, cấp **quyền đọc thông báo** cho EasyCash qua Android.
3. Bật nguồn **BIDV SmartBanking**, chọn **Đổi tài khoản đích** và gán tài khoản vừa tạo. Chỉ bật công tắc mà chưa gán tài khoản thì chưa ghi được giao dịch.
4. Xem giao dịch nhận diện trong **Giao dịch → Chờ xem lại**, kiểm tra danh mục/thu chi trước khi xác nhận ở bản hiện tại.

Quyền **gửi thông báo** là quyền riêng, dùng cho cảnh báo/ngày nhắc và thông báo chẩn đoán. Android có thể trì hoãn lịch chạy nền. App không đọc được lịch sử thông báo đã xóa; khi listener kết nối lại, app thử xử lý thông báo BIDV còn hiện, vẫn tuân theo nguồn đã bật và chống trùng.

Nếu chưa nhận giao dịch, xem [hướng dẫn chẩn đoán từ rc.3](easycash-docs/docs/RELEASE_1.0.0_RC3.md). Có thể thử khi phát sinh giao dịch tự nhiên; không cần chuyển tiền chỉ để kiểm tra.

## Quyền riêng tư

- App không khai báo quyền `INTERNET`; dữ liệu không được gửi lên máy chủ.
- Chỉ xử lý nguồn nằm trong whitelist và được người dùng bật.
- Loại thông báo có dấu hiệu OTP/mật khẩu trước khi lưu; không ghi nội dung ngân hàng vào Logcat.
- Nhật ký sự kiện chỉ giữ metadata/hash và tự dọn sau 30 ngày; khóa chống trùng của giao dịch được giữ riêng.
- Backup dùng AES-GCM với mật khẩu; cần giữ file và mật khẩu để khôi phục. CSV là bản xuất đọc được, không thay thế backup mã hóa.
- Không có đăng nhập/cloud sync hoặc khóa ứng dụng PIN/sinh trắc trong phạm vi dùng cá nhân hiện tại.

## Build từ mã nguồn

Yêu cầu: JDK 21 (như CI), Android SDK có platform **37**, platform-tools và chấp nhận SDK licenses. Gradle Wrapper đi kèm repository; JVM target của mã ứng dụng là 17. Có thể mở dự án bằng Android Studio tương thích cấu hình Gradle/AGP trong repo.

```bash
git clone https://github.com/huukhoa123/EasyCash.git
cd EasyCash
```

Trỏ SDK qua `local.properties` với `sdk.dir=...` hoặc môi trường Android SDK của máy. Không commit file cấu hình cục bộ.

Trên Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug assembleRelease testDebugUnitTest :domain:test
```

Trên macOS/Linux:

```bash
./gradlew assembleDebug assembleRelease testDebugUnitTest :domain:test
```

APK debug nằm trong `app/build/outputs/apk/debug/`. Release chỉ được ký khi có cấu hình riêng ở `.signing/release.properties`; không có cấu hình này sẽ build unsigned. Khóa ký và mật khẩu không nằm trong Git. Không cài APK unsigned hoặc dùng khóa mới để cập nhật bản đang có dữ liệu.

## Cấu trúc dự án

| Module | Trách nhiệm |
|---|---|
| `app` | Application, Activity, Hilt wiring, cấu hình đóng gói/ký |
| `ui` | Jetpack Compose, theme, điều hướng và ViewModel; phụ thuộc `domain` |
| `domain` | Model và repository interface Kotlin thuần |
| `data` | Room, parser, NotificationListenerService, backup và WorkManager |
| `easycash-docs` | Đặc tả, quyết định, hợp đồng và bàn giao |

Tiền dùng `Long` theo đơn vị VND. Database Room có migration; không dùng số thực để tính tiền. CI chạy build debug/release, unit test và kiểm tra quyền mạng/ranh giới module; APK trên CI phục vụ kiểm tra, APK có ký phát hành nằm ở Releases.

## Tài liệu và phản hồi

- [Đặc tả sản phẩm](easycash-docs/docs/PROJECT_SPECIFICATION.md)
- [Database](easycash-docs/docs/DATABASE.md)
- [Quy tắc nhận diện thông báo](easycash-docs/docs/NOTIFICATION_PARSING.md) — đọc cùng handoff để biết tiến độ triển khai mới nhất.
- [Thiết kế UI sáng mới](easycash-docs/docs/UI_REFRESH_LIGHT.md)
- [Checklist phát hành và chuyển dữ liệu](easycash-docs/docs/RELEASE_CHECKLIST.md)
- [Quy tắc làm việc](easycash-docs/AGENTS.md), [contract](easycash-docs/docs/agents/CONTRACTS.md) và [handoff](easycash-docs/docs/agents/HANDOFF.md)

Báo lỗi qua [GitHub Issues](https://github.com/huukhoa123/EasyCash/issues), kèm phiên bản EasyCash, phiên bản Android, các bước và kết quả mong đợi/thực tế. Che tên, số tài khoản, nội dung chuyển khoản, mã tham chiếu và số dư trên ảnh; không gửi OTP, mật khẩu, khóa ký hoặc file backup cá nhân.
