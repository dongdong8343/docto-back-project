package com.ssginc8.docto.user.model;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.ssginc8.docto.global.error.exception.userException.InvalidPasswordException;
import com.ssginc8.docto.global.error.exception.userException.PasswordHasSequenceException;
import com.ssginc8.docto.global.error.exception.userException.PasswordTooShortException;
import com.ssginc8.docto.global.error.exception.userException.PasswordTooSimpleException;
import com.ssginc8.docto.global.error.exception.userException.SameAsPreviousPasswordException;

import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Password {
	private static final int MIN_PASSWORD_LENGTH = 8;
	private static final int MIN_PASSWORD_COMPLEXITY = 2;
	private static final int BAD_SEQUENCE_LENGTH = 4;
	private static final String[] BAD_SEQUENCES = {
		"abcdefghijklmnopqrstuvwxyz", "qwertyuiop", "asdfghjkl", "zxcvbnm", "0123456789"
	};
	private static final String SPECIAL_CHAR_REGEX = ".*[!@#$%^&*()_+\\-={}|\\[\\]:\";'<>?,./`~].*";

	private final String value;

	public static Password fromRaw(String raw) {
		Password password = new Password(raw);

		password.validate();

		return password;
	}

	// 비밀번호 맞는지 확인
	public void validate() {
		if (StringUtils.isBlank(value) || value.length() < MIN_PASSWORD_LENGTH) {
			throw new PasswordTooShortException();
		}

		int typeCount = 0;
		if (value.matches(".*[A-Z].*")) typeCount++; // 대문자
		if (value.matches(".*[a-z].*")) typeCount++; // 소문자
		if (value.matches(".*[0-9].*")) typeCount++; // 숫자
		if (value.matches(SPECIAL_CHAR_REGEX)) typeCount++; // 특수문자

		if (typeCount < MIN_PASSWORD_COMPLEXITY) {
			throw new PasswordTooSimpleException();
		}

		String lowerPassword = value.toLowerCase();
		for (String seq : BAD_SEQUENCES) {
			for (int i = 0; i <= seq.length() - BAD_SEQUENCE_LENGTH; i++) {
				String subSeq = seq.substring(i, i + BAD_SEQUENCE_LENGTH);
				if (lowerPassword.contains(subSeq)) {
					throw new PasswordHasSequenceException();
				}
			}
		}
	}

	// 비밀번호 동일한지 확인하는 메서드
	public void matches(BCryptPasswordEncoder encoder, String storedPassword) {
		if (!encoder.matches(value, storedPassword)) {
			throw new InvalidPasswordException();
		}
	}

	// 비밀번호 변경 시 이전 비밀번호와 같은지 비교 (같다면 예외)
	public void checkSameAs(BCryptPasswordEncoder encoder, String storedPassword) {
		if (encoder.matches(value, storedPassword)) {
			throw new SameAsPreviousPasswordException();
		}
	}
}
