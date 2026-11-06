package com.bigproject.modules.employee.dto.response;

import com.bigproject.modules.employee.dto.EmployeeProfile;
import java.time.Instant;

public record EmployeeRevisionResponse(
    long id,
    long requestId,
    int revisionNumber,
    Long baselineVersion,
    EmployeeProfile baseline,
    EmployeeProfile target,
    String reason,
    long submittedBy,
    Instant submittedAtUtc) {}
