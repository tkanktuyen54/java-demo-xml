package com.bigproject.core.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  CookieCsrfTokenRepository csrfRepository() {
    return CookieCsrfTokenRepository.withHttpOnlyFalse();
  }

  @Bean
  SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  SessionAuthenticationStrategy sessionAuthenticationStrategy(CookieCsrfTokenRepository csrf) {
    return new CompositeSessionAuthenticationStrategy(
        List.of(new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrf)));
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, CookieCsrfTokenRepository csrf, SecurityContextRepository contexts)
      throws Exception {
    http.csrf(config -> config.spa().csrfTokenRepository(csrf))
        .securityContext(config -> config.securityContextRepository(contexts))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/error")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/",
                        "/api/courses",
                        "/api/courses/**",
                        "/api/auth/csrf",
                        "/api/config",
                        "/api/media/lessons/*",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register")
                    .permitAll()
                    .requestMatchers("/api/tbl-user", "/api/tbl-address", "/api/table-address")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            config ->
                config
                    .authenticationEntryPoint(
                        (request, response, ex) -> {
                          response.setStatus(401);
                          response.setContentType("application/json;charset=UTF-8");
                          response
                              .getWriter()
                              .write("{\"message\":\"Vui lòng đăng nhập để tiếp tục.\"}");
                        })
                    .accessDeniedHandler(
                        (request, response, ex) -> {
                          response.setStatus(403);
                          response.setContentType("application/json;charset=UTF-8");
                          response
                              .getWriter()
                              .write(
                                  "{\"message\":\"Không có quyền truy cập hoặc phiên bảo mật đã hết hạn. Vui lòng tải lại trang.\"}");
                        }))
        .logout(
            config ->
                config
                    .logoutUrl("/api/auth/logout")
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler((request, response, auth) -> response.setStatus(204)));
    return http.build();
  }
}
