package com.bigproject.modules.learning.service;

import com.bigproject.modules.course.dto.*;
import com.bigproject.modules.course.entity.Course;
import com.bigproject.modules.course.repository.CourseRepository;
import com.bigproject.modules.course.service.CourseService;
import com.bigproject.modules.enrollment.repository.EnrollmentRepository;
import com.bigproject.modules.learning.repository.ProgressRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearningService {
  private final CourseService courses;
  private final CourseRepository videos;
  private final EnrollmentRepository enrollments;
  private final ProgressRepository progress;

  public LearningService(
      CourseService courses,
      CourseRepository videos,
      EnrollmentRepository enrollments,
      ProgressRepository progress) {
    this.courses = courses;
    this.videos = videos;
    this.enrollments = enrollments;
    this.progress = progress;
  }

  private Course requireEnrollment(long userId, String slug) {
    Course course = courses.require(slug);
    if (!enrollments.exists(userId, course.id())) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Bạn cần đăng ký khóa học để xem bài học này.");
    }
    return course;
  }

  public VideoLesson video(long userId, String slug, long lessonId) {
    Course course = requireEnrollment(userId, slug);
    return videos
        .video(course.id(), lessonId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Không tìm thấy bài học trong khóa học này."));
  }

  public List<Long> completed(long userId, String slug) {
    return progress.completed(userId, requireEnrollment(userId, slug).id());
  }

  @Transactional
  public List<Long> complete(long userId, String slug, long lessonId) {
    video(userId, slug, lessonId);
    enrollments.lockUser(userId);
    progress.complete(userId, lessonId);
    return completed(userId, slug);
  }
}
