package com.bigproject.modules.course.entity;

import java.math.BigDecimal;
import java.util.List;

public record Course(
    long id,
    String slug,
    String title,
    String subtitle,
    String description,
    String category,
    String level,
    String instructor,
    String instructorTitle,
    BigDecimal price,
    BigDecimal originalPrice,
    double hours,
    double rating,
    int studentCount,
    String coverKey,
    String badge,
    List<String> outcomes,
    int lessonCount) {}
