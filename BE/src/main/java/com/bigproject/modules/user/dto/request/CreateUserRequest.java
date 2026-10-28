package com.bigproject.modules.user.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record CreateUserRequest(
    String username,
    String password,
    @JsonProperty("first_name") String firstName,
    @JsonProperty("last_name") String lastName,
    String email,
    @JsonProperty("date_of_birth") LocalDate dateOfBirth,
    String gender,
    String phone) {}
