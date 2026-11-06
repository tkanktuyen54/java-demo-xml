package com.bigproject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class EmployeeUuidMigrationTests {
  @Test
  void backfillsExistingEmployeeWithoutChangingKeysProfileOrWorkflowHistory() {
    var dataSource =
        new DriverManagerDataSource(
            "jdbc:h2:mem:employee-uuid-"
                + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
            "sa",
            "");
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .target("4")
        .load()
        .migrate();
    var jdbc = new JdbcTemplate(dataSource);
    jdbc.update(
        """
        INSERT INTO employee(employee_code, full_name, department, position, profile_json, version, created_at)
        VALUES ('EMP-000001', 'Legacy employee', 'Operations', 'Driver', '{}', 7, CURRENT_TIMESTAMP)
        """);
    long employeeId =
        jdbc.queryForObject(
            "SELECT id FROM employee WHERE employee_code = 'EMP-000001'", Long.class);
    jdbc.update(
        """
        INSERT INTO employee_contact(employee_id, type, email, is_primary)
        VALUES (?, 'Work', 'legacy@employee.test', TRUE)
        """,
        employeeId);
    jdbc.update(
        """
        INSERT INTO tbl_user(username, password, first_name, last_name, email)
        VALUES ('legacy-user', 'test-hash', 'Legacy', 'User', 'legacy@user.test')
        """);
    long userId =
        jdbc.queryForObject("SELECT id FROM tbl_user WHERE username = 'legacy-user'", Long.class);
    jdbc.update(
        """
        INSERT INTO employee_request(type, subject_id, requester_id, baseline_version, created_at, last_modified_at)
        VALUES ('Update', ?, ?, 7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        """,
        employeeId,
        userId);
    long requestId =
        jdbc.queryForObject(
            "SELECT id FROM employee_request WHERE subject_id = ?", Long.class, employeeId);
    jdbc.update(
        """
        INSERT INTO employee_request_activity(request_id, action, actor_id, occurred_at)
        VALUES (?, 'DraftCreated', ?, CURRENT_TIMESTAMP)
        """,
        requestId,
        userId);

    var flyway =
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    String publicId =
        jdbc.queryForObject(
            "SELECT public_id FROM employee WHERE id = ?", String.class, employeeId);
    assertThat(UUID.fromString(publicId).toString()).isEqualTo(publicId);
    assertThat(
            jdbc.queryForObject(
                "SELECT id FROM employee WHERE public_id = ?", Long.class, publicId))
        .isEqualTo(employeeId);
    assertThat(
            jdbc.queryForObject(
                "SELECT version FROM employee WHERE public_id = ?", Long.class, publicId))
        .isEqualTo(7);
    assertThat(
            jdbc.queryForObject(
                "SELECT profile_json FROM employee WHERE id = ?", String.class, employeeId))
        .isEqualTo("{}");
    assertThat(jdbc.queryForObject("SELECT employee_id FROM employee_contact", Long.class))
        .isEqualTo(employeeId);
    assertThat(
            jdbc.queryForObject(
                "SELECT subject_id FROM employee_request WHERE id = ?", Long.class, requestId))
        .isEqualTo(employeeId);
    assertThat(jdbc.queryForObject("SELECT request_id FROM employee_request_activity", Long.class))
        .isEqualTo(requestId);
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT public_id FROM employee WHERE id = ?", String.class, employeeId))
        .isEqualTo(publicId);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
        INSERT INTO employee(employee_code, full_name, department, position, profile_json, created_at)
        VALUES ('MISSING-UUID', 'Missing UUID', 'Operations', 'Driver', '{}', CURRENT_TIMESTAMP)
        """))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
        INSERT INTO employee(public_id, employee_code, full_name, department, position, profile_json, created_at)
        VALUES (?, 'DUPLICATE-UUID', 'Duplicate UUID', 'Operations', 'Driver', '{}', CURRENT_TIMESTAMP)
        """,
                    publicId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
