package com.ssginc8.docto.calendar.repository.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PatientMedicationQ {
	private Long id;
	private String medicationName;
	private LocalDate startDate;
	private LocalDate endDate;
	private LocalTime time;
	private DayOfWeek day;
}