package com.bigproject.modules.auth.dto;

import com.bigproject.modules.user.entity.Account;

public record UserResponse(long id, String name, String email) {
  public static UserResponse from(Account account) {
    return new UserResponse(
        account.id(), (account.firstName() + " " + account.lastName()).trim(), account.email());
  }
}
