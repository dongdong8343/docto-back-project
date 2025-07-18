package com.ssginc8.docto.user.validator;

import io.micrometer.common.util.StringUtils;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.ssginc8.docto.global.error.exception.emailException.EmailVerificationFailedException;
import com.ssginc8.docto.global.error.exception.userException.DuplicateEmailException;
import com.ssginc8.docto.global.error.exception.userException.InvalidPasswordException;
import com.ssginc8.docto.global.error.exception.userException.PasswordHasSequenceException;
import com.ssginc8.docto.global.error.exception.userException.PasswordTooShortException;
import com.ssginc8.docto.global.error.exception.userException.PasswordTooSimpleException;
import com.ssginc8.docto.global.error.exception.userException.SameAsPreviousPasswordException;
import com.ssginc8.docto.global.error.exception.userException.UserMismatchException;
import com.ssginc8.docto.user.entity.User;
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

	public void validateOwnership(Long currentUserId, Long targerUserId) {
		if (!Objects.equals(currentUserId, targerUserId)) {
			throw new UserMismatchException();
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
		if (StringUtils.isBlank(password) || password.length() < 8) {
			throw new PasswordTooShortException();
		}

		int typeCount = 0;
		if (password.matches(".*[A-Z].*"))
			typeCount++; // 대문자
		if (password.matches(".*[a-z].*"))
			typeCount++; // 소문자
		if (password.matches(".*[0-9].*"))
			typeCount++; // 숫자
		if (password.matches(".*[!@#$%^&*()_+\\-={}|\\[\\]:\";'<>?,./`~].*"))
			typeCount++; // 특수문자

		if (typeCount < 2) {
			throw new PasswordTooSimpleException();
		}

		// 연속된 키보드 문자열 제한
		String[] badSequences = {
			"abcdefghijklmnopqrstuvwxyz", "qwertyuiop", "asdfghjkl", "zxcvbnm",
			"0123456789"
		};
		String lowerPassword = password.toLowerCase();

		for (String seq : badSequences) {
			for (int i = 0; i < seq.length() - 3; i++) {
				String subSeq = seq.substring(i, i + 4);
				if (lowerPassword.contains(subSeq)) {
					throw new PasswordHasSequenceException();
				}
			}
		}
	}

}
