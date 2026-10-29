package com.bigproject.modules.chat.controller;

import com.bigproject.core.security.CurrentUser;
import com.bigproject.modules.chat.dto.ChatRequest;
import com.bigproject.modules.chat.service.ChatService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class ChatController {
  private final ChatService chat;

  public ChatController(ChatService chat) {
    this.chat = chat;
  }

  @PostMapping(value = "/api/chat/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter message(
      @Valid @RequestBody ChatRequest request,
      HttpSession session,
      @AuthenticationPrincipal CurrentUser user) {
    var state = chat.begin(session);
    var emitter = new SseEmitter(30_000L);
    Thread.ofVirtual()
        .name("course-advisor")
        .start(
            () -> {
              try {
                emitter.send(
                    SseEmitter.event()
                        .name("status")
                        .data(Map.of("message", "Đang tìm khóa học phù hợp…")));
                var reply = chat.answer(request, state, user == null ? null : user.id());
                emitter.send(
                    SseEmitter.event().name("answer").data(Map.of("text", reply.message())));
                emitter.send(SseEmitter.event().name("courses").data(reply.courses()));
                emitter.send(
                    SseEmitter.event().name("done").data(Map.of("intent", reply.intent())));
                emitter.complete();
              } catch (Exception ex) {
                try {
                  String message =
                      ex instanceof ResponseStatusException status
                          ? status.getReason()
                          : "Trợ lý chưa trả lời được. Bạn thử lại nhé.";
                  emitter.send(SseEmitter.event().name("error").data(Map.of("message", message)));
                  emitter.complete();
                } catch (IOException | IllegalStateException disconnected) {
                  emitter.completeWithError(disconnected);
                }
              } finally {
                chat.finish(state);
              }
            });
    return emitter;
  }
}
