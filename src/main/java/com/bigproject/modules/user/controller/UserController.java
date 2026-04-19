package com.bigproject.modules.user.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigproject.modules.user.dto.request.CreateUserRequest;
import com.bigproject.modules.user.service.UserService;

@RestController
@RequestMapping("/api")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/tbl-user")
	public ResponseEntity<List<Map<String, Object>>> listUsers() {
		return ResponseEntity.ok(userService.listUsers());
	}

	@PostMapping("/tbl-user")
	public ResponseEntity<Map<String, Object>> createUser(@RequestBody CreateUserRequest body) {
		Map<String, Object> created = userService.createUser(body);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping({ "/tbl-address", "/table-address" })
	public ResponseEntity<List<Map<String, Object>>> listAddresses() {
		return ResponseEntity.ok(userService.listAddresses());
	}

}
