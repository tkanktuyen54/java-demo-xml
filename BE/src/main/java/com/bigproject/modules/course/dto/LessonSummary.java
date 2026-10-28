package com.bigproject.modules.course.dto;

public record LessonSummary(
    long id, String title, String chapter, int position, int durationMinutes, boolean preview) {}
