package com.bigproject.modules.employee.repository;

import com.bigproject.modules.employee.dto.EmployeeDtos.Page;
import com.bigproject.modules.employee.dto.EmployeeProfile;
import com.bigproject.modules.employee.dto.EmployeeRequestDtos.*;
import com.bigproject.modules.employee.entity.EmployeeRequest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeRequestRepository {
  private final JdbcTemplate jdbc;
  private final EmployeeProfileJson json;

  public EmployeeRequestRepository(JdbcTemplate jdbc, EmployeeProfileJson json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public long create(
      Type type,
      Long subjectId,
      long requesterId,
      Long baselineVersion,
      EmployeeProfile baseline,
      EmployeeProfile target,
      Instant now) {
    GeneratedKeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
          INSERT INTO employee_request(type, subject_id, requester_id, baseline_version,
            baseline_json, target_json, target_name, created_at, last_modified_at)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
          """,
                  new String[] {"id"});
          statement.setString(1, type.name());
          statement.setObject(2, subjectId);
          statement.setLong(3, requesterId);
          statement.setObject(4, baselineVersion);
          statement.setString(5, json.write(baseline));
          statement.setString(6, json.write(target));
          statement.setString(7, target == null ? null : target.fullName());
          statement.setTimestamp(8, Timestamp.from(now));
          statement.setTimestamp(9, Timestamp.from(now));
          return statement;
        },
        keys);
    return Objects.requireNonNull(keys.getKey()).longValue();
  }

  public Optional<EmployeeRequest> find(long id, boolean lock) {
    return jdbc
        .query(
            "SELECT * FROM employee_request WHERE id = ? AND deleted = FALSE"
                + (lock ? " FOR UPDATE" : ""),
            this::detail,
            id)
        .stream()
        .findFirst();
  }

  public void save(EmployeeRequest next) {
    int changed =
        jdbc.update(
            """
        UPDATE employee_request SET state = ?, subject_id = ?, target_json = ?, version = ?,
          revision_number = ?, latest_revision_id = ?, reason = ?, last_modified_at = ?, target_name = ?
        WHERE id = ? AND version = ? AND deleted = FALSE
        """,
            next.state().name(),
            next.subjectId(),
            json.write(next.target()),
            next.version(),
            next.revisionNumber(),
            next.latestRevisionId(),
            next.reason(),
            Timestamp.from(next.lastModifiedAtUtc()),
            next.target() == null ? null : next.target().fullName(),
            next.id(),
            next.version() - 1);
    if (changed != 1) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.CONFLICT, "employee_request_version_conflict");
    }
  }

  public long freeze(EmployeeRequest request, EmployeeProfile target, String reason, Instant now) {
    GeneratedKeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
          INSERT INTO employee_request_revision(request_id, revision_number, baseline_version,
            baseline_json, target_json, reason, submitted_by, submitted_at)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?)
          """,
                  new String[] {"id"});
          statement.setLong(1, request.id());
          statement.setInt(2, request.revisionNumber() + 1);
          statement.setObject(3, request.baselineVersion());
          statement.setString(4, json.write(request.baseline()));
          statement.setString(5, json.write(target));
          statement.setString(6, reason);
          statement.setLong(7, request.requesterId());
          statement.setTimestamp(8, Timestamp.from(now));
          return statement;
        },
        keys);
    return Objects.requireNonNull(keys.getKey()).longValue();
  }

  public List<Revision> revisions(long requestId) {
    return jdbc.query(
        "SELECT * FROM employee_request_revision WHERE request_id = ? ORDER BY revision_number",
        this::revision,
        requestId);
  }

  public Optional<Revision> revision(long requestId, long revisionId) {
    return jdbc
        .query(
            "SELECT * FROM employee_request_revision WHERE request_id = ? AND id = ?",
            this::revision,
            requestId,
            revisionId)
        .stream()
        .findFirst();
  }

  public Page<ListItem> list(
      int page,
      int size,
      long actorId,
      String type,
      String state,
      String search,
      boolean includeCompleted) {
    String from =
        """
        FROM employee_request r LEFT JOIN employee e ON e.id = r.subject_id
        WHERE r.deleted = FALSE AND (r.state <> 'Draft' OR r.requester_id = ?)
          AND (? = TRUE OR r.state <> 'Approved') AND (? = '' OR r.type = ?)
          AND (? = '' OR r.state = ?)
          AND (LOWER(COALESCE(r.target_name, '')) LIKE ? ESCAPE '!'
            OR LOWER(COALESCE(e.employee_code, '')) LIKE ? ESCAPE '!')
        """;
    Object[] args = {actorId, includeCompleted, type, type, state, state, search, search};
    long count = jdbc.queryForObject("SELECT COUNT(*) " + from, Long.class, args);
    var values = new java.util.ArrayList<>(java.util.Arrays.asList(args));
    values.add(size);
    values.add(((long) page - 1) * size);
    var items =
        jdbc.query(
            "SELECT r.*, e.employee_code, e.public_id AS subject_public_id " + from + " ORDER BY r.id DESC LIMIT ? OFFSET ?",
            (rs, row) ->
                new ListItem(
                    rs.getLong("id"),
                    Type.valueOf(rs.getString("type")),
                    State.valueOf(rs.getString("state")),
                    rs.getString("subject_public_id") == null ? null : UUID.fromString(rs.getString("subject_public_id")),
                    rs.getString("employee_code"),
                    rs.getString("target_name"),
                    rs.getLong("requester_id"),
                    rs.getLong("version"),
                    rs.getObject("latest_revision_id", Long.class),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("last_modified_at").toInstant()),
            values.toArray());
    return Page.of(items, count, page, size);
  }

  public void audit(
      long requestId, Long revisionId, String action, long actorId, String reason, Instant now) {
    jdbc.update(
        """
        INSERT INTO employee_request_activity(request_id, revision_id, action, actor_id, reason, occurred_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        requestId,
        revisionId,
        action,
        actorId,
        reason,
        Timestamp.from(now));
  }

  public Page<Activity> activities(long requestId, int page, int size) {
    long count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM employee_request_activity WHERE request_id = ?",
            Long.class,
            requestId);
    var items =
        jdbc.query(
            """
        SELECT * FROM employee_request_activity WHERE request_id = ? ORDER BY id DESC LIMIT ? OFFSET ?
        """,
            (rs, row) ->
                new Activity(
                    rs.getLong("id"),
                    rs.getLong("request_id"),
                    rs.getObject("revision_id", Long.class),
                    rs.getString("action"),
                    rs.getLong("actor_id"),
                    rs.getString("reason"),
                    rs.getTimestamp("occurred_at").toInstant()),
            requestId,
            size,
            ((long) page - 1) * size);
    return Page.of(items, count, page, size);
  }

  public void delete(long id, Instant now) {
    jdbc.update(
        "UPDATE employee_request SET deleted = TRUE, version = version + 1, last_modified_at = ? WHERE id = ?",
        Timestamp.from(now),
        id);
  }

  private EmployeeRequest detail(ResultSet rs, int row) throws SQLException {
    return new EmployeeRequest(
        rs.getLong("id"),
        Type.valueOf(rs.getString("type")),
        State.valueOf(rs.getString("state")),
        rs.getObject("subject_id", Long.class),
        rs.getLong("requester_id"),
        rs.getObject("baseline_version", Long.class),
        json.read(rs.getString("baseline_json")),
        json.read(rs.getString("target_json")),
        rs.getLong("version"),
        rs.getInt("revision_number"),
        rs.getObject("latest_revision_id", Long.class),
        rs.getString("reason"),
        rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("last_modified_at").toInstant());
  }

  private Revision revision(ResultSet rs, int row) throws SQLException {
    return new Revision(
        rs.getLong("id"),
        rs.getLong("request_id"),
        rs.getInt("revision_number"),
        rs.getObject("baseline_version", Long.class),
        json.read(rs.getString("baseline_json")),
        json.read(rs.getString("target_json")),
        rs.getString("reason"),
        rs.getLong("submitted_by"),
        rs.getTimestamp("submitted_at").toInstant());
  }
}
