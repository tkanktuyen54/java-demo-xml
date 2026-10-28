package com.bigproject.modules.learning.repository;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProgressRepository {
  private final JdbcTemplate jdbc;

  public ProgressRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Long> completed(long userId, long courseId) {
    return jdbc.queryForList(
        "SELECT p.lesson_id FROM lesson_progress p JOIN lesson l ON l.id = p.lesson_id WHERE p.user_id = ? AND l.course_id = ?",
        Long.class,
        userId,
        courseId);
  }

  public void complete(long userId, long lessonId) {
    // The caller locks the user's row, serializing repeated completion requests.
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM lesson_progress WHERE user_id = ? AND lesson_id = ?",
            Integer.class,
            userId,
            lessonId);
    if (count == null || count == 0) {
      jdbc.update(
          "INSERT INTO lesson_progress (user_id, lesson_id) VALUES (?, ?)", userId, lessonId);
    }
  }
}
