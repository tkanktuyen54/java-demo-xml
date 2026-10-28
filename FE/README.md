# Khoa Frontend

Angular 22, standalone components, signals, router và HttpClient. Giao diện tiếng Việt, hỗ trợ desktop và điện thoại.

```text
src/app/
├── core/        # API, auth, route guard, models
├── features/
│   ├── catalog/
│   ├── course/
│   ├── auth/
│   ├── checkout/
│   ├── my-learning/
│   └── learning/
└── shared/      # Course card, hình minh họa, trang 404
```

## Chạy bằng Docker

Đội FE quản lý `Dockerfile`, `compose.yaml` và cấu hình API ngay trong thư mục `FE`, không cần mã nguồn BE hoặc MySQL. Từ thư mục `FE`:

```powershell
# Build image frontend
docker compose build frontend

# Chỉ chạy frontend
docker compose up -d

# Build lại và chạy khi sửa code
docker compose up -d --build frontend

# Xem log hoặc dừng frontend
docker compose logs -f frontend
docker compose stop
```

Website Docker: `http://localhost:4201`. Cổng này tách khỏi Angular dev server ở `4200`.

Copy `.env.example` thành `.env` trong `FE` để cấu hình:

| Biến                    | Mặc định                           | Ý nghĩa                                        |
| ----------------------- | ---------------------------------- | ---------------------------------------------- |
| `FRONTEND_BIND_ADDRESS` | `127.0.0.1`                        | Dùng `0.0.0.0` khi cần truy cập FE từ máy khác |
| `FRONTEND_PORT`         | `4201`                             | Cổng website trên máy chạy Docker              |
| `BACKEND_URL`           | `http://host.docker.internal:8081` | Địa chỉ API nhìn từ container Nginx            |

Khi BE ở máy khác, đổi `BACKEND_URL` thành IP/domain của máy BE, ví dụ `http://192.168.1.50:8081` hoặc `https://api.example.com`. Chỉ dùng URL gốc, không thêm `/api` hoặc dấu `/` cuối URL. Đội BE cần cấu hình để API nhận kết nối từ máy FE. Với Docker Engine trên Linux, BE cùng máy cũng cần đặt `BACKEND_BIND_ADDRESS=0.0.0.0` để nhận kết nối qua `host.docker.internal`.

Sau khi đổi `.env`, chạy `docker compose up -d`; không cần build lại image. Nginx dùng `nginx.conf.template` để nạp `BACKEND_URL` khi container khởi động. Trình duyệt vẫn gọi `/api` cùng origin với FE, nên cookie và CSRF tiếp tục hoạt động qua proxy.

Compose FE không khởi động BE/MySQL và không yêu cầu Docker network của BE. Khi BE chưa chạy, FE vẫn khởi động và phục vụ giao diện; các thao tác cần API sẽ báo chưa kết nối được máy chủ.

## Chạy để phát triển

Yêu cầu Node.js 24.15+ và npm. Chạy backend cổng 8080 trước.

```powershell
npm.cmd ci
npm.cmd start
```

macOS/Linux dùng `npm ci` và `npm start`. Mở `http://localhost:4200`.

`proxy.conf.json` chuyển `/api/**` về `http://127.0.0.1:8080`. Không cần bật CORS hoặc cấu hình URL backend trong từng component.

## Build

```powershell
npm.cmd run check
npm.cmd run build
```

## Format code

Dùng Prettier, thụt lề 2 dấu cách, xuống dòng cho TypeScript, Angular template, CSS và JSON. Cấu hình tại `.prettierrc.json`; `.editorconfig` ở thư mục gốc thống nhất UTF-8 và xuống dòng LF.

```powershell
npm.cmd run format
npm.cmd run format:check
```

`format` sửa định dạng, `format:check` chỉ kiểm tra. Bỏ qua thư mục build, dependencies và lockfile.

Kết quả tại `dist/khoa/browser`. Nginx phục vụ Angular và chuyển API đến backend, có fallback cho việc tải lại các route như `/courses/angular-thuc-chien` và `/learn/angular-thuc-chien`.

Các màn hình: danh mục (tìm kiếm, lọc, sắp xếp), chi tiết/video xem thử, đăng nhập/đăng ký, xác nhận checkout demo, góc học tập, trình xem bài học và tiến độ. Route mua/học yêu cầu đăng nhập; quyền truy cập video được kiểm tra tiếp ở backend.

Font Google Fonts có fallback font hệ thống. Hình minh họa khóa học được vẽ bằng HTML/CSS, không phụ thuộc ảnh bên ngoài. Video demo được lưu trong backend, phát qua endpoint có kiểm tra quyền và hỗ trợ HTTP byte ranges; không phụ thuộc dịch vụ video bên ngoài khi chạy.
