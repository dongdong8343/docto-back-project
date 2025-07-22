package com.ssginc8.docto.medication.controller;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssginc8.docto.guardian.repository.PatientGuardianRepository;
import com.ssginc8.docto.medication.dto.*;
import com.ssginc8.docto.medication.entity.*;
import com.ssginc8.docto.medication.repository.*;
import com.ssginc8.docto.patient.repository.PatientRepository;
import com.ssginc8.docto.restdocs.RestDocsConfig;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.repository.UserRepository;

@ActiveProfiles("prod")
@ExtendWith(RestDocumentationExtension.class)
@SpringBootTest
@Import(RestDocsConfig.class)
class MedicationControllerTest {

	@Autowired protected RestDocumentationResultHandler restDocs;

	private MockMvc mockMvc;

	@Autowired private WebApplicationContext context;
	@Autowired private ObjectMapper objectMapper;

	@Autowired private MedicationInformationRepository medicationInformationRepository;
	@Autowired private MedicationAlertTimeRepository medicationAlertTimeRepository;
	@Autowired private MedicationAlertDayRepository medicationAlertDayRepository;
	@Autowired private MedicationLogRepository medicationLogRepository;
	@Autowired private PatientRepository patientRepository;
	@Autowired private PatientGuardianRepository patientGuardianRepository;

	private Long savedMedicationId;
	@Autowired
	private UserRepository userRepository;
	private Long savedUserId;
	private String savedUserUuid;

	@BeforeEach
	void setUp(WebApplicationContext context, RestDocumentationContextProvider restDocumentation) {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
			.apply(documentationConfiguration(restDocumentation))
			.alwaysDo(restDocs)
			.addFilters(new CharacterEncodingFilter("UTF-8", true))
			.build();

		// 데이터 초기화
		medicationLogRepository.deleteAll();
		medicationAlertDayRepository.deleteAll();
		medicationAlertTimeRepository.deleteAll();
		medicationInformationRepository.deleteAll();

		patientGuardianRepository.deleteAll();
		patientRepository.deleteAll();
		userRepository.deleteAll();

		// 고정 UUID
		String fixedUuid = "test-uuid-12345";

		String randomEmail = "test" + UUID.randomUUID().toString() + "@example.com";

		// 테스트 데이터 삽입
		User user = userRepository.save(
			User.createUser("test", "password", randomEmail, "EMAIL", "GUARDIAN", false, fixedUuid)
		);
		savedUserId = user.getUserId();
		savedUserUuid = user.getUuid();

		MedicationInformation info = medicationInformationRepository.save(
			MedicationInformation.create(user, 1L, "타이레놀") // 🔥 patientGuardianId 추가
		);
		savedMedicationId = info.getMedicationId();

		MedicationAlertTime alertTime = medicationAlertTimeRepository.save(
			MedicationAlertTime.create(info, LocalTime.now().plusHours(1))
		);

		medicationAlertDayRepository.saveAll(List.of(
			MedicationAlertDay.create(alertTime, DayOfWeek.MONDAY),
			MedicationAlertDay.create(alertTime, DayOfWeek.TUESDAY)
		));

		medicationLogRepository.save(
			MedicationLog.create(alertTime, info, MedicationStatus.TAKEN, alertTime.getTimeToTake().atDate(java.time.LocalDate.now()))
		);
	}

	@Test
	@DisplayName("복약 스케줄 등록")
	void registerMedication() throws Exception {
		MedicationScheduleRequest request = new MedicationScheduleRequest(savedUserId,
			"타이레놀", LocalTime.now().plusHours(2),
			List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), 1L);

		mockMvc.perform(post("/api/v1/medications")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				requestFields(
					fieldWithPath("userId").description("환자 ID"),
					fieldWithPath("medicationName").description("약 이름"),
					fieldWithPath("timeToTake").description("복약 시간 (HH:mm:ss 형식)"),
					fieldWithPath("days[]").description("복약 요일 리스트 (예: MONDAY, WEDNESDAY)"),
					fieldWithPath("patientGuardianId").description("보호자 ID")
				)
			));
	}

	@Test
	@WithMockUser(username = "test-uuid-12345", roles = {"USER"})
	@DisplayName("복약 로그 조회")
	void getMedicationLogs() throws Exception {
		mockMvc.perform(get("/api/v1/medications/me/logs")
				.with(user("test-uuid-1234")) // UUID를 직접 Authentication의 name에 세팅
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				responseFields(
					fieldWithPath("content[].medicationLogId").description("복약 로그 ID"),
					fieldWithPath("content[].medicationId").description("약 ID"),
					fieldWithPath("content[].status").description("복약 상태 (TAKEN, MISSED)"),
					fieldWithPath("content[].timeToTake").description("복약 예정 시간"),
					// pageable 하위 필드
					fieldWithPath("pageable.pageNumber").ignored(),
					fieldWithPath("pageable.pageSize").ignored(),
					fieldWithPath("pageable.offset").ignored(),
					fieldWithPath("pageable.paged").ignored(),
					fieldWithPath("pageable.unpaged").ignored(),
					fieldWithPath("pageable.sort.empty").ignored(),
					fieldWithPath("pageable.sort.sorted").ignored(),
					fieldWithPath("pageable.sort.unsorted").ignored(),
					fieldWithPath("totalElements").ignored(),
					fieldWithPath("totalPages").ignored(),
					fieldWithPath("size").ignored(),
					fieldWithPath("number").ignored(),
					fieldWithPath("sort.empty").ignored(),
					fieldWithPath("sort.sorted").ignored(),
					fieldWithPath("sort.unsorted").ignored(),
					fieldWithPath("first").ignored(),
					fieldWithPath("last").ignored(),
					fieldWithPath("numberOfElements").ignored(),
					fieldWithPath("empty").ignored()
				)
			));

	}

	@Test
	@DisplayName("복약 완료 처리")
	void completeMedication() throws Exception {
		MedicationCompleteRequest request = new MedicationCompleteRequest(MedicationStatus.TAKEN);

		mockMvc.perform(patch("/api/v1/medications/{medicationId}/complete", savedMedicationId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				pathParameters(
					parameterWithName("medicationId").description("약 ID")
				),
				requestFields(
					fieldWithPath("status").description("복약 상태 (TAKEN, MISSED)")
				)
			));
	}

	@Test
	@DisplayName("복약 시간 수정")
	void updateMedicationTime() throws Exception {
		MedicationUpdateRequest request = MedicationUpdateRequest.builder()
			.newTimeToTake(LocalTime.now().plusHours(3))
			.newDays(List.of(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
			.build();

		mockMvc.perform(patch("/api/v1/medications/{medicationId}", savedMedicationId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				pathParameters(
					parameterWithName("medicationId").description("약 ID")
				),
				requestFields(
					fieldWithPath("newTimeToTake").description("변경할 복약 시간 (HH:mm:ss 형식)"),
					fieldWithPath("newDays[]").description("변경할 요일 리스트 (예: MONDAY, WEDNESDAY)")
				)
			));
	}

	@Test
	@DisplayName("복약 스케줄 삭제")
	void deleteMedicationSchedule() throws Exception {
		mockMvc.perform(delete("/api/v1/medications/{medicationId}", savedMedicationId))
			.andExpect(status().isNoContent())
			.andDo(restDocs.document(
				pathParameters(
					parameterWithName("medicationId").description("삭제할 약 ID")
				)
			));
	}
}
