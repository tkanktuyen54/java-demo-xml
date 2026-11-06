package com.bigproject.modules.employee.repository;

import com.bigproject.modules.employee.dto.EmployeeProfile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class EmployeeProfileJson {
  private final ObjectMapper json;

  public EmployeeProfileJson(ObjectMapper json) {
    this.json = json;
  }

  public String write(EmployeeProfile profile) {
    return profile == null ? null : json.writeValueAsString(profile);
  }

  public EmployeeProfile read(String value) {
    return value == null ? null : json.readValue(value, EmployeeProfile.class);
  }
}
