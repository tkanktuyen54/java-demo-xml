# Khoa. — Website bán khóa học

Hai project độc lập trong cùng repository:

- **FE/**: Angular 22 — giao diện tiếng Việt, responsive.
- **BE/**: Java 21 / Spring Boot 4 — giữ cấu trúc `core + modules` của Java hiện tại.
- **Database**: MySQL 8.4, schema và dữ liệu mẫu được quản lý bằng Flyway.

## Tính năng

- Trang khám phá với 6 khóa học mẫu: tìm kiếm, lọc theo danh mục, sắp xếp theo giá/đánh giá.
- Click khóa học để xem mô tả, giảng viên, giá, lộ trình và video xem thử.
- Đăng ký, đăng nhập, đăng xuất; mật khẩu mã hóa BCrypt, phiên đăng nhập qua cookie HttpOnly và CSRF.
- Đăng ký khóa học qua **thanh toán demo**; tạo đơn hàng trong MySQL và mở quyền học, không thu tiền.
- Góc học tập, trình xem video, chuyển bài, đánh dấu hoàn thành và lưu tiến độ theo tài khoản.
- Kiểm tra quyền ở backend: cần đăng nhập và có đơn đăng ký đúng khóa học để lấy URL video bài học.

## Chạy toàn bộ bằng Docker

Cần Docker Desktop đang chạy. Từ thư mục gốc:

```powershell
docker compose -p khoa up -d --build
```

- Website: http://localhost:4200
- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- MySQL: `localhost:3307`, database/user `khoa`, password mặc định `khoa_local_password`.

Tạo tài khoản trên màn hình **Bắt đầu học**, rồi chọn khóa học → **Đăng ký khóa học** → **Xác nhận đăng ký demo** → xem video.

Database dùng Docker volume nên dữ liệu vẫn còn sau khi dừng container. Dừng dịch vụ bằng `docker compose -p khoa stop`. Có thể copy `.env.example` thành `.env` để đổi mật khẩu database trước lần khởi tạo đầu tiên; `.env` được bỏ qua bởi Git. Không chạy đồng thời Docker frontend/backend và dev server trên cùng cổng.

## Chạy để phát triển

Cần JDK 21, Node.js 24.15+ và Docker Desktop cho MySQL.

**1. MySQL** — terminal tại thư mục gốc:

```powershell
docker compose -p khoa up -d mysql
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

# Kiểm tra cả qua proxy Angular
node scripts/smoke.mjs http://127.0.0.1:4200
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
