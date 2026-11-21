# AGENTS.md — EasyCash

> File điều hành chung cho **Codex** (backend) và **Antigravity** (frontend). File này **luôn được nạp**, nên giữ ngắn.
> Chi tiết nằm trong `docs/`. Chỉ mở đúng tài liệu bạn cần cho task hiện tại, đừng đọc hết mọi thứ mỗi phiên.
> Nếu công cụ của bạn ưu tiên file khác tên (`GEMINI.md`, rule riêng…), file đó chỉ được trỏ về đây, không chứa luật riêng.
> Phần "Nguyên tắc làm việc" dựa trên [andrej-karpathy-skills](https://github.com/multica-ai/andrej-karpathy-skills) (MIT), Việt hoá và bổ sung luật cho làm việc nhiều agent.

---

## 0. Bắt đầu mỗi phiên

Đọc theo thứ tự:

1. `AGENTS.md` (file này)
2. `docs/agents/TASKS.md` — ai đang làm gì
3. `docs/agents/CONTRACTS.md` — hợp đồng backend ↔ frontend
4. `docs/agents/HANDOFF.md` — 10 mục cuối

Sau đó chỉ mở tài liệu theo task:

| Tài liệu | Mở khi |
|---|---|
| `docs/PROJECT_SPECIFICATION.md` | Cần biết yêu cầu, phạm vi, tiêu chí hoàn thành. **Nguồn tham chiếu chính thức.** |
| `docs/DATABASE.md` | Đụng tới Room, entity, DAO, migration (Codex) |
| `docs/NOTIFICATION_PARSING.md` | Đụng tới listener, parser, chống trùng (Codex) |
| `docs/UI_UX.md` | Đụng tới giao diện, theme, màn hình (Antigravity) |
| `docs/agents/DECISIONS.md` | Trước khi đề xuất đổi kiến trúc, để biết đã chốt gì |

Nếu hai tài liệu mâu thuẫn nhau: dừng lại, ghi vào `HANDOFF.md`, hỏi Khoa. Không tự chọn.

---

## 1. Sản phẩm (tóm tắt)

**EasyCash – Personal Finance Manager**: app Android quản lý tài chính cá nhân. Theo dõi thu/chi, nhiều tài khoản ngân hàng và ví điện tử, và **tự nhận diện giao dịch** từ thông báo của app ngân hàng/ví mà người dùng chủ động cấp quyền. **Offline-first, Privacy-first.** Nguồn thông báo v1: MoMo, BIDV.

**Không làm trong v1:** đăng nhập, đồng bộ Cloud, backend server, hỏi mật khẩu/OTP ngân hàng, API nội bộ ngân hàng, iOS.

---

## 2. Luật bất biến (vi phạm = lỗi nghiêm trọng)

1. **Không khai báo quyền `INTERNET`.** Không thêm thư viện cần mạng (analytics, crash report gửi ra ngoài, ads).
2. **Không đọc, lưu, hay ghi log OTP/mật khẩu.** Thông báo OTP bị loại ngay ở bước lọc, trước khi parse, và **không lưu gì cả**.
3. **Chỉ xử lý nguồn thông báo nằm trong whitelist và đang được người dùng bật.**
4. **Không ghi nội dung thông báo, số tiền, số tài khoản ra Logcat** (kể cả debug build chia sẻ ra ngoài).
5. **Tiền là `Long` (đồng VND). Cấm `Float`/`Double` cho tính tiền.**
6. **Giao dịch từ thông báo không phải dữ liệu đã xác minh từ ngân hàng.** Số dư là **ước tính**. UI và tài liệu phải nói đúng như vậy.
7. Người dùng thu hồi quyền đọc thông báo bất kỳ lúc nào thì app vẫn dùng bình thường ở chế độ nhập tay.

---

## 3. Nguyên tắc làm việc (Karpathy)

Thiên về **cẩn thận hơn nhanh**. Việc rất nhỏ (typo, một dòng hiển nhiên) thì dùng phán đoán.

**3.1 Nghĩ trước khi code.** Nêu giả định. Không chắc thì hỏi. Nhiều cách hiểu thì liệt kê, đừng im lặng chọn một. Có cách đơn giản hơn thì nói. Khi rối, dừng, ghi vào `HANDOFF.md` rồi hỏi.

**3.2 Đơn giản là trên hết.** Code tối thiểu đủ giải quyết vấn đề. Không thêm tính năng chưa được yêu cầu, không trừu tượng hoá cho code dùng một lần, không "linh hoạt" chưa ai cần, không xử lý lỗi cho tình huống không thể xảy ra. 200 dòng viết được 50 thì viết lại. *Phép thử:* senior engineer có bảo "phức tạp quá" không?

**3.3 Thay đổi phẫu thuật.** Chỉ đụng thứ bắt buộc. Không tiện tay refactor/đổi format/sửa comment không liên quan. Theo style hiện có. Thấy code chết không liên quan thì ghi lại, đừng xoá. Chỉ dọn thứ **do chính bạn** làm thừa. *Phép thử:* mỗi dòng đổi truy được về task. **Bổ sung cho nhiều agent:** không sửa vùng của agent kia.

**3.4 Thực thi theo mục tiêu.** Biến việc mơ hồ thành mục tiêu kiểm chứng được:

| Thay vì… | Hãy… |
|---|---|
| "Thêm validation" | Viết test cho input sai, rồi làm cho pass |
| "Sửa lỗi" | Viết test tái hiện lỗi, rồi làm cho pass |
| "Refactor X" | Đảm bảo test pass trước và sau |

Việc nhiều bước thì ghi kế hoạch: `1. [bước] → verify: [cách kiểm tra]`.

**3.5 Không bịa (bổ sung cho dự án này).** Những thứ chỉ kiểm chứng được trên máy thật hoặc tài liệu chính thức (định dạng thông báo MoMo/BIDV, package name, hành vi hệ thống Android theo hãng máy) **không được đoán**. Hai cờ dùng trong mọi tài liệu:

- **[CẦN XÁC MINH]**: chưa chắc, phải kiểm chứng trước khi dựa vào.
- **[ĐỀ XUẤT]**: gợi ý, chưa phải yêu cầu. Không triển khai khi Khoa chưa duyệt.

Gặp cờ đó thì làm phần không phụ thuộc, ghi vào `HANDOFF.md` phần cần Khoa kiểm chứng.

---

## 4. Module và ranh giới sở hữu

Bốn Gradle module, để **trình biên dịch ép ranh giới** thay vì chỉ dựa vào quy ước (xem `DECISIONS.md` D-002):

```
:domain   Kotlin thuần (không Android). Model, repository interface, use case.
:data     Room, repository impl, parser, NotificationListenerService, Keystore, WorkManager.
:ui       Compose, theme, navigation, ViewModel. CHỈ phụ thuộc :domain.
:app      Application, MainActivity, Hilt wiring. Phụ thuộc cả ba.
```

Quy tắc phụ thuộc: `:ui → :domain`, `:data → :domain`, `:app → :ui, :data, :domain`. **`:ui` không bao giờ import `:data`.**

| | **Codex — Backend** | **Antigravity — Frontend** |
|---|---|---|
| Sở hữu | `:data`, `:domain`, `:app/di` | `:ui`, `:app/MainActivity` |
| Test | Unit, parser fixture, Room in-memory, migration | Compose preview, Compose UI Test, test ViewModel |
| Không được sửa | `:ui` | `:data`, `:domain` |

`:domain` do Codex viết. Antigravity **đọc, không sửa**. Muốn đổi → đề xuất theo mục 5.2.

**Fake để frontend không phải chờ:** Antigravity tự viết fake của repository interface (trong `src/debug` hoặc test của `:ui`, không đưa vào bản release) để dựng và preview màn hình trước khi backend xong.

---

## 5. Hai agent giao tiếp bằng cách nào

Hai agent không gọi trực tiếp nhau. Kênh duy nhất là **file trong `docs/agents/`**. Khoa chuyển lượt khi cần.

**5.1 Nhận việc.** Mở `TASKS.md`, đổi task sang `IN_PROGRESS` kèm tên mình. Không nhận task đang có chủ. Xong thì đổi `REVIEW` và thêm một mục `HANDOFF.md`.

**5.2 Đổi contract** (`:domain` hoặc `CONTRACTS.md`):
1. Ghi đề xuất vào mục **Proposals** của `CONTRACTS.md`: đổi gì, vì sao, ảnh hưởng ai.
2. **Dừng** phần phụ thuộc vào thay đổi, làm việc khác trong lúc chờ.
3. Agent kia ghi `ACK` hoặc `OBJECT` kèm lý do. Có `OBJECT` mà chưa thống nhất thì hỏi Khoa.
4. Có `ACK` mới sửa code, rồi chuyển đề xuất sang **Active**.

Thêm thì rẻ, đổi/xoá thì đắt. Ưu tiên thêm.

**5.3 Bàn giao (`HANDOFF.md`).** Chỉ thêm, không sửa mục cũ. Mỗi mục: `ngày giờ · agent · đã xong · agent kia cần làm gì · điều chưa chắc`. Viết cho người chưa biết gì về phiên của bạn.

**5.4 Bị chặn.** Ghi `BLOCKED` vào task, nêu rõ chờ gì từ ai, chuyển sang task khác. Không đoán, không tự làm phần của agent kia để lách.

---

## 6. Git

- Theo yêu cầu Khoa ngày 2026-11-21, chỉ dùng nhánh `main`; commit và push trực tiếp lên nhánh này.
- Không tạo nhánh phụ hoặc PR trừ khi Khoa yêu cầu. GitHub Actions chạy kiểm tra mỗi lần push `main`.
- Commit nhỏ, message rõ: `feat(parser): …`, `feat(ui): …`, `fix(...)`.
- Không force-push nhánh của agent kia. Xung đột ở vùng chung thì dừng và báo qua `HANDOFF.md`.
- **Không commit keystore, mật khẩu ký, hay mẫu thông báo thật chưa che dữ liệu.**

---

## 7. Lộ trình (chi tiết trong spec, mục 15)

| Phase | Tên |
|---|---|
| 1 | Foundation & Architecture |
| 2 | Core Finance Management |
| 3 | Automatic Transaction Detection |
| 4 | Financial Intelligence |
| 5 | Security & Data Management |
| 6 | Testing & APK Release |

Làm tuần tự. Phase chỉ kết thúc khi đạt tiêu chí hoàn thành của phase đó trong spec.

---

## 8. Definition of Done (mọi task)

- [ ] Đáp ứng đúng tiêu chí ghi trong task, không hơn.
- [ ] `./gradlew assembleDebug` chạy được.
- [ ] `./gradlew testDebugUnitTest` pass; logic mới có test; parser mới có fixture.
- [ ] Không vi phạm mục 2 (đặc biệt: manifest không có `INTERNET`, không log dữ liệu nhạy cảm).
- [ ] Không có thay đổi ngoài phạm vi task.
- [ ] `TASKS.md` và `HANDOFF.md` đã cập nhật.

**Báo cáo trung thực:** không nói "xong" nếu chưa chạy các lệnh trên. Nói rõ phần nào đã kiểm chứng, phần nào chưa, phần nào cần thử trên máy thật.
