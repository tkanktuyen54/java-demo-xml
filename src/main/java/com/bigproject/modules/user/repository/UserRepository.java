package com.bigproject.modules.user.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

	private final JdbcTemplate jdbcTemplate;

	public UserRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<Map<String, Object>> findAllUsers() {
		return jdbcTemplate.queryForList("SELECT * FROM tbl_user ORDER BY id");
	}

	public List<Map<String, Object>> findAllAddresses() {
		return jdbcTemplate.queryForList("SELECT * FROM tbl_address ORDER BY id");
	}

	public Map<String, Object> insertUser(
			String username,
			String password,
			String firstName,
			String lastName,
			String email,
			LocalDate dateOfBirth,
			String gender,
			String phone) {
		return jdbcTemplate.queryForObject(
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
				dateOfBirth,
				gender,
				phone);
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

}
