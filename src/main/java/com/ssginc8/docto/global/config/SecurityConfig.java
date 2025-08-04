package com.ssginc8.docto.global.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ssginc8.docto.auth.handler.CustomAccessDeniedHandler;
import com.ssginc8.docto.auth.handler.CustomAuthenticationEntryPoint;
import com.ssginc8.docto.auth.handler.OAuth2SuccessHandler;
import com.ssginc8.docto.auth.jwt.filter.TokenAuthenticationFilter;
import com.ssginc8.docto.auth.jwt.provider.TokenProvider;
import com.ssginc8.docto.auth.jwt.service.RefreshTokenServiceImpl;
import com.ssginc8.docto.auth.service.UserDetailService;
import com.ssginc8.docto.user.provider.UserProvider;
import com.ssginc8.docto.util.CookieUtil;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final UserProvider userProvider;
	private final TokenProvider tokenProvider;
	private final RefreshTokenServiceImpl refreshTokenServiceImpl;
	private final UserDetailService userDetailService;
	private final CookieUtil cookieUtil;
	private final DefaultOAuth2UserService oAuth2UserService;
	private final OAuth2SuccessHandler oAuth2SuccessHandler;
	private final LogoutHandler logoutHandler;

	private final CustomAuthenticationEntryPoint authenticationEntryPoint;
	private final CustomAccessDeniedHandler accessDeniedHandler;

	// 특정 http 요청에 대해 웹 기반 보안 구성
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		return http
			.cors(Customizer.withDefaults())
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth

				/**
				 * Public : 인증 없이 접근 가능
				 */
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

				.requestMatchers(
					"/static/**", "/docs/index.html",
					"/api/v1/auth/**", "/api/v1/users/register", "/api/v1/users/social", "/api/v1/users/doctors",
					"/api/v1/users/check-email", "/api/v1/users/email/find",
					"/api/v1/users/password-reset", "/api/v1/users/email/verify-code/send",
					"/api/v1/users/email/verify-code/confirm", "/api/v1/auth/session/provider-id", "/"
				).permitAll()

				.requestMatchers(
					HttpMethod.POST, "/api/v1/patients"
				).permitAll()

				/**
				 * 로그인 사용자 공통
				 */
				.requestMatchers(
					"/api/v1/users/me",
					"/api/v1/reviews/*/report",
					"/api/v1/csrooms/**",
					"/ws-chat/**",
					"/api/v1/users/check-password"
				).authenticated()

				.requestMatchers(HttpMethod.GET,
					"/api/v1/hospitals/**",
					"/api/v1/doctors/**",
					"/api/v1/appointments/**",
					"/api/v1/reviews",
					"/api/v1/qnas/**")
				.authenticated()



				.requestMatchers(HttpMethod.POST, "/api/v1/appointments/*/payment-request").hasRole("HOSPITAL_ADMIN")
				.requestMatchers(HttpMethod.PATCH, "/api/v1/doctors/**").hasRole("DOCTOR")

				/**
				 * 의사 + 병원 관리자
				 */
				.requestMatchers(HttpMethod.GET, "/api/v1/hospitals/*/reviews").hasAnyRole("DOCTOR", "HOSPITAL_ADMIN")
				.requestMatchers(HttpMethod.POST, "/api/v1/doctors/**").hasAnyRole("DOCTOR","HOSPITAL_ADMIN")

				/**
				 * 환자 전용
				 */
				.requestMatchers(
					"/api/v1/patients/**",
					"/api/v1/calendar/patient",
					"/api/v1/guardians/{patientId}/invite",
					"/api/v1/guardians/{patientId}/pending-invites"
				).hasRole("PATIENT")

				/**
				 * 보호자 전용
				 */
				.requestMatchers(
					"/api/v1/patients/**",
					"/api/v1/guardians/**",
					"/api/v1/reviews/**",
					"/api/v1/calendar/guardian",
					"/api/v1/payment-history/{paymentRequestId}",
					"/api/v1/payments/init",
					"/api/v1/payments/confirm"
				).hasRole("GUARDIAN")

				.requestMatchers(HttpMethod.PATCH, "/api/v1/guardians/respond").hasRole("GUARDIAN")
				.requestMatchers(HttpMethod.POST, "/api/v1/appointments/**").hasRole("GUARDIAN")
				.requestMatchers(HttpMethod.GET, "/api/v1/qnas/**").hasRole("GUARDIAN")

				/**
				 * 환자 + 보호자 공통
				 */
				.requestMatchers("/api/v1/users/me/reviews").hasAnyRole("PATIENT", "GUARDIAN")
				.requestMatchers("/api/v1/reviews/**").hasAnyRole("PATIENT", "GUARDIAN")
				.requestMatchers("/api/v1/medications/**").hasAnyRole("PATIENT", "GUARDIAN")

				/**
				 * 병원 관리자 전용
				 */
				.requestMatchers(
					"/api/v1/hospitals/**",
					"/api/v1/doctors/**",
					"/api/v1/calendar/hospital",
					"/api/v1/file/**"
				).hasRole("HOSPITAL_ADMIN")

				/**
				 * 의사 전용
				 */
				.requestMatchers(
					"/api/v1/doctors/**",
					"/api/v1/qnas/*/comments/**",
					"/api/v1/calendar/doctor"
				).hasRole("DOCTOR")
				.requestMatchers(HttpMethod.GET, "/api/v1/hospitals/**").hasRole("DOCTOR")
				.requestMatchers(HttpMethod.POST, "/api/v1/qnas/*/comments").hasRole("DOCTOR")


				/**
				 * 병원 관리자 + 보호자 공통
				 */
				.requestMatchers(HttpMethod.PATCH, "/api/v1/appointments/**").hasAnyRole("HOSPITAL_ADMIN", "GUARDIAN")

				/**
				 * 시스템 관리자 전용
				 */
				.requestMatchers("/api/v1/admin/**").hasRole("SYSTEM_ADMIN")
				.requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/**").hasRole("SYSTEM_ADMIN")

				.anyRequest().authenticated()
			) // 그외 모든 요청 인증 필요
			.csrf(AbstractHttpConfigurer::disable)
			.oauth2Login(oauth2 -> oauth2
				// "api/v1/auth/login" 뒤에 /kakao or /naver 붙여서 로그인 요청
				.authorizationEndpoint(endpoint -> endpoint.baseUri("/api/v1/auth/login"))
				.redirectionEndpoint(endpoint -> endpoint.baseUri("/api/v1/auth/callback/*"))
				.userInfoEndpoint(endpoint -> endpoint.userService(oAuth2UserService))
				.successHandler(oAuth2SuccessHandler) // 로그인 성공시 OAuth2SuccessHandler 동작
			)
			.logout(logout -> logout.logoutUrl("/api/v1/auth/logout")
				.addLogoutHandler(logoutHandler)
				.logoutSuccessHandler((request, response, authentication) -> SecurityContextHolder.clearContext())
			)
			.addFilterBefore(new TokenAuthenticationFilter(userProvider, tokenProvider, refreshTokenServiceImpl, cookieUtil),
				UsernamePasswordAuthenticationFilter.class)
			.exceptionHandling(e -> e
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.build();
	}

	// 인증 관리자 관련 설정
	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity http,
		BCryptPasswordEncoder bCryptPasswordEncoder) throws Exception {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailService); // 사용자 정보를 가져올 서비스 생성
		authProvider.setPasswordEncoder(bCryptPasswordEncoder); // 비밀번호 암호화하기 위한 인코더 설정
		return new ProviderManager(authProvider);
	}

	@Bean
	public BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "https://docto.click")); // 프론트 주소 명시
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
