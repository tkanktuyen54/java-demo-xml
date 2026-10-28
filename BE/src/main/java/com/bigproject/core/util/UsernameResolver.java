package com.bigproject.core.util;

public final class UsernameResolver {

  private UsernameResolver() {}

  public static String resolve(String explicit, String email, String firstName, String lastName) {
    if (explicit != null && !explicit.isBlank()) {
      return explicit.trim();
    }
    if (email != null && email.contains("@")) {
      return email.substring(0, email.indexOf('@')).trim();
    }
    String slug = (firstName + "_" + lastName).replaceAll("[^a-zA-Z0-9._-]", "").toLowerCase();
    if (!slug.isBlank()) {
      return slug;
    }
    return "user_" + System.currentTimeMillis();
  }
}
