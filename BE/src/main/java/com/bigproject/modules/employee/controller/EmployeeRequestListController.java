package com.bigproject.modules.employee.controller;

import com.bigproject.core.security.CurrentUser;
import com.bigproject.modules.employee.dto.response.EmployeeRequestListItemResponse;
import com.bigproject.modules.employee.dto.response.PageResponse;
import com.bigproject.modules.employee.service.EmployeeRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee-requests")
@Tag(name = "Employee requests")
public class EmployeeRequestListController {
  private final EmployeeRequestService requests;

  public EmployeeRequestListController(EmployeeRequestService requests) {
    this.requests = requests;
  }

  @GetMapping
  @Operation(summary = "List creation/update requests; drafts are private to their requester")
  public PageResponse<EmployeeRequestListItemResponse> list(
      @AuthenticationPrincipal CurrentUser user,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(defaultValue = "") String type,
      @RequestParam(defaultValue = "") String state,
      @RequestParam(defaultValue = "") String search,
      @RequestParam(defaultValue = "false") boolean includeCompleted) {
    return requests.list(user.id(), page, pageSize, type, state, search, includeCompleted);
  }
}
