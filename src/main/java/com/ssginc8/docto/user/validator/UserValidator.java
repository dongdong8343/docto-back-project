package com.ssginc8.docto.user.validator;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.ssginc8.docto.global.error.exception.emailException.EmailVerificationFailedException;
import com.ssginc8.docto.global.error.exception.userException.*;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.provider.UserProvider;
import com.ssginc8.docto.user.service.dto.AddUser;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class UserValidator {
	private final UserProvider userProvider;

	// 이메일 검사 메서드
	public void validate(AddUser.Request request) {
		checkEmail(request.getEmail());
	}

	public void validateEmail(String email) {
		checkEmail(email);
	}

	public void validateCode(String inputCode, String storedCode) {
		if (!Objects.equals(inputCode, storedCode)) {
			throw new EmailVerificationFailedException();
		}
	}

	public void validateUpdateEmail(String email, Long userId) {
		Optional<User> user = userProvider.loadUserByEmail(email);

		if (user.isPresent()) {
			if (Objects.equals(userId, user.get().getUserId())) {
				return;
			}
			throw new DuplicateEmailException();
		}
	}

	// 이메일 중복 검증 메서드
	private void checkEmail(String email) {
		if (userProvider.loadUserByEmail(email).isPresent()) {
			throw new DuplicateEmailException();
		}
	}
}
