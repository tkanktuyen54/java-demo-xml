package com.bigproject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatIntegrationTests {
  private static final AtomicReference<String> LAST_REQUEST = new AtomicReference<>();
  private static final HttpServer AI = startAi();
  @Autowired MockMvc mvc;

  private static HttpServer startAi() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/v1/advise",
          exchange -> {
            String request =
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            LAST_REQUEST.set(request);
            String body =
                request.contains("simulate-failure")
                    ? "{}"
                    : """
            {"message":"Đây là khóa Angular.","intent":"open","courses":[
              {"slug":"angular-thuc-chien","title":"Wrong title","price":1,"level":"Cơ bản","reason":"Xây dựng ứng dụng web"}
            ]}
            """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(
                request.contains("simulate-failure") ? 503 : 200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
          });
      server.start();
      return server;
    } catch (IOException ex) {
      throw new ExceptionInInitializerError(ex);
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry properties) {
    properties.add("app.ai.base-url", () -> "http://127.0.0.1:" + AI.getAddress().getPort());
  }

  @AfterAll
  static void stopAi() {
    AI.stop(0);
  }

  @Test
  void publicChatStreamsLivePricesWithoutGrantingLearningAccess() throws Exception {
    var result =
        mvc.perform(
                post("/api/chat/messages")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"Mở khóa Angular\"}"))
            .andExpect(request().asyncStarted())
            .andReturn();
    result.getAsyncResult(10000);
    mvc.perform(asyncDispatch(result))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
        .andExpect(content().string(containsString("event:status")))
        .andExpect(content().string(containsString("event:answer")))
        .andExpect(content().string(containsString("event:courses")))
        .andExpect(content().string(containsString("599000")))
        .andExpect(content().string(containsString("\"enrolled\":false")))
        .andExpect(content().string(containsString("event:done")));
    assertThat(LAST_REQUEST.get())
        .contains("599000")
        .doesNotContain("videoUrl", "password", "email");
  }

  @Test
  void chatValidatesInputAndRequiresCsrfEvenForAnonymousVisitors() throws Exception {
    mvc.perform(
            post("/api/chat/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Python\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/chat/messages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\" \"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/chat/messages")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Python\",\"courseSlug\":\"../admin\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void historyIsBoundedAndIsolatedBySession() throws Exception {
    MockHttpSession first = new MockHttpSession();
    for (int index = 0; index < 2; index++) {
      var result =
          mvc.perform(
                  post("/api/chat/messages")
                      .session(first)
                      .with(csrf())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content("{\"message\":\"Python\"}"))
              .andReturn();
      result.getAsyncResult(10000);
      mvc.perform(asyncDispatch(result)).andExpect(status().isOk());
    }
    assertThat(LAST_REQUEST.get()).contains("\"role\":\"user\"", "\"role\":\"assistant\"");
    var separate =
        mvc.perform(
                post("/api/chat/messages")
                    .session(new MockHttpSession())
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"Java\"}"))
            .andReturn();
    separate.getAsyncResult(10000);
    mvc.perform(asyncDispatch(separate)).andExpect(status().isOk());
    assertThat(LAST_REQUEST.get()).contains("\"history\":[]");
  }

  @Test
  void upstreamFailureProducesAnErrorEvent() throws Exception {
    var result =
        mvc.perform(
                post("/api/chat/messages")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"simulate-failure\"}"))
            .andReturn();
    result.getAsyncResult(10000);
    mvc.perform(asyncDispatch(result))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("event:error")));
  }
}
