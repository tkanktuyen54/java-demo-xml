package com.bigproject.modules.user.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.bigproject.core.exception.BadRequestException;
import com.bigproject.core.util.UsernameResolver;
import com.bigproject.modules.user.dto.request.CreateUserRequest;
import com.bigproject.modules.user.repository.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public List<Map<String, Object>> listUsers() {
		return userRepository.findAllUsers();
	}

	public List<Map<String, Object>> listAddresses() {
		return userRepository.findAllAddresses();
	}

	public Map<String, Object> createUser(CreateUserRequest body) {
		if (body == null || body.firstName() == null || body.firstName().isBlank()
				|| body.lastName() == null || body.lastName().isBlank()) {
			throw new BadRequestException("first_name_and_last_name_required");
		}
		if (body.password() == null || body.password().isBlank()) {
			throw new BadRequestException("password_required");
		}
		String firstName = body.firstName().trim();
		String lastName = body.lastName().trim();
		String password = body.password().trim();
		String email = body.email() != null && !body.email().isBlank() ? body.email().trim() : null;
		String gender = body.gender() != null && !body.gender().isBlank() ? body.gender().trim() : null;
		String phone = body.phone() != null && !body.phone().isBlank() ? body.phone().trim() : null;
		String username = UsernameResolver.resolve(body.username(), email, firstName, lastName);
		return userRepository.insertUser(username, password, firstName, lastName, email, body.dateOfBirth(), gender, phone);
	}

}
