package com.ssginc8.docto.user.service.dto;

import org.springframework.web.multipart.MultipartFile;

import com.ssginc8.docto.auth.jwt.dto.Token;
import com.ssginc8.docto.user.entity.User;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

public class SocialSignup {
	@ToString
	@Getter
	@Setter
	@NoArgsConstructor
	public static class Request {
		private String providerId;

		@NotBlank(message = "휴대폰 번호는 비어있을 수 없습니다.")
		private String phone;

		private MultipartFile profileImage;

		@NotBlank(message = "주소는 비어있을 수 없습니다.")
		private String address;

		@NotBlank(message = "역할은 비어있을 수 없습니다.")
		private String role;

		@Builder
		public Request(String providerId, String phone, MultipartFile profileImage, String address, String role) {
			this.providerId = providerId;
			this.phone = phone;
			this.profileImage = profileImage;
			this.address = address;
			this.role = role;
		}
	}

	@Getter
	@NoArgsConstructor
	public static class Response {
		private Long userId;
		private String role;
		private String accessToken;
		private String refreshToken;
		private int accessTokenCookieMaxAge;
		private int refreshTokenCookieMaxAge;

		@Builder
		public Response(Long userId, String role, String accessToken, String refreshToken, int accessTokenCookieMaxAge,
			int refreshTokenCookieMaxAge) {
			this.userId = userId;
			this.role = role;
			this.accessToken = accessToken;
			this.refreshToken = refreshToken;
			this.accessTokenCookieMaxAge = accessTokenCookieMaxAge;
			this.refreshTokenCookieMaxAge = refreshTokenCookieMaxAge;
		}
	}

	public static Response toResponse(User user, Token tokens) {
		return SocialSignup.Response.builder()
			.userId(user.getUserId())
			.role(user.getRole().getKey())
			.accessToken(tokens.getAccessToken())
			.refreshToken(tokens.getRefreshToken())
			.accessTokenCookieMaxAge(tokens.getAccessTokenCookieMaxAge())
			.refreshTokenCookieMaxAge(tokens.getRefreshTokenCookieMaxAge())
			.build();
	}
}
