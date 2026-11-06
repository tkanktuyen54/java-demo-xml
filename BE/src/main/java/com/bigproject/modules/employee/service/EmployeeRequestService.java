package com.bigproject.modules.employee.service;

import com.bigproject.modules.employee.dto.EmployeeProfile;
import com.bigproject.modules.employee.dto.request.CreateAndSubmitEmployeeRequest;
import com.bigproject.modules.employee.dto.request.CreateEmployeeDraftRequest;
import com.bigproject.modules.employee.dto.request.EmployeeActionRequest;
import com.bigproject.modules.employee.dto.request.EmployeeDecisionRequest;
import com.bigproject.modules.employee.dto.request.ReplaceEmployeeDraftRequest;
import com.bigproject.modules.employee.dto.request.SubmitEmployeeRequest;
import com.bigproject.modules.employee.dto.response.EmployeeActivityResponse;
import com.bigproject.modules.employee.dto.response.EmployeeRequestDetailResponse;
import com.bigproject.modules.employee.dto.response.EmployeeRequestListItemResponse;
import com.bigproject.modules.employee.dto.response.EmployeeRevisionResponse;
import com.bigproject.modules.employee.dto.response.PageResponse;
import com.bigproject.modules.employee.entity.EmployeeRequest;
import com.bigproject.modules.employee.entity.EmployeeRequestState;
import com.bigproject.modules.employee.entity.EmployeeRequestType;
import com.bigproject.modules.employee.repository.EmployeeRepository;
import com.bigproject.modules.employee.repository.EmployeeRequestRepository;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class EmployeeRequestService {
  private static final Set<EmployeeRequestState> EDITABLE =
      Set.of(
          EmployeeRequestState.Draft, EmployeeRequestState.Rejected, EmployeeRequestState.Revoked);
  private final EmployeeRequestRepository requests;
  private final EmployeeRepository employees;
  private final EmployeeService profiles;
  private final Clock clock;

  public EmployeeRequestService(
      EmployeeRequestRepository requests,
      EmployeeRepository employees,
      EmployeeService profiles,
      Clock clock) {
    this.requests = requests;
    this.employees = employees;
    this.profiles = profiles;
    this.clock = clock;
  }

  public PageResponse<EmployeeRequestListItemResponse> list(
      long actorId,
      int page,
      int size,
      String type,
      String state,
      String search,
      boolean includeCompleted) {
    EmployeeQuery.page(page, size);
    EmployeeQuery.allowed(type, Set.of("", "Creation", "Update"), "invalid_employee_request_type");
    EmployeeQuery.allowed(
        state,
        Set.of("", "Draft", "PendingApproval", "Rejected", "Revoked", "Approved", "Cancelled"),
        "invalid_employee_request_state");
    return requests.list(
        page, size, actorId, type, state, EmployeeQuery.search(search), includeCompleted);
  }

  public EmployeeRequestDetailResponse detail(EmployeeRequestType type, long id, long actorId) {
    return publicDetail(read(type, id, actorId, false));
  }

  @Transactional
  public EmployeeRequestDetailResponse create(
      EmployeeRequestType type, CreateEmployeeDraftRequest body, long actorId) {
    Long subjectId = null;
    EmployeeProfile baseline = null;
    Long baselineVersion = null;
    EmployeeProfile target = body.target();
    if (type == EmployeeRequestType.Creation) {
      if (body.subjectId() != null) {
        throw error(HttpStatus.BAD_REQUEST, "creation_subject_id_must_be_absent");
      }
    } else {
      if (body.subjectId() == null) {
        throw error(HttpStatus.BAD_REQUEST, "employee_subject_id_required");
      }
      var employee = profiles.require(body.subjectId(), true);
      subjectId = employee.id();
      baseline = employee.profile();
      baselineVersion = employee.version();
      if (target == null) {
        target = baseline;
      }
    }
    long id =
        requests.create(
            type, subjectId, actorId, baselineVersion, baseline, target, clock.instant());
    requests.audit(id, null, "DraftCreated", actorId, null, clock.instant());
    return publicDetail(read(type, id, actorId, false));
  }

  @Transactional
  public EmployeeRequestDetailResponse createAndSubmit(
      EmployeeRequestType type, CreateAndSubmitEmployeeRequest body, long actorId) {
    EmployeeRequestDetailResponse draft =
        create(type, new CreateEmployeeDraftRequest(body.subjectId(), body.target()), actorId);
    return submit(
        type, draft.id(), new SubmitEmployeeRequest(draft.version(), null, body.reason()), actorId);
  }

  @Transactional
  public EmployeeRequestDetailResponse replace(
      EmployeeRequestType type, long id, ReplaceEmployeeDraftRequest body, long actorId) {
    EmployeeRequest request = owned(type, id, actorId, body.version());
    state(request, EDITABLE);
    if (type == EmployeeRequestType.Update && body.target() == null) {
      throw error(HttpStatus.BAD_REQUEST, "employee_target_required");
    }
    return save(
        request,
        EmployeeRequestState.Draft,
        request.subjectId(),
        body.target(),
        request.revisionNumber(),
        request.latestRevisionId(),
        request.reason(),
        "DraftSaved",
        actorId,
        null);
  }

  @Transactional
  public EmployeeRequestDetailResponse submit(
      EmployeeRequestType type, long id, SubmitEmployeeRequest body, long actorId) {
    EmployeeRequest request = owned(type, id, actorId, body.version());
    state(request, EDITABLE);
    EmployeeProfile target = body.target() == null ? request.target() : body.target();
    String reason = reason(body.reason());
    checkBaseline(request);
    profiles.validateTarget(
        target, request.baseline(), request.subjectId() == null ? 0 : request.subjectId());
    long revisionId = requests.freeze(request, target, reason, clock.instant());
    return save(
        request,
        EmployeeRequestState.PendingApproval,
        request.subjectId(),
        target,
        request.revisionNumber() + 1,
        revisionId,
        reason,
        "Submitted",
        actorId,
        reason);
  }

  @Transactional
  public EmployeeRequestDetailResponse revoke(
      EmployeeRequestType type, long id, EmployeeActionRequest body, long actorId) {
    EmployeeRequest request = owned(type, id, actorId, body.version());
    state(request, Set.of(EmployeeRequestState.PendingApproval));
    return save(
        request,
        EmployeeRequestState.Revoked,
        request.subjectId(),
        request.target(),
        request.revisionNumber(),
        request.latestRevisionId(),
        request.reason(),
        "Revoked",
        actorId,
        clean(body.reason()));
  }

  @Transactional
  public EmployeeRequestDetailResponse cancel(
      EmployeeRequestType type, long id, EmployeeActionRequest body, long actorId) {
    EmployeeRequest request = owned(type, id, actorId, body.version());
    state(
        request,
        Set.of(
            EmployeeRequestState.PendingApproval,
            EmployeeRequestState.Rejected,
            EmployeeRequestState.Revoked));
    return save(
        request,
        EmployeeRequestState.Cancelled,
        request.subjectId(),
        request.target(),
        request.revisionNumber(),
        request.latestRevisionId(),
        request.reason(),
        "Cancelled",
        actorId,
        reason(body.reason()));
  }

  @Transactional
  public EmployeeRequestDetailResponse decide(
      EmployeeRequestType type,
      long id,
      EmployeeDecisionRequest body,
      long actorId,
      boolean approve) {
    EmployeeRequest request = read(type, id, actorId, true);
    version(request, body.version());
    state(request, Set.of(EmployeeRequestState.PendingApproval));
    if (request.requesterId() == actorId) {
      throw error(HttpStatus.FORBIDDEN, "employee_request_self_approval_forbidden");
    }
    if (!Objects.equals(request.latestRevisionId(), body.revisionId())) {
      throw error(HttpStatus.CONFLICT, "employee_request_revision_conflict");
    }
    EmployeeRevisionResponse revision = requests.revision(id, body.revisionId()).orElseThrow();
    String comment = approve ? clean(body.reason()) : reason(body.reason());
    Long subjectId = request.subjectId();
    if (approve) {
      checkBaseline(request);
      profiles.validateTarget(
          revision.target(), revision.baseline(), subjectId == null ? 0 : subjectId);
      try {
        if (type == EmployeeRequestType.Creation) {
          subjectId = employees.create(revision.target(), clock.instant());
        } else {
          employees.update(subjectId, revision.target(), clock.instant());
        }
      } catch (DuplicateKeyException ex) {
        throw error(HttpStatus.CONFLICT, "employee_email_exists");
      }
    }
    return save(
        request,
        approve ? EmployeeRequestState.Approved : EmployeeRequestState.Rejected,
        subjectId,
        revision.target(),
        request.revisionNumber(),
        revision.id(),
        revision.reason(),
        approve ? "Approved" : "Rejected",
        actorId,
        comment);
  }

  @Transactional
  public void delete(EmployeeRequestType type, long id, long version, long actorId) {
    EmployeeRequest request = owned(type, id, actorId, version);
    state(
        request,
        Set.of(
            EmployeeRequestState.Draft,
            EmployeeRequestState.Rejected,
            EmployeeRequestState.Revoked,
            EmployeeRequestState.Cancelled));
    requests.audit(id, request.latestRevisionId(), "Deleted", actorId, null, clock.instant());
    requests.delete(id, clock.instant());
  }

  public List<EmployeeRevisionResponse> revisions(EmployeeRequestType type, long id, long actorId) {
    read(type, id, actorId, false);
    return requests.revisions(id);
  }

  public EmployeeRevisionResponse revision(
      EmployeeRequestType type, long id, long revisionId, long actorId) {
    read(type, id, actorId, false);
    return requests
        .revision(id, revisionId)
        .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "employee_revision_not_found"));
  }

  public PageResponse<EmployeeActivityResponse> activities(
      EmployeeRequestType type, long id, long actorId, int page, int size) {
    EmployeeQuery.page(page, size);
    read(type, id, actorId, false);
    return requests.activities(id, page, size);
  }

  private EmployeeRequest read(EmployeeRequestType type, long id, long actorId, boolean lock) {
    EmployeeRequest request =
        requests
            .find(id, lock)
            .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "employee_request_not_found"));
    if (request.type() != type
        || request.state() == EmployeeRequestState.Draft && request.requesterId() != actorId) {
      throw error(HttpStatus.NOT_FOUND, "employee_request_not_found");
    }
    return request;
  }

  private EmployeeRequest owned(
      EmployeeRequestType type, long id, long actorId, long expectedVersion) {
    EmployeeRequest request = read(type, id, actorId, true);
    if (request.requesterId() != actorId) {
      throw error(HttpStatus.FORBIDDEN, "employee_request_owner_required");
    }
    version(request, expectedVersion);
    return request;
  }

  private void checkBaseline(EmployeeRequest request) {
    if (request.type() == EmployeeRequestType.Update) {
      var employee = profiles.require(request.subjectId(), true);
      if (!Objects.equals(request.baselineVersion(), employee.version())) {
        throw error(HttpStatus.CONFLICT, "employee_profile_changed_create_new_request");
      }
    }
  }

  private EmployeeRequestDetailResponse save(
      EmployeeRequest previous,
      EmployeeRequestState state,
      Long subjectId,
      EmployeeProfile target,
      int revisionNumber,
      Long revisionId,
      String reason,
      String action,
      long actorId,
      String comment) {
    EmployeeRequest next =
        new EmployeeRequest(
            previous.id(),
            previous.type(),
            state,
            subjectId,
            previous.requesterId(),
            previous.baselineVersion(),
            previous.baseline(),
            target,
            previous.version() + 1,
            revisionNumber,
            revisionId,
            reason,
            previous.createdAtUtc(),
            clock.instant());
    requests.save(next);
    requests.audit(next.id(), revisionId, action, actorId, comment, clock.instant());
    return publicDetail(next);
  }

  private EmployeeRequestDetailResponse publicDetail(EmployeeRequest request) {
    var subjectId =
        request.subjectId() == null
            ? null
            : profiles.require(request.subjectId(), false).publicId();
    return new EmployeeRequestDetailResponse(
        request.id(),
        request.type(),
        request.state(),
        subjectId,
        request.requesterId(),
        request.baselineVersion(),
        request.baseline(),
        request.target(),
        request.version(),
        request.revisionNumber(),
        request.latestRevisionId(),
        request.reason(),
        request.createdAtUtc(),
        request.lastModifiedAtUtc());
  }

  private static void version(EmployeeRequest request, long version) {
    if (request.version() != version) {
      throw error(HttpStatus.CONFLICT, "employee_request_version_conflict");
    }
  }

  private static void state(EmployeeRequest request, Set<EmployeeRequestState> allowed) {
    if (!allowed.contains(request.state())) {
      throw error(HttpStatus.CONFLICT, "employee_request_action_not_allowed");
    }
  }

  private static String reason(String value) {
    String reason = clean(value);
    if (reason == null) {
      throw error(HttpStatus.BAD_REQUEST, "employee_request_reason_required");
    }
    return reason;
  }

  private static String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static ResponseStatusException error(HttpStatus status, String message) {
    return new ResponseStatusException(status, message);
  }
}
