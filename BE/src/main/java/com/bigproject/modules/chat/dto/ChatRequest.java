package com.bigproject.modules.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatRequest(
    @NotBlank(message = "Bạn hãy nhập câu hỏi.")
        @Size(max = 2000, message = "Câu hỏi tối đa 2.000 ký tự.")
        String message,
    @Pattern(regexp = "[a-z0-9-]{1,150}", message = "Mã khóa học không hợp lệ.")
        String courseSlug) {}
