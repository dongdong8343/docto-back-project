package com.ssginc8.docto.guardian.provider;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ssginc8.docto.global.error.exception.guardianException.GuardianNotFoundException;
import com.ssginc8.docto.global.error.exception.guardianException.GuardianRequestNotFoundException;
import com.ssginc8.docto.guardian.entity.PatientGuardian;
import com.ssginc8.docto.guardian.entity.Status;
import com.ssginc8.docto.guardian.repository.PatientGuardianRepository;
import com.ssginc8.docto.patient.entity.Patient;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.global.error.exception.guardianException.GuardianMappingNotFoundException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientGuardianProvider {

	private final PatientGuardianRepository patientGuardianRepository;

	@Transactional
	public PatientGuardian save(PatientGuardian patientGuardian) {
		return patientGuardianRepository.save(patientGuardian);
	}

	public List<PatientGuardian> getPatientGuardianListByGuardian(User user) {
		return patientGuardianRepository.findAllByUser(user);
	}

	@Transactional
	public void deleteMapping(Long guardianId, Long patientId) {
		patientGuardianRepository.softDeleteByUserIdAndPatientId(guardianId, patientId);
	}

	public PatientGuardian validateAndGetPatientGuardian(User guardian, Patient patient) {
		return patientGuardianRepository.findByUserAndPatient(guardian, patient)
			.filter(pg -> pg.getDeletedAt() == null) // 논리삭제 제외
			.filter(pg -> pg.getStatus() == Status.ACCEPTED) // 수락된 상태만
			.orElseThrow(GuardianMappingNotFoundException::new); // 보호자-환자 매핑 없으면 에러
	}

	@Transactional(readOnly = true)
	public List<PatientGuardian> getAllAcceptedGuardiansByPatientId(Long patientId) {
		// return patientGuardianRepository.findByPatient_PatientIdAndStatusAndDeletedAtIsNull(patientId, Status.ACCEPTED);
		return patientGuardianRepository
			.findByPatient_PatientIdAndStatus(patientId, Status.ACCEPTED)
			.stream()
			// deletedAt이 null인(=아직 활성화된) 매핑만 남긴다
			.filter(pg -> pg.getDeletedAt() == null)
			.collect(Collectors.toList());
	}

	public Optional<PatientGuardian> findByUserAndPatient(User guardian, Patient patient) {
		return patientGuardianRepository.findByUserAndPatient(guardian, patient);
	}

	@Transactional(readOnly = true)
	public Optional<PatientGuardian> findPendingOrAcceptedMapping(User guardian, Patient patient) {
		return patientGuardianRepository.findByUserAndPatient(guardian, patient)
			.filter(pg -> pg.getDeletedAt() == null) // 논리 삭제 안 된 것만
			.filter(pg -> pg.getStatus() == Status.PENDING || pg.getStatus() == Status.ACCEPTED); // 초대 대기 중 또는 수락된 보호자
	}

	// public PatientGuardian findPendingMapping(User guardian, Patient patient) {
	// 	return patientGuardianRepository.findByUserAndPatientAndStatus(guardian, patient, Status.PENDING)
	// 		.orElse(null); // 없으면 null
	// }

	/**
	 *
	 * @param patientGuardianId
	 * @return
	 */
	@Transactional(readOnly = true)
	public PatientGuardian getPatientGuardianById(Long patientGuardianId) {
		return patientGuardianRepository.findByIdWithPatientAndUser(patientGuardianId)
			.orElseThrow(GuardianNotFoundException::new);
	}

	public PatientGuardian getById(Long requestId) {
		return patientGuardianRepository.findById(requestId)
			.filter(pg -> pg.getDeletedAt() == null)
			.orElseThrow(GuardianRequestNotFoundException::new);
	}

	public PatientGuardian getMapping(Long guardianId, Long patientId) {
		return patientGuardianRepository.findByUserIdAndPatientId(guardianId, patientId)
			.filter(pg -> pg.getDeletedAt() == null)
			.orElseThrow(GuardianMappingNotFoundException::new);
	}

	public List<PatientGuardian> getAllAcceptedMappings(Long guardianId) {
		return patientGuardianRepository.findAcceptedPatientsByUserId(guardianId);
	}

	/**
	 * inviteCode 로 조회할 때, 동일 inviteCode 매핑이 여러 개일 수 있으므로
	 * 최신 invitedAt 기준으로 하나만 꺼내 반환합니다.
	 */
	public PatientGuardian findByInviteCode(String inviteCode) {
		return patientGuardianRepository.findAllByInviteCode(inviteCode).stream()
			.filter(pg -> pg.getDeletedAt() == null)                  // soft‑delete 제외
			.max(Comparator.comparing(PatientGuardian::getInvitedAt)) // 초대 시각 최신순
			.orElseThrow(() -> new GuardianRequestNotFoundException());
	}

	/** 특정 환자에 대해 PENDING 상태인 매핑만 반환 */
	public List<PatientGuardian> getPendingInvitesByPatientId(Long patientId) {
		return patientGuardianRepository
			.findByPatient_PatientIdAndStatus(patientId, Status.PENDING)
			.stream()
			.filter(pg -> pg.getDeletedAt() == null)
			.collect(Collectors.toList());
	}
	/**
	 * PENDING 상태인 매핑 중, invitedAt 기준으로 가장 최근 매핑 하나만 반환
	 */
	public PatientGuardian findLatestPendingMapping(User guardian, Patient patient) {
		return patientGuardianRepository
			.findAllByUserAndPatientAndStatus(guardian, patient, Status.PENDING)
			.stream()
			.filter(pg -> pg.getDeletedAt() == null)  // soft‑delete 제외
			.max(Comparator.comparing(PatientGuardian::getInvitedAt))
			.orElse(null);
	}
}
