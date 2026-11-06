package com.bigproject.modules.employee.dto.response;

import com.bigproject.modules.employee.dto.EmployeeProfile;
import com.bigproject.modules.employee.entity.EmployeeRequestState;
import com.bigproject.modules.employee.entity.EmployeeRequestType;
import java.time.Instant;
import java.util.UUID;

public record EmployeeRequestDetailResponse(
    long id,
    EmployeeRequestType type,
    EmployeeRequestState state,
    UUID subjectId,
    long requesterId,
    Long baselineVersion,
    EmployeeProfile baseline,
    EmployeeProfile target,
    long version,
    int revisionNumber,
    Long latestRevisionId,
    String reason,
    Instant createdAtUtc,
    Instant lastModifiedAtUtc) {}
