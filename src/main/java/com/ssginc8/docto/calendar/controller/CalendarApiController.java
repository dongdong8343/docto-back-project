package com.ssginc8.docto.calendar.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssginc8.docto.calendar.service.CalendarService;
import com.ssginc8.docto.calendar.service.dto.PatientGuardianMedications;
import com.ssginc8.docto.calendar.service.dto.PatientMedication;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/api/v1/calendar")
@RestController
public class CalendarApiController {
	private final CalendarService calendarService;

	@GetMapping("/me/medications")
	public PatientMedication.Response listMyMedications(@ModelAttribute PatientMedication.Request request) {
		return calendarService.listMyMedications(request);
	}

	@GetMapping("/guardians/me/patients/medications")
	public PatientGuardianMedications.Response listMyPatientsMedications(@ModelAttribute PatientGuardianMedications.Request request) {
		return calendarService.listMyPatientsMedications(request);
	}
}