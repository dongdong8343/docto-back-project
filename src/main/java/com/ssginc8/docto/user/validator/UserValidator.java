package com.ssginc8.docto.user.validator;

import java.util.Objects;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.ssginc8.docto.global.error.exception.emailException.EmailVerificationFailedException;
import com.ssginc8.docto.global.error.exception.userException.*;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.provider.UserProvider;
import com.ssginc8.docto.user.service.dto.AddUser;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class UserValidator {
	private final UserProvider userProvider;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;

	private static final int MIN_PASSWORD_LENGTH = 8;
	private static final int MIN_PASSWORD_COMPLEXITY = 2;
	private static final int BAD_SEQUENCE_LENGTH = 4;
	private static final String[] BAD_SEQUENCES = {
		"abcdefghijklmnopqrstuvwxyz", "qwertyuiop", "asdfghjkl", "zxcvbnm", "0123456789"
	};
	private static final String SPECIAL_CHAR_REGEX = ".*[!@#$%^&*()_+\\-={}|\\[\\]:\";'<>?,./`~].*";

	// 이메일 + 비밀번호 검사 메서드
	public void validate(AddUser.Request request) {
		checkEmail(request.getEmail());
		checkPassword(request.getPassword());
	}

	public void isPasswordMatch(String inputPassword, String storedPassword) {
		if (!bCryptPasswordEncoder.matches(inputPassword, storedPassword)) {
			throw new InvalidPasswordException();
		}
	}

	public void validateEmail(String email) {
		checkEmail(email);
	}

	public void validatePassword(String storedPassword, String inputPassword) {
		if (bCryptPasswordEncoder.matches(inputPassword, storedPassword)) {
			throw new SameAsPreviousPasswordException();
		}

		checkPassword(inputPassword);
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

	// 비밀번호 검증 메서드
	private void checkPassword(String password) {
		if (StringUtils.isBlank(password) || password.length() < MIN_PASSWORD_LENGTH) {
			throw new PasswordTooShortException();
		}

		int typeCount = 0;
		if (password.matches(".*[A-Z].*")) typeCount++; // 대문자
		if (password.matches(".*[a-z].*")) typeCount++; // 소문자
		if (password.matches(".*[0-9].*")) typeCount++; // 숫자
		if (password.matches(SPECIAL_CHAR_REGEX)) typeCount++; // 특수문자

		if (typeCount < MIN_PASSWORD_COMPLEXITY) {
			throw new PasswordTooSimpleException();
		}

		String lowerPassword = password.toLowerCase();
		for (String seq : BAD_SEQUENCES) {
			for (int i = 0; i <= seq.length() - BAD_SEQUENCE_LENGTH; i++) {
				String subSeq = seq.substring(i, i + BAD_SEQUENCE_LENGTH);
				if (lowerPassword.contains(subSeq)) {
					throw new PasswordHasSequenceException();
				}
			}
		}
	}
}
