package com.bigproject.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnProperty(
    name = "springdoc.api-docs.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class OpenApiDownloadFilter extends OncePerRequestFilter {
  private final String apiDocsPath;

  public OpenApiDownloadFilter(
      @Value("${springdoc.api-docs.path:/v3/api-docs}") String apiDocsPath) {
    this.apiDocsPath = apiDocsPath;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if ("GET".equals(request.getMethod()) && "true".equals(request.getParameter("download"))) {
      String path = request.getRequestURI().substring(request.getContextPath().length());
      String filename = null;
      if (apiDocsPath.equals(path)) {
        filename = "khoa-openapi.json";
      } else if ((apiDocsPath + ".yaml").equals(path)) {
        filename = "khoa-openapi.yaml";
      }
      if (filename != null) {
        response.setHeader(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(filename).build().toString());
      }
    }
    filterChain.doFilter(request, response);
  }
}
