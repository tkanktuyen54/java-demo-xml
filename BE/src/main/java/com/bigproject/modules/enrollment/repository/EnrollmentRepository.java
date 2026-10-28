package com.bigproject.modules.enrollment.repository;

import com.bigproject.modules.enrollment.dto.Enrollment;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EnrollmentRepository {
  private final JdbcTemplate jdbc;

  public EnrollmentRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public boolean exists(long userId, long courseId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM course_order WHERE user_id = ? AND course_id = ? AND status IN ('DEMO', 'PAID')",
            Integer.class,
            userId,
            courseId);
    return count != null && count > 0;
  }

  public void create(long userId, long courseId, BigDecimal amount) {
    // A unique (user_id, course_id) constraint also protects simultaneous checkout requests.
    jdbc.update(
        "INSERT INTO course_order (user_id, course_id, amount, status) VALUES (?, ?, ?, 'DEMO')",
        userId,
        courseId,
        amount);
  }

  public List<Enrollment> findByUser(long userId) {
    return jdbc.query(
        """
            SELECT o.id, o.amount, o.status, c.slug, c.title, c.instructor, c.cover_key,
                (SELECT COUNT(*) FROM lesson l WHERE l.course_id = c.id) AS lesson_count,
                (SELECT COUNT(*) FROM lesson_progress p JOIN lesson l ON l.id = p.lesson_id
                    WHERE p.user_id = o.user_id AND l.course_id = c.id) AS completed_lessons
            FROM course_order o JOIN course c ON c.id = o.course_id
            WHERE o.user_id = ? AND o.status IN ('DEMO', 'PAID') ORDER BY o.created_at DESC, o.id DESC
            """,
        (rs, row) ->
            new Enrollment(
                rs.getLong("id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("instructor"),
                rs.getString("cover_key"),
                rs.getInt("lesson_count"),
                rs.getInt("completed_lessons"),
                rs.getBigDecimal("amount"),
                rs.getString("status")),
        userId);
  }

  public void lockUser(long userId) {
    jdbc.queryForObject("SELECT id FROM tbl_user WHERE id = ? FOR UPDATE", Long.class, userId);
  }
}
