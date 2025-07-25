package com.ssginc8.docto.patient.controller;

import java.util.List;

import com.ssginc8.docto.auth.provider.CurrentUserProvider;
import com.ssginc8.docto.guardian.dto.GuardianResponse;
import com.ssginc8.docto.guardian.service.PatientGuardianService;
import com.ssginc8.docto.patient.dto.PatientRequest;
import com.ssginc8.docto.patient.dto.PatientResponse;
import com.ssginc8.docto.patient.service.PatientService;
import com.ssginc8.docto.user.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 환자 관련 API 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/patients")
public class PatientController {

	private final CurrentUserProvider currentUserProvider;
	private final PatientService patientService;
	private final PatientGuardianService guardianService;

	/**
	 * 환자 등록 API
	 */
	@PostMapping
	public ResponseEntity<Long> create(@RequestBody PatientRequest dto) {
		return ResponseEntity.ok(patientService.createPatient(dto));
	}

	/**
	 * 환자 삭제 API (soft delete)
	 */
	@DeleteMapping("/{patientId}")
	public ResponseEntity<Void> delete(@PathVariable Long patientId) {
		patientService.deletePatient(patientId);
		return ResponseEntity.noContent().build();
	}

	/**
	 * 내(로그인된 환자) 입장에서
	 * 특정 보호자 연결 해제(soft‑delete)
	 */
	@DeleteMapping("/me/guardians/{mappingId}")
	public ResponseEntity<Void> removeMyGuardian(
		@PathVariable Long mappingId
	) {
		// 1) 로그인된 보호자 → userId
		Long guardianUserId = currentUserProvider.getUserFromUuid().getUserId();

		// 2) userId → 환자ID 조회
		Long patientId = patientService
			.getPatientByUserId(guardianUserId)
			.getPatientId();

		// 3) mappingId 로 삭제
		guardianService.deleteMappingByMappingId(mappingId);

		return ResponseEntity.noContent().build();
	}

	/**
	 * 보호자 목록 조회 API
	 */
	@GetMapping("/{patientId}/guardians")
	public ResponseEntity<List<GuardianResponse>> getGuardians(@PathVariable Long patientId) {
		List<GuardianResponse> guardians = guardianService.getGuardiansByPatientId(patientId);
		return ResponseEntity.ok(guardians);
	}

	@GetMapping("/me")
	public ResponseEntity<PatientResponse> getMyPatientInfo() {
		Long userId = currentUserProvider.getUserFromUuid().getUserId();
		PatientResponse patientInfo = patientService.getPatientByUserId(userId);
		return ResponseEntity.ok(patientInfo);
	}
}
