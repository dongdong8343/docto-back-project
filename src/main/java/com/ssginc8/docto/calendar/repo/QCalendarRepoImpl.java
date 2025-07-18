package com.ssginc8.docto.calendar.repo;

import static com.ssginc8.docto.guardian.entity.QPatientGuardian.patientGuardian;
import static com.ssginc8.docto.medication.entity.QMedicationAlertDay.medicationAlertDay;
import static com.ssginc8.docto.medication.entity.QMedicationAlertTime.medicationAlertTime;
import static com.ssginc8.docto.medication.entity.QMedicationInformation.medicationInformation;
import static com.ssginc8.docto.patient.entity.QPatient.patient;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssginc8.docto.appointment.entity.AppointmentStatus;
import com.ssginc8.docto.appointment.entity.QAppointment;
import com.ssginc8.docto.calendar.service.dto.CalendarRequest;
import com.ssginc8.docto.doctor.entity.QDoctor;
import com.ssginc8.docto.guardian.entity.QPatientGuardian;
import com.ssginc8.docto.guardian.entity.PatientGuardian;
import com.ssginc8.docto.guardian.entity.QPatientGuardian;
import com.ssginc8.docto.guardian.entity.Status;
import com.ssginc8.docto.hospital.entity.QHospital;
import com.ssginc8.docto.medication.entity.QMedicationAlertDay;
import com.ssginc8.docto.medication.entity.QMedicationAlertTime;
import com.ssginc8.docto.medication.entity.QMedicationInformation;
import com.ssginc8.docto.patient.entity.QPatient;
import com.ssginc8.docto.user.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@Repository
public class QCalendarRepoImpl implements QCalendarRepo {
	private final JPAQueryFactory queryFactory;

	@Override
	@Transactional(readOnly = true)
	public List<Tuple> fetchMedicationsByPatient(User patient) {


		return queryFactory
			.select(medicationInformation.medicationId, medicationInformation.medicationName, medicationAlertTime.timeToTake, medicationAlertDay.dayOfWeek.stringValue(), Expressions.stringTemplate("'환자이름없음'"), Expressions.stringTemplate("'0'"), information.startDate, information.endDate)
			.from(medicationInformation)
			.join(medicationInformation.alertTimes, alertTime)
			.join(alertTime.alertDays, alertDay)
			.join(patientGuardian).on(information.patientGuardianId.eq(patientGuardian.patientGuardianId))
			.join(patientGuardian.patient, qpatient)
			.where(qpatient.user.eq(patient), patientGuardian.status.eq(Status.ACCEPTED), information.deletedAt.isNull(), patientGuardian.deletedAt.isNull())
			.fetch();
	}

	@Override
	@Transactional(readOnly = true)
	public List<Tuple> fetchAppointmentsByPatient(User user, CalendarRequest request) {
		QPatient patient = QPatient.patient;
		QPatientGuardian patientGuardian = QPatientGuardian.patientGuardian;
		QAppointment appointment = QAppointment.appointment;
		QHospital hospital = QHospital.hospital;

		return queryFactory
			.select(appointment.appointmentId, hospital.name, appointment.appointmentTime)
			.from(appointment)
			.join(appointment.hospital, hospital)
			.join(appointment.patientGuardian, patientGuardian)
			.join(patientGuardian.patient, patient)
			.where(patient.user.eq(user))
			.where(appointment.deletedAt.isNull(),
				appointment.status.ne(AppointmentStatus.CANCELED),
				appointment.status.ne(AppointmentStatus.REQUESTED),
				appointment.appointmentTime.goe(request.getStartDateTime()),
				appointment.appointmentTime.lt(request.getEndDateTime())).fetch();
	}

	@Override
	public List<Tuple> fetchMedicationsByGuardian(User guardian) {
		QMedicationInformation qMedicationInformation = medicationInformation;
		QMedicationAlertTime qMedicationAlertTime = medicationAlertTime;
		QMedicationAlertDay qMedicationAlertDay = medicationAlertDay;
		QPatientGuardian qPatientGuardian = patientGuardian;
		QPatient qPatient = patient;

		return queryFactory
			.select(qMedicationInformation.medicationId, qMedicationInformation.medicationName,
				qMedicationAlertTime.timeToTake, qMedicationAlertDay.dayOfWeek.stringValue(), qPatient.user.name, 
              qPatientGuardian.patientGuardianId, qMedicationInformation.startDate, qMedicationInformation.endDate)
			.from(qMedicationInformation)
			.join(qMedicationInformation.alertTimes, qMedicationAlertTime)
			.join(qMedicationAlertTime.alertDays, qMedicationAlertDay)
			.join(qPatientGuardian).on(qMedicationInformation.patientGuardianId.eq(qPatientGuardian.patientGuardianId))
			.join(qPatientGuardian.patient, qPatient)
			.where(qPatientGuardian.user.eq(guardian), qPatientGuardian.status.eq(Status.ACCEPTED), qMedicationInformation.deletedAt.isNull(), qPatientGuardian.deletedAt.isNull())
			.fetch();
	}

