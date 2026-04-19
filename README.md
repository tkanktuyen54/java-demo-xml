# big-project

Spring Boot (Java 21). Cấu trúc gói đúng hướng **core + modules** (giống Angular features).

## `src/main/java/com/bigproject/`

```
BigProjectApplication.java

core/                    ← code chung toàn dự án (không copy theo module)
├── config/
├── exception/
├── util/
└── security/            ← chỗ cấu hình Spring Security / JWT sau (chưa bật starter)

modules/                 ← mọi tính năng theo domain
├── user/                ← API user / address (đang có code)
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/  (request/, response/)
│   └── mapper/
├── project/             ← skeleton: controller, service, repository, entity, dto
├── task/
├── contract/
├── employee/
└── home/                ← chỉ HomeController GET / (entry tùy chọn)
```

## Chạy

```bash
./mvnw spring-boot:run
```

Profile: `dev` mặc định (`application-dev.yml`). Prod: `SPRING_PROFILES_ACTIVE=prod`.

## API

- `GET /` — `modules.home.HomeController`
- `GET/POST .../api/tbl-user`, `GET .../api/tbl-address` — `modules.user`
- Swagger: `/swagger-ui.html`
