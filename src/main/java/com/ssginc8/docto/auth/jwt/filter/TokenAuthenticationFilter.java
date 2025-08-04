package com.ssginc8.docto.auth.jwt.filter;

import java.io.IOException;
import java.util.Set;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ssginc8.docto.auth.jwt.dto.CreateAccessToken;
import com.ssginc8.docto.auth.jwt.dto.TokenType;
import com.ssginc8.docto.auth.jwt.provider.TokenProvider;
import com.ssginc8.docto.auth.jwt.service.RefreshTokenServiceImpl;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.provider.UserProvider;
import com.ssginc8.docto.util.CookieUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {
	private final UserProvider userProvider;
	private final TokenProvider tokenProvider;
	private final RefreshTokenServiceImpl refreshTokenServiceImpl;
	private final CookieUtil cookieUtil;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		log.info("----------------------필터 동작-----------------------");
		String accessToken = cookieUtil.getToken(request, TokenType.ACCESS_TOKEN.getTokenType());

		if (tokenProvider.validToken(accessToken)) { // 토큰 검증 -> 토큰이 유효하다면
			// 토큰 기반으로 인증 정보 가져오기
			SecurityContextHolder.getContext().setAuthentication(getAuthentication(accessToken));
		} else { // 유효하지 않다면
			String refreshToken = cookieUtil.getToken(request, TokenType.REFRESH_TOKEN.getTokenType());
			if (tokenProvider.validToken(refreshToken)) {
				CreateAccessToken.Response createAccessToken = refreshTokenServiceImpl.createNewAccessToken(
					refreshToken);
				accessToken = createAccessToken.getAccessToken();

				response.addCookie(
					cookieUtil.createCookie(TokenType.ACCESS_TOKEN.getTokenType(), createAccessToken.getAccessToken(),
						createAccessToken.getAccessTokenCookieMaxAge()));

				SecurityContextHolder.getContext().setAuthentication(getAuthentication(accessToken));
			}
		}

		filterChain.doFilter(request, response);
	}

	private Authentication getAuthentication(String accessToken) {
		String uuid = tokenProvider.getUuid(accessToken);

		User user = userProvider.loadUserByUuid(uuid);

		Set<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority(user.getRole().getKey()));

		return new UsernamePasswordAuthenticationToken(new org.springframework.security.core.userdetails.User
			(String.valueOf(user.getUserId()), "", authorities), accessToken, authorities);
	}
}