package com.bigproject.modules.enrollment.controller;

import com.bigproject.core.security.CurrentUser;
import com.bigproject.modules.enrollment.dto.Enrollment;
import com.bigproject.modules.enrollment.service.EnrollmentService;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class EnrollmentController {
  private final EnrollmentService enrollments;

  public EnrollmentController(EnrollmentService enrollments) {
    this.enrollments = enrollments;
  }

  @GetMapping("/api/config")
  public Map<String, Boolean> config() {
    return Map.of("demoCheckoutEnabled", enrollments.demoEnabled());
  }

  @GetMapping("/api/enrollments")
  public List<Enrollment> mine(@AuthenticationPrincipal CurrentUser user) {
    return enrollments.mine(user.id());
  }

  @PostMapping("/api/enrollments/{slug}/demo-checkout")
  public Enrollment checkout(@PathVariable String slug, @AuthenticationPrincipal CurrentUser user) {
    return enrollments.checkout(user.id(), slug);
  }
}
