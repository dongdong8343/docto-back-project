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

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssginc8.docto.calendar.repository.dto.PatientGuardianMedicationQ;
import com.ssginc8.docto.calendar.repository.dto.PatientMedicationQ;
import com.ssginc8.docto.calendar.repository.dto.QPatientGuardianMedicationQ;
import com.ssginc8.docto.calendar.repository.dto.QPatientMedicationQ;
import com.ssginc8.docto.calendar.service.dto.PatientGuardianMedications;
import com.ssginc8.docto.calendar.service.dto.PatientMedication;
import com.ssginc8.docto.guardian.entity.Status;
import com.ssginc8.docto.user.entity.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Repository
public class CalendarQueryQueryRepositoryImpl implements CalendarQueryRepository {
	private final JPAQueryFactory queryFactory;

	@Override
	public List<PatientMedicationQ> fetchMedicationsByPatient(User patientEntity, PatientMedication.Request request) {
		return baseMedicationQuery(request.getStartDate(), request.getEndDate())
			.select(new QPatientMedicationQ(
				medicationInformation.medicationId,
				medicationInformation.medicationName,
				medicationInformation.startDate,
				medicationInformation.endDate,
				medicationAlertTime.timeToTake,
				medicationAlertDay.dayOfWeek
				)
			)
			.join(patientGuardian).on(medicationInformation.patientGuardianId.eq(patientGuardian.patientGuardianId))
			.join(patientGuardian.patient, patient)
			.where(patient.user.eq(patientEntity), acceptedGuardian())
			.fetch();
	}

	@Override
	public List<PatientGuardianMedicationQ> fetchMedicationsByGuardian(User guardian,
		PatientGuardianMedications.Request request) {
		return baseMedicationQuery(request.getStartDate(), request.getEndDate())
			.select(new QPatientGuardianMedicationQ(
				medicationInformation.medicationId,
				medicationInformation.patientGuardianId,
				medicationInformation.medicationName,
				medicationInformation.startDate,
				medicationInformation.endDate,
				medicationAlertTime.timeToTake,
				medicationAlertDay.dayOfWeek
				)
			)
			.where(medicationInformation.user.eq(guardian))
			.fetch();
	}

	private JPAQuery<?> baseMedicationQuery(LocalDate start, LocalDate end) {
		return queryFactory
			.from(medicationInformation)
			.join(medicationInformation.alertTimes, medicationAlertTime)
			.join(medicationAlertTime.alertDays, medicationAlertDay)
			.where(medicationInformation.deletedAt.isNull(), isWithinMedicationWindow(start, end));
	}

	private BooleanExpression acceptedGuardian() {
		return patientGuardian.status.eq(Status.ACCEPTED).and(patientGuardian.deletedAt.isNull());
	}

	private BooleanExpression isWithinMedicationWindow(LocalDate start, LocalDate end) {
		return medicationInformation.startDate.loe(end)
			.and(medicationInformation.endDate.goe(start));
	}
}

