package com.bigproject.modules.course.dto;

public record VideoLesson(
    long id, String title, int durationMinutes, String videoUrl, boolean demo) {}
