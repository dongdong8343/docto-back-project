package com.ssginc8.docto.guardian.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssginc8.docto.guardian.dto.GuardianStatusRequest;
import com.ssginc8.docto.guardian.entity.PatientGuardian;
import com.ssginc8.docto.guardian.entity.Status;
import com.ssginc8.docto.guardian.repository.PatientGuardianRepository;
import com.ssginc8.docto.patient.entity.Patient;
import com.ssginc8.docto.patient.repository.PatientRepository;
import com.ssginc8.docto.restdocs.RestDocsConfig;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.repository.UserRepository;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("prod")
@ExtendWith(RestDocumentationExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
@Import(RestDocsConfig.class)
class GuardianControllerTest {

	@Autowired protected RestDocumentationResultHandler restDocs;
	private MockMvc mockMvc;

	@Autowired private UserRepository userRepository;
	@Autowired private PatientRepository patientRepository;
	@Autowired private PatientGuardianRepository patientGuardianRepository;
	@Autowired private ObjectMapper objectMapper;

	private User guardianUser;
	private Patient patient;
	private PatientGuardian patientGuardian;

	@BeforeEach
	void setUp(WebApplicationContext context, RestDocumentationContextProvider restDocumentation) {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
			.apply(documentationConfiguration(restDocumentation))
			.alwaysDo(restDocs)
			.addFilters(new CharacterEncodingFilter("UTF-8", true))
			.build();

		// User 엔티티 생성
		String randomEmail = "guardian_" + UUID.randomUUID() + "@example.com";
		guardianUser = User.createUser(
			"guardian-user", "password", randomEmail,
			"EMAIL", "GUARDIAN", false, UUID.randomUUID().toString()
		);
		userRepository.save(guardianUser);

		// Patient 엔티티 생성
		patient = Patient.create(guardianUser, "암호화된주민등록번호");
		patientRepository.save(patient);

		// PatientGuardian 엔티티 생성
		patientGuardian = PatientGuardian.create(guardianUser, patient, LocalDateTime.now());
		patientGuardian.updateInviteCode("test-invite-code"); // 초대코드 덮어쓰기
		patientGuardianRepository.save(patientGuardian);

		// ✅ Mock SecurityContext
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			guardianUser.getUuid(), null, null
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	@Test
	@DisplayName("보호자 권한 수락/거절 API")
	void respondToGuardianRequest() throws Exception {
		GuardianStatusRequest request = new GuardianStatusRequest(Status.ACCEPTED.name(), "test-invite-code");

		mockMvc.perform(patch("/api/v1/guardians/request/{requestId}", patientGuardian.getPatientGuardianId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andDo(document("guardian-controller-test/respond-to-guardian-request",
				pathParameters(
					parameterWithName("requestId").description("보호자 권한 요청 ID")
				),
				requestFields(
					fieldWithPath("status").description("수락(ACCEPTED) 또는 거절(REJECTED) 상태"),
					fieldWithPath("inviteCode").description("초대 코드")
				)
			));
	}

	@Test
	@DisplayName("보호자-환자 매핑 해제 API")
	void deleteGuardianMapping() throws Exception {
		mockMvc.perform(delete("/api/v1/guardians/{userId}/patients/{patientId}",
				guardianUser.getUserId(), patient.getPatientId()))
			.andExpect(status().isNoContent())
			.andDo(document("guardian-controller-test/delete-guardian-mapping",
				pathParameters(
					parameterWithName("userId").description("보호자 유저 ID"),
					parameterWithName("patientId").description("환자 ID")
				)
			));
	}

	@Test
	@DisplayName("보호자가 가진 환자 목록 조회 API")
	void getAllAcceptedPatients() throws Exception {
		// 수락 상태로 바꿔야 조회 가능
		patientGuardian.updateStatus(Status.ACCEPTED);
		patientGuardianRepository.save(patientGuardian);

		mockMvc.perform(get("/api/v1/guardians/me/patients"))
			.andExpect(status().isOk())
			.andDo(document("guardian-controller-test/get-all-accepted-patients"));
	}
}