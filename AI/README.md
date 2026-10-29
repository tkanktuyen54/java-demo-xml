# Trợ lý khóa học — Python + PyTorch

Angular mở sidebar bên phải → Spring Boot giữ phiên, CSRF và dữ liệu website → Python tư vấn → SSE trả kết quả và thẻ khóa học.

## Chạy local

Dùng Python 3.11 cho môi trường mới. Từ `AI`:

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements-dev.txt
python -m advisor.catalog data/courses.example.csv
uvicorn advisor.app:app --host 127.0.0.1 --port 8000
```

Trên Windows, kích hoạt bằng `.venv\Scripts\activate`. Intel macOS dùng wheel PyTorch 2.2.2 vì các bản mới không còn phát hành wheel cho nền tảng này; Linux/Apple Silicon dùng bản 2.6.0 được pin trong requirements. Máy Intel hiện tại đã được kiểm tra với Python 3.9.6; môi trường mới nên dùng Python 3.11.

Chạy BE bằng JDK 21 như README gốc; mặc định gọi AI tại `http://127.0.0.1:8000`. Có thể đặt `AI_SERVICE_URL` và cùng một `AI_SERVICE_TOKEN` cho hai tiến trình. Sau đó chạy FE bằng Node 24.15+ và `npm start`. Mở icon **Tư vấn khóa học** ở góc phải.

API nội bộ `POST /v1/advise` chỉ dành cho BE. Không publish cổng Python ra Internet. Nếu đặt `AI_SERVICE_TOKEN`, BE gửi header `X-Advisor-Token`. API `/health` cho biết đã nạp checkpoint và bao nhiêu khóa từ file.

## Nhập Excel thật

```bash
python -m advisor.catalog /duong-dan/khoa-hoc.xlsx --output data/catalog.json
# Chọn sheet cụ thể nếu cần:
python -m advisor.catalog /duong-dan/khoa-hoc.xlsx --sheet Courses
```

Hàng đầu là tên cột. Bắt buộc `slug`, `title`, `price`; cũng chấp nhận **Mã khóa học**, **Tên khóa học**, **Giá**. `slug` là mã duy nhất dạng `python-co-ban`, cùng mã với website nếu khóa đã được bán trên website.

Các cột bổ sung: `level` / Trình độ, `category` / Danh mục, `description` / Mô tả, `instructor` / Giảng viên, `hours` / Thời lượng, `outcomes` / Kết quả đạt được, `audience` / Đối tượng, `prerequisites` / Yêu cầu đầu vào. Tách các đầu ra bằng dấu `|`. File CSV UTF-8 mẫu nằm trong `data/courses.example.csv`; có thể mở bằng Excel và lưu lại thành `.xlsx`.

Giá là số nguyên VND, chấp nhận `499000`, `499.000 ₫`, `499,000 VND`. Không nhận số âm, số lẻ, mã trùng hoặc công thức Excel. Một dòng sai khiến toàn bộ lần nhập bị từ chối; file JSON cũ được giữ nguyên. Giới hạn 5.000 khóa/lần. Khởi động lại dịch vụ AI sau khi nhập để nạp danh mục mới.

Quy tắc dữ liệu:

- Khóa đã có trên website: tên, giá, trình độ, nội dung từ MySQL được ưu tiên. Excel bổ sung đối tượng và yêu cầu đầu vào. Cập nhật giá trên website không cần train lại.
- Khóa chỉ có trong Excel: bot có thể giới thiệu thông tin và giá trong file; FE ghi rõ chưa mở đăng ký và không tạo đường dẫn học/mua.
- Nhập file là cập nhật **kho kiến thức tư vấn**, không tự tạo khóa, bài giảng hay đơn hàng trong MySQL. Muốn mở bán một khóa mới phải có dữ liệu khóa học/bài giảng trên website.

## Mô hình tự xây và huấn luyện

`advisor/model.py` chứa **TinyGPT decoder-only Transformer**, tự xây từ các lớp PyTorch: byte tokenizer UTF-8, token/position embeddings, causal multi-head attention, MLP, LayerNorm và language-model head. Khởi tạo trọng số ngẫu nhiên, không tải mô hình hay tokenizer đã huấn luyện.

Có ba chế độ:

1. `text`: pretrain dự đoán byte tiếp theo từ văn bản tiếng Việt; mỗi tài liệu được ngăn bằng một dòng trống.
2. `chat`: huấn luyện tiếp từ hội thoại JSONL có `messages` gồm `role` và `content`; chỉ tính loss trên phần trả lời của assistant.
3. `intents`: học nhận biết sáu ý định `recommend`, `price`, `syllabus`, `open`, `policy`, `greeting`; dùng được trong dịch vụ tư vấn.

Chạy thử huấn luyện từ đầu bằng tập ví dụ nhỏ:

