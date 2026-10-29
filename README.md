# Khoa. — Website bán khóa học

Hai project độc lập trong cùng repository:

- **FE/**: Angular 22 — giao diện tiếng Việt, responsive.
- **BE/**: Java 21 / Spring Boot 4 — giữ cấu trúc `core + modules` của Java hiện tại.
- **Database**: MySQL 8.4, schema và dữ liệu mẫu được quản lý bằng Flyway.
- **AI/**: Python / PyTorch — tư vấn khóa học, nhập danh mục Excel/CSV và mô hình Transformer nhỏ để nghiên cứu huấn luyện từ đầu. Hướng dẫn chạy, nhập dữ liệu và train tại [AI/README.md](AI/README.md).

## Tính năng

- Trang khám phá với 6 khóa học mẫu: tìm kiếm, lọc theo danh mục, sắp xếp theo giá/đánh giá.
- Click khóa học để xem mô tả, giảng viên, giá, lộ trình và video xem thử.
- Đăng ký, đăng nhập, đăng xuất; mật khẩu mã hóa BCrypt, phiên đăng nhập qua cookie HttpOnly và CSRF.
- Đăng ký khóa học qua **thanh toán demo**; tạo đơn hàng trong MySQL và mở quyền học, không thu tiền.
- Góc học tập, trình xem video, chuyển bài, đánh dấu hoàn thành và lưu tiến độ theo tài khoản.
- Kiểm tra quyền ở backend: cần đăng nhập và có đơn đăng ký đúng khóa học để lấy URL video bài học.
- Icon tư vấn mở sidebar bên phải toàn chiều cao; gợi ý khóa học theo chủ đề, trình độ và ngân sách qua SSE, kèm nút xem/đăng ký/vào học.

## Chạy Docker riêng cho từng đội

Cần Docker Desktop đang chạy. Mỗi đội có Dockerfile, Compose và cấu hình môi trường riêng:

- `BE/compose.yaml`: backend và MySQL; project Compose `khoa` để giữ volume dữ liệu hiện tại.
- `FE/compose.yaml`: chỉ frontend; project Compose `khoa-fe`, gọi API qua `BACKEND_URL`.

Từ thư mục gốc, build và chạy từng phần bằng các lệnh riêng:

```powershell
# Đội BE: chạy backend và MySQL
docker compose -f BE/compose.yaml up -d --build

# Đội FE: chỉ chạy frontend
docker compose -f FE/compose.yaml up -d --build
```

- Website Docker: http://localhost:4201
- API: http://localhost:8081
- Swagger: http://localhost:8081/swagger-ui.html
- MySQL: `localhost:3307`, database/user `khoa`, password mặc định `khoa_local_password`.

Backend Docker dùng cổng `8081` trên máy, ánh xạ vào cổng `8080` trong container để tránh trùng với backend chạy trực tiếp bằng IDE ở cổng `8080`.

Frontend không có `depends_on` backend và không dùng chung Docker network với BE. Mặc định Nginx chuyển `/api` đến `http://host.docker.internal:8081`, còn trình duyệt luôn gọi API cùng origin với FE để giữ cookie và CSRF. FE có thể khởi động khi BE chưa chạy; các chức năng cần API sẽ hoạt động khi BE sẵn sàng.

Mỗi đội có thể copy `.env.example` trong thư mục của mình thành `.env`. Chạy Compose từ chính thư mục đó để nạp cấu hình riêng. Khi BE chạy trên máy khác, đặt `BACKEND_URL` trong `FE/.env` thành địa chỉ BE có thể truy cập, ví dụ `http://192.168.1.50:8081`; không thêm `/api` hoặc dấu `/` cuối URL. Đội BE đặt `BACKEND_BIND_ADDRESS=0.0.0.0` trong `BE/.env` để nhận kết nối từ máy khác. Với Docker Engine trên Linux, cũng dùng địa chỉ bind này nếu FE truy cập BE qua `host.docker.internal`.

Thay đổi `BACKEND_URL` chỉ cần chạy lại `docker compose up -d` trong `FE`, không cần build lại image. FE đang phát triển bằng `npm start` vẫn dùng backend IDE ở cổng `8080` theo `FE/proxy.conf.json`.

Tạo tài khoản trên màn hình **Bắt đầu học**, rồi chọn khóa học → **Đăng ký khóa học** → **Xác nhận đăng ký demo** → xem video.

Database tiếp tục dùng volume `khoa_khoa_mysql`, nên dữ liệu hiện tại được giữ lại. Dừng từng đội từ thư mục gốc bằng `docker compose -f BE/compose.yaml stop` hoặc `docker compose -f FE/compose.yaml stop`; dừng FE không ảnh hưởng BE/MySQL. Mật khẩu database được đặt trong `BE/.env` trước lần khởi tạo đầu tiên; `.env` được bỏ qua bởi Git. Nếu đã có `.env` ở thư mục gốc từ cấu hình cũ, chuyển các biến database sang `BE/.env`.

## Chạy để phát triển

Cần JDK 21, Node.js 24.15+ và Docker Desktop cho MySQL.

**1. MySQL** — terminal tại thư mục gốc:

```powershell
docker compose -f BE/compose.yaml up -d mysql
```

**2. Backend** — terminal riêng:

```powershell
cd BE
$env:JAVA_HOME = 'C:\duong-dan\jdk-21'
.\mvnw.cmd spring-boot:run
```

Máy hiện tại có JDK 21 tại `C:\Users\tuyen.pv\.jdks\jbr-21.0.11`; Java mặc định đang là 17, nên cần chọn JDK 21 khi chạy backend.

**3. Frontend** — terminal riêng:

```powershell
cd FE
npm.cmd ci
npm.cmd start
```

Mở http://localhost:4200. Angular proxy `/api` về backend nên cookie và CSRF hoạt động cùng origin.

Để dùng chatbot khi chạy local, chạy thêm dịch vụ Python tại cổng `8000` theo [AI/README.md](AI/README.md). Docker Compose BE tự chạy dịch vụ này. Không cần API key của nhà cung cấp LLM bên ngoài.

macOS/Linux: đặt `JAVA_HOME` tới JDK 21, dùng `./mvnw spring-boot:run`, `npm ci` và `npm start`.

## Kiểm tra

```powershell
# Trong BE
.\mvnw.cmd test
.\mvnw.cmd package

# Trong FE
npm.cmd run check
npm.cmd run build

# Tại thư mục gốc, khi backend và MySQL đang chạy
node scripts/smoke.mjs

# Kiểm tra backend Docker
node scripts/smoke.mjs http://127.0.0.1:8081

# Kiểm tra cả qua proxy Angular
node scripts/smoke.mjs http://127.0.0.1:4200

# Kiểm tra qua Nginx của frontend Docker
node scripts/smoke.mjs http://127.0.0.1:4201
```

Smoke test tạo tài khoản và đơn demo mới, kiểm tra cookie/CSRF thật, login/logout, quyền xem video, chống đơn trùng và tiến độ sau khi đăng nhập lại. Không xóa dữ liệu hiện có.

Xem cấu trúc và API chi tiết ở [BE/README.md](BE/README.md); hướng dẫn Angular ở [FE/README.md](FE/README.md).

## Format code

- FE: trong `FE`, chạy `npm.cmd run format`; kiểm tra bằng `npm.cmd run format:check`.
- BE: trong `BE`, chạy `.\mvnw.cmd spotless:apply`; kiểm tra bằng `.\mvnw.cmd spotless:check`.

FE và BE đều dùng 2 dấu cách: Prettier cho frontend, Spotless cho Java và `pom.xml`. `.editorconfig` thống nhất thụt lề, UTF-8 và xuống dòng LF. Không format lại SQL migration đã áp dụng.

## Nội dung mẫu và phạm vi hiện tại

Khóa học, giảng viên, giá và đánh giá đều là dữ liệu minh họa. Video hiện dùng clip **Flower của MDN (CC0)**, được lưu sẵn trong backend để kiểm tra trình phát; đây chưa phải bài giảng Angular, Java hoặc các chủ đề được ghi trong danh mục. Giao diện có ghi rõ video minh họa. API phát video hỗ trợ HTTP byte ranges để tua, và kiểm tra quyền cả khi gọi trực tiếp URL media.

Chưa kết nối cổng thanh toán thật, chưa có trang quản trị khóa học hoặc upload video. Checkout demo chỉ bật mặc định với profile `dev`; profile `prod` tắt demo, yêu cầu cookie HTTPS và tắt Swagger. Khi triển khai thật, cấu hình database riêng, HTTPS và thay nội dung mẫu. Tích hợp thanh toán cần tạo quyền học sau khi backend xác minh webhook thành công. Thay clip dùng chung bằng nội dung riêng theo từng bài học hoặc kho lưu trữ riêng và URL ký có thời hạn sau khi kiểm tra quyền.

Nguồn kỹ thuật tham khảo: [tương thích phiên bản Angular](https://angular.dev/reference/versions), [Spring Security cho SPA và CSRF](https://docs.spring.io/spring-security/reference/7.0/servlet/exploits/csrf.html).
