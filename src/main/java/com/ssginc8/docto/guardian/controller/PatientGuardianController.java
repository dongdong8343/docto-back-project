package com.ssginc8.docto.guardian.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ssginc8.docto.auth.provider.CurrentUserProvider;
import com.ssginc8.docto.guardian.dto.GuardianInfoResponse;
import com.ssginc8.docto.guardian.dto.GuardianInviteRequest;
import com.ssginc8.docto.guardian.dto.GuardianInviteResponse;
import com.ssginc8.docto.guardian.dto.GuardianStatusRequest;
import com.ssginc8.docto.guardian.dto.PatientSummaryResponse;
import com.ssginc8.docto.guardian.dto.PendingInviteResponse;
import com.ssginc8.docto.guardian.service.PatientGuardianService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/guardians")
public class PatientGuardianController {

	private final CurrentUserProvider currentUserProvider;
	private final PatientGuardianService guardianService;

	@PatchMapping("/request/{requestId}")
	public ResponseEntity<Void> respond(
		@PathVariable Long requestId,
		@RequestBody GuardianStatusRequest request) {

		guardianService.updateStatus(requestId, request.getInviteCode(), request.getStatus());
		return ResponseEntity.ok().build();
	}

	@PatchMapping("/respond")
	// @CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
	public ResponseEntity<Void> respondByInviteCode(
		@RequestBody GuardianStatusRequest request) {
		guardianService.updateStatusByInviteCode(request.getInviteCode(), request.getStatus());
		return ResponseEntity.ok().build();
	}

	/**
	 * 환자별 “초대중” 보호자 목록 조회
	 */
	@GetMapping("/{patientId}/pending-invites")
	public ResponseEntity<List<PendingInviteResponse>> getPendingInvites(
		@PathVariable Long patientId
	) {
		List<PendingInviteResponse> list = guardianService.getPendingInvites(patientId);
		return ResponseEntity.ok(list);
	}

	@DeleteMapping("/{userId}/patients/{patientId}")
	public ResponseEntity<Void> deleteMapping(@PathVariable Long userId, @PathVariable Long patientId) {
		guardianService.deleteMapping(userId, patientId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{patientId}/invite")
	public ResponseEntity<GuardianInviteResponse> inviteGuardian(
		@PathVariable Long patientId,
		@RequestBody GuardianInviteRequest request) {

		return ResponseEntity.ok(guardianService.inviteGuardian(patientId, request.getGuardianEmail()));
	}

	@GetMapping("/me/patients")
	public ResponseEntity<List<PatientSummaryResponse>> getAllAcceptedMappings() {
		Long guardianId = currentUserProvider.getUserFromUserId().getUserId();
		List<PatientSummaryResponse> patients = guardianService.getAllAcceptedMappings(guardianId);
		return ResponseEntity.ok(patients);
	}

	/**
	 * 내(로그인한 보호자) – 특정 환자 연결 해제 (soft‑delete)
	 */
	@DeleteMapping("/me/patients/{patientId}")
	public ResponseEntity<Void> deleteMyMapping(@PathVariable Long patientId) {
		Long guardianId = currentUserProvider.getUserFromUserId().getUserId();
		guardianService.deleteMapping(guardianId, patientId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/me")
	public ResponseEntity<GuardianInfoResponse> getMyGuardianInfo() {
		var user = currentUserProvider.getUserFromUserId(); // 현재 로그인된 유저

		var response = GuardianInfoResponse.builder()
			.guardianId(user.getUserId()) // userId가 guardianId
			.name(user.getName())
			.email(user.getEmail())
			.role(user.getRole())
			.build();

		return ResponseEntity.ok(response);
	}
}
