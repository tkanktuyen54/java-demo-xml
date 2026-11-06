package com.bigproject.modules.employee.dto.response;

import com.bigproject.modules.employee.entity.EmployeeRequestState;
import com.bigproject.modules.employee.entity.EmployeeRequestType;
import java.time.Instant;
import java.util.UUID;

public record EmployeeRequestListItemResponse(
    long id,
    EmployeeRequestType type,
    EmployeeRequestState state,
    UUID subjectId,
    String employeeCode,
    String fullName,
    long requesterId,
    long version,
    Long latestRevisionId,
    Instant createdAtUtc,
    Instant lastModifiedAtUtc) {}
