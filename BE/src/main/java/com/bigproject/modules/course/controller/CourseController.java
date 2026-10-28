package com.bigproject.modules.course.controller;

import com.bigproject.core.security.CurrentUser;
import com.bigproject.modules.course.dto.*;
import com.bigproject.modules.course.entity.Course;
import com.bigproject.modules.course.service.CourseService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
public class CourseController {
  private final CourseService courses;

  public CourseController(CourseService courses) {
    this.courses = courses;
  }

  @GetMapping
  public List<Course> list(
      @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String category) {
    return courses.list(q, category);
  }

  @GetMapping("/{slug}")
  public CourseDetail detail(@PathVariable String slug, @AuthenticationPrincipal CurrentUser user) {
    return courses.detail(slug, user == null ? null : user.id());
  }

  @GetMapping("/{slug}/preview")
  public VideoLesson preview(@PathVariable String slug) {
    return courses.preview(slug);
  }
}
