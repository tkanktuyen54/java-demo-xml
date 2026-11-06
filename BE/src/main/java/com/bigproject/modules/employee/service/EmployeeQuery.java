package com.bigproject.modules.employee.service;

import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class EmployeeQuery {
  private EmployeeQuery() {}

  public static void page(int page, int pageSize) {
    if (page < 1 || pageSize < 1 || pageSize > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_pagination");
    }
  }

  public static String search(String search) {
    String value = search == null ? "" : search.trim();
    if (value.length() > 200 || value.length() == 1) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "search_requires_2_to_200_characters");
    }
    return "%"
        + value.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_")
        + "%";
  }

  public static void allowed(String value, Set<String> values, String error) {
    if (!values.contains(value)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
    }
  }
}
