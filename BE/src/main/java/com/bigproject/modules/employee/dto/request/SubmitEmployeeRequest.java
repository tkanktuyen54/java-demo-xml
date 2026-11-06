package com.bigproject.modules.employee.dto.request;

import com.bigproject.modules.employee.dto.EmployeeProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SubmitEmployeeRequest(
    @NotNull @Positive Long version,
    @Valid EmployeeProfile target,
    @NotBlank @Size(max = 2000) String reason) {}
