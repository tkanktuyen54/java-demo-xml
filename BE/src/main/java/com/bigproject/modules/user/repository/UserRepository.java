package com.bigproject.modules.user.repository;

import com.bigproject.modules.user.entity.Account;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
  private final JdbcTemplate jdbc;
  private static final String PUBLIC_COLUMNS =
      "id, username, first_name, last_name, email, date_of_birth, gender, phone";

  public UserRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> findAllUsers() {
    return jdbc.queryForList("SELECT " + PUBLIC_COLUMNS + " FROM tbl_user ORDER BY id");
  }

  public List<Map<String, Object>> findAllAddresses() {
    return jdbc.queryForList("SELECT * FROM tbl_address ORDER BY id");
  }

  public Optional<Account> findByEmail(String email) {
    return jdbc
        .query(
            "SELECT id, email, password, first_name, last_name FROM tbl_user WHERE email = ?",
            (rs, row) ->
                new Account(
                    rs.getLong("id"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("first_name"),
                    rs.getString("last_name")),
            email)
        .stream()
        .findFirst();
  }

  public Optional<Account> findById(long id) {
    return jdbc
        .query(
            "SELECT id, email, password, first_name, last_name FROM tbl_user WHERE id = ?",
            (rs, row) ->
                new Account(
                    rs.getLong("id"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("first_name"),
                    rs.getString("last_name")),
            id)
        .stream()
        .findFirst();
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
    GeneratedKeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        connection -> {
          PreparedStatement statement =
              connection.prepareStatement(
                  """
                INSERT INTO tbl_user (username, password, first_name, last_name, email, date_of_birth, gender, phone)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                  new String[] {"id"});
          statement.setString(1, username);
          statement.setString(2, password);
          statement.setString(3, firstName);
          statement.setString(4, lastName);
          statement.setString(5, email);
          statement.setObject(6, dateOfBirth);
          statement.setString(7, gender);
          statement.setString(8, phone);
          return statement;
        },
        keys);
    Number id = keys.getKey();
    if (id == null) {
      throw new IllegalStateException("Missing generated user ID");
    }
    return jdbc.queryForMap(
        "SELECT " + PUBLIC_COLUMNS + " FROM tbl_user WHERE id = ?", id.longValue());
  }
}
