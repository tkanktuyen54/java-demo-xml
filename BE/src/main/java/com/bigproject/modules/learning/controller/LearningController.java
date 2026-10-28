package com.bigproject.modules.learning.controller;

import com.bigproject.core.security.CurrentUser;
import com.bigproject.modules.course.dto.VideoLesson;
import com.bigproject.modules.learning.service.LearningService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/learning/{slug}")
public class LearningController {
  private final LearningService learning;

  public LearningController(LearningService learning) {
    this.learning = learning;
  }

  @GetMapping("/lessons/{lessonId}")
  public VideoLesson video(
      @PathVariable String slug,
      @PathVariable long lessonId,
      @AuthenticationPrincipal CurrentUser user) {
    return learning.video(user.id(), slug, lessonId);
  }

  @GetMapping("/progress")
  public List<Long> progress(@PathVariable String slug, @AuthenticationPrincipal CurrentUser user) {
    return learning.completed(user.id(), slug);
  }

  @PostMapping("/lessons/{lessonId}/complete")
  public List<Long> complete(
      @PathVariable String slug,
      @PathVariable long lessonId,
      @AuthenticationPrincipal CurrentUser user) {
    return learning.complete(user.id(), slug, lessonId);
  }
}
