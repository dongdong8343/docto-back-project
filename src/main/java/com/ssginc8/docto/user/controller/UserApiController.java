package com.ssginc8.docto.user.controller;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssginc8.docto.auth.jwt.dto.TokenType;
import com.ssginc8.docto.user.entity.Role;
import com.ssginc8.docto.user.service.UserService;
import com.ssginc8.docto.user.service.dto.AddDoctorList;
import com.ssginc8.docto.user.service.dto.AddUser;
import com.ssginc8.docto.user.service.dto.AdminUserList;
import com.ssginc8.docto.user.service.dto.CheckPassword;
import com.ssginc8.docto.user.service.dto.EmailVerification;
import com.ssginc8.docto.user.service.dto.FindEmail;
import com.ssginc8.docto.user.service.dto.GetProviderId;
import com.ssginc8.docto.user.service.dto.Login;
import com.ssginc8.docto.user.service.dto.ResetPassword;
import com.ssginc8.docto.user.service.dto.SendVerifyCode;
import com.ssginc8.docto.user.service.dto.SocialSignup;
import com.ssginc8.docto.user.service.dto.UpdateUser;
import com.ssginc8.docto.user.service.dto.UserInfo;
import com.ssginc8.docto.util.CookieUtil;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@RestController
public class UserApiController {
	private final UserService userService;
	private final CookieUtil cookieUtil;

	@GetMapping("/auth/session/provider-id")
	public GetProviderId.Response getProviderId(HttpSession session) {
		String providerId = (String)session.getAttribute("providerId");

		if (Objects.isNull(providerId)) {
			return null;
		}

		return GetProviderId.Response.builder()
			.providerId(providerId).build();
	}

	@GetMapping("/users/me")
	public UserInfo.Response getMyInfo() {
		return userService.getMyInfo();
	}

	@GetMapping("/users/check-email")
	public void checkEmailDuplicate(@RequestParam(value = "email") String email) {
		userService.checkEmail(email);
	}

	@GetMapping("/admin/users")
	public Page<AdminUserList.Response> getUsers(@RequestParam(value = "role", required = false) Role role,
		Pageable pageable) {
		return userService.getUsers(role, pageable);
	}

	@PostMapping("/users/email/find")
	public FindEmail.Response findEmail(@RequestBody FindEmail.Request request) {
		return userService.findEmail(request);
	}

	@PostMapping("/users/register")
	public AddUser.Response emailSignup(@Valid AddUser.Request request) {
		return userService.createUser(request);
	}

	@PostMapping("/users/social")
	public SocialSignup.Response socialSignup(@Valid SocialSignup.Request request, HttpServletResponse response) {
		SocialSignup.Response socialSignupResponse = userService.updateSocialInfo(request);

		response.addCookie(
			cookieUtil.createCookie(TokenType.ACCESS_TOKEN.getTokenType(), socialSignupResponse.getAccessToken(),
				socialSignupResponse.getAccessTokenCookieMaxAge()));

		response.addCookie(
			cookieUtil.createCookie(TokenType.REFRESH_TOKEN.getTokenType(), socialSignupResponse.getRefreshToken(),
				socialSignupResponse.getRefreshTokenCookieMaxAge()));

		return socialSignupResponse;
	}

	@PostMapping("/users/doctors")
	public AddDoctorList.Response registerDoctor(@RequestBody @Valid AddDoctorList.Request request) {
		return userService.registerDoctor(request);
	}

	@PostMapping("/auth/login")
	public ResponseEntity<String> login(@RequestBody Login.Request request, HttpServletResponse response) {
		Login.Response loginResponse = userService.login(request);

		response.addCookie(
			cookieUtil.createCookie(TokenType.ACCESS_TOKEN.getTokenType(), loginResponse.getAccessToken(),
				loginResponse.getAccessTokenCookieMaxAge()));

		response.addCookie(
			cookieUtil.createCookie(TokenType.REFRESH_TOKEN.getTokenType(), loginResponse.getRefreshToken(),
				loginResponse.getRefreshTokenCookieMaxAge()));

		return ResponseEntity.ok("로그인 성공");
	}

	@PostMapping("/users/email/verify-code/send")
	public ResponseEntity<Void> sendVerificationCode(@RequestBody @Valid SendVerifyCode.Request request) {
		userService.sendVerificationCode(request);

		return ResponseEntity.ok().build();
	}

	@PostMapping("/users/email/verify-code/confirm")
	public ResponseEntity<Void> confirmVerificationCode(@RequestBody @Valid EmailVerification.Request request) {
		userService.confirmVerificationCode(request);

		return ResponseEntity.ok().build();
	}

	@PostMapping("/users/password-reset")
	public ResponseEntity<Void> resetPassword(@RequestBody ResetPassword.Request request) {
		userService.resetPassword(request);

		return ResponseEntity.ok().build();
	}

	@PostMapping("/users/check-password")
	public void checkPassword(@RequestBody CheckPassword.Request request) {
		userService.checkPassword(request);
	}

	@PatchMapping("/users/me")
	public ResponseEntity<Void> updateInfo(UpdateUser.Request request) {
		userService.updateInfo(request);

		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/users/me")
	public ResponseEntity<Void> deleteAccount() {
		userService.deleteAccount();

		return ResponseEntity.noContent().build();
	}
}
