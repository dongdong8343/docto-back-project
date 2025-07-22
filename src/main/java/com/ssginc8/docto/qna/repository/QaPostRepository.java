package com.ssginc8.docto.qna.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ssginc8.docto.appointment.entity.Appointment;
import com.ssginc8.docto.qna.entity.QaPost;
import com.ssginc8.docto.qna.entity.QaStatus;

public interface QaPostRepository extends JpaRepository<QaPost, Long> {

	Optional<QaPost> findByAppointment(Appointment appointment);


	Page<QaPost> findAllByAppointment_PatientGuardian_User_UserId(
		Long userId,
		Pageable pageable
	);

	Page<QaPost> findAllByStatus(QaStatus status, Pageable pageable);


	Page<QaPost> findAllByAppointmentDoctorUserUuidAndStatus(
		String uuid,
		QaStatus status,
		Pageable pageable
	);


}
