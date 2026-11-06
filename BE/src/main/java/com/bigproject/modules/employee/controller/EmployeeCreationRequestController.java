package com.bigproject.modules.employee.controller;

import com.bigproject.modules.employee.entity.EmployeeRequestType;
import com.bigproject.modules.employee.service.EmployeeRequestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee-creation-requests")
@Tag(name = "Employee creation requests")
public class EmployeeCreationRequestController extends EmployeeRequestController {
  public EmployeeCreationRequestController(EmployeeRequestService requests) {
    super(requests, EmployeeRequestType.Creation);
  }
}
