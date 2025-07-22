package com.ssginc8.docto.review.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.restdocs.http.HttpDocumentation.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssginc8.docto.auth.jwt.dto.Token;
import com.ssginc8.docto.auth.jwt.dto.TokenType;
import com.ssginc8.docto.auth.jwt.provider.TokenProvider;
import com.ssginc8.docto.restdocs.RestDocsConfig;
import com.ssginc8.docto.review.dto.ReviewCreateRequest;
import com.ssginc8.docto.review.dto.ReviewUpdateRequest;
import com.ssginc8.docto.review.repository.ReviewRepository;
import com.ssginc8.docto.user.entity.Role;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.repository.UserRepository;

import jakarta.servlet.http.Cookie;

@ActiveProfiles("prod")
@ExtendWith(RestDocumentationExtension.class)
@SpringBootTest
@Import(RestDocsConfig.class)
public class ReviewControllerTest {

	@Autowired
	protected RestDocumentationResultHandler restDocs;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TokenProvider tokenProvider;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ReviewRepository reviewRepository;




	@BeforeEach
	public void setUp(WebApplicationContext context, RestDocumentationContextProvider restDocumentation) {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
			.apply(documentationConfiguration(restDocumentation))
			.alwaysDo(restDocs)
			.apply(springSecurity())
			.addFilters(new CharacterEncodingFilter("UTF-8", true))
			.build();
	}

