package com.bigproject.modules.employee.dto.response;

import java.time.Instant;

public record EmployeeActivityResponse(
    long id,
    long requestId,
    Long revisionId,
    String action,
    long actorId,
    String reason,
    Instant occurredAtUtc) {}
