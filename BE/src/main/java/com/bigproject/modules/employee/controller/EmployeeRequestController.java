package com.bigproject.modules.employee.controller;

import com.bigproject.core.security.CurrentUser;
import com.bigproject.modules.employee.dto.request.CreateAndSubmitEmployeeRequest;
import com.bigproject.modules.employee.dto.request.CreateEmployeeDraftRequest;
import com.bigproject.modules.employee.dto.request.EmployeeActionRequest;
import com.bigproject.modules.employee.dto.request.EmployeeDecisionRequest;
import com.bigproject.modules.employee.dto.request.ReplaceEmployeeDraftRequest;
import com.bigproject.modules.employee.dto.request.SubmitEmployeeRequest;
import com.bigproject.modules.employee.dto.response.EmployeeActivityResponse;
import com.bigproject.modules.employee.dto.response.EmployeeRequestDetailResponse;
import com.bigproject.modules.employee.dto.response.EmployeeRevisionResponse;
import com.bigproject.modules.employee.dto.response.PageResponse;
import com.bigproject.modules.employee.entity.EmployeeRequestType;
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
  private final EmployeeRequestType type;

  protected EmployeeRequestController(EmployeeRequestService requests, EmployeeRequestType type) {
    this.requests = requests;
    this.type = type;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Open a creation or update draft; updates require subjectId")
  public EmployeeRequestDetailResponse create(
      @Valid @RequestBody CreateEmployeeDraftRequest body,
      @AuthenticationPrincipal CurrentUser user) {
    return requests.create(type, body, user.id());
  }

  @PostMapping("/submissions")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create and submit atomically with a complete target and reason")
  public EmployeeRequestDetailResponse createAndSubmit(
      @Valid @RequestBody CreateAndSubmitEmployeeRequest body,
      @AuthenticationPrincipal CurrentUser user) {
    return requests.createAndSubmit(type, body, user.id());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Read the effective draft or submitted request")
  public EmployeeRequestDetailResponse detail(
      @PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
    return requests.detail(type, id, user.id());
  }

  @PutMapping("/{id}/draft")
  @Operation(
      summary = "Replace a draft; correcting rejected/revoked requests opens a private draft")
  public EmployeeRequestDetailResponse replace(
      @PathVariable long id,
      @Valid @RequestBody ReplaceEmployeeDraftRequest body,
      @AuthenticationPrincipal CurrentUser user) {
    return requests.replace(type, id, body, user.id());
  }

  @PostMapping("/{id}/submit")
  @Operation(summary = "Freeze a new immutable revision and send it for approval")
  public EmployeeRequestDetailResponse submit(
      @PathVariable long id,
      @Valid @RequestBody SubmitEmployeeRequest body,
      @AuthenticationPrincipal CurrentUser user) {
    return requests.submit(type, id, body, user.id());
  }

  @PostMapping("/{id}/revoke")
  @Operation(summary = "Requester revokes a pending submission without erasing its revision")
  public EmployeeRequestDetailResponse revoke(
      @PathVariable long id,
      @Valid @RequestBody EmployeeActionRequest body,
      @AuthenticationPrincipal CurrentUser user) {
    return requests.revoke(type, id, body, user.id());
  }

  @PostMapping("/{id}/cancel")
  @Operation(summary = "Requester cancels a submitted request with a reason")
  public EmployeeRequestDetailResponse cancel(
      @PathVariable long id,
      @Valid @RequestBody EmployeeActionRequest body,
      @AuthenticationPrincipal CurrentUser user) {
    return requests.cancel(type, id, body, user.id());
  }

  @PostMapping("/{id}/approve")
  @Operation(summary = "Independent approver applies exactly the selected submitted revision")
  public EmployeeRequestDetailResponse approve(
      @PathVariable long id,
      @Valid @RequestBody EmployeeDecisionRequest body,
      @AuthenticationPrincipal CurrentUser user) {
    return requests.decide(type, id, body, user.id(), true);
  }

  @PostMapping("/{id}/reject")
  @Operation(summary = "Independent approver returns a submission for correction with a reason")
  public EmployeeRequestDetailResponse reject(
      @PathVariable long id,
      @Valid @RequestBody EmployeeDecisionRequest body,
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
  public List<EmployeeRevisionResponse> revisions(
      @PathVariable long id, @AuthenticationPrincipal CurrentUser user) {
    return requests.revisions(type, id, user.id());
  }

  @GetMapping("/{id}/revisions/{revisionId}")
  @Operation(summary = "Read the exact baseline, target and reason submitted in one revision")
  public EmployeeRevisionResponse revision(
      @PathVariable long id,
      @PathVariable long revisionId,
      @AuthenticationPrincipal CurrentUser user) {
    return requests.revision(type, id, revisionId, user.id());
  }

  @GetMapping("/{id}/activity-history")
  @Operation(summary = "Read request actions and approval history, newest first")
  public PageResponse<EmployeeActivityResponse> activities(
      @PathVariable long id,
      @AuthenticationPrincipal CurrentUser user,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return requests.activities(type, id, user.id(), page, pageSize);
  }
}
