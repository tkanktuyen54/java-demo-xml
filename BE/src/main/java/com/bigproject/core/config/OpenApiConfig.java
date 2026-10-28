package com.bigproject.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI bigProjectOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Khoa Course API")
                .version("1.0.0")
                .description(
                    """
                    [Download OpenAPI Document (JSON)](../v3/api-docs?download=true) · \
                    [Download OpenAPI Document (YAML)](../v3/api-docs.yaml?download=true)

                    Tài liệu API cho website bán khóa học Khoa.

                    **Đăng nhập trên Swagger:** gọi `GET /api/auth/csrf` trước, sau đó \
                    gọi `POST /api/auth/register` để tạo tài khoản hoặc `POST /api/auth/login` \
                    với email và mật khẩu đã đăng ký. Swagger gửi cookie đăng nhập và \
                    header CSRF cùng các yêu cầu tiếp theo.

                    Sau khi đăng nhập hoặc đăng xuất, gọi lại `GET /api/auth/csrf` \
                    trước khi thực hiện yêu cầu POST tiếp theo.
                    """));
  }
}
