package com.bigproject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CoursePlatformIntegrationTests {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  private MockHttpSession register(String email) throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/api/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                {"name":"Học Viên Test","email":"%s","password":"TestPass123!"}
                """
                            .formatted(email)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.password").doesNotExist())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  @Test
  void catalogueIsPublicAndDoesNotExposePrivateVideoUrls() throws Exception {
    mvc.perform(get("/api/courses"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(6)))
        .andExpect(content().string(not(containsString("videoUrl"))));
    mvc.perform(get("/api/courses").param("category", "Lập trình"))
        .andExpect(jsonPath("$", hasSize(3)));
    mvc.perform(get("/api/courses").param("q", "Angular")).andExpect(jsonPath("$", hasSize(1)));
    mvc.perform(get("/api/courses").param("q", "%")).andExpect(jsonPath("$", hasSize(0)));
    mvc.perform(get("/api/courses/angular-thuc-chien"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lessons", hasSize(4)))
        .andExpect(jsonPath("$.enrolled").value(false))
        .andExpect(content().string(not(containsString("videoUrl"))));
    mvc.perform(get("/api/courses/angular-thuc-chien/preview"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.videoUrl").isNotEmpty());
    mvc.perform(get("/api/courses/not-found")).andExpect(status().isNotFound());
    mvc.perform(get("/api/learning/angular-thuc-chien/lessons/2"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/enrollments")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/media/lessons/1").header("Range", "bytes=0-127"))
        .andExpect(status().isPartialContent())
        .andExpect(content().contentType("video/mp4"))
        .andExpect(header().string("Content-Range", "bytes 0-127/1128375"))
        .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).hasSize(128));
    mvc.perform(get("/api/media/lessons/2")).andExpect(status().isUnauthorized());
  }

  @Test
  void authenticationValidatesHashesPasswordsAndInvalidatesLogoutSession() throws Exception {
    String email = "auth-" + UUID.randomUUID() + "@example.com";
    mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"A\",\"email\":\"invalid\",\"password\":\"123\"}"))
        .andExpect(status().isBadRequest());
    MockHttpSession registered = register(email);
    assertThat(
            jdbc.queryForObject(
                "SELECT password FROM tbl_user WHERE email = ?", String.class, email))
        .startsWith("$2")
        .isNotEqualTo("TestPass123!");
    mvc.perform(get("/api/auth/me").session(registered))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email));
    mvc.perform(
            post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Duplicate\",\"email\":\""
                        + email
                        + "\",\"password\":\"TestPass123!\"}"))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"incorrect\"}"))
        .andExpect(status().isUnauthorized());
    MockHttpSession login =
        (MockHttpSession)
            mvc.perform(
                    post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"email\":\""
                                + email.toUpperCase()
                                + "\",\"password\":\"TestPass123!\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
    mvc.perform(post("/api/auth/logout").session(login).with(csrf()))
        .andExpect(status().isNoContent());
    assertThat(login.isInvalid()).isTrue();
    mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void checkoutIsIdempotentAndProgressBelongsToTheCorrectCourseAndUser() throws Exception {
    String email = "learner-" + UUID.randomUUID() + "@example.com";
    MockHttpSession session = register(email);
    mvc.perform(get("/api/learning/angular-thuc-chien/lessons/2").session(session))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/media/lessons/2").session(session)).andExpect(status().isForbidden());
    mvc.perform(post("/api/enrollments/angular-thuc-chien/demo-checkout").session(session))
        .andExpect(status().isForbidden());
    for (int i = 0; i < 2; i++) {
      mvc.perform(
              post("/api/enrollments/angular-thuc-chien/demo-checkout")
                  .session(session)
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"amount\":1}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("DEMO"))
          .andExpect(jsonPath("$.amount").value(599000));
    }
    Long userId = jdbc.queryForObject("SELECT id FROM tbl_user WHERE email = ?", Long.class, email);
    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM course_order WHERE user_id = ?", Integer.class, userId))
        .isEqualTo(1);
    mvc.perform(get("/api/courses/angular-thuc-chien").session(session))
        .andExpect(jsonPath("$.enrolled").value(true));
    mvc.perform(get("/api/learning/angular-thuc-chien/lessons/2").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(2));
    mvc.perform(get("/api/media/lessons/2").session(session).header("Range", "bytes=0-127"))
        .andExpect(status().isPartialContent())
        .andExpect(content().contentType("video/mp4"));
    mvc.perform(get("/api/media/lessons/10").session(session)).andExpect(status().isForbidden());
    mvc.perform(get("/api/learning/angular-thuc-chien/lessons/9").session(session))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/learning/java-spring-boot/lessons/9").session(session))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/learning/angular-thuc-chien/lessons/9/complete")
                .session(session)
                .with(csrf()))
        .andExpect(status().isNotFound());
    for (int i = 0; i < 2; i++) {
      mvc.perform(
              post("/api/learning/angular-thuc-chien/lessons/2/complete")
                  .session(session)
                  .with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(1)));
    }
    mvc.perform(get("/api/enrollments").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].completedLessons").value(1));
    MockHttpSession otherUser = register("other-" + UUID.randomUUID() + "@example.com");
    mvc.perform(get("/api/enrollments").session(otherUser)).andExpect(jsonPath("$", hasSize(0)));
    mvc.perform(get("/api/learning/angular-thuc-chien/progress").session(otherUser))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/tbl-user").session(session)).andExpect(status().isForbidden());
  }
}