	@Override
	public List<Tuple> fetchAppointmentsByGuardian(User guardian, CalendarRequest request) {
		QPatient qPatient = patient;
		QPatientGuardian qPatientGuardian = patientGuardian;
		QAppointment qAppointment = QAppointment.appointment;
		QHospital qHospital = QHospital.hospital;

		return queryFactory
			.select(qAppointment.appointmentId, qHospital.name, qAppointment.appointmentTime, qPatient.user.name, qPatientGuardian.patientGuardianId)
			.from(qAppointment)
			.join(qAppointment.hospital, qHospital)
			.join(qAppointment.patientGuardian, qPatientGuardian)
			.join(qPatientGuardian.patient, qPatient)
			.where(qPatientGuardian.user.eq(guardian), qPatientGuardian.status.eq(Status.ACCEPTED), qAppointment.deletedAt.isNull(),
				qAppointment.appointmentTime.goe(request.getStartDateTime()),
				qAppointment.appointmentTime.lt(request.getEndDateTime()),
				qPatientGuardian.deletedAt.isNull(),
				qAppointment.status.ne(AppointmentStatus.CANCELED),
				qAppointment.status.ne(AppointmentStatus.REQUESTED))
			.orderBy(qAppointment.appointmentTime.asc())
			.fetch();
	}

	@Override
	public List<Tuple> fetchAppointmentsByHospitalAdmin(User hospitalAdmin, CalendarRequest request) {
		QHospital qHospital = QHospital.hospital;
		QDoctor qDoctor = QDoctor.doctor;
		QAppointment qAppointment = QAppointment.appointment;

		return queryFactory
			.select(qAppointment.appointmentId, qHospital.name, qAppointment.appointmentTime, qDoctor.user.name)
			.from(qAppointment)
			.join(qAppointment.doctor, qDoctor)
			.join(qAppointment.hospital, qHospital)
			.where(qAppointment.deletedAt.isNull(), qHospital.user.eq(hospitalAdmin),
				qAppointment.status.ne(AppointmentStatus.CANCELED),
				qAppointment.status.ne(AppointmentStatus.REQUESTED),
				qAppointment.appointmentTime.goe(request.getStartDateTime()),
				qAppointment.appointmentTime.lt(request.getEndDateTime()))
			.orderBy(qAppointment.appointmentTime.asc())
			.fetch();
	}

	@Override
	public List<Tuple> fetchAppointmentsByDoctor(User doctor, CalendarRequest request) {
		QAppointment qAppointment = QAppointment.appointment;
		QHospital qHospital = QHospital.hospital;
		QDoctor qDoctor = QDoctor.doctor;

		return queryFactory
			.select(qAppointment.appointmentId, qHospital.name, qAppointment.appointmentTime)
			.from(qAppointment)
			.join(qAppointment.hospital, qHospital)
			.join(qAppointment.doctor, qDoctor)
			.where(qDoctor.user.eq(doctor))
			.where(qAppointment.deletedAt.isNull(),
				qAppointment.status.ne(AppointmentStatus.CANCELED),
				qAppointment.status.ne(AppointmentStatus.REQUESTED),
				qAppointment.appointmentTime.goe(request.getStartDateTime()),
				qAppointment.appointmentTime.lt(request.getEndDateTime()))
			.orderBy(qAppointment.appointmentTime.asc())
			.fetch();
	}

	@Override
	public List<PatientGuardian> fetchAcceptedGuardiansByGuardianUser(User guardianUser) {
		QPatientGuardian qPatientGuardian = patientGuardian;

		return queryFactory
			.selectFrom(qPatientGuardian)
			.where(qPatientGuardian.user.eq(guardianUser),
				qPatientGuardian.status.eq(Status.ACCEPTED),
				qPatientGuardian.deletedAt.isNull())
			.fetch();
	}
}
