package com.ssginc8.docto.patients.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssginc8.docto.patient.dto.PatientRequest;
import com.ssginc8.docto.patient.entity.Patient;
import com.ssginc8.docto.patient.repository.PatientRepository;
import com.ssginc8.docto.restdocs.RestDocsConfig;
import com.ssginc8.docto.user.entity.Role;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

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

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

@ActiveProfiles("prod")
@ExtendWith(RestDocumentationExtension.class)
@SpringBootTest
@Import(RestDocsConfig.class)
public class PatientControllerTest {

	@Autowired
	protected RestDocumentationResultHandler restDocs;
	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private EntityManager em;
	@Autowired
	private PatientRepository patientRepository;
	@Autowired
	private UserRepository userRepository;

	private Long savedPatientId;
	private Long testUserId;
	private String guardianEmail;

	@BeforeEach
	void setUp(RestDocumentationContextProvider provider) {
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
			.apply(documentationConfiguration(provider))
			.alwaysDo(restDocs)
			.addFilters(new CharacterEncodingFilter("UTF-8", true))
			.build();

		// 테스트용 유저 생성
		String uniqueId = String.valueOf(System.currentTimeMillis());
		User user = User.createUserByEmail(
			"user" + uniqueId + "@example.com",
			null,
			null,
			null,
			null,
			Role.PATIENT,
			null
		);
		User savedUser = userRepository.save(user);
		testUserId = savedUser.getUserId();

		// 해당 유저로 환자 생성
		Patient patient = patientRepository.save(Patient.create(savedUser, "900101-1234567"));
		savedPatientId = patient.getPatientId();

		// 테스트용 보호자 유저 생성
		guardianEmail = "guardian" + System.currentTimeMillis() + "@example.com";
		User guardianUser = User.createUserByEmail(
			guardianEmail,
			null,
			null,
			null,
			null,
			Role.GUARDIAN,
			null
		);
		userRepository.save(guardianUser);
	}

	@Test
	@DisplayName("환자 등록")
	void createPatient() throws Exception {
		String uniqueId = String.valueOf(System.currentTimeMillis() + 1);
		User user = User.createUserByEmail(
			"user" + uniqueId + "@example.com",
			null,
			null,
			null,
			null,
			Role.PATIENT,
			null
		);
		User savedNewUser = userRepository.save(user);

		PatientRequest request = new PatientRequest(savedNewUser.getUserId(), "010101-2345678");

		mockMvc.perform(post("/api/v1/patients")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isNumber()) // Long 타입이면 숫자 확인
			.andDo(restDocs.document(
				requestFields(
					fieldWithPath("userId").description("User ID"),
					fieldWithPath("residentRegistrationNumber").description("주민등록번호")
				)
				// 📌 responseFields 삭제! Long 하나는 문서화 안 해.
			));
	}

	@Test
	@DisplayName("환자 전체 조회")
	void getAllPatients() throws Exception {
		mockMvc.perform(get("/api/v1/admin/patients"))
			.andExpect(status().isOk())
			.andDo(restDocs.document(
				responseFields(
					fieldWithPath("content[].patientId").description("환자 ID"),
					fieldWithPath("content[].userId").description("유저 ID"),
					fieldWithPath("content[].residentRegistrationNumber").description("주민등록번호"),
					fieldWithPath("pageable.sort.empty").ignored(),
					fieldWithPath("pageable.sort.sorted").ignored(),
					fieldWithPath("pageable.sort.unsorted").ignored(),
					fieldWithPath("pageable.offset").ignored(),
					fieldWithPath("pageable.pageNumber").ignored(),
					fieldWithPath("pageable.pageSize").ignored(),
					fieldWithPath("pageable.paged").ignored(),
					fieldWithPath("pageable.unpaged").ignored(),
					fieldWithPath("sort.empty").ignored(),
					fieldWithPath("sort.sorted").ignored(),
					fieldWithPath("sort.unsorted").ignored(),
					fieldWithPath("totalPages").ignored(),
					fieldWithPath("totalElements").ignored(),
					fieldWithPath("last").ignored(),
					fieldWithPath("size").ignored(),
					fieldWithPath("number").ignored(),
					fieldWithPath("first").ignored(),
					fieldWithPath("numberOfElements").ignored(),
					fieldWithPath("empty").ignored()
				)
			));
	}


	@Test
	@DisplayName("환자 삭제 (소프트 삭제)")
	void deletePatient() throws Exception {
		mockMvc.perform(delete("/api/v1/patients/{patientId}", savedPatientId))
			.andExpect(status().isNoContent())
			.andDo(restDocs.document(
				pathParameters(
					parameterWithName("patientId").description("삭제할 환자 ID")
				)
			));
	}

	@Test
	@DisplayName("보호자 초대")
	void inviteGuardian() throws Exception {
		mockMvc.perform(post("/api/v1/guardians/{patientId}/invite", savedPatientId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("guardianEmail", guardianEmail))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.inviteCode").exists())
			.andDo(restDocs.document(
				pathParameters(
					parameterWithName("patientId").description("환자 ID")
				),
				requestFields(
					fieldWithPath("guardianEmail").description("초대할 보호자 이메일")
				),
				responseFields(
					fieldWithPath("inviteCode").description("초대 코드")
				)
			));
	}
}
