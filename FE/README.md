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

## Chạy

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
