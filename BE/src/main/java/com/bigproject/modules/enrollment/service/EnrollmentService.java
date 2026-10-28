package com.bigproject.modules.enrollment.service;

import com.bigproject.modules.course.entity.Course;
import com.bigproject.modules.course.service.CourseService;
import com.bigproject.modules.enrollment.dto.Enrollment;
import com.bigproject.modules.enrollment.repository.EnrollmentRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EnrollmentService {
  private final EnrollmentRepository enrollments;
  private final CourseService courses;
  private final boolean demoEnabled;

  public EnrollmentService(
      EnrollmentRepository enrollments,
      CourseService courses,
      @Value("${app.demo-checkout-enabled:false}") boolean demoEnabled) {
    this.enrollments = enrollments;
    this.courses = courses;
    this.demoEnabled = demoEnabled;
  }

  public List<Enrollment> mine(long userId) {
    return enrollments.findByUser(userId);
  }

  public boolean demoEnabled() {
    return demoEnabled;
  }

  @Transactional
  public Enrollment checkout(long userId, String slug) {
    if (!demoEnabled) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Thanh toán demo đang tắt. Chưa cấu hình cổng thanh toán thật.");
    }
    Course course = courses.require(slug);
    enrollments.lockUser(userId);
    if (!enrollments.exists(userId, course.id())) {
      enrollments.create(userId, course.id(), course.price());
    }
    return mine(userId).stream().filter(item -> item.slug().equals(slug)).findFirst().orElseThrow();
  }
}
