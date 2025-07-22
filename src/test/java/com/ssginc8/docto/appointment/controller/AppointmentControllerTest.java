package com.ssginc8.docto.appointment.controller;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.snippet.Attributes.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssginc8.docto.appointment.dto.AppointmentRequest;
import com.ssginc8.docto.appointment.dto.RescheduleRequest;
import com.ssginc8.docto.appointment.dto.UpdateRequest;
import com.ssginc8.docto.auth.jwt.dto.Token;
import com.ssginc8.docto.auth.jwt.dto.TokenType;
import com.ssginc8.docto.auth.jwt.provider.TokenProvider;
import com.ssginc8.docto.restdocs.RestDocsConfig;
import com.ssginc8.docto.user.entity.Role;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.repository.UserRepository;

import jakarta.servlet.http.Cookie;

@ActiveProfiles("prod")
@ExtendWith(RestDocumentationExtension.class)
@SpringBootTest
@Import(RestDocsConfig.class)
public class AppointmentControllerTest {

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

	@BeforeEach
	public void setUp(WebApplicationContext context, RestDocumentationContextProvider restDocumentation) {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
			.apply(documentationConfiguration(restDocumentation))
			// .apply(springSecurity())
			.alwaysDo(MockMvcResultHandlers.print())
			.alwaysDo(restDocs)
			.addFilters(new CharacterEncodingFilter("UTF-8", true)) // 한글 깨짐 방지
			.build();

	}

