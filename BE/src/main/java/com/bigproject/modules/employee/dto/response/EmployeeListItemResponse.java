package com.bigproject.modules.employee.dto.response;

import com.bigproject.modules.employee.entity.EmployeeStatus;
import java.util.UUID;

public record EmployeeListItemResponse(
    UUID id,
    String employeeCode,
    String fullName,
    String avatarUrl,
    String department,
    String position,
    String phoneNumber,
    String email,
    EmployeeStatus status,
    long version) {}
