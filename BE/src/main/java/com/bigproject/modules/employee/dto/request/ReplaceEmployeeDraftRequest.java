package com.bigproject.modules.employee.dto.request;

import com.bigproject.modules.employee.dto.EmployeeProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReplaceEmployeeDraftRequest(
    @NotNull @Positive Long version, @Valid EmployeeProfile target) {}
