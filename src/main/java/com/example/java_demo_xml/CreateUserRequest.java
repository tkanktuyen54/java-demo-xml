package com.example.java_demo_xml;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateUserRequest(
		String username,
		String password,
		@JsonProperty("first_name") String firstName,
		@JsonProperty("last_name") String lastName,
		String email,
		@JsonProperty("date_of_birth") LocalDate dateOfBirth,
		String gender,
		String phone
) {
}
