package com.ssginc8.docto.calendar.repository.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import com.querydsl.core.annotations.QueryProjection;

import lombok.Getter;

@Getter
public class PatientGuardianMedicationQ {
	private Long medicationId;
	private Long patientGuardianId;
	private String medicationName;
	private LocalDate startDate;
	private LocalDate endDate;
	private LocalTime time;
	private DayOfWeek day;

	@QueryProjection
	public PatientGuardianMedicationQ(Long medicationId, Long patientGuardianId, String medicationName,
		LocalDate startDate, LocalDate endDate, LocalTime time, DayOfWeek day) {
		this.medicationId = medicationId;
		this.patientGuardianId = patientGuardianId;
		this.medicationName = medicationName;
		this.startDate = startDate;
		this.endDate = endDate;
		this.time = time;
		this.day = day;
	}
}
