# AGENTS.md — EasyCash

> File điều hành chung cho **Codex** (backend) và **Antigravity** (frontend).
> Đọc hết file này trước khi làm bất kỳ việc gì. Nếu công cụ của bạn ưu tiên file tên khác (vd `GEMINI.md`, `.agent/rules/`), file đó chỉ được phép trỏ về đây, không được chứa luật riêng.
>
> Phần "Nguyên tắc làm việc" dựa trên [andrej-karpathy-skills](https://github.com/multica-ai/andrej-karpathy-skills) (MIT), được Việt hoá và bổ sung luật cho làm việc nhiều agent.

---

## 0. Thứ tự đọc khi bắt đầu mỗi phiên

1. `AGENTS.md` (file này)
2. `docs/agents/TASKS.md` — ai đang làm gì
3. `docs/agents/CONTRACTS.md` — hợp đồng giữa backend và frontend
4. `docs/agents/HANDOFF.md` — 20 mục cuối, xem agent kia vừa để lại gì

Không bắt đầu code khi chưa đọc đủ 4 file trên.

---

## 1. Sản phẩm

**EasyCash – Personal Finance Manager**: app Android quản lý tài chính cá nhân.

- Theo dõi thu nhập, chi tiêu; nhiều tài khoản ngân hàng và ví điện tử trong một app.
- Quản lý mục tiêu tiết kiệm (tiết kiệm cho một mục đích cụ thể).
- **Tự nhận diện giao dịch** từ thông báo của app ngân hàng/ví mà người dùng chủ động cấp quyền, phân tích và lưu thẳng vào thiết bị.
- **Offline-first, Privacy-first.**

### Không làm trong v1 (non-goals)

- Đăng nhập / tài khoản người dùng.
- Đồng bộ Cloud, backend server, analytics gửi ra ngoài.
- Hỏi mật khẩu ngân hàng, OTP, liên kết API ngân hàng.
- iOS, đa nền tảng.

Muốn làm thêm bất cứ thứ gì trong danh sách trên → dừng lại, hỏi Khoa.

---

## 2. Luật bất biến (vi phạm = lỗi nghiêm trọng)

1. **Không khai báo quyền `INTERNET`** trong `AndroidManifest.xml` ở v1. Không thêm thư viện cần mạng (analytics, crash reporting gửi ra ngoài, ads).
2. **Không bao giờ đọc, lưu hay xử lý OTP/mật khẩu.** Thông báo có dấu hiệu OTP phải bị bỏ qua ngay ở bước lọc, trước khi parse.
3. **Chỉ xử lý thông báo từ danh sách app được whitelist** (`NotificationSourceRegistry`). App ngoài danh sách thì bỏ qua, không đọc nội dung.
4. **Dữ liệu tài chính chỉ nằm trong DB cục bộ.** Không ghi nội dung thông báo hay số tiền ra log ở bản release.
5. **Tiền tệ dùng `Long` (đơn vị đồng, không có phần thập phân). Cấm `Float/Double` cho tiền.**
6. Người dùng phải tắt/thu hồi được quyền đọc thông báo bất cứ lúc nào, app vẫn dùng bình thường ở chế độ nhập tay.

---

## 3. Nguyên tắc làm việc (Karpathy, áp dụng cho cả 2 agent)

Thiên về **cẩn thận hơn nhanh**. Với việc rất nhỏ (sửa typo, một dòng hiển nhiên) thì dùng phán đoán, không cần đủ nghi thức.

### 3.1 Nghĩ trước khi code
- Nêu rõ giả định. Không chắc thì hỏi, đừng đoán.
- Có nhiều cách hiểu thì liệt kê ra, đừng im lặng chọn một.
- Có cách đơn giản hơn thì nói. Phản biện khi cần.
- Khi rối, dừng lại, gọi tên chỗ chưa rõ, ghi vào `HANDOFF.md` rồi hỏi.

### 3.2 Đơn giản là trên hết
- Code tối thiểu đủ giải quyết vấn đề. Không thêm tính năng chưa được yêu cầu.
- Không trừu tượng hoá cho code chỉ dùng một lần.
- Không thêm "linh hoạt/cấu hình" chưa ai cần. Không xử lý lỗi cho tình huống không thể xảy ra.
- 200 dòng viết được 50 dòng thì viết lại.
- **Phép thử:** một senior engineer có bảo "làm phức tạp quá" không? Nếu có, đơn giản hoá.

### 3.3 Thay đổi phẫu thuật
- Chỉ đụng vào thứ bắt buộc phải đụng. Không "tiện tay" refactor, đổi format, sửa comment của code không liên quan.
- Theo đúng style code hiện có, kể cả khi bạn sẽ làm khác.
- Thấy code chết không liên quan: ghi lại, đừng xoá.
- Chỉ dọn thứ **do chính thay đổi của bạn** làm thừa (import, biến, hàm).
- **Phép thử:** mỗi dòng đổi phải truy được về đúng task đang làm.
- **Bổ sung cho nhiều agent:** không sửa file trong vùng sở hữu của agent kia (xem mục 4). Cần sửa thì đi qua mục 7.

### 3.4 Thực thi theo mục tiêu
Biến việc mơ hồ thành mục tiêu kiểm chứng được:

| Thay vì…            | Hãy…                                            |
| ------------------- | ----------------------------------------------- |
| "Thêm validation"   | Viết test cho input sai, rồi làm cho test pass  |
| "Sửa lỗi"           | Viết test tái hiện lỗi, rồi làm cho nó pass     |
| "Refactor X"        | Đảm bảo test pass trước và sau                  |

Việc nhiều bước thì ghi kế hoạch ngắn:

```
1. [Bước] → verify: [cách kiểm tra]
2. [Bước] → verify: [cách kiểm tra]
```

Tiêu chí mạnh cho phép tự lặp đến khi xong. "Làm cho chạy được" thì không.

---

## 4. Vai trò và ranh giới sở hữu

| | **Codex — Backend** | **Antigravity — Frontend** |
|---|---|---|
| Phụ trách | Logic, dữ liệu, xử lý thông báo | Giao diện, điều hướng, trải nghiệm |
| Sở hữu thư mục | `data/`, `domain/`, `di/`, `service/` | `ui/` |
| Test | Unit test, parser test, DB test | Compose preview, UI test, test ViewModel nếu có |
| Không được sửa | `ui/` | `data/`, `domain/`, `service/` |

**Vùng chung — `model/` và `docs/agents/`:** không ai tự ý sửa `model/`. Muốn đổi, đi theo quy trình đề xuất ở mục 7.2.

**ViewModel thuộc về frontend** (`ui/`), chỉ gọi `domain/` thông qua use case / repository interface. Frontend không import gì từ `data/`.

---

## 5. Kiến trúc và tech stack

> Đây là quyết định mặc định, vì tính năng nhận diện thông báo cần API gốc của Android (`NotificationListenerService`). Muốn đổi stack thì phải được Khoa duyệt và ghi vào `DECISIONS.md`.

- **Ngôn ngữ:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Kiến trúc:** MVVM + Repository, luồng dữ liệu một chiều (`StateFlow`)
- **Lưu trữ:** Room (SQLite) cục bộ
- **DI:** Hilt
- **Bất đồng bộ:** Kotlin Coroutines + Flow
- **Test:** JUnit, Turbine (Flow), Room in-memory
- **Một module `app` duy nhất trong v1.** Chia theo package, chưa tách Gradle module (giữ đơn giản).

```
app/src/main/java/com/easycash/
├── model/        # CHUNG – data class thuần, không phụ thuộc Android
├── domain/       # Codex – repository interface + use case
├── data/         # Codex – Room, repository impl, parser
│   ├── db/
│   ├── repository/
│   └── notification/   # NotificationParser, rule theo từng nguồn
├── service/      # Codex – NotificationListenerService, kiểm tra quyền
├── di/           # Codex – Hilt module
└── ui/           # Antigravity – theme, navigation, màn hình, ViewModel
    ├── theme/
    ├── navigation/
    ├── home/
    ├── transactions/
    ├── accounts/
    ├── goals/
    ├── stats/
    ├── settings/
    └── onboarding/     # hướng dẫn cấp quyền đọc thông báo
```

### Luồng nhận diện giao dịch (backend)

```
Notification đến
  → NotificationListenerService
  → lọc: có nằm trong whitelist không? có dấu hiệu OTP không? → bỏ qua nếu vi phạm
  → NotificationParser (rule theo từng nguồn) → ParsedTransaction
  → chống trùng (hash của nguồn + nội dung + thời điểm)
  → ghép tài khoản, gán danh mục mặc định
  → lưu Room với source = NOTIFICATION
  → UI nhận qua Flow, người dùng có thể sửa hoặc xoá
```

Danh sách nguồn (package name của từng app ngân hàng/ví) là **cấu hình**, không hard-code rải rác. Package name phải được kiểm chứng trên máy thật trước khi đưa vào danh sách, không đoán.

---

## 6. Hợp đồng giữa hai agent

**Quy tắc cốt lõi: contract-first.** Backend chốt interface và model trước, frontend làm trên interface đó.

1. Codex định nghĩa/cập nhật `model/` và interface trong `domain/` trước, ghi vào `CONTRACTS.md`.
2. Antigravity code UI dựa trên interface đó, dùng **Fake repository** (`FakeTransactionRepository`…) đặt trong `ui/` hoặc `test/` để không phải chờ backend.
3. Khi Codex xong bản thật, đổi Hilt binding từ Fake sang bản thật. Frontend không cần sửa gì nếu contract giữ nguyên.

### Khung model ban đầu (Codex hoàn thiện ở Phase 0)

```kotlin
// model/ — thuần Kotlin, tiền là Long (đồng)
enum class TransactionType { INCOME, EXPENSE }
enum class TransactionSource { MANUAL, NOTIFICATION }
enum class AccountType { CASH, BANK, EWALLET }

data class Account(val id: Long, val name: String, val type: AccountType, val balance: Long)
data class Category(val id: Long, val name: String, val type: TransactionType)
data class Transaction(
    val id: Long, val accountId: Long, val categoryId: Long?,
    val type: TransactionType, val amount: Long, val note: String,
    val occurredAtMillis: Long, val source: TransactionSource,
)
data class SavingsGoal(
    val id: Long, val name: String, val targetAmount: Long,
    val savedAmount: Long, val deadlineMillis: Long?,
)
```

```kotlin
// domain/ — chỉ trả Flow cho dữ liệu quan sát được, suspend cho thao tác ghi
interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    suspend fun add(t: Transaction): Long
    suspend fun update(t: Transaction)
    suspend fun delete(id: Long)
}
```

Nếu `CONTRACTS.md` và code thật lệch nhau, **`CONTRACTS.md` là chuẩn**, trừ khi hai bên đã thống nhất đổi (mục 7.2).

---

## 7. Cách hai agent "nói chuyện" với nhau

Hai agent không gọi trực tiếp nhau. Giao tiếp qua **file trong repo**, đây là kênh duy nhất. Khoa là người chuyển lượt khi cần.

```
docs/agents/
├── TASKS.md       # bảng việc: ai nhận, trạng thái
├── CONTRACTS.md   # hợp đồng + đề xuất thay đổi
├── HANDOFF.md     # nhật ký bàn giao, chỉ ghi thêm (append-only)
└── DECISIONS.md   # quyết định kiến trúc đã chốt
```

### 7.1 Nhận việc
- Trước khi làm, mở `TASKS.md`, đổi task sang `IN_PROGRESS` kèm tên mình. Không nhận task đang có chủ.
- Xong thì đổi sang `REVIEW` và ghi một mục vào `HANDOFF.md`.

### 7.2 Muốn đổi contract (`model/` hoặc interface `domain/`)
1. Ghi mục mới vào phần **Proposals** trong `CONTRACTS.md`: đổi gì, vì sao, ảnh hưởng ai.
2. **Dừng** phần phụ thuộc vào thay đổi đó. Làm việc khác trong lúc chờ.
3. Agent kia ghi `ACK` hoặc `OBJECT` kèm lý do. Có `OBJECT` mà chưa thống nhất → hỏi Khoa.
4. Chỉ khi có `ACK` mới sửa `model/` / interface, rồi chuyển đề xuất sang mục **Active**.

Đề xuất thêm thì rẻ, đổi/xoá field thì đắt. Ưu tiên thêm hơn đổi.

### 7.3 Bàn giao (`HANDOFF.md`)
Mỗi mục: `ngày giờ · agent · việc đã xong · việc agent kia cần làm tiếp · điều chưa chắc chắn`. Chỉ thêm, không sửa mục cũ. Viết cho người chưa biết gì về phiên của bạn.

### 7.4 Bị chặn
Ghi `BLOCKED` vào task + một mục trong `HANDOFF.md` nêu rõ đang chờ gì từ ai. Rồi chuyển sang task khác. **Không đoán** và không tự làm phần của agent kia để "lách".

---

## 8. Git

- Không commit thẳng vào `main`. Khoa là người merge.
- Nhánh: `codex/<task>` và `antigravity/<task>`.
- Một task = một nhánh = một PR nhỏ. Commit nhỏ, message rõ: `feat(parser): …`, `feat(ui): …`, `fix(...)`.
- Không bao giờ force-push nhánh của agent kia.
- Bắt đầu task mới từ `main` mới nhất. Xung đột ở vùng chung thì dừng và báo qua `HANDOFF.md`.

---

## 9. Lộ trình

Làm tuần tự, mỗi phase phải qua Definition of Done trước khi sang phase sau.

| Phase | Mục tiêu | Codex | Antigravity |
|---|---|---|---|
| **0. Nền móng** | Project chạy được, contract chốt | Khởi tạo project, Room, Hilt, `model/`, interface | Theme, navigation, khung màn hình với dữ liệu giả |
| **1. Thu/chi thủ công** | Nhập tay hoàn chỉnh | Repository, use case, DB, test | Màn hình thêm/sửa/xoá giao dịch, danh sách, tài khoản |
| **2. Tự nhận diện** | Giao dịch đến từ thông báo | Listener, whitelist, parser + test fixture, chống trùng | Onboarding cấp quyền, trạng thái quyền, đánh dấu giao dịch tự động |
| **3. Tiết kiệm** | Mục tiêu tiết kiệm | Logic mục tiêu, tính tiến độ | Màn hình mục tiêu, tiến độ |
| **4. Thống kê** | Báo cáo theo thời gian/danh mục | Truy vấn tổng hợp | Biểu đồ, bộ lọc |
| **5. Dữ liệu cục bộ** | Xuất/nhập sao lưu | Export/import file cục bộ | Màn hình sao lưu, cài đặt |

---

## 10. Definition of Done

Một task chỉ được coi là xong khi:

- [ ] Đáp ứng đúng tiêu chí đã ghi trong task (mục 3.4), không hơn.
- [ ] `./gradlew assembleDebug` chạy được.
- [ ] `./gradlew testDebugUnitTest` pass. Logic mới có test đi kèm; parser có fixture cho từng nguồn.
- [ ] Không vi phạm luật bất biến ở mục 2 (đặc biệt: không có `INTERNET` trong manifest).
- [ ] Không có thay đổi ngoài phạm vi task. Mỗi dòng đổi truy được về task.
- [ ] `TASKS.md` và `HANDOFF.md` đã cập nhật.

Báo cáo trung thực: **không** nói "xong" nếu chưa chạy được các lệnh trên. Nói rõ phần nào đã kiểm chứng, phần nào chưa.

---

## 11. Rủi ro đã biết (xử lý khi tới phase liên quan)

- **Định dạng thông báo mỗi nguồn khác nhau và có thể đổi** theo bản cập nhật app. Parser phải có test fixture và thất bại một cách an toàn (không parse được thì bỏ qua, không lưu số sai).
- **Android có thể ngắt listener** khi tối ưu pin trên một số hãng máy. Cần xử lý kết nối lại và hướng dẫn người dùng (frontend: màn hình trạng thái).
- **Trùng giao dịch** khi một giao dịch gây ra nhiều thông báo (vd cả app ngân hàng và ví). Cần chiến lược chống trùng và test.

---

## Phụ lục: mẫu file khởi tạo

### `docs/agents/TASKS.md`
```markdown
# Tasks

| ID | Task | Owner | Status | Ghi chú |
|----|------|-------|--------|---------|
| T-001 | Khởi tạo project Android (Kotlin, Compose, Room, Hilt) | codex | TODO | |
| T-002 | Chốt model/ và interface domain/ ban đầu | codex | TODO | Chặn T-004 |
| T-003 | Theme + navigation + khung màn hình | antigravity | TODO | |
| T-004 | Màn hình danh sách giao dịch (dùng Fake repo) | antigravity | TODO | Chờ T-002 |

Status: TODO · IN_PROGRESS · REVIEW · DONE · BLOCKED
```

### `docs/agents/CONTRACTS.md`
```markdown
# Contracts

## Active
(chép model/ và interface đã chốt vào đây, kèm ngày)

## Proposals
### P-001 · <tiêu đề> · <agent> · <ngày>
- Đổi gì:
- Vì sao:
- Ảnh hưởng:
- Phản hồi: (ACK / OBJECT — agent kia điền)
```

### `docs/agents/HANDOFF.md`
```markdown
# Handoff (chỉ ghi thêm)

### 2026-MM-DD HH:MM · codex
- Đã xong:
- Agent kia cần làm tiếp:
- Chưa chắc chắn:
```

### `docs/agents/DECISIONS.md`
```markdown
# Decisions

### D-001 · Native Kotlin + Compose
- Lý do: cần NotificationListenerService (API gốc Android)
- Ngày / người duyệt:
```
