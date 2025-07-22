package com.ssginc8.docto.appointment.provider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ssginc8.docto.appointment.dto.AppointmentDailyCountResponse;
import com.ssginc8.docto.appointment.dto.AppointmentSearchCondition;
import com.ssginc8.docto.appointment.entity.Appointment;
import com.ssginc8.docto.appointment.entity.AppointmentStatus;
import com.ssginc8.docto.appointment.repository.AppointmentRepository;
import com.ssginc8.docto.doctor.entity.Doctor;
import com.ssginc8.docto.global.error.exception.appointmentException.AppointmentNotFoundException;
import com.ssginc8.docto.guardian.entity.PatientGuardian;
import com.ssginc8.docto.user.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@RequiredArgsConstructor
@Log4j2
public class AppointmentProvider {

	private final AppointmentRepository appointmentRepository;

	/**
	 * 어드민 예약 리스트 조회
	 */
	@Transactional(readOnly = true)
	public Page<Appointment> getAppointmentListByCondition(Pageable pageable, AppointmentSearchCondition condition) {

		return appointmentRepository.findAllByCondition(condition, pageable);
	}

	/**
	 * appointmentId로 Appointment 단건 조회
	 */
	@Transactional(readOnly = true)
	public Appointment getAppointmentById(Long appointmentId) {
		return appointmentRepository.findById(appointmentId)
			.orElseThrow(AppointmentNotFoundException::new);
	}

	/**
	 * 중복되는 Appointment 존재 여부 조회
	 */
	@Transactional(readOnly = true)
	public boolean existsDuplicateAppointment(
		PatientGuardian patientGuardian, Doctor doctor, LocalDateTime appointmentTime
	) {
		return appointmentRepository.existsByPatientGuardianAndDoctorAndAppointmentTimeAndStatusNot(
			patientGuardian, doctor, appointmentTime, AppointmentStatus.CANCELED);
	}

	/**
	 * save Appointment
	 */
	@Transactional
	public Appointment save(Appointment appointment) {
		return appointmentRepository.save(appointment);
	}

	/**
	 * 시간 범위 내 환자의 Appointment 존재 여부
	 */
	@Transactional(readOnly = true)
	public boolean existsByPatientAndTimeRange(Long patientId, LocalDateTime start, LocalDateTime end) {
		return appointmentRepository.existsByPatientGuardian_Patient_PatientIdAndAppointmentTimeBetween(patientId, start, end);
	}

	/**
	 * 30분 단위의 시간 슬롯 안의 Appointment Count
	 */
	@Transactional(readOnly = true)
	public int countAppointmentsInSlot(Long doctorId, LocalDateTime slotStart, LocalDateTime slotEnd) {
		return appointmentRepository.countAppointmentsInSlot(doctorId, slotStart, slotEnd);
	}

	/**
	 * 환자의 Appointment 조회
	 */
	@Transactional(readOnly = true)
	public Page<Appointment> getAppointmentsByPatient(Long userId, Pageable pageable) {
		return appointmentRepository.findByPatientGuardian_Patient_User_UserIdOrderByAppointmentTimeAsc(userId, pageable);
	}

	/**
	 * 보호자의 Appointment 조회
	 */
	@Transactional(readOnly = true)
	public Page<Appointment> getAppointmentsByGuardian(Long userId, Pageable pageable) {
		return appointmentRepository.findByPatientGuardian_User_UserIdOrderByAppointmentTimeAsc(userId, pageable);
	}

	/**
	 * 의사의 Appointment 조회
	 */
	@Transactional(readOnly = true)
	public Page<Appointment> getAppointmentsByDoctor(Long userId, Pageable pageable, LocalDate date) {
		if (date != null) {
			LocalDateTime startOfDay = date.atStartOfDay();
			LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

			return appointmentRepository.findByDoctor_User_UserIdAndAppointmentTimeBetweenOrderByAppointmentTimeAsc(
				userId, startOfDay, endOfDay, pageable
			);
		} else {
			return appointmentRepository.findByDoctor_User_UserIdOrderByAppointmentTimeAsc(userId, pageable);
		}

	}

	/**
	 * 병원의 Appointment 조회
	 */
	@Transactional(readOnly = true)
	public Page<Appointment> getAppointmentsByHospital(Long userId, Pageable pageable, LocalDate date) {
		if (date != null) {
			// 00:00 ~ 23:59 까지 범위로 조회
			LocalDateTime startOfDay = date.atStartOfDay();
			LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

			return appointmentRepository.findByHospital_User_UserIdAndAppointmentTimeBetweenOrderByAppointmentTimeAsc(
				userId, startOfDay, endOfDay, pageable
			);
		} else {
			return appointmentRepository.findByHospital_User_UserIdOrderByAppointmentTimeAsc(userId, pageable);
		}
	}

