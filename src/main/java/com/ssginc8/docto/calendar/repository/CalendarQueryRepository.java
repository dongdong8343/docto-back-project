package com.ssginc8.docto.calendar.repository;

import java.util.List;

import com.ssginc8.docto.calendar.repository.dto.PatientGuardianMedicationQ;
import com.ssginc8.docto.calendar.repository.dto.PatientMedicationQ;
import com.ssginc8.docto.calendar.service.dto.PatientGuardianMedications;
import com.ssginc8.docto.calendar.service.dto.PatientMedication;
import com.ssginc8.docto.user.entity.User;

public interface CalendarQueryRepository {
	List<PatientMedicationQ> fetchMedicationsByPatient(User patient, PatientMedication.Request request);

	List<PatientGuardianMedicationQ> fetchMedicationsByGuardian(User guardian, PatientGuardianMedications.Request request);
}
