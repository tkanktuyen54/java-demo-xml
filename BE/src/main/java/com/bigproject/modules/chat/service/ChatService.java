package com.bigproject.modules.chat.service;

import com.bigproject.modules.chat.dto.ChatRequest;
import com.bigproject.modules.course.entity.Course;
import com.bigproject.modules.course.service.CourseService;
import com.bigproject.modules.enrollment.repository.EnrollmentRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {
  private static final String STATE = ChatService.class.getName();
  private final CourseService courses;
  private final EnrollmentRepository enrollments;
  private final RestClient ai;

  public ChatService(
      CourseService courses,
      EnrollmentRepository enrollments,
      @Value("${app.ai.base-url:http://127.0.0.1:8000}") String baseUrl,
      @Value("${app.ai.token:}") String token) {
    this.courses = courses;
    this.enrollments = enrollments;
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(3))
                .build());
    factory.setReadTimeout(Duration.ofSeconds(15));
    ai =
        RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .defaultHeader("X-Advisor-Token", token)
            .build();
  }

  public State begin(HttpSession session) {
    synchronized (session) {
      var state = (State) session.getAttribute(STATE);
      if (state == null) {
        state = new State();
        session.setAttribute(STATE, state);
      }
      synchronized (state) {
        long now = System.currentTimeMillis();
        if (now - state.windowStart >= 60_000) {
          state.windowStart = now;
          state.requests = 0;
        }
        if (state.busy || state.requests >= 20) {
          throw new ResponseStatusException(
              HttpStatus.TOO_MANY_REQUESTS, "Bạn hãy chờ câu trả lời hoặc thử lại sau một phút.");
        }
        state.requests++;
        state.busy = true;
        return state;
      }
    }
  }

  public Reply answer(ChatRequest request, State state, Long userId) {
    List<Course> catalog = courses.list("", "");
    List<Turn> history;
    synchronized (state) {
      history = List.copyOf(state.history);
    }
    AiReply result;
    try {
      result =
          ai.post()
              .uri("/v1/advise")
              .body(new AiRequest(request.message(), request.courseSlug(), history, catalog))
              .retrieve()
              .body(AiReply.class);
    } catch (RestClientException ex) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Trợ lý đang chưa kết nối được. Bạn thử lại sau hoặc xem danh mục khóa học nhé.");
    }
    if (result == null
        || result.message() == null
        || result.message().isBlank()
        || result.courses() == null
        || result.message().length() > 12000) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Trợ lý chưa trả lời được. Bạn thử lại nhé.");
    }
    Map<String, Course> live =
        catalog.stream().collect(Collectors.toMap(Course::slug, Function.identity()));
    List<Suggestion> suggestions = new ArrayList<>();
    for (AiCourse item : result.courses().stream().limit(3).toList()) {
      if (item == null || item.slug() == null || !item.slug().matches("[a-z0-9-]{1,150}")) {
        continue;
      }
      Course course = live.get(item.slug());
      if (course != null) {
        boolean enrolled = userId != null && enrollments.exists(userId, course.id());
        suggestions.add(
            new Suggestion(
                course.slug(),
                course.title(),
                course.price(),
                course.level(),
                item.reason(),
                true,
                enrolled));
      } else if (item.title() != null && item.price() != null && item.price().signum() >= 0) {
        suggestions.add(
            new Suggestion(
                item.slug(),
                item.title(),
                item.price(),
                item.level(),
                item.reason(),
                false,
                false));
      }
    }
    synchronized (state) {
      state.history.add(new Turn("user", request.message()));
      state.history.add(new Turn("assistant", result.message()));
      while (state.history.size() > 12) {
        state.history.removeFirst();
      }
    }
    return new Reply(result.message(), result.intent(), suggestions);
  }

  public void finish(State state) {
    synchronized (state) {
      state.busy = false;
    }
  }

  public static class State {
    private long windowStart = System.currentTimeMillis();
    private int requests;
    private boolean busy;
    private final List<Turn> history = new ArrayList<>();
  }

  public record Turn(String role, String content) {}

  public record AiRequest(
      String message, String courseSlug, List<Turn> history, List<Course> courses) {}

  public record AiCourse(
      String slug, String title, BigDecimal price, String level, String reason) {}

  public record AiReply(String message, String intent, List<AiCourse> courses) {}

  public record Suggestion(
      String slug,
      String title,
      BigDecimal price,
      String level,
      String reason,
      boolean available,
      boolean enrolled) {}

  public record Reply(String message, String intent, List<Suggestion> courses) {}
}
