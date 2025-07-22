package com.ssginc8.docto.calendar.repository;

import java.util.List;

import com.querydsl.core.Tuple;
import com.ssginc8.docto.calendar.service.dto.CalendarRequest;
import com.ssginc8.docto.guardian.entity.PatientGuardian;
import com.ssginc8.docto.user.entity.User;

public interface QCalendarRepository {
	List<Tuple> fetchMedicationsByPatient(User patient);

	List<Tuple> fetchAppointmentsByPatient(User patient, CalendarRequest request);

	List<Tuple> fetchMedicationsByGuardian(User guardian);

	List<Tuple> fetchAppointmentsByGuardian(User guardian, CalendarRequest request);

	List<Tuple> fetchAppointmentsByHospitalAdmin(User hospitalAdmin, CalendarRequest request);

	List<Tuple> fetchAppointmentsByDoctor(User doctor, CalendarRequest request);
  
	List<PatientGuardian> fetchAcceptedGuardiansByGuardianUser(User guardianUser);
}
