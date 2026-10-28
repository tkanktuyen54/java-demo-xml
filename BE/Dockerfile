# Giai đoạn 1: dùng Maven 3.9 và JDK 21 để build ứng dụng; đặt tên giai đoạn là build.
FROM maven:3.9-eclipse-temurin-21 AS build

# Đặt /app làm thư mục làm việc trong image; Docker tạo thư mục nếu chưa có.
WORKDIR /app

# Chép pom.xml từ thư mục nguồn build (BE) vào /app để khai báo thư viện và plugin Maven.
COPY pom.xml ./

# Tải trước các thư viện và plugin Maven; -B bật chế độ chạy không tương tác.
# Tách bước này để Docker có thể dùng lại cache khi chỉ mã nguồn thay đổi.
RUN mvn -B dependency:go-offline

# Chép src vào /app/src, bao gồm mã Java, tài nguyên và các file SQL migration.
COPY src ./src

# Biên dịch, chạy test và đóng gói ứng dụng thành file JAR trong /app/target.
RUN mvn -B package

# Giai đoạn 2: tạo image chạy ứng dụng bằng JRE 21 trên Alpine Linux.
# Giai đoạn này chỉ lấy file JAR từ giai đoạn build, giúp image không chứa Maven và mã nguồn.
FROM eclipse-temurin:21-jre-alpine

# Đặt /app làm thư mục làm việc của image chạy ứng dụng.
WORKDIR /app

# Tạo nhóm app và người dùng hệ thống app thuộc nhóm đó; && chạy lệnh sau nếu lệnh trước thành công.
RUN addgroup -S app && adduser -S app -G app

# Lấy file JAR từ giai đoạn build, chép vào /app/app.jar trong image chạy ứng dụng.
COPY --from=build /app/target/big-project-0.0.1-SNAPSHOT.jar app.jar

# Chạy các lệnh tiếp theo và tiến trình ứng dụng bằng người dùng app thay vì root.
USER app

# Khai báo ứng dụng dùng cổng 8080 trong container; cần -p hoặc ports trong Compose để truy cập từ máy.
# EXPOSE không tự cấu hình cổng lắng nghe của Spring Boot hoặc mở cổng ra ngoài container.
EXPOSE 8080

# Khi container khởi động, chạy java -jar app.jar để khởi động ứng dụng Spring Boot.
ENTRYPOINT ["java", "-jar", "app.jar"]