	@Test
	@DisplayName("리뷰 생성")
	@Sql(statements = {
		"DELETE FROM tbl_review_keyword",
		"DELETE FROM tbl_review"
	})
	@WithMockUser(username = "c017604f-290b-482e-b62d-42bb9bf34440", roles = {"PATIENT"})
	void reviewCreate() throws Exception {
		ReviewCreateRequest request = new ReviewCreateRequest();
		request.setUserId(22L);
		request.setHospitalId(2L);
		request.setDoctorId(5L);
		request.setAppointmentId(5L);
		request.setContents("친절하고 전문적이었습니다.");
		request.setKeywords(List.of("CLEAN_HOSPITAL", "FAST", "THOROUGH"));

		mockMvc.perform(post("/api/v1/reviews")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(content().string(matchesPattern("\\d+")))
			.andDo(restDocs.document(
				requestFields(
					fieldWithPath("userId").type(JsonFieldType.NUMBER).description("리뷰 작성자 유저 ID"),
					fieldWithPath("hospitalId").type(JsonFieldType.NUMBER).description("병원 ID"),
					fieldWithPath("doctorId").type(JsonFieldType.NUMBER).description("의사 ID"),
					fieldWithPath("appointmentId").type(JsonFieldType.NUMBER).description("예약 ID"),
					fieldWithPath("keywords[]").type(JsonFieldType.ARRAY).description("키워드 목록 (3~8개)"),
					fieldWithPath("contents").type(JsonFieldType.STRING).description("리뷰 내용 (최대 1000자)")
				),
				responseBody()

			));
	}



	@Test
	@DisplayName("병원에서 리뷰 조회")
	void getAllReviews() throws Exception {
		mockMvc.perform(get("/api/v1/hospitals/{hospitalId}/reviews", 2L)
				.param("page", "0")
				.param("size", "5"))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				pathParameters(
					parameterWithName("hospitalId")
						.attributes(key("constraints").value("숫자 입력"))
						.description("Required, 병원 ID")
				),
				queryParameters(
					parameterWithName("page")
						.attributes(
							key("constraints").value("0 이상의 정수"),
							key("defaultValue").value("0"))
						.description("Optional, 페이지 번호 (0부터 시작)").optional(),
					parameterWithName("size")
						.attributes(
							key("constraints").value("1 이상의 정수"),
							key("defaultValue").value("5"))
						.description("Optional, 페이지당 항목 수").optional()
				)
			));
	}

	@Test
	@DisplayName("관리자 전체 리뷰 조회")
	void getAllReviewsAdmin() throws Exception {
		mockMvc.perform(get("/api/v1/admin/reviews")
				.param("page", "0")
				.param("size", "5"))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				queryParameters(
					parameterWithName("page")
						.attributes(
							key("constraints").value("0 이상의 정수"),
							key("defaultValue").value("0"))
						.description("Optional, 페이지 번호 (0부터 시작)")
						.optional(),
					parameterWithName("size")
						.attributes(
							key("constraints").value("1 이상의 정수"),
							key("defaultValue").value("5"))
						.description("Optional, 페이지당 항목 수")
						.optional()
				)
			));
	}

	@Test
	@DisplayName("내 리뷰 리스트 조회")
	void getMyReviewList() throws Exception {
		Token token = makeTokenByPatient();

		mockMvc.perform(get("/api/v1/users/me/reviews")
				.cookie(
					new Cookie(TokenType.ACCESS_TOKEN.getTokenType(), token.getAccessToken()),
					new Cookie(TokenType.REFRESH_TOKEN.getTokenType(), token.getRefreshToken())
				)
				.param("page", "0")
				.param("size", "5")
				.accept(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isOk())
			.andDo(document("get-my-reviews",

				queryParameters(
					parameterWithName("page")
						.attributes(
							key("constraints").value("0 이상의 정수"),
							key("defaultValue").value("0")
						)
						.description("Optional, 페이지 번호 (0부터 시작)").optional(),
					parameterWithName("size")
						.attributes(
							key("constraints").value("1 이상의 정수"),
							key("defaultValue").value("5")
						)
						.description("Optional, 페이지당 항목 수").optional()
				)
			)
			);
	}






	@Test
	@DisplayName("리뷰 수정")
	void reviewUpdate() throws Exception {
		ReviewUpdateRequest request = new ReviewUpdateRequest();
		request.setContents("업데이트된 리뷰 내용입니다.");
		request.setKeywords(List.of("WANT_RETURN", "THOROUGH"));

		mockMvc.perform(patch("/api/v1/reviews/{reviewId}", 125L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())

			.andExpect(jsonPath("$.reviewId").value(125))
			.andExpect(jsonPath("$.contents").value("업데이트된 리뷰 내용입니다."))
			.andExpect(jsonPath("$.keywords", hasSize(2)))
			.andExpect(jsonPath("$.keywords", containsInAnyOrder("WANT_RETURN", "THOROUGH")))
			.andExpect(jsonPath("$.updatedAt").value(notNullValue()))

			.andDo(restDocs.document(
				requestFields(
					fieldWithPath("contents").type(JsonFieldType.STRING).description("수정할 리뷰 내용"),
					fieldWithPath("keywords[]").type(JsonFieldType.ARRAY).description("수정할 키워드 목록")),
				responseFields(
					fieldWithPath("reviewId").type(JsonFieldType.NUMBER).description("수정된 리뷰 ID"),
					fieldWithPath("contents").type(JsonFieldType.STRING).description("수정된 리뷰 내용"),
					fieldWithPath("keywords[]").type(JsonFieldType.ARRAY).description("수정된 키워드 목록"),
					fieldWithPath("createdAt").type(JsonFieldType.STRING).description("리뷰 생성 일시 (ISO-8601)"),
					fieldWithPath("updatedAt").type(JsonFieldType.STRING).description("리뷰 최종 수정 일시 (ISO-8601)")
				)
			));
	}



	@Test
	@DisplayName("리뷰 삭제")
	void reviewDelete() throws Exception {
		mockMvc.perform(
				delete("/api/v1/reviews/{reviewId}", 125L)
			)
			.andExpect(status().isNoContent())
			.andDo(restDocs.document(
				pathParameters(parameterWithName("reviewId").description("삭제할 리뷰 ID")),
				httpResponse()
			));

	}


	@Test
	@DisplayName("리뷰 신고 기능")
	void reportReview() throws Exception {
		Long reviewId = 125L;

		mockMvc.perform(
				post("/api/v1/reviews/{reviewId}/report", reviewId)
			)
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				pathParameters(
					parameterWithName("reviewId").description("신고할 리뷰 ID")
				),
				httpResponse()
			));
	}

	private Token makeTokenByPatient() {
		User user = User.createUserByEmail("jiin001028@naver.com", "Fkdl4ej5115",
			"짱아", "010-2222-2522", "대연동", Role.PATIENT, null);

		user = userRepository.save(user);


		return tokenProvider.generateTokens(user.getUuid(), user.getRole().getKey());
	}

}


