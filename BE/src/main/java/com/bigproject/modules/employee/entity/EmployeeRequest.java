package com.bigproject.modules.employee.entity;

import com.bigproject.modules.employee.dto.EmployeeProfile;
import java.time.Instant;

/** Request persistence state, including the internal employee foreign key. */
public record EmployeeRequest(
    long id,
    EmployeeRequestType type,
    EmployeeRequestState state,
    Long subjectId,
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
