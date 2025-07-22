package com.ssginc8.docto.appointment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import com.ssginc8.docto.appointment.dto.AppointmentDailyCountResponse;
import com.ssginc8.docto.appointment.entity.Appointment;
import com.ssginc8.docto.appointment.entity.AppointmentStatus;
import com.ssginc8.docto.doctor.entity.Doctor;
import com.ssginc8.docto.guardian.entity.PatientGuardian;
import com.ssginc8.docto.user.entity.User;

public interface AppointmentRepository
	extends JpaRepository<Appointment, Long>, QuerydslPredicateExecutor<Appointment>, QAppointmentRepository {

	@EntityGraph(attributePaths = {
		"hospital",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Optional<Appointment> findById(Long appointmentId);

	boolean existsByPatientGuardianAndDoctorAndAppointmentTimeAndStatusNot(
		PatientGuardian patientGuardian,
		Doctor doctor,
		LocalDateTime appointmentTime,
		AppointmentStatus status
	);

	@Query("""
			SELECT COUNT(a) > 0
			FROM Appointment a
			WHERE a.patientGuardian.patient.patientId = :patientId
				AND a.appointmentTime BETWEEN :start AND :end
				AND a.status <> com.ssginc8.docto.appointment.entity.AppointmentStatus.CANCELED
		""")
	boolean existsByPatientGuardian_Patient_PatientIdAndAppointmentTimeBetween(
		@Param("patientId") Long patientId,
		@Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end);

	@Query("SELECT COUNT(a) FROM Appointment a " +
		"WHERE a.doctor.doctorId = :doctorId " +
		"AND a.appointmentTime >= :slotStart " +
		"AND a.appointmentTime < :slotEnd " +
		"AND a.status = 'CONFIRMED'")
	int countAppointmentsInSlot(
		@Param("doctorId") Long doctorId,
		@Param("slotStart") LocalDateTime slotStart,
		@Param("slotEnd") LocalDateTime slotEnd
	);

	/**
	 * 로그인 한 유저의 예약 리스트 가져오기
	 */
	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByPatientGuardian_Patient_User_UserIdOrderByAppointmentTimeAsc(Long userId,
		Pageable pageable);

	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByPatientGuardian_User_UserIdOrderByAppointmentTimeAsc(Long userId, Pageable pageable);

	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByDoctor_User_UserIdOrderByAppointmentTimeAsc(Long userId, Pageable pageable);

	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByDoctor_User_UserIdAndAppointmentTimeBetweenOrderByAppointmentTimeAsc(
		Long userId, LocalDateTime start, LocalDateTime end, Pageable pageable
	);

	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByHospital_User_UserIdOrderByAppointmentTimeAsc(Long userId, Pageable pageable);

	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByHospital_User_UserIdAndAppointmentTimeBetweenOrderByAppointmentTimeAsc(
		Long userId, LocalDateTime start, LocalDateTime end, Pageable pageable
	);

	/**
	 * 알림 전송을 위해 Appointment Fetch Join (Lazy 방지)
	 * @param id
	 * @return
	 */
	@Query("""
			   SELECT a FROM Appointment a
			   JOIN FETCH a.patientGuardian pg
			   JOIN FETCH pg.user
			   JOIN FETCH a.hospital
			   JOIN FETCH pg.patient p
			   JOIN FETCH p.user
			   WHERE a.appointmentId = :id
		""")
	Optional<Appointment> findByAppointmentIdWithUser(@Param("id") Long id);

	List<Appointment> findAllByAppointmentTimeBetweenAndStatusNot(LocalDateTime start, LocalDateTime end,
		AppointmentStatus status);

	@Query("""
			SELECT a.patientGuardian.user
			FROM Appointment a
			WHERE a.status = com.ssginc8.docto.appointment.entity.AppointmentStatus.NO_SHOW
			  AND a.appointmentTime >= :sinceDate
			GROUP BY a.patientGuardian.user
			HAVING COUNT(a) >= :threshold
		""")
	List<User> findUsersWithNoShowSince(
		@Param("sinceDate") LocalDateTime sinceDate,
		@Param("threshold") Long threshold
	);

	@Query("SELECT new com.ssginc8.docto.appointment.dto.AppointmentDailyCountResponse(" +
		"CAST(FUNCTION('DATE', a.appointmentTime) AS java.time.LocalDate), COUNT(a)) " +
		"FROM Appointment a " +
		"WHERE a.hospital.hospitalId = :hospitalId " +
		"AND a.appointmentTime BETWEEN :start AND :end " +
		"GROUP BY FUNCTION('DATE', a.appointmentTime) " +
		"ORDER BY FUNCTION('DATE', a.appointmentTime)")
	List<AppointmentDailyCountResponse> countAppointmentsByDateRange(
		@Param("hospitalId") Long hospitalId,
		@Param("start") LocalDateTime start,
		@Param("end") LocalDateTime end
	);

	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByPatientGuardian_Patient_User_UserIdAndStatusInOrderByAppointmentTimeAsc(
		Long userId, List<AppointmentStatus> statuses, Pageable pageable
	);

	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByPatientGuardian_User_UserIdAndStatusInOrderByAppointmentTimeAsc(
		Long userId, List<AppointmentStatus> statuses, Pageable pageable
	);

	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByDoctor_User_UserIdAndStatusInOrderByAppointmentTimeAsc(
		Long userId, List<AppointmentStatus> statuses, Pageable pageable
	);

	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByHospital_User_UserIdAndStatusInOrderByAppointmentTimeAsc(
		Long userId, List<AppointmentStatus> statuses, Pageable pageable
	);

	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByHospital_User_UserIdAndAppointmentTimeBetweenAndStatusInOrderByAppointmentTimeAsc(
		Long userId, LocalDateTime start, LocalDateTime end, List<AppointmentStatus> statuses, Pageable pageable
	);


	/**
	 * Inactive 예약들을 위한 DESC
	 */
	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByPatientGuardian_Patient_User_UserIdAndStatusInOrderByAppointmentTimeDesc(
		Long userId, List<AppointmentStatus> statuses, Pageable pageable
	);

	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByPatientGuardian_User_UserIdAndStatusInOrderByAppointmentTimeDesc(
		Long userId, List<AppointmentStatus> statuses, Pageable pageable
	);

	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByDoctor_User_UserIdAndStatusInOrderByAppointmentTimeDesc(
		Long userId, List<AppointmentStatus> statuses, Pageable pageable
	);

	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByHospital_User_UserIdAndStatusInOrderByAppointmentTimeDesc(
		Long userId, List<AppointmentStatus> statuses, Pageable pageable
	);

	@EntityGraph(attributePaths = {
		"hospital.user",
		"doctor.user",
		"patientGuardian.patient.user"
	})
	Page<Appointment> findByHospital_User_UserIdAndAppointmentTimeBetweenAndStatusInOrderByAppointmentTimeDesc(
		Long userId, LocalDateTime start, LocalDateTime end, List<AppointmentStatus> statuses, Pageable pageable
	);
}
