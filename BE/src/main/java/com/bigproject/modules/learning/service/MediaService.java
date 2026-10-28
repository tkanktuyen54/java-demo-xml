package com.bigproject.modules.learning.service;

import com.bigproject.core.security.CurrentUser;
import com.bigproject.modules.course.repository.CourseRepository;
import com.bigproject.modules.enrollment.repository.EnrollmentRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MediaService {
  private final CourseRepository courses;
  private final EnrollmentRepository enrollments;

  public MediaService(CourseRepository courses, EnrollmentRepository enrollments) {
    this.courses = courses;
    this.enrollments = enrollments;
  }

  public Resource video(long lessonId, CurrentUser user) {
    var access =
        courses
            .mediaAccess(lessonId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy video bài học."));
    if (!access.preview()) {
      if (user == null) {
        throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để xem bài học.");
      }
      if (!enrollments.exists(user.id(), access.courseId())) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Bạn cần đăng ký khóa học để xem video này.");
      }
    }
    // Bundled demonstration clip; replace this mapping with licensed, per-lesson media for
    // production.
    return new ClassPathResource("media/flower.mp4");
  }
}
