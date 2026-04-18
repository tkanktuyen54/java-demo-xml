package com.example.java_demo_xml;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserDataController {

	private final JdbcTemplate jdbcTemplate;

	public UserDataController(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	// GET /api/tbl-user — đọc toàn bộ bảng tbl_user
	@GetMapping("/tbl-user")
	public ResponseEntity<?> listUsers() {
		try {
			List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM tbl_user ORDER BY id");
			return ResponseEntity.ok(rows);
		} catch (CannotGetJdbcConnectionException ex) {
			return databaseUnavailable(ex);
		} catch (DataAccessException ex) {
			return databaseQueryFailed(ex);
		}
	}

	// POST /api/tbl-user — thêm user (JSON body → CreateUserRequest)
	@PostMapping("/tbl-user")
	public ResponseEntity<?> createUser(@RequestBody CreateUserRequest body) {
		// Kiểm tra bắt buộc: họ tên + password (DB NOT NULL)
		if (body == null || body.firstName() == null || body.firstName().isBlank()
				|| body.lastName() == null || body.lastName().isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "first_name_and_last_name_required"));
		}
		if (body.password() == null || body.password().isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "password_required"));
		}
		String firstName = body.firstName().trim();
		String lastName = body.lastName().trim();
		String password = body.password().trim();
		String email = body.email() != null && !body.email().isBlank() ? body.email().trim() : null;
		String gender = body.gender() != null && !body.gender().isBlank() ? body.gender().trim() : null;
		String phone = body.phone() != null && !body.phone().isBlank() ? body.phone().trim() : null;
		String username = resolveUsername(body.username(), email, firstName, lastName);
		try {
			// INSERT một dòng; RETURNING không gồm password trong JSON trả về
			Map<String, Object> row = jdbcTemplate.queryForObject(
					"""
							INSERT INTO tbl_user (username, password, first_name, last_name, email, date_of_birth, gender, phone)
							VALUES (?, ?, ?, ?, ?, ?, ?, ?)
							RETURNING id, username, first_name, last_name, email, date_of_birth, gender, phone
							""",
					(rs, rowNum) -> mapUserRow(rs),
					username,
					password,
					firstName,
					lastName,
					email,
					body.dateOfBirth(),
					gender,
					phone);
			return ResponseEntity.status(HttpStatus.CREATED).body(row);
		} catch (CannotGetJdbcConnectionException ex) {
			return databaseUnavailable(ex);
		} catch (DataAccessException ex) {
			return databaseQueryFailed(ex);
		}
	}

	// GET /api/tbl-address — đọc toàn bộ bảng tbl_address
	@GetMapping({ "/tbl-address" })
	public ResponseEntity<?> listAddresses() {
		try {
			List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM tbl_address ORDER BY id");
			return ResponseEntity.ok(rows);
		} catch (CannotGetJdbcConnectionException ex) {
			return databaseUnavailable(ex);
		} catch (DataAccessException ex) {
			return databaseQueryFailed(ex);
		}
	}

	static String resolveUsername(String explicit, String email, String firstName, String lastName) {
		if (explicit != null && !explicit.isBlank()) {
			return explicit.trim();
		}
		if (email != null && email.contains("@")) {
			return email.substring(0, email.indexOf('@')).trim();
		}
		String slug = (firstName + "_" + lastName).replaceAll("[^a-zA-Z0-9._-]", "").toLowerCase();
		if (!slug.isBlank()) {
			return slug;
		}
		return "user_" + System.currentTimeMillis();
	}

	private static Map<String, Object> mapUserRow(ResultSet rs) throws SQLException {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", rs.getLong("id"));
		m.put("username", rs.getObject("username"));
		m.put("first_name", rs.getString("first_name"));
		m.put("last_name", rs.getString("last_name"));
		m.put("email", rs.getObject("email"));
		m.put("date_of_birth", rs.getObject("date_of_birth"));
		m.put("gender", rs.getObject("gender"));
		m.put("phone", rs.getObject("phone"));
		return m;
	}

	// Không kết nối được DB → thường 503
	private static ResponseEntity<Map<String, Object>> databaseUnavailable(CannotGetJdbcConnectionException ex) {
		Throwable root = ex.getMostSpecificCause();
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
				"ok", false,
				"error", "database_unreachable",
				"cause", root.getClass().getSimpleName() + ": " + root.getMessage()));
	}

	// Lỗi SQL / DB khác → 500 + chi tiết
	private static ResponseEntity<Map<String, Object>> databaseQueryFailed(DataAccessException ex) {
		Throwable root = ex.getMostSpecificCause();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
				"ok", false,
				"error", "database_error",
				"cause", root.getClass().getSimpleName() + ": " + root.getMessage()));
	}

}
