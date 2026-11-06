package com.bigproject.modules.employee.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class EmployeeDtos {
  private EmployeeDtos() {}

  public enum Status {
    Active,
    Suspended,
    Terminated
  }

  public record Detail(
      UUID id,
      String employeeCode,
      Status status,
      EmployeeProfile profile,
      long version,
      Instant createdAtUtc,
      Instant lastModifiedAtUtc) {}

  public record ListItem(
      UUID id,
      String employeeCode,
      String fullName,
      String avatarUrl,
      String department,
      String position,
      String phoneNumber,
      String email,
      Status status,
      long version) {}

  public record Page<T>(List<T> items, long totalCount, int page, int pageSize, long totalPages) {
    public static <T> Page<T> of(List<T> items, long count, int page, int size) {
      return new Page<>(items, count, page, size, (count + size - 1) / size);
    }
  }
}