	@Test
	@DisplayName("예약 접수 테스트")
	void requestAppointment() throws Exception {
		AppointmentRequest request = AppointmentRequest.builder()
			.userId(2L)
			.patientId(1L)
			.hospitalId(1L)
			.doctorId(1L)
			.symptom("기침과 열")
			.question("감기일까요?")
			.appointmentType("SCHEDULED")
			.paymentType("ONSITE")
			.appointmentTime(LocalDateTime.now().plusDays(1))
			.build();

		mockMvc.perform(post("/api/v1/appointments")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				requestFields(
					fieldWithPath("userId").type(JsonFieldType.NUMBER).description("보호자 ID"),
					fieldWithPath("patientId").type(JsonFieldType.NUMBER).description("환자 ID"),
					fieldWithPath("hospitalId").type(JsonFieldType.NUMBER).description("병원 ID"),
					fieldWithPath("doctorId").type(JsonFieldType.NUMBER).description("의사 ID"),
					fieldWithPath("symptom").type(JsonFieldType.STRING).description("증상"),
					fieldWithPath("question").type(JsonFieldType.STRING).description("추가 질문").optional(),
					fieldWithPath("appointmentType").type(JsonFieldType.STRING).description("예약 타입"),
					fieldWithPath("paymentType").type(JsonFieldType.STRING).description("결제 방식"),
					fieldWithPath("appointmentTime").type(JsonFieldType.STRING).description("예약 시간 (ISO-8601)")
				)
			));
	}

	@Test
	@DisplayName("예약 상세 조회")
	void getAppointmentDetail() throws Exception {
		mockMvc.perform(get("/api/v1/appointments/{appointmentId}", 1L))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				pathParameters(
					parameterWithName("appointmentId").description("예약 ID")
				),
				responseFields(
					fieldWithPath("appointmentId").type(JsonFieldType.NUMBER).description("예약 ID"),
					fieldWithPath("hospitalId").type(JsonFieldType.NUMBER).description("병원 ID"),
					fieldWithPath("doctorId").type(JsonFieldType.NUMBER).description("의사 ID"),
					fieldWithPath("patientGuardianId").type(JsonFieldType.NUMBER).description("보호자 ID"),

					fieldWithPath("hospitalName").type(JsonFieldType.STRING).description("병원명"),
					fieldWithPath("doctorName").type(JsonFieldType.STRING).description("의사 이름"),
					fieldWithPath("patientName").type(JsonFieldType.STRING).description("환자 이름"),

					fieldWithPath("symptom").type(JsonFieldType.STRING).description("증상"),
					fieldWithPath("question").type(JsonFieldType.STRING).description("추가 질문"),

					fieldWithPath("appointmentTime").type(JsonFieldType.STRING).description("예약 시간 (ISO-8601 형식)"),
					fieldWithPath("appointmentType").type(JsonFieldType.STRING).description("예약 유형 (SCHEDULED, IMMEDIATE, TELEMEDICINE)"),
					fieldWithPath("paymentType").type(JsonFieldType.STRING).description("결제 방식 (예: ONSITE, ONLINE 등)"),
					fieldWithPath("status").type(JsonFieldType.STRING).description("예약 상태 (REQUESTED, CONFIRMED, COMPLETED, CANCELLED)"),

					fieldWithPath("createdAt").type(JsonFieldType.STRING).description("예약 생성 시간 (ISO-8601 형식)")
				)
			))
		;
	}

	@Test
	@DisplayName("예약 리스트 조회")
	void getAppointmentList() throws Exception {
		mockMvc.perform(get("/api/v1/appointments")
				.param("userId", "1")
				.param("page", "0")
				.param("size", "5"))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				queryParameters(
					parameterWithName("userId")
						.attributes(key("constraints").value("숫자 입력"))
						.description("Optional, 유저 ID").optional(),
					parameterWithName("hospitalId")
						.attributes(key("constraints").value("숫자 입력"))
						.description("Optional, 병원 ID").optional(),
					parameterWithName("doctorId")
						.attributes(key("constraints").value("숫자 입력"))
						.description("Optional, 의사 ID").optional(),
					parameterWithName("page")
						.attributes(
							key("constraints").value("0 이상의 정수"),
							key("defaultValue").value("0"))
						.description("Optional, 페이지 번호 (0부터 시작)")
						.optional(),
					parameterWithName("size")
						.attributes(
							key("constraints").value("1 이상의 정수"),
							key("defaultValue").value("10"))
						.description("Optional, 페이지당 항목 수")
						.optional()
				)
			));
	}

	@Test
	@DisplayName("내 예약 리스트 조회")
	void getMyAppointmentList() throws Exception {

		Token token = makeTokenByHospitalAdmin();

		mockMvc.perform(get("/api/v1/users/me/appointments")
				.cookie(
					new Cookie(TokenType.ACCESS_TOKEN.getTokenType(), token.getAccessToken()),
					new Cookie(TokenType.REFRESH_TOKEN.getTokenType(), token.getRefreshToken())
				))
			.andExpect(status().isOk())
			.andDo(restDocs.document(

			));
	}

	@Test
	@DisplayName("예약 상태 변경")
	void updateAppointmentStatus() throws Exception {
		UpdateRequest request = new UpdateRequest();
		request.setStatus("CONFIRMED");

		mockMvc.perform(patch("/api/v1/appointments/{appointmentId}/status", 2L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				pathParameters(
					parameterWithName("appointmentId").description("예약 ID")
				),
				requestFields(
					fieldWithPath("status").type(JsonFieldType.STRING).description("예약 상태 (REQUESTED, CONFIRMED, COMPLETED, CANCELLED)")
				)
			));
	}

	@Test
	@DisplayName("재예약")
	void rescheduleAppointment() throws Exception {
		RescheduleRequest request = new RescheduleRequest();
		request.setNewTime(String.valueOf(LocalDateTime.now().plusDays(2)));

		mockMvc.perform(post("/api/v1/appointments/{appointmentId}/reschedule", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				pathParameters(
					parameterWithName("appointmentId").description("예약 ID")
				),
				requestFields(
					fieldWithPath("newTime")
						.type(JsonFieldType.STRING)
						.description("새로운 예약 시간")
						.attributes(
							key("format").value("yyyy-MM-dd'T'HH:mm:ss"),
							key("constraints").value("현재 시각 이후의 날짜여야 함")
						)
				)
			));
	}

	private Token makeTokenByHospitalAdmin() {
		User user = User.createUserByEmail("mang114@naver.com", "Fkdlej5115",
			"맹구", "010-2222-2222", "어딘가", Role.HOSPITAL_ADMIN, null);

		user = userRepository.save(user);

		return tokenProvider.generateTokens(user.getUuid(), user.getRole().getKey());
	}
}
