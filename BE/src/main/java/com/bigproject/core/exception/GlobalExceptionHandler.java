package com.bigproject.core.exception;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String, String>> status(ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode())
        .body(
            Map.of(
                "message",
                ex.getReason() == null ? "Không thể thực hiện yêu cầu." : ex.getReason()));
  }

  @ExceptionHandler(BadRequestException.class)
  ResponseEntity<Map<String, String>> badRequest(BadRequestException ex) {
    return ResponseEntity.badRequest().body(Map.of("message", ex.getErrorCode()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getDefaultMessage())
            .findFirst()
            .orElse("Dữ liệu chưa hợp lệ.");
    return ResponseEntity.badRequest().body(Map.of("message", message));
  }

  @ExceptionHandler(DuplicateKeyException.class)
  ResponseEntity<Map<String, String>> duplicate(DuplicateKeyException ex) {
    return ResponseEntity.status(409).body(Map.of("message", "Email hoặc dữ liệu này đã tồn tại."));
  }

  @ExceptionHandler(DataAccessException.class)
  ResponseEntity<Map<String, String>> databaseError(DataAccessException ex) {
    log.error("Database operation failed", ex);
    return ResponseEntity.status(503)
        .body(Map.of("message", "Chưa thể kết nối dữ liệu. Vui lòng thử lại sau."));
  }
}
