package com.ssginc8.docto.calendar.provider;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ssginc8.docto.calendar.repository.CalendarQueryRepository;
import com.ssginc8.docto.calendar.repository.dto.PatientGuardianMedicationQ;
import com.ssginc8.docto.calendar.repository.dto.PatientMedicationQ;
import com.ssginc8.docto.calendar.service.dto.PatientGuardianMedications;
import com.ssginc8.docto.calendar.service.dto.PatientMedication;
import com.ssginc8.docto.user.entity.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CalendarProvider {
	private final CalendarQueryRepository calendarQueryRepository;

	public List<PatientMedicationQ> fetchMedicationsByPatient(User patient, PatientMedication.Request request) {
		return calendarQueryRepository.fetchMedicationsByPatient(patient, request);
	}

	public List<PatientGuardianMedicationQ> fetchMedicationsByGuardian(User guardian,
		PatientGuardianMedications.Request request) {
		return calendarQueryRepository.fetchMedicationsByGuardian(guardian, request);
	}
}
