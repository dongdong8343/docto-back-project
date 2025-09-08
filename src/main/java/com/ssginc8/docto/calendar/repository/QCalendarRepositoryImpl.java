package com.ssginc8.docto.calendar.repository;

import static com.ssginc8.docto.guardian.entity.QPatientGuardian.*;
import static com.ssginc8.docto.medication.entity.QMedicationAlertDay.*;
import static com.ssginc8.docto.medication.entity.QMedicationAlertTime.*;
import static com.ssginc8.docto.medication.entity.QMedicationInformation.*;
import static com.ssginc8.docto.patient.entity.QPatient.*;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssginc8.docto.calendar.repository.dto.PatientMedicationQ;
import com.ssginc8.docto.calendar.service.dto.PatientMedication;
import com.ssginc8.docto.guardian.entity.Status;
import com.ssginc8.docto.user.entity.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Repository
public class QCalendarRepositoryImpl implements QCalendarRepository {
	private final JPAQueryFactory queryFactory;

	@Override
	public List<PatientMedicationQ> fetchMedicationsByPatient(User patientEntity, PatientMedication.Request request) {
		return queryFactory
			.select(Projections.constructor(PatientMedicationQ.class,
				medicationInformation.medicationId,
				medicationInformation.medicationName,
				medicationInformation.startDate,
				medicationInformation.endDate,
				medicationAlertTime.timeToTake,
				medicationAlertDay.dayOfWeek
			))
			.from(medicationInformation)
			.join(medicationInformation.alertTimes, medicationAlertTime)
			.join(medicationAlertTime.alertDays, medicationAlertDay)
			.join(patientGuardian).on(medicationInformation.patientGuardianId.eq(patientGuardian.patientGuardianId))
			.join(patientGuardian.patient, patient)
			.where(medicationInformation.deletedAt.isNull(), patient.user.eq(patientEntity), acceptedGuardian(), isWithinMedicationWindow(request.getStartDate(), request.getEndDate()))
			.fetch();
	}

	private BooleanExpression acceptedGuardian() {
		return patientGuardian.status.eq(Status.ACCEPTED).and(patientGuardian.deletedAt.isNull());
	}

	private BooleanExpression isWithinMedicationWindow(LocalDate startDateTime, LocalDate endDateTime) {
		return medicationInformation.startDate.goe(startDateTime)
			.and(medicationInformation.endDate.lt(endDateTime));
	}
}

