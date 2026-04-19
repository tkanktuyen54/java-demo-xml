package com.bigproject.core.exception;

import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<Map<String, String>> badRequest(BadRequestException ex) {
		return ResponseEntity.badRequest().body(Map.of("error", ex.getErrorCode()));
	}

	@ExceptionHandler(CannotGetJdbcConnectionException.class)
	public ResponseEntity<Map<String, Object>> databaseUnavailable(CannotGetJdbcConnectionException ex) {
		Throwable root = ex.getMostSpecificCause();
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
				"ok", false,
				"error", "database_unreachable",
				"cause", root.getClass().getSimpleName() + ": " + root.getMessage()));
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<Map<String, Object>> databaseError(DataAccessException ex) {
		Throwable root = ex.getMostSpecificCause();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
				"ok", false,
				"error", "database_error",
				"cause", root.getClass().getSimpleName() + ": " + root.getMessage()));
	}

}
