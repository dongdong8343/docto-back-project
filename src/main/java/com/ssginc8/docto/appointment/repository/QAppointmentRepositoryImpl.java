package com.ssginc8.docto.appointment.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssginc8.docto.appointment.dto.AppointmentSearchCondition;
import com.ssginc8.docto.appointment.entity.Appointment;
import com.ssginc8.docto.appointment.entity.QAppointment;
import com.ssginc8.docto.doctor.entity.QDoctor;
import com.ssginc8.docto.guardian.entity.QPatientGuardian;
import com.ssginc8.docto.hospital.entity.QHospital;
import com.ssginc8.docto.patient.entity.QPatient;
import com.ssginc8.docto.user.entity.QUser;

import jakarta.persistence.EntityManager;

@Repository
public class QAppointmentRepositoryImpl implements QAppointmentRepository {

	private final JPAQueryFactory queryFactory;

	public QAppointmentRepositoryImpl(EntityManager em) {
		this.queryFactory = new JPAQueryFactory(em);
	}

	/**
	 * Admin의 예약 리스트 조회 (조건 : 환자 혹은 보호자의 userId, hospitalId, doctorId)
	 */
	@Override
	public Page<Appointment> findAllByCondition(AppointmentSearchCondition condition, Pageable pageable) {
		QAppointment appointment = QAppointment.appointment;
		QPatientGuardian guardian = QPatientGuardian.patientGuardian;
		QPatient patient = QPatient.patient;
		QHospital hospital = QHospital.hospital;
		QDoctor doctor = QDoctor.doctor;
		QUser patientUser = new QUser("patientUser");
		QUser guardianUser = new QUser("guardianUser");
		QUser doctorUser = new QUser("doctorUser");

		BooleanBuilder builder = new BooleanBuilder();

		// 로그인한 유저가 보호자 또는 환자인 경우
		if (condition.getUserId() != null) {
			builder.and(
				guardian.user.userId.eq(condition.getUserId())	// 보호자
					.or(patient.user.userId.eq(condition.getUserId())));	// 보호자
		}

		if (condition.getHospitalId() != null) {
			builder.and(appointment.hospital.hospitalId.eq(condition.getHospitalId()));
		}

		if (condition.getDoctorId() != null) {
			builder.and(appointment.doctor.user.userId.eq(condition.getDoctorId()));
		}

		// content 쿼리
		List<Appointment> content = queryFactory
			.selectFrom(appointment)
			.join(appointment.patientGuardian, guardian).fetchJoin()
			.join(guardian.user, guardianUser).fetchJoin()
			.join(guardian.patient, patient).fetchJoin()
			.join(patient.user, patientUser).fetchJoin()
			.join(appointment.hospital, hospital).fetchJoin()
			.join(appointment.doctor, doctor).fetchJoin()
			.join(doctor.user, doctorUser).fetchJoin()
			.where(builder)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		// count 쿼리 (join fetch 제거)
		Long total = queryFactory
			.select(appointment.count())
			.from(appointment)
			.join(appointment.patientGuardian, guardian)
			.join(guardian.patient, patient)
			.join(patient.user, patientUser)
			.where(builder)
			.fetchOne();

		return new PageImpl<>(content, pageable, total != null ? total : 0);
	}
}
