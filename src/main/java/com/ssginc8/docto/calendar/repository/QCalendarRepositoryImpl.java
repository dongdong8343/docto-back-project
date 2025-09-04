package com.ssginc8.docto.calendar.repository;

import static com.ssginc8.docto.appointment.entity.QAppointment.*;
import static com.ssginc8.docto.doctor.entity.QDoctor.*;
import static com.ssginc8.docto.guardian.entity.QPatientGuardian.*;
import static com.ssginc8.docto.hospital.entity.QHospital.*;
import static com.ssginc8.docto.medication.entity.QMedicationAlertDay.*;
import static com.ssginc8.docto.medication.entity.QMedicationAlertTime.*;
import static com.ssginc8.docto.medication.entity.QMedicationInformation.*;
import static com.ssginc8.docto.patient.entity.QPatient.*;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssginc8.docto.appointment.entity.AppointmentStatus;
import com.ssginc8.docto.calendar.service.dto.CalendarRequest;
import com.ssginc8.docto.guardian.entity.PatientGuardian;
import com.ssginc8.docto.guardian.entity.Status;
import com.ssginc8.docto.user.entity.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Repository
public class QCalendarRepositoryImpl implements QCalendarRepository {
	private static final EnumSet<AppointmentStatus> CALENDAR_DISPLAY
		= EnumSet.of(AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED);

	private final JPAQueryFactory queryFactory;

	@Override
	public List<Tuple> fetchMedicationsByPatient(User patientEntity) {
		return baseMedicationQuery(
			medicationInformation.medicationId,
			medicationInformation.medicationName,
			medicationAlertTime.timeToTake,
			medicationAlertDay.dayOfWeek.stringValue(),
			Expressions.constant("환자이름없음"), // 보호자가 조회하는 복약 정보와 튜플 인덱스를 맞추기 위함
			Expressions.constant("0"),
			medicationInformation.startDate,
			medicationInformation.endDate)
			.where(patient.user.eq(patientEntity))
			.fetch();
	}

	@Override
	public List<Tuple> fetchAppointmentsByPatient(User patientEntity, CalendarRequest request) {
		return baseAppointmentQuery(
			request,
			appointment.appointmentId,
			hospital.name,
			appointment.appointmentTime)
			.join(appointment.patientGuardian, patientGuardian)
			.join(patientGuardian.patient, patient)
			.where(patient.user.eq(patientEntity)).fetch();
	}

	@Override
	public List<Tuple> fetchMedicationsByGuardian(User guardian) {
		return baseMedicationQuery(
			medicationInformation.medicationId,
			medicationInformation.medicationName,
			medicationAlertTime.timeToTake,
			medicationAlertDay.dayOfWeek.stringValue(),
			patient.user.name,
			patientGuardian.patientGuardianId,
			medicationInformation.startDate,
			medicationInformation.endDate)
			.where(patientGuardian.user.eq(guardian))
			.fetch();
	}

	@Override
	public List<Tuple> fetchAppointmentsByGuardian(User guardian, CalendarRequest request) {
		return baseAppointmentQuery(
			request,
			appointment.appointmentId,
			hospital.name,
			appointment.appointmentTime,
			patient.user.name,
			patientGuardian.patientGuardianId)
			.join(appointment.patientGuardian, patientGuardian)
			.join(patientGuardian.patient, patient)
			.where(
				patientGuardian.user.eq(guardian),
				acceptedGuardian())
			.fetch();
	}

	@Override
	public List<Tuple> fetchAppointmentsByHospitalAdmin(User hospitalAdmin, CalendarRequest request) {
		return baseAppointmentQuery(
			request,
			appointment.appointmentId,
			hospital.name,
			appointment.appointmentTime,
			doctor.user.name)
			.join(appointment.doctor, doctor)
			.where(hospital.user.eq(hospitalAdmin))
			.fetch();
	}

	@Override
	public List<Tuple> fetchAppointmentsByDoctor(User doctorEntity, CalendarRequest request) {
		return baseAppointmentQuery(
			request,
			appointment.appointmentId,
			hospital.name,
			appointment.appointmentTime)
			.join(appointment.doctor, doctor)
			.where(doctor.user.eq(doctorEntity))
			.fetch();
	}

	@Override
	public List<PatientGuardian> fetchAcceptedGuardiansByGuardianUser(User guardianUser) {
		return queryFactory
			.selectFrom(patientGuardian)
			.where(patientGuardian.user.eq(guardianUser),
				acceptedGuardian())
			.fetch();
	}

	// 예약 공통 쿼리
	private JPAQuery<Tuple> baseAppointmentQuery(CalendarRequest request, Expression<?>... select) {
		return queryFactory
			.select(select)
			.from(appointment)
			.join(appointment.hospital, hospital)
			.where(appointment.deletedAt.isNull(),
				hasCalendarDisplayStatus(),
				within(request))
			.orderBy(byTimeAsc(), appointment.appointmentId.asc());
	}

	// 복약 공통 쿼리
	private JPAQuery<Tuple> baseMedicationQuery(Expression<?>... select) {
		return queryFactory
			.select(select)
			.from(medicationInformation)
			.join(medicationInformation.alertTimes, medicationAlertTime)
			.join(medicationAlertTime.alertDays, medicationAlertDay)
			.join(patientGuardian).on(medicationInformation.patientGuardianId.eq(patientGuardian.patientGuardianId))
			.join(patientGuardian.patient, patient)
			.where(medicationInformation.deletedAt.isNull(),
				acceptedGuardian());
	}

	private BooleanExpression within(CalendarRequest request) {
		return appointment.appointmentTime.goe(request.getStartDateTime())
			.and(appointment.appointmentTime.lt(request.getEndDateTime()));
	}

	private BooleanExpression acceptedGuardian() {
		return patientGuardian.status.eq(Status.ACCEPTED).and(patientGuardian.deletedAt.isNull());
	}

	private BooleanExpression hasCalendarDisplayStatus() {
		return appointment.status.in(CALENDAR_DISPLAY);
	}

	private OrderSpecifier<LocalDateTime> byTimeAsc() {
		return appointment.appointmentTime.asc();
	}
}

