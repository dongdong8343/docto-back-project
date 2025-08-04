package com.ssginc8.docto.user.validator;

import java.util.Objects;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.ssginc8.docto.global.error.exception.emailException.EmailVerificationFailedException;
import com.ssginc8.docto.global.error.exception.userException.*;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.model.Password;
import com.ssginc8.docto.user.provider.UserProvider;
import com.ssginc8.docto.user.service.dto.AddUser;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class UserValidator {
	private final UserProvider userProvider;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;

	// 이메일 + 비밀번호 검사 메서드
	public void validate(AddUser.Request request) {
		checkEmail(request.getEmail());

		Password.fromRaw(request.getPassword());
	}

	public void isPasswordMatch(String inputPassword, String storedPassword) {
		Password password = Password.fromRaw(inputPassword);

		password.matches(bCryptPasswordEncoder, storedPassword);
	}

	public void validateEmail(String email) {
		checkEmail(email);
	}

	// 비밀번호 변경 시 이전 비밀번호와 같은지 비교 (같다면 예외)
	// 변경할 비밀번호가 기준에 부합하는지 확인
	public void validatePasswordChange(String storedPassword, String inputPassword) {
		Password password = Password.fromRaw(inputPassword);

		password.checkSameAs(bCryptPasswordEncoder, storedPassword);
		password.validate();
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
