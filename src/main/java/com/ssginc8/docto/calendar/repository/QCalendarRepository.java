package com.ssginc8.docto.calendar.repository;

import java.util.List;

import com.ssginc8.docto.calendar.repository.dto.PatientMedicationQ;
import com.ssginc8.docto.calendar.service.dto.PatientMedication;
import com.ssginc8.docto.user.entity.User;

public interface QCalendarRepository {
	List<PatientMedicationQ> fetchMedicationsByPatient(User patient, PatientMedication.Request request);
}
