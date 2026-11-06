package com.bigproject.modules.employee.entity;

import com.bigproject.modules.employee.dto.EmployeeProfile;
import java.time.Instant;
import java.util.UUID;

/** Persisted employee; the numeric id is used only for internal relationships. */
public record Employee(
    long id,
    UUID publicId,
    String employeeCode,
    EmployeeStatus status,
    EmployeeProfile profile,
    long version,
    Instant createdAtUtc,
    Instant lastModifiedAtUtc) {}