	/**
	 * 알림 전송을 위해 Appointment 에서 User를 미리 로딩
	 */
	@Transactional(readOnly = true)
	public Optional<Appointment> findByIdWithUser(Long id) {
		return appointmentRepository.findByAppointmentIdWithUser(id);
	}

	/**
	 * 특정 기간 내 완료되지 않은 예약 목록 조회
	 */
	public List<Appointment> getAppointmentsNotCompletedBetween(LocalDateTime start, LocalDateTime end) {
		return appointmentRepository.findAllByAppointmentTimeBetweenAndStatusNot(
			start, end, AppointmentStatus.COMPLETED
		);
	}

	public List<User> getUsersWithNoShowSince(LocalDateTime since, Long threshold) {
		return appointmentRepository.findUsersWithNoShowSince(since, threshold);
	}

	/**
	 * 일일 진료 수
	 */
	@Transactional(readOnly = true)
	public List<AppointmentDailyCountResponse> countAppointmentsByDateRange(Long userId, LocalDate start, LocalDate end) {
		LocalDateTime startDateTime = start.atStartOfDay();
		LocalDateTime endDateTime = end.atTime(LocalTime.MAX);
		return appointmentRepository.countAppointmentsByDateRange(userId, startDateTime, endDateTime);
	}

	/**
	 * Status 인자로 받는
	 */
	@Transactional(readOnly = true)
	public Page<Appointment> getAppointmentsByPatientAndStatusIn(Long userId, List<AppointmentStatus> statuses, Pageable pageable) {
		return appointmentRepository.findByPatientGuardian_Patient_User_UserIdAndStatusInOrderByAppointmentTimeAsc(userId, statuses, pageable);
	}

	@Transactional(readOnly = true)
	public Page<Appointment> getAppointmentsByGuardianAndStatusIn(Long userId, List<AppointmentStatus> statuses, Pageable pageable) {
		return appointmentRepository.findByPatientGuardian_User_UserIdAndStatusInOrderByAppointmentTimeAsc(userId, statuses, pageable);
	}

	@Transactional(readOnly = true)
	public Page<Appointment> getAppointmentsByDoctorAndStatusIn(Long userId, List<AppointmentStatus> statuses, Pageable pageable) {
		return appointmentRepository.findByDoctor_User_UserIdAndStatusInOrderByAppointmentTimeAsc(userId, statuses, pageable);
	}

	// 병원 관리자는 날짜와 상태를 모두 고려
	@Transactional(readOnly = true)
	public Page<Appointment> getAppointmentsByHospitalAndStatusIn(Long userId, List<AppointmentStatus> statuses, Pageable pageable, LocalDate date) {
		if (date != null) {
			LocalDateTime startOfDay = date.atStartOfDay();
			LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
			return appointmentRepository.findByHospital_User_UserIdAndAppointmentTimeBetweenAndStatusInOrderByAppointmentTimeAsc(
				userId, startOfDay, endOfDay, statuses, pageable
			);
		} else {
			return appointmentRepository.findByHospital_User_UserIdAndStatusInOrderByAppointmentTimeAsc(userId, statuses, pageable);
		}
	}

	/**
	 * DESC 정렬
	 */
	@Transactional(readOnly = true)
	public Page<Appointment> getAppointmentsByPatientAndStatusInDesc(Long userId, List<AppointmentStatus> statuses, Pageable pageable) {
		return appointmentRepository.findByPatientGuardian_Patient_User_UserIdAndStatusInOrderByAppointmentTimeDesc(userId, statuses, pageable);
	}

	@Transactional(readOnly = true)
	public Page<Appointment> getAppointmentsByGuardianAndStatusInDesc(Long userId, List<AppointmentStatus> statuses, Pageable pageable) {
		return appointmentRepository.findByPatientGuardian_User_UserIdAndStatusInOrderByAppointmentTimeDesc(userId, statuses, pageable);
	}

	@Transactional(readOnly = true)
	public Page<Appointment> getAppointmentsByDoctorAndStatusInDesc(Long userId, List<AppointmentStatus> statuses, Pageable pageable) {
		return appointmentRepository.findByDoctor_User_UserIdAndStatusInOrderByAppointmentTimeDesc(userId, statuses, pageable);
	}

	@Transactional(readOnly = true)
	public Page<Appointment> getAppointmentsByHospitalAndStatusInDesc(Long userId, List<AppointmentStatus> statuses, Pageable pageable, LocalDate date) {
		if (date != null) {
			LocalDateTime startOfDay = date.atStartOfDay();
			LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
			return appointmentRepository.findByHospital_User_UserIdAndAppointmentTimeBetweenAndStatusInOrderByAppointmentTimeDesc(
				userId, startOfDay, endOfDay, statuses, pageable
			);
		} else {
			return appointmentRepository.findByHospital_User_UserIdAndStatusInOrderByAppointmentTimeDesc(userId, statuses, pageable);
		}
	}
}
