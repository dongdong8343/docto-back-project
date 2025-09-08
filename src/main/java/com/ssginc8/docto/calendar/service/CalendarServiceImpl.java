package com.ssginc8.docto.calendar.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssginc8.docto.auth.provider.CurrentUserProvider;
import com.ssginc8.docto.calendar.provider.CalendarProvider;
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
	public PatientMedication.Response getPatientCalendars(PatientMedication.Request request) {
		// user의 정보를 꺼낸다.
		User user = currentUserProvider.getUserFromUserId();

		// provider로 request를 넘겨준다.
		return PatientMedication.Response.fromPatientMedicationQ(
			calendarProvider.fetchMedicationsByPatient(user, request));
	}

}