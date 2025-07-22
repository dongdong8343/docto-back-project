package com.ssginc8.docto.auth.jwt.provider;

import org.springframework.stereotype.Component;

import com.ssginc8.docto.auth.jwt.entity.RefreshToken;
import com.ssginc8.docto.auth.jwt.repository.RefreshTokenRepository;
import com.ssginc8.docto.global.error.exception.tokenException.InvalidRefreshTokenException;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Component
@Log4j2
public class RefreshTokenProvider {
	private final RefreshTokenRepository refreshTokenRepository;

	public RefreshToken findByUuid(String uuid) {
		return refreshTokenRepository.findByUuid(uuid).orElse(null);
	}

	public RefreshToken findByRefreshToken(String refreshToken) {
		return refreshTokenRepository.findByRefreshToken(refreshToken)
			.orElseThrow(InvalidRefreshTokenException::new);
	}

	public void saveRefreshToken(RefreshToken refreshToken) {
		refreshTokenRepository.save(refreshToken);
	}

	public void deleteRefreshToken(String refreshToken) {
		refreshTokenRepository.deleteRefreshTokenByRefreshToken(refreshToken);
	}
}