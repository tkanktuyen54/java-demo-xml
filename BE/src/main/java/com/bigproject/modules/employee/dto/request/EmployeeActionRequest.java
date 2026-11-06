package com.bigproject.modules.employee.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EmployeeActionRequest(
    @NotNull @Positive Long version, @Size(max = 2000) String reason) {}
