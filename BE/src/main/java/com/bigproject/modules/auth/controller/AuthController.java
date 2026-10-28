package com.bigproject.modules.auth.controller;

import com.bigproject.core.security.CurrentUser;
import com.bigproject.modules.auth.dto.*;
import com.bigproject.modules.auth.service.AuthService;
import com.bigproject.modules.user.entity.Account;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService auth;
  private final SecurityContextRepository contexts;
  private final SessionAuthenticationStrategy sessions;

  public AuthController(
      AuthService auth,
      SecurityContextRepository contexts,
      SessionAuthenticationStrategy sessions) {
    this.auth = auth;
    this.contexts = contexts;
    this.sessions = sessions;
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken());
  }

  @PostMapping("/login")
  public UserResponse login(
      @Valid @RequestBody LoginRequest body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return signIn(auth.login(body), request, response);
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse register(
      @Valid @RequestBody RegisterRequest body,
      HttpServletRequest request,
      HttpServletResponse response) {
    return signIn(auth.register(body), request, response);
  }

  @GetMapping("/me")
  public UserResponse me(@AuthenticationPrincipal CurrentUser user) {
    return UserResponse.from(auth.me(user.id()));
  }

  private UserResponse signIn(
      Account account, HttpServletRequest request, HttpServletResponse response) {
    var authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            new CurrentUser(account.id(), account.email()),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    sessions.onAuthentication(authentication, request, response);
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, request, response);
    return UserResponse.from(account);
  }
}
