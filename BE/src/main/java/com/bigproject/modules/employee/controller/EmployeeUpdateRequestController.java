package com.bigproject.modules.employee.controller;

import com.bigproject.modules.employee.entity.EmployeeRequestType;
import com.bigproject.modules.employee.service.EmployeeRequestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employee-update-requests")
@Tag(name = "Employee update requests")
public class EmployeeUpdateRequestController extends EmployeeRequestController {
  public EmployeeUpdateRequestController(EmployeeRequestService requests) {
    super(requests, EmployeeRequestType.Update);
  }
}
