package com.bigproject.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Vui lòng nhập họ tên.")
        @Size(min = 2, max = 100, message = "Họ tên từ 2 đến 100 ký tự.")
        String name,
    @NotBlank(message = "Vui lòng nhập email.")
        @Email(message = "Email chưa hợp lệ.")
        @Size(max = 254)
        String email,
    @NotBlank(message = "Vui lòng nhập mật khẩu.")
        @Size(min = 8, max = 72, message = "Mật khẩu từ 8 đến 72 ký tự.")
        String password) {}
