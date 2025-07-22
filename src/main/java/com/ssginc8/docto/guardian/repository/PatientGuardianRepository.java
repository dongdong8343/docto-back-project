package com.ssginc8.docto.guardian.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.ssginc8.docto.guardian.entity.PatientGuardian;
import com.ssginc8.docto.guardian.entity.Status;
import com.ssginc8.docto.patient.entity.Patient;
import com.ssginc8.docto.user.entity.User;

/**
 * 환자-보호자 관계를 관리하는 JPA 레포지토리
 */
public interface PatientGuardianRepository extends JpaRepository<PatientGuardian, Long> {

	Optional<PatientGuardian> findByPatientGuardianIdAndStatus(Long patientGuardianId, Status status);

	void deleteByUser_UserIdAndPatient_PatientId(Long userId, Long patientId);

	List<PatientGuardian> findAllByStatus(Status status);

	List<PatientGuardian> findAllByUser(User user);

	@Query("SELECT pg FROM PatientGuardian pg WHERE pg.user.userId = :userId AND pg.status = 'ACCEPTED' AND pg.deletedAt IS NULL")
	List<PatientGuardian> findAcceptedPatientsByUserId(Long userId);

	@Query("SELECT pg FROM PatientGuardian pg WHERE pg.user.userId = :userId AND pg.patient.patientId = :patientId AND pg.deletedAt IS NULL")
	Optional<PatientGuardian> findByUserIdAndPatientId(Long userId, Long patientId);

	@Modifying
	@Transactional
	@Query("""
      UPDATE PatientGuardian pg
         SET pg.deletedAt = CURRENT_TIMESTAMP
       WHERE pg.user.userId    = :userId
         AND pg.patient.patientId = :patientId
    """)
	void softDeleteByUserIdAndPatientId(
		@Param("userId") Long userId,
		@Param("patientId") Long patientId
	);

	Optional<PatientGuardian> findByUserAndPatient(User user, Patient patient);

	List<PatientGuardian> findByPatient_PatientIdAndStatus(Long patientId, Status status);

	// Optional<PatientGuardian> findByUserAndPatientAndStatus(User user, Patient patient, Status status);

	/**
	 * 특정 보호자-환자 쌍에 대해, PENDING 상태인 매핑을 모두 반환
	 */
	List<PatientGuardian> findAllByUserAndPatientAndStatus(
		User user,
		Patient patient,
		Status status
	);

	// Optional<PatientGuardian> findByInviteCode(String inviteCode);

	@Query("SELECT pg FROM PatientGuardian pg JOIN FETCH pg.patient p JOIN FETCH p.user u WHERE pg.patientGuardianId = :id")
	Optional<PatientGuardian> findByIdWithPatientAndUser(@Param("id") Long id);

	/**
	 * 동일한 inviteCode 를 가진 모든 매핑을 리스트로 조회합니다.
	 */
	List<PatientGuardian> findAllByInviteCode(String inviteCode);

}

