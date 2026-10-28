package com.bigproject.modules.course.dto;

import com.bigproject.modules.course.entity.Course;
import java.util.List;

public record CourseDetail(Course course, List<LessonSummary> lessons, boolean enrolled) {}