```bash
python -m advisor.train --task intents --data data/intents.example.jsonl --epochs 30 --output artifacts/intents.pt
AI_MODEL_PATH=artifacts/intents.pt uvicorn advisor.app:app --host 127.0.0.1 --port 8000
```

Tập ví dụ chỉ để kiểm tra quy trình và học nghiên cứu. Mô hình nhỏ này chưa có khả năng hội thoại tổng quát như LLM thương mại. Khi chưa chỉ định checkpoint, chatbot vẫn hoạt động bằng truy xuất TF-IDF tính với PyTorch và câu trả lời có cấu trúc theo dữ liệu thật. Khi nạp checkpoint intent, dự đoán thử nghiệm của mô hình có thể điều chỉnh cách trả lời giá/nội dung; các quy tắc nghiệp vụ và bộ lọc giá/trình độ vẫn được áp dụng. Trọng số intent không chứa bảng giá.

Để nghiên cứu mô hình sinh văn bản và tiếp tục train bằng dữ liệu riêng:

```bash
python -m advisor.train --task text --data /duong-dan/tieng-viet.txt --epochs 10 --output artifacts/pretrain.pt
python -m advisor.train --task chat --data /duong-dan/hoi-thoai.jsonl --resume artifacts/pretrain.pt --epochs 10 --output artifacts/chat.pt
python -m advisor.generate --checkpoint artifacts/chat.pt --prompt '<user>
Tôi muốn học Python
<assistant>
' --max-tokens 150
```

Tùy chọn `--device cuda` hoặc `--device mps` nếu thiết bị và bản PyTorch hỗ trợ; mặc định CPU. Có thể tăng `--width`, `--layers`, `--heads`, `--context` cho lần khởi tạo mới. `--resume` nạp lại trọng số/config, khởi tạo optimizer mới để huấn luyện tiếp, không phải khôi phục chính xác toàn bộ trạng thái một lần chạy bị ngắt.

Train/validation chia theo hash cố định của câu hỏi/tài liệu, loại trùng trước khi chia. Mỗi epoch báo validation loss, perplexity và intent accuracy khi phù hợp; lưu checkpoint có validation loss thấp nhất. Cần thêm tập test riêng và các cuộc tư vấn đã được duyệt để đánh giá trước khi dùng thực tế. Validation trên tập ví dụ nhỏ không chứng minh chất lượng trên khách hàng thật. Không tự động đưa hội thoại khách vào tập train.

Checkpoint `text`/`chat` dùng để nghiên cứu sinh văn bản qua CLI; dịch vụ bán khóa học chỉ nhận checkpoint `intents`, tránh dùng văn bản chưa kiểm chứng làm giá hoặc quyết định cấp quyền học. Có thể phát triển tiếp cách sinh câu trả lời sau khi có đủ dữ liệu và đánh giá phù hợp.

## SSE và thao tác khóa học

FE dùng `fetch` POST, gửi cookie và CSRF token, đọc các sự kiện `status`, `answer`, `courses`, `done`, `error`. Đây là stream sự kiện nghiệp vụ, chưa phải stream từng token văn bản của LLM. Proxy Nginx đã tắt buffering.

BE giữ tối đa 12 lượt user/assistant trong phiên, giới hạn 20 câu hỏi/phút/phiên và một yêu cầu đang chạy/phiên. Bộ giới hạn này dành cho phạm vi ứng dụng hiện tại; khi mở public cần bổ sung giới hạn ở gateway để không phụ thuộc riêng vào cookie phiên.

Thẻ khóa học có nút Xem khóa học, Đăng ký hoặc Vào học. Bot không gọi checkout hay tạo quyền học. Người dùng thực hiện quy trình checkout hiện có; backend tiếp tục kiểm tra đăng nhập và quyền với API bài học.

## Docker

Từ thư mục gốc:

```bash
docker compose -f BE/compose.yaml up -d --build
docker compose -f FE/compose.yaml up -d --build
```

Compose BE chạy thêm `advisor` trong mạng nội bộ, không mở port AI ra máy host. `AI/data` và `AI/artifacts` được mount read-only vào container; nhập file hoặc train bằng CLI bên ngoài rồi restart advisor. Để dùng checkpoint intent, đặt `AI_MODEL_PATH=/app/artifacts/intents.pt` trong `BE/.env`, sau đó chạy lại Compose.

## Kiểm tra

```bash
python -m pytest -q
ruff format --check .
ruff check .
# Từ thư mục gốc khi BE và AI đang chạy:
node scripts/chat-smoke.mjs http://127.0.0.1:8080
```

Các test kiểm tra bộ lọc giá/trình độ, chuyển chủ đề, dữ liệu website ưu tiên Excel, giá lỗi và mã trùng, token dịch vụ, causal attention không nhìn tương lai, cập nhật trọng số thật và lưu/nạp checkpoint.
