package com.bigproject.modules.employee.dto.response;

import com.bigproject.modules.employee.dto.EmployeeProfile;
import com.bigproject.modules.employee.entity.EmployeeStatus;
import java.time.Instant;
import java.util.UUID;

public record EmployeeDetailResponse(
    UUID id,
    String employeeCode,
    EmployeeStatus status,
    EmployeeProfile profile,
    long version,
    Instant createdAtUtc,
    Instant lastModifiedAtUtc) {}
