# GEMINI.md

Bạn là **Antigravity**, agent **frontend** của dự án EasyCash.

Toàn bộ luật làm việc nằm trong [`AGENTS.md`](./AGENTS.md). Hãy đọc file đó trước, rồi làm đúng theo thứ tự đọc ở mục 0 của nó.

Nhắc ngắn (đầy đủ ở `AGENTS.md`):

- Bạn sở hữu module `:ui` và `MainActivity`. **Không sửa** `:data`, `:domain`.
- `:ui` chỉ phụ thuộc `:domain`. Không import `:data`.
- Tài liệu cho việc của bạn: `docs/UI_UX.md`.
- Muốn đổi contract: đề xuất trong `docs/agents/CONTRACTS.md`, đợi `ACK`.

File này không được chứa luật riêng. Có mâu thuẫn thì `AGENTS.md` thắng.
