package com.bigproject.modules.employee.controller;

import com.bigproject.modules.employee.dto.EmployeeDtos.*;
import com.bigproject.modules.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Approved employee profiles")
public class EmployeeController {
  private final EmployeeService employees;

  public EmployeeController(EmployeeService employees) {
    this.employees = employees;
  }

  @GetMapping
  @Operation(summary = "List approved employees with paging, search, filters and sorting")
  public Page<ListItem> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(defaultValue = "") String search,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "") String department,
      @RequestParam(defaultValue = "employeeCode") String sortBy,
      @RequestParam(defaultValue = "asc") String sortDirection) {
    return employees.list(page, pageSize, search, status, department, sortBy, sortDirection);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Read an approved employee profile")
  public Detail detail(@PathVariable UUID id) {
    return employees.detail(id);
  }
}
