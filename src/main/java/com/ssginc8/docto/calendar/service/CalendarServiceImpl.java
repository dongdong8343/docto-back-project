package com.ssginc8.docto.calendar.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssginc8.docto.auth.provider.CurrentUserProvider;
import com.ssginc8.docto.calendar.provider.CalendarProvider;
import com.ssginc8.docto.calendar.service.dto.PatientGuardianMedications;
import com.ssginc8.docto.calendar.service.dto.PatientMedication;
import com.ssginc8.docto.user.entity.User;

import lombok.RequiredArgsConstructor;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class CalendarServiceImpl implements CalendarService {
	private final CurrentUserProvider currentUserProvider;
	private final CalendarProvider calendarProvider;

	@Override
	public PatientMedication.Response listMyMedications(PatientMedication.Request request) {
		User user = currentUserProvider.getUserFromUserId();

		return PatientMedication.Response.fromPatientMedicationQ(
			calendarProvider.fetchMedicationsByPatient(user, request));
	}

	@Override
	public PatientGuardianMedications.Response listMyPatientsMedications(PatientGuardianMedications.Request request) {
		User user = currentUserProvider.getUserFromUserId();

		return PatientGuardianMedications.Response.fromRows(
			calendarProvider.fetchMedicationsByGuardian(user, request)
		);
	}

}