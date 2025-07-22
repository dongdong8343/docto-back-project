package com.ssginc8.docto.auth.jwt.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssginc8.docto.auth.jwt.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	Optional<RefreshToken> findByRefreshToken(String refreshToken);

	Optional<RefreshToken> findByUuid(String uuid);

	void deleteRefreshTokenByRefreshToken(String refreshToken);
}