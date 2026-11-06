package com.bigproject.modules.employee.controller;

import com.bigproject.core.security.CurrentUser;
import com.bigproject.modules.employee.dto.EmployeeDtos.Page;
import com.bigproject.modules.employee.dto.EmployeeRequestDtos.*;
import com.bigproject.modules.employee.service.EmployeeRequestService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

public abstract class EmployeeRequestController {
  private final EmployeeRequestService requests;
  private final Type type;

  protected EmployeeRequestController(EmployeeRequestService requests, Type type) {
    this.requests = requests;
    this.type = type;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Open a creation or update draft; updates require subjectId")
  public Detail create(
      @Valid @RequestBody CreateDraft body, @AuthenticationPrincipal CurrentUser user) {
    return requests.create(type, body, user.id());
  }

  @PostMapping("/submissions")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create and submit atomically with a complete target and reason")
  public Detail createAndSubmit(
      @Valid @RequestBody Submission body, @AuthenticationPrincipal CurrentUser user) {
    return requests.createAndSubmit(type, body, user.id());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Read the effective draft or submitted request")
  public Detail detail(@PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
    return requests.detail(type, id, user.id());
  }

  @PutMapping("/{id}/draft")
  @Operation(
      summary = "Replace a draft; correcting rejected/revoked requests opens a private draft")
  public Detail replace(
      @PathVariable long id,
      @Valid @RequestBody ReplaceDraft body,
      @AuthenticationPrincipal CurrentUser user) {
    return requests.replace(type, id, body, user.id());
  }

  @PostMapping("/{id}/submit")
  @Operation(summary = "Freeze a new immutable revision and send it for approval")
  public Detail submit(
      @PathVariable long id,
      @Valid @RequestBody Submit body,
      @AuthenticationPrincipal CurrentUser user) {
    return requests.submit(type, id, body, user.id());
  }

  @PostMapping("/{id}/revoke")
  @Operation(summary = "Requester revokes a pending submission without erasing its revision")
  public Detail revoke(
      @PathVariable long id,
      @Valid @RequestBody Action body,
      @AuthenticationPrincipal CurrentUser user) {
    return requests.revoke(type, id, body, user.id());
  }

  @PostMapping("/{id}/cancel")
  @Operation(summary = "Requester cancels a submitted request with a reason")
  public Detail cancel(
      @PathVariable long id,
      @Valid @RequestBody Action body,
      @AuthenticationPrincipal CurrentUser user) {
    return requests.cancel(type, id, body, user.id());
  }

  @PostMapping("/{id}/approve")
  @Operation(summary = "Independent approver applies exactly the selected submitted revision")
  public Detail approve(
      @PathVariable long id,
      @Valid @RequestBody Decision body,
      @AuthenticationPrincipal CurrentUser user) {
    return requests.decide(type, id, body, user.id(), true);
  }

  @PostMapping("/{id}/reject")
  @Operation(summary = "Independent approver returns a submission for correction with a reason")
  public Detail reject(
      @PathVariable long id,
      @Valid @RequestBody Decision body,
      @AuthenticationPrincipal CurrentUser user) {
    return requests.decide(type, id, body, user.id(), false);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Soft-delete an unapplied request and retain its audit evidence")
  public Map<String, Long> delete(
      @PathVariable long id,
      @RequestParam long version,
      @AuthenticationPrincipal CurrentUser user) {
    requests.delete(type, id, version, user.id());
    return Map.of("id", id);
  }

  @GetMapping("/{id}/revisions")
  @Operation(summary = "List immutable submitted revisions, oldest first")
  public List<Revision> revisions(
      @PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
    return requests.revisions(type, id, user.id());
  }

  @GetMapping("/{id}/revisions/{revisionId}")
  @Operation(summary = "Read the exact baseline, target and reason submitted in one revision")
  public Revision revision(
      @PathVariable long id,
      @PathVariable long revisionId,
      @AuthenticationPrincipal CurrentUser user) {
    return requests.revision(type, id, revisionId, user.id());
  }

  @GetMapping("/{id}/activity-history")
  @Operation(summary = "Read request actions and approval history, newest first")
  public Page<Activity> activities(
      @PathVariable long id,
      @AuthenticationPrincipal CurrentUser user,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return requests.activities(type, id, user.id(), page, pageSize);
  }
}
