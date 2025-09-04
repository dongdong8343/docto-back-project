package com.ssginc8.docto.user.validator;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.ssginc8.docto.global.error.exception.emailException.EmailVerificationFailedException;
import com.ssginc8.docto.global.error.exception.userException.*;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.provider.UserProvider;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class UserValidator {
	private final UserProvider userProvider;

	// 이메일 중복 검증 -> Service 계층에서 email 기반으로 user를 찾아서 넘겨줌
	public void assertAvailableForCreate(Optional<User> user) {
		if (user.isPresent()) {
			throw new DuplicateEmailException();
		}
	}

	public void assertAvailableForUpdate(Optional<User> user, Long userId) {
		if (user.isPresent()) {
			if (Objects.equals(userId, user.get().getUserId())) {
				return;
			}
			throw new DuplicateEmailException();
		}
	}

	public void validateCode(String inputCode, String storedCode) {
		if (!Objects.equals(inputCode, storedCode)) {
			throw new EmailVerificationFailedException();
		}
	}

}
