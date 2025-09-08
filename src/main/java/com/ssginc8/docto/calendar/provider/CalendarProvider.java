package com.ssginc8.docto.calendar.provider;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ssginc8.docto.calendar.repository.QCalendarRepository;
import com.ssginc8.docto.calendar.repository.dto.PatientMedicationQ;
import com.ssginc8.docto.calendar.service.dto.PatientMedication;
import com.ssginc8.docto.user.entity.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CalendarProvider {
	private final QCalendarRepository qCalendarRepository;

	public List<PatientMedicationQ> fetchMedicationsByPatient(User patient, PatientMedication.Request request) {
		return qCalendarRepository.fetchMedicationsByPatient(patient, request);
	}
}
