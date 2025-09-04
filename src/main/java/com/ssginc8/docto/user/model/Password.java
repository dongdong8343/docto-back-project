package com.ssginc8.docto.user.model;

import java.util.Arrays;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.ssginc8.docto.global.error.exception.userException.InvalidPasswordException;
import com.ssginc8.docto.global.error.exception.userException.PasswordHasSequenceException;
import com.ssginc8.docto.global.error.exception.userException.PasswordTooShortException;
import com.ssginc8.docto.global.error.exception.userException.PasswordTooSimpleException;
import com.ssginc8.docto.global.error.exception.userException.SameAsPreviousPasswordException;

import io.micrometer.common.util.StringUtils;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
@Getter
public class Password {
	private static final int MIN_PASSWORD_LENGTH = 8;
	private static final int MIN_PASSWORD_COMPLEXITY = 2;
	private static final int BAD_SEQUENCE_LENGTH = 4;
	private static final String[] BAD_SEQUENCES = {
		"abcdefghijklmnopqrstuvwxyz", "qwertyuiop", "asdfghjkl", "zxcvbnm", "0123456789"
	};
	private static final String SPECIAL_CHAR_REGEX = ".*[!@#$%^&*()_+\\-={}|\\[\\]:\";'<>?,./`~].*";
	private static final String[] COMPLEXITY_PATTERNS = {
		".*[A-Z].*",           // 대문자
		".*[a-z].*",           // 소문자
		".*[0-9].*",           // 숫자
		SPECIAL_CHAR_REGEX     // 특수문자
	};

	private String value;

	private Password(String value) {
		this.value = value;
	}

	public static Password fromRaw(BCryptPasswordEncoder bCryptPasswordEncoder, String password) {
		validate(password);

		return new Password(bCryptPasswordEncoder.encode(password));
	}

	// 비밀번호 맞는지 확인
	private static void validate(String password) {
		if (StringUtils.isBlank(password) || password.length() < MIN_PASSWORD_LENGTH) {
			throw new PasswordTooShortException();
		}

		long typeCount = Arrays.stream(COMPLEXITY_PATTERNS)
			.filter(password::matches)
			.count();

		if (typeCount < MIN_PASSWORD_COMPLEXITY) {
			throw new PasswordTooSimpleException();
		}

		checkBadSequence(password.toLowerCase());
	}

	private static void checkBadSequence(String lowerPassword) {
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
		if (!encoder.matches(storedPassword, value)) {
			throw new InvalidPasswordException();
		}
	}

	// 비밀번호 변경 시 이전 비밀번호와 같은지 비교 (같다면 예외)
	public void checkSameAs(BCryptPasswordEncoder encoder, String newPassword) {
		if (encoder.matches(newPassword, value)) {
			throw new SameAsPreviousPasswordException();
		}
	}
}
