package com.bigproject.modules.enrollment.dto;

import java.math.BigDecimal;

public record Enrollment(
    long orderId,
    String slug,
    String title,
    String instructor,
    String coverKey,
    int lessonCount,
    int completedLessons,
    BigDecimal amount,
    String status) {}
