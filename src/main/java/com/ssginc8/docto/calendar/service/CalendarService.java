package com.ssginc8.docto.calendar.service;

import com.ssginc8.docto.calendar.service.dto.PatientGuardianMedications;
import com.ssginc8.docto.calendar.service.dto.PatientMedication;

public interface CalendarService {
	PatientMedication.Response listMyMedications(PatientMedication.Request request);

	PatientGuardianMedications.Response listMyPatientsMedications(PatientGuardianMedications.Request request);
}