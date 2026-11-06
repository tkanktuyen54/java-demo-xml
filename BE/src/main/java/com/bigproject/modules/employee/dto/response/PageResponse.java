package com.bigproject.modules.employee.dto.response;

import java.util.List;

public record PageResponse<T>(
    List<T> items, long totalCount, int page, int pageSize, long totalPages) {
  public static <T> PageResponse<T> of(List<T> items, long count, int page, int size) {
    return new PageResponse<>(items, count, page, size, (count + size - 1) / size);
  }
}
