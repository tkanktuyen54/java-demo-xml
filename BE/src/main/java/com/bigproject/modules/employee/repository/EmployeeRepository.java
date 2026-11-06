package com.bigproject.modules.employee.repository;

import com.bigproject.modules.employee.dto.EmployeeProfile;
import com.bigproject.modules.employee.dto.response.EmployeeListItemResponse;
import com.bigproject.modules.employee.dto.response.PageResponse;
import com.bigproject.modules.employee.entity.Employee;
import com.bigproject.modules.employee.entity.EmployeeStatus;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class EmployeeRepository {
  private static final Map<String, String> SORT_COLUMNS =
      Map.of(
          "employeeCode",
          "e.employee_code",
          "fullName",
          "e.full_name",
          "department",
          "e.department",
          "position",
          "e.position",
          "status",
          "e.status",
          "createdAtUtc",
          "e.created_at");
  private static final String FILTER =
      """
      FROM employee e
      WHERE (LOWER(e.employee_code) LIKE ? ESCAPE '!' OR LOWER(e.full_name) LIKE ? ESCAPE '!'
        OR EXISTS (SELECT 1 FROM employee_contact c WHERE c.employee_id = e.id
          AND (LOWER(c.email) LIKE ? ESCAPE '!' OR LOWER(c.phone_number) LIKE ? ESCAPE '!')))
        AND (? = '' OR e.status = ?) AND (? = '' OR e.department = ?)
      """;
  private final JdbcTemplate jdbc;
  private final EmployeeProfileJson json;

  public EmployeeRepository(JdbcTemplate jdbc, EmployeeProfileJson json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public PageResponse<EmployeeListItemResponse> list(
      int page,
      int size,
      String search,
      String status,
      String department,
      String sortBy,
      String direction) {
    Object[] args = {search, search, search, search, status, status, department, department};
    long count = jdbc.queryForObject("SELECT COUNT(*) " + FILTER, Long.class, args);
    String order =
        SORT_COLUMNS.get(sortBy) + " " + direction.toUpperCase(Locale.ROOT) + ", e.id ASC";
    var values = new java.util.ArrayList<>(java.util.Arrays.asList(args));
    values.add(size);
    values.add(((long) page - 1) * size);
    var items =
        jdbc.query(
            "SELECT e.* " + FILTER + " ORDER BY " + order + " LIMIT ? OFFSET ?",
            (rs, row) -> {
              EmployeeProfile profile = json.read(rs.getString("profile_json"));
              var contact =
                  profile.contacts().stream()
                      .sorted(
                          java.util.Comparator.comparing(EmployeeProfile.Contact::isPrimary)
                              .reversed()
                              .thenComparing(EmployeeProfile.Contact::type))
                      .findFirst()
                      .orElse(null);
              return new EmployeeListItemResponse(
                  UUID.fromString(rs.getString("public_id")),
                  rs.getString("employee_code"),
                  rs.getString("full_name"),
                  profile.avatar(),
                  rs.getString("department"),
                  rs.getString("position"),
                  contact == null ? null : contact.phoneNumber(),
                  contact == null ? null : contact.email(),
                  EmployeeStatus.valueOf(rs.getString("status")),
                  rs.getLong("version"));
            },
            values.toArray());
    return PageResponse.of(items, count, page, size);
  }

  public Optional<Employee> find(long id, boolean lock) {
    return jdbc
        .query(
            "SELECT * FROM employee WHERE id = ?" + (lock ? " FOR UPDATE" : ""),
            this::mapEmployee,
            id)
        .stream()
        .findFirst();
  }

  public Optional<Employee> findByPublicId(UUID publicId, boolean lock) {
    return jdbc
        .query(
            "SELECT * FROM employee WHERE public_id = ?" + (lock ? " FOR UPDATE" : ""),
            this::mapEmployee,
            publicId.toString())
        .stream()
        .findFirst();
  }

  private Employee mapEmployee(ResultSet rs, int row) throws SQLException {
    return new Employee(
        rs.getLong("id"),
        UUID.fromString(rs.getString("public_id")),
        rs.getString("employee_code"),
        EmployeeStatus.valueOf(rs.getString("status")),
        json.read(rs.getString("profile_json")),
        rs.getLong("version"),
        rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("last_modified_at") == null
            ? null
            : rs.getTimestamp("last_modified_at").toInstant());
  }

  public long create(EmployeeProfile profile, Instant now) {
    GeneratedKeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
          INSERT INTO employee(public_id, full_name, department, position, profile_json, created_at)
          VALUES (?, ?, ?, ?, ?, ?)
          """,
                  new String[] {"id"});
          statement.setString(1, UUID.randomUUID().toString());
          statement.setString(2, profile.fullName());
          statement.setString(3, profile.assignment().department());
          statement.setString(4, profile.assignment().position());
          statement.setString(5, json.write(profile));
          statement.setTimestamp(6, Timestamp.from(now));
          return statement;
        },
        keys);
    long id = java.util.Objects.requireNonNull(keys.getKey()).longValue();
    jdbc.update("UPDATE employee SET employee_code = ? WHERE id = ?", "EMP-%06d".formatted(id), id);
    replaceContacts(id, profile);
    return id;
  }

  public void update(long id, EmployeeProfile profile, Instant now) {
    jdbc.update(
        """
        UPDATE employee SET full_name = ?, department = ?, position = ?, profile_json = ?,
          version = version + 1, last_modified_at = ? WHERE id = ?
        """,
        profile.fullName(),
        profile.assignment().department(),
        profile.assignment().position(),
        json.write(profile),
        Timestamp.from(now),
        id);
    replaceContacts(id, profile);
  }

  public boolean emailExists(String email, long exceptEmployeeId) {
    return jdbc.queryForObject(
            "SELECT COUNT(*) FROM employee_contact WHERE email = ? AND employee_id <> ?",
            Long.class,
            email,
            exceptEmployeeId)
        > 0;
  }

  private void replaceContacts(long id, EmployeeProfile profile) {
    jdbc.update("DELETE FROM employee_contact WHERE employee_id = ?", id);
    for (var contact : profile.contacts()) {
      jdbc.update(
          """
          INSERT INTO employee_contact(employee_id, type, email, phone_number, is_primary)
          VALUES (?, ?, ?, ?, ?)
          """,
          id,
          contact.type().name(),
          contact.email(),
          contact.phoneNumber(),
          contact.isPrimary());
    }
  }
}
