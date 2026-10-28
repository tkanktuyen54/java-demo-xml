package com.bigproject.modules.course.service;

import com.bigproject.modules.course.dto.*;
import com.bigproject.modules.course.entity.Course;
import com.bigproject.modules.course.repository.CourseRepository;
import com.bigproject.modules.enrollment.repository.EnrollmentRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CourseService {
  private final CourseRepository courses;
  private final EnrollmentRepository enrollments;

  public CourseService(CourseRepository courses, EnrollmentRepository enrollments) {
    this.courses = courses;
    this.enrollments = enrollments;
  }

  public List<Course> list(String query, String category) {
    return courses.findAll(query.trim(), category.trim());
  }

  public Course require(String slug) {
    return courses
        .findBySlug(slug)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học."));
  }

  public CourseDetail detail(String slug, Long userId) {
    Course course = require(slug);
    return new CourseDetail(
        course,
        courses.lessons(course.id()),
        userId != null && enrollments.exists(userId, course.id()));
  }

  public VideoLesson preview(String slug) {
    return courses
        .preview(require(slug).id())
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Khóa học chưa có video xem thử."));
  }
}
