package com.bigproject.modules.employee.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class EmployeeRequestDtos {
  private EmployeeRequestDtos() {}

  public enum Type {
    Creation,
    Update
  }

  public enum State {
    Draft,
    PendingApproval,
    Rejected,
    Revoked,
    Approved,
    Cancelled
  }

  public record CreateDraft(@Positive Long subjectId, @Valid EmployeeProfile target) {}

  public record Submission(
      @Positive Long subjectId,
      @NotNull @Valid EmployeeProfile target,
      @NotBlank @Size(max = 2000) String reason) {}

  public record ReplaceDraft(@NotNull @Positive Long version, @Valid EmployeeProfile target) {}

  public record Submit(
      @NotNull @Positive Long version,
      @Valid EmployeeProfile target,
      @NotBlank @Size(max = 2000) String reason) {}

  public record Action(@NotNull @Positive Long version, @Size(max = 2000) String reason) {}

  public record Decision(
      @NotNull @Positive Long version,
      @NotNull @Positive Long revisionId,
      @Size(max = 2000) String reason) {}

  public record Detail(
      long id,
      Type type,
      State state,
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

  public record ListItem(
      long id,
      Type type,
      State state,
      Long subjectId,
      String employeeCode,
      String fullName,
      long requesterId,
      long version,
      Long latestRevisionId,
      Instant createdAtUtc,
      Instant lastModifiedAtUtc) {}

  public record Revision(
      long id,
      long requestId,
      int revisionNumber,
      Long baselineVersion,
      EmployeeProfile baseline,
      EmployeeProfile target,
      String reason,
      long submittedBy,
      Instant submittedAtUtc) {}

  public record Activity(
      long id,
      long requestId,
      Long revisionId,
      String action,
      long actorId,
      String reason,
      Instant occurredAtUtc) {}
}
