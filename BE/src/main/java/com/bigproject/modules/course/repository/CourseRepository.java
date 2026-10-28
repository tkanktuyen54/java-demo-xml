package com.bigproject.modules.course.repository;

import com.bigproject.modules.course.dto.LessonAccess;
import com.bigproject.modules.course.dto.LessonSummary;
import com.bigproject.modules.course.dto.VideoLesson;
import com.bigproject.modules.course.entity.Course;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CourseRepository {
  private final JdbcTemplate jdbc;
  private static final String SELECT =
      "SELECT c.*, (SELECT COUNT(*) FROM lesson l WHERE l.course_id = c.id) AS lesson_count FROM course c ";
  private static final RowMapper<Course> MAPPER =
      (rs, row) ->
          new Course(
              rs.getLong("id"),
              rs.getString("slug"),
              rs.getString("title"),
              rs.getString("subtitle"),
              rs.getString("description"),
              rs.getString("category"),
              rs.getString("level"),
              rs.getString("instructor"),
              rs.getString("instructor_title"),
              rs.getBigDecimal("price"),
              rs.getBigDecimal("original_price"),
              rs.getDouble("hours"),
              rs.getDouble("rating"),
              rs.getInt("student_count"),
              rs.getString("cover_key"),
              rs.getString("badge"),
              Arrays.asList(rs.getString("outcomes").split("\\|")),
              rs.getInt("lesson_count"));

  public CourseRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Course> findAll(String query, String category) {
    String term =
        "%"
            + query
                .toLowerCase(java.util.Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
            + "%";
    return jdbc.query(
        SELECT
            + """
            WHERE (LOWER(c.title) LIKE ? ESCAPE '!' OR LOWER(c.instructor) LIKE ? ESCAPE '!')
            AND (? = '' OR c.category = ?) ORDER BY c.id
            """,
        MAPPER,
        term,
        term,
        category,
        category);
  }

  public Optional<Course> findBySlug(String slug) {
    return jdbc.query(SELECT + "WHERE c.slug = ?", MAPPER, slug).stream().findFirst();
  }

  public List<LessonSummary> lessons(long courseId) {
    return jdbc.query(
        "SELECT id, title, chapter, position, duration_minutes, preview FROM lesson WHERE course_id = ? ORDER BY position",
        (rs, row) ->
            new LessonSummary(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("chapter"),
                rs.getInt("position"),
                rs.getInt("duration_minutes"),
                rs.getBoolean("preview")),
        courseId);
  }

  public Optional<VideoLesson> preview(long courseId) {
    return jdbc
        .query(
            "SELECT id, title, duration_minutes, video_url FROM lesson WHERE course_id = ? AND preview = TRUE ORDER BY position LIMIT 1",
            (rs, row) ->
                new VideoLesson(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getInt("duration_minutes"),
                    rs.getString("video_url"),
                    true),
            courseId)
        .stream()
        .findFirst();
  }

  public Optional<VideoLesson> video(long courseId, long lessonId) {
    return jdbc
        .query(
            "SELECT id, title, duration_minutes, video_url FROM lesson WHERE course_id = ? AND id = ?",
            (rs, row) ->
                new VideoLesson(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getInt("duration_minutes"),
                    rs.getString("video_url"),
                    true),
            courseId,
            lessonId)
        .stream()
        .findFirst();
  }

  public Optional<LessonAccess> mediaAccess(long lessonId) {
    return jdbc
        .query(
            "SELECT course_id, preview FROM lesson WHERE id = ?",
            (rs, row) -> new LessonAccess(rs.getLong("course_id"), rs.getBoolean("preview")),
            lessonId)
        .stream()
        .findFirst();
  }
}
