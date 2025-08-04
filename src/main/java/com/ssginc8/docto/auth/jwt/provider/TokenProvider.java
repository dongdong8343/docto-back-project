package com.ssginc8.docto.auth.jwt.provider;

import java.time.Duration;
import java.util.Date;
import java.util.Set;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import com.ssginc8.docto.auth.jwt.dto.CreateAccessToken;
import com.ssginc8.docto.auth.jwt.dto.Token;
import com.ssginc8.docto.auth.jwt.property.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@Service
public class TokenProvider {
	private final JwtProperties jwtProperties;

	private static final Duration ACCESS_TOKEN_EXPIRY = Duration.ofMinutes(15);
	private static final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(7);

	public CreateAccessToken.Response generateAccessToken(String uuid, String role) {
		return CreateAccessToken.Response.create(makeAccessToken(uuid, role), (int)ACCESS_TOKEN_EXPIRY.getSeconds());
	}

	public Token generateTokens(String uuid, String role) {
		String accessToken = makeAccessToken(uuid, role);
		String refreshToken = makeRefreshToken(uuid);

		return Token.create(accessToken, refreshToken, (int)ACCESS_TOKEN_EXPIRY.getSeconds(),
			(int)REFRESH_TOKEN_EXPIRY.getSeconds());
	}

	private String makeAccessToken(String uuid, String role) {
		Date now = new Date();

		return Jwts.builder()
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE) // 헤더 타입 - JWT
			.setIssuer(jwtProperties.getIssuer()) // 내용 iss(발급자) : properties 파일에서 설정한 값
			.setIssuedAt(now) // 내용 iat(발급일시) : 현재 시간
			.setExpiration(new Date(now.getTime() + ACCESS_TOKEN_EXPIRY.toMillis())) // 내용 exp(만료일시)
			.setSubject(uuid) // 내용 sub(토큰 제목) : 유저의 uuid
			.claim("role", role) // 클레임 role : 유저 권한
			.signWith(SignatureAlgorithm.HS256, jwtProperties.getSecretKey()) // 서명 - 비밀 값과 함께 암호화
			.compact();
	}

	private String makeRefreshToken(String uuid) {
		Date now = new Date();

		return Jwts.builder()
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE)
			.setIssuer(jwtProperties.getIssuer())
			.setIssuedAt(now)
			.setExpiration(new Date(now.getTime() + REFRESH_TOKEN_EXPIRY.toMillis()))
			.setSubject(uuid)
			.signWith(SignatureAlgorithm.HS256, jwtProperties.getSecretKey())
			.compact();
	}

	// 토큰 유효성 검증
	public boolean validToken(String token) {
		try {
			Jwts.parser()
				.setSigningKey(jwtProperties.getSecretKey()).build() // 비밀 값으로 복호화
				.parseClaimsJws(token);

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	// 토큰 기반으로 uuid 가져오는 메서드
	public String getUuid(String token) {
		return getClaims(token).getSubject();
	}

	private Claims getClaims(String token) {
		return Jwts.parser()
			.setSigningKey(jwtProperties.getSecretKey())
			.build()
			.parseClaimsJws(token)
			.getBody();
	}
}
