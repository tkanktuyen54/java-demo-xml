package com.bigproject.modules.auth.service;

import com.bigproject.modules.auth.dto.LoginRequest;
import com.bigproject.modules.auth.dto.RegisterRequest;
import com.bigproject.modules.user.entity.Account;
import com.bigproject.modules.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final String dummyHash;

  public AuthService(UserRepository users, PasswordEncoder passwords) {
    this.users = users;
    this.passwords = passwords;
    this.dummyHash = passwords.encode(UUID.randomUUID().toString());
  }

  public Account login(LoginRequest request) {
    checkPasswordLength(request.password());
    Account account = users.findByEmail(normalize(request.email())).orElse(null);
    boolean matches =
        passwords.matches(request.password(), account == null ? dummyHash : account.password());
    if (account == null || !matches) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu chưa đúng.");
    }
    return account;
  }

  @Transactional
  public Account register(RegisterRequest request) {
    checkPasswordLength(request.password());
    String email = normalize(request.email());
    String name = request.name().trim();
    if (name.length() < 2) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập họ tên hợp lệ.");
    }
    if (users.findByEmail(email).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email này đã được đăng ký.");
    }
    int split = name.lastIndexOf(' ');
    String first = split < 0 ? name : name.substring(0, split);
    String last = split < 0 ? "" : name.substring(split + 1);
    users.insertUser(
        UUID.randomUUID().toString(),
        passwords.encode(request.password()),
        first,
        last,
        email,
        null,
        null,
        null);
    return users.findByEmail(email).orElseThrow();
  }

  public Account me(long id) {
    return users
        .findById(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn."));
  }

  private static String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private static void checkPasswordLength(String password) {
    if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu tối đa 72 byte UTF-8.");
    }
  }
}
