package com.ssginc8.docto.calendar.service;

import com.ssginc8.docto.calendar.service.dto.PatientMedication;

public interface CalendarService {
	PatientMedication.Response getPatientCalendars(PatientMedication.Request request);
}