package com.bigproject.modules.employee.dto.request;

import com.bigproject.modules.employee.dto.EmployeeProfile;
import jakarta.validation.Valid;
import java.util.UUID;

public record CreateEmployeeDraftRequest(UUID subjectId, @Valid EmployeeProfile target) {}
