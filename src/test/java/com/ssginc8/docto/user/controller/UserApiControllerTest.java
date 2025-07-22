package com.ssginc8.docto.user.controller;

import static org.springframework.restdocs.cookies.CookieDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssginc8.docto.auth.jwt.dto.Token;
import com.ssginc8.docto.auth.jwt.dto.TokenType;
import com.ssginc8.docto.auth.jwt.provider.TokenProvider;
import com.ssginc8.docto.restdocs.RestDocsConfig;
import com.ssginc8.docto.user.entity.Role;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.repository.UserRepository;
import com.ssginc8.docto.user.service.dto.AddDoctorList;
import com.ssginc8.docto.user.service.dto.CheckPassword;
import com.ssginc8.docto.user.service.dto.FindEmail;
import com.ssginc8.docto.user.service.dto.Login;
import com.ssginc8.docto.user.service.dto.ResetPassword;
import com.ssginc8.docto.user.service.dto.SendVerifyCode;

import jakarta.servlet.http.Cookie;

@Transactional
@ActiveProfiles("prod")
@ExtendWith(RestDocumentationExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@Import(RestDocsConfig.class)
public class UserApiControllerTest {

	@Autowired
	protected RestDocumentationResultHandler restDocs;

	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TokenProvider tokenProvider;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@BeforeEach
	public void setUp(WebApplicationContext context, RestDocumentationContextProvider restDocumentation) {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
			.apply(documentationConfiguration(restDocumentation))
			.apply(springSecurity())
			.alwaysDo(restDocs)
			.addFilters(new CharacterEncodingFilter("UTF-8", true))
			.build();
	}

	@Test
	@DisplayName("내 정보 조회 테스트")
	void getMyInfoTest() throws Exception {

		Token token = makeTokenBySystemAdmin();

		mockMvc.perform(get("/api/v1/users/me")
				.cookie(
					new Cookie(TokenType.ACCESS_TOKEN.getTokenType(), token.getAccessToken()),
					new Cookie(TokenType.REFRESH_TOKEN.getTokenType(), token.getRefreshToken())
				))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				responseFields(
					fieldWithPath("userId").type(JsonFieldType.NUMBER).description("유저 아이디"),
					fieldWithPath("email").type(JsonFieldType.STRING).description("사용자 이메일"),
					fieldWithPath("name").type(JsonFieldType.STRING).description("사용자 이름"),
					fieldWithPath("phone").type(JsonFieldType.STRING).description("전화번호"),
					fieldWithPath("address").type(JsonFieldType.STRING).description("주소"),
					fieldWithPath("role").type(JsonFieldType.STRING).description("사용자 역할"),
					fieldWithPath("profileImageUrl").type(JsonFieldType.STRING)
						.optional()
						.description("프로필 이미지 URL (없을 수 있음, 있다면 url 반환)")
				)
			));
	}

	@Test
	@DisplayName("이메일 중복 테스트")
	void checkEmailDuplicateTest() throws Exception {
		mockMvc.perform(get("/api/v1/users/check-email")
				.param("email", "wow777@naver.com"))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				queryParameters(
					parameterWithName("email").description("중복 확인할 이메일 주소")
				)
			));
	}

	@Test
	@DisplayName("이메일 찾기 테스트")
	void findEmailTest() throws Exception {
		FindEmail.Request request = FindEmail.Request.builder()
			.name("홍길동")
			.phone("010-1234-5678")
			.build();

		mockMvc.perform(post("/api/v1/users/email/find")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				requestFields(
					fieldWithPath("name").type(JsonFieldType.STRING).description("사용자 이름"),
					fieldWithPath("phone").type(JsonFieldType.STRING).description("전화번호")
				),
				responseFields(
					fieldWithPath("email").type(JsonFieldType.STRING).description("조회된 이메일")
				)
			));
	}

	@Test
	@DisplayName("유저 리스트 조회 테스트")
	void getUsersTest() throws Exception {

		Token token = makeTokenBySystemAdmin();

		mockMvc.perform(get("/api/v1/admin/users")
				.cookie(
					new Cookie(TokenType.ACCESS_TOKEN.getTokenType(), token.getAccessToken()),
					new Cookie(TokenType.REFRESH_TOKEN.getTokenType(), token.getRefreshToken())
				)
				.param("role", "DOCTOR")
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				queryParameters(
					parameterWithName("role").optional().description("필터링할 사용자 역할 (예: ROLE_USER, ROLE_SYSTEM_ADMIN)"),
					parameterWithName("page").optional().description("조회할 페이지 번호 (0부터 시작)"),
					parameterWithName("size").optional().description("페이지당 항목 수")
				),
				responseFields(
					fieldWithPath("content[].email").type(JsonFieldType.STRING).description("사용자 이메일"),
					fieldWithPath("content[].name").type(JsonFieldType.STRING).description("사용자 이름"),
					fieldWithPath("content[].role").type(JsonFieldType.STRING).description("사용자 역할"),
					fieldWithPath("content[].profileImageUrl").type(JsonFieldType.STRING)
						.optional().description("프로필 이미지 URL"),
					fieldWithPath("pageable.pageNumber").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
					fieldWithPath("pageable.pageSize").type(JsonFieldType.NUMBER).description("페이지당 항목 수"),
					fieldWithPath("pageable.offset").type(JsonFieldType.NUMBER).description("데이터 시작 offset"),
					fieldWithPath("pageable.paged").type(JsonFieldType.BOOLEAN).description("페이징 여부"),
					fieldWithPath("pageable.unpaged").type(JsonFieldType.BOOLEAN).description("페이징되지 않음 여부"),
					fieldWithPath("pageable.sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 정보 비어 있음 여부"),
					fieldWithPath("pageable.sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬됨 여부"),
					fieldWithPath("pageable.sort.unsorted").type(JsonFieldType.BOOLEAN).description("정렬되지 않음 여부"),

					fieldWithPath("sort.empty").type(JsonFieldType.BOOLEAN).description("정렬 정보 비어 있음 여부"),
					fieldWithPath("sort.sorted").type(JsonFieldType.BOOLEAN).description("정렬됨 여부"),
					fieldWithPath("sort.unsorted").type(JsonFieldType.BOOLEAN).description("정렬되지 않음 여부"),

					fieldWithPath("last").type(JsonFieldType.BOOLEAN).description("마지막 페이지 여부"),
					fieldWithPath("totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
					fieldWithPath("totalElements").type(JsonFieldType.NUMBER).description("전체 데이터 수"),
					fieldWithPath("size").type(JsonFieldType.NUMBER).description("페이지 크기"),
					fieldWithPath("number").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
					fieldWithPath("first").type(JsonFieldType.BOOLEAN).description("첫 번째 페이지 여부"),
					fieldWithPath("numberOfElements").type(JsonFieldType.NUMBER).description("현재 페이지 데이터 수"),
					fieldWithPath("empty").type(JsonFieldType.BOOLEAN).description("현재 페이지가 비었는지 여부")
				)
			));
	}

	@Test
	@DisplayName("이메일 회원가입 테스트")
	void emailSignupTest() throws Exception {

		//MockMultipartFile profileImage = new MockMultipartFile("profileImage", "image.jpg", "image/jpeg", new byte[] {1, 2, 3, 4});

		mockMvc.perform(
				multipart("/api/v1/users/register")
					//.file(profileImage)
					.param("email", "dfs@example.com")
					.param("password", "Fkdlej5115")
					.param("name", "동동이")
					.param("phone", "010-1111-4543")
					.param("address", "서울")
					.param("role", "PATIENT")
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				requestParts(
					partWithName("profileImage").optional().description("프로필 이미지 (선택)")
				),
				formParameters(
					parameterWithName("email").optional().description("사용자 이메일"),
					parameterWithName("password").optional().description("비밀번호"),
					parameterWithName("name").optional().description("이름"),
					parameterWithName("phone").optional().description("전화번호"),
					parameterWithName("address").optional().description("주소"),
					parameterWithName("role").optional().description("사용자 역할")
				),
				responseFields(
					fieldWithPath("userId").type(JsonFieldType.NUMBER).description("생성된 사용자 ID"),
					fieldWithPath("role").type(JsonFieldType.STRING).description("사용자 역할")
				)
			));
	}

	@Test
	@DisplayName("소셜 회원가입 테스트")
	void socialSignupTest() throws Exception {
		//MockMultipartFile profileImage = new MockMultipartFile("profileImage", "profile.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});

		mockMvc.perform(
				multipart("/api/v1/users/social")
					//.file(profileImage)
					.param("providerId", "4271713030")
					.param("phone", "010-5678-1234")
					.param("address", "부산시")
					.param("role", "SYSTEM_ADMIN")
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				requestParts(
					partWithName("profileImage").optional().description("프로필 이미지 (선택)")
				),
				formParameters(
					parameterWithName("providerId").optional().description("소셜 로그인 제공자의 고유 식별자"),
					parameterWithName("phone").optional().description("사용자 전화번호"),
					parameterWithName("address").optional().description("사용자 주소"),
					parameterWithName("role").optional().description("사용자 역할")
				),
				responseFields(
					fieldWithPath("userId").type(JsonFieldType.NUMBER).description("생성된 사용자 ID"),
					fieldWithPath("role").type(JsonFieldType.STRING).description("사용자 역할")
				)
			));
	}

	@Test
	@DisplayName("의사 리스트 등록 테스트")
	void registerDoctorTest() throws Exception {
		AddDoctorList.DoctorInfo doctor1 = new AddDoctorList.DoctorInfo();
		ReflectionTestUtils.setField(doctor1, "email", "doctor76@example.com");
		ReflectionTestUtils.setField(doctor1, "password", "Fkdlej5115");
		ReflectionTestUtils.setField(doctor1, "name", "홍누구");
		ReflectionTestUtils.setField(doctor1, "phone", "010-1111-2222");

		AddDoctorList.DoctorInfo doctor2 = new AddDoctorList.DoctorInfo();
		ReflectionTestUtils.setField(doctor2, "email", "doctor43@example.com");
		ReflectionTestUtils.setField(doctor2, "password", "Fkdlej5115");
		ReflectionTestUtils.setField(doctor2, "name", "김철수");
		ReflectionTestUtils.setField(doctor2, "phone", "010-3333-4444");

		AddDoctorList.Request request = new AddDoctorList.Request();
		ReflectionTestUtils.setField(request, "doctorInfos", List.of(doctor1, doctor2));

		Token token = makeTokenByHospitalAdmin();

		mockMvc.perform(post("/api/v1/users/doctors")
				.cookie(
					new Cookie(TokenType.ACCESS_TOKEN.getTokenType(), token.getAccessToken()),
					new Cookie(TokenType.REFRESH_TOKEN.getTokenType(), token.getRefreshToken())
				)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				requestFields(
					fieldWithPath("doctorInfos[].email").description("의사 이메일"),
					fieldWithPath("doctorInfos[].password").description("의사 비밀번호"),
					fieldWithPath("doctorInfos[].name").description("의사 이름"),
					fieldWithPath("doctorInfos[].phone").description("의사 전화번호")
				),
				responseFields(
					fieldWithPath("ids[]").description("생성된 의사 ID 리스트")
				)
			));
	}

	@DisplayName("로그인 API 테스트")
	@Test
	void loginTest() throws Exception {
		mockMvc.perform(
				post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new Login.Request("doctor1@example.com", "securePass123!")))
			)
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				requestFields(
					fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
					fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호")
				),
				responseBody(),
				responseCookies(
					cookieWithName("accessToken").description("Access 토큰 쿠키"),
					cookieWithName("refreshToken").description("Refresh 토큰 쿠키")
				)
			));
	}

	@DisplayName("이메일 인증 코드 발송 API 테스트")
	@Test
	void sendEmailVerificationCodeTest() throws Exception {
		SendVerifyCode.Request request = new SendVerifyCode.Request();
		ReflectionTestUtils.setField(request, "email", "udevel@naver.com");

		mockMvc.perform(
				post("/api/v1/users/email/verify-code/send")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
			)
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				requestFields(
					fieldWithPath("email").type(JsonFieldType.STRING).description("인증 코드를 보낼 이메일 주소")
				)
			));
	}

	// @DisplayName("이메일 인증 코드 확인 API 테스트")
	// @Test
	// void confirmEmailVerificationCodeTest() throws Exception {
	// 	EmailVerification.Request request = new EmailVerification.Request();
	// 	ReflectionTestUtils.setField(request, "email", "udevel@naver.com");
	// 	ReflectionTestUtils.setField(request, "code", "AONZ4UL1");
	//
	// 	mockMvc.perform(
	// 			post("/api/v1/users/email/verify-code/confirm")
	// 				.contentType(MediaType.APPLICATION_JSON)
	// 				.content(objectMapper.writeValueAsString(request))
	// 		)
	// 		.andExpect(status().isOk())
	// 		.andDo(restDocs.document(
	// 			requestFields(
	// 				fieldWithPath("email").type(JsonFieldType.STRING).description("이메일 주소"),
	// 				fieldWithPath("code").type(JsonFieldType.STRING).description("이메일로 발송된 인증 코드")
	// 			)
	// 		));
	// }

	@DisplayName("비밀번호 재설정 API 테스트")
	@Test
	void resetPasswordTest() throws Exception {
		ResetPassword.Request request = new ResetPassword.Request();
		ReflectionTestUtils.setField(request, "email", "doctor1@example.com");
		ReflectionTestUtils.setField(request, "password", "Fkdlej5115");

		mockMvc.perform(
				post("/api/v1/users/password-reset")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
			)
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				requestFields(
					fieldWithPath("email").type(JsonFieldType.STRING).description("사용자의 이메일 주소"),
					fieldWithPath("password").type(JsonFieldType.STRING).description("새 비밀번호")
				)
			));
	}

	@DisplayName("비밀번호 확인 API 테스트")
	@Test
	void checkPasswordTest() throws Exception {
		User user = User.createUserByEmail("test@test.com", bCryptPasswordEncoder.encode("Fkdlej5115"), "test", "01011111111", "부산", Role.GUARDIAN,
			null);

		user = userRepository.save(user);

		CheckPassword.Request request = new CheckPassword.Request();
		ReflectionTestUtils.setField(request, "userId", user.getUserId());
		ReflectionTestUtils.setField(request, "password", "Fkdlej5115");

		Token token = tokenProvider.generateTokens(user.getUuid(), user.getRole().getKey());

		mockMvc.perform(
				post("/api/v1/users/check-password")
					.cookie(
						new Cookie(TokenType.ACCESS_TOKEN.getTokenType(), token.getAccessToken()),
						new Cookie(TokenType.REFRESH_TOKEN.getTokenType(), token.getRefreshToken())
					)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
			)
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				requestFields(
					fieldWithPath("userId").type(JsonFieldType.NUMBER).description("유저 아이디"),
					fieldWithPath("password").type(JsonFieldType.STRING).description("유저의 비밀번호")
				)
			));
	}

	@DisplayName("회원 정보 수정 API 테스트")
	@Test
	void updateUserInfoTest() throws Exception {
		//MockMultipartFile profileImage = new MockMultipartFile("profileImage", "profile.jpg", MediaType.IMAGE_JPEG_VALUE, "dummy-image-content".getBytes());

		MockMultipartHttpServletRequestBuilder builder =
			multipart("/api/v1/users/me");
		builder.with(request -> {
			request.setMethod("PATCH"); // PATCH 우회 설정
			return request;
		});

		Token token = makeTokenBySystemAdmin();

		mockMvc.perform(builder
				//.file(profileImage)
				.param("name", "하이")
				.param("email", "wow1111@naver.com")
				.param("phone", "010-3232-5678")
				.param("address", "부산광역시")
				.cookie(
					new Cookie(TokenType.ACCESS_TOKEN.getTokenType(), token.getAccessToken()),
					new Cookie(TokenType.REFRESH_TOKEN.getTokenType(), token.getRefreshToken())
				)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isNoContent())
			.andDo(restDocs.document(
				requestParts(
					partWithName("profileImage").optional().description("변경할 프로필 이미지")
				),
				formParameters(
					parameterWithName("name").optional().description("사용자 이름"),
					parameterWithName("email").optional().description("사용자 이메일"),
					parameterWithName("phone").optional().description("휴대폰 번호"),
					parameterWithName("address").optional().description("주소")
				)
			));
	}

	@DisplayName("회원 탈퇴 API 테스트")
	@Test
	void deleteAccountTest() throws Exception {

		Token token = makeTokenBySystemAdmin();

		mockMvc.perform(delete("/api/v1/users/me")
				.cookie(
					new Cookie(TokenType.ACCESS_TOKEN.getTokenType(), token.getAccessToken()),
					new Cookie(TokenType.REFRESH_TOKEN.getTokenType(), token.getRefreshToken())
				)
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isNoContent())
			.andDo(restDocs.document(
				requestCookies(
					cookieWithName("accessToken").description("엑세스 토큰 쿠키"),
					cookieWithName("refreshToken").description("리프레시 토큰 쿠키")
				)
			));
	}

	private Token makeTokenBySystemAdmin() {
		User user = User.createUserByEmail("dong112@naver.com", "Fkdlej5115",
			"짱구", "010-1111-1111", "어딘가", Role.SYSTEM_ADMIN, null);

		user = userRepository.save(user);

		return tokenProvider.generateTokens(user.getUuid(), user.getRole().getKey());
	}

	private Token makeTokenByHospitalAdmin() {
		User user = User.createUserByEmail("mang112@naver.com", "Fkdlej5115",
			"맹구", "010-2222-2222", "어딘가", Role.HOSPITAL_ADMIN, null);

		user = userRepository.save(user);

		return tokenProvider.generateTokens(user.getUuid(), user.getRole().getKey());
	}

}