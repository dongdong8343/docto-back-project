package com.ssginc8.docto.calendar.provider;

import java.util.List;

import org.springframework.stereotype.Service;

import com.querydsl.core.Tuple;
import com.ssginc8.docto.calendar.repository.QCalendarRepository;
import com.ssginc8.docto.calendar.service.dto.CalendarRequest;
import com.ssginc8.docto.guardian.entity.PatientGuardian;
import com.ssginc8.docto.user.entity.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CalendarProvider {
	private final QCalendarRepository qCalendarRepository;

	public List<Tuple> fetchMedicationsByPatient(User patient) {
		return qCalendarRepository.fetchMedicationsByPatient(patient);
	}

	public List<Tuple> fetchAppointmentsByPatient(User patient, CalendarRequest request) {
		return qCalendarRepository.fetchAppointmentsByPatient(patient, request);
	}

	public List<Tuple> fetchMedicationsByGuardian(User guardian) {
		return qCalendarRepository.fetchMedicationsByGuardian(guardian);
	}

	public List<Tuple> fetchAppointmentsByGuardian(User guardian, CalendarRequest request) {
		return qCalendarRepository.fetchAppointmentsByGuardian(guardian, request);
	}

	public List<Tuple> fetchAppointmentsByHospitalAdmin(User hospitalAdmin, CalendarRequest request) {
		return qCalendarRepository.fetchAppointmentsByHospitalAdmin(hospitalAdmin, request);
	}

	public List<Tuple> fetchAppointmentsByDoctor(User doctor, CalendarRequest request) {
		return qCalendarRepository.fetchAppointmentsByDoctor(doctor, request);
	}

	public List<PatientGuardian> fetchAcceptedGuardiansByGuardianUser(User guardianUser) {
		return qCalendarRepository.fetchAcceptedGuardiansByGuardianUser(guardianUser);
	}
}
