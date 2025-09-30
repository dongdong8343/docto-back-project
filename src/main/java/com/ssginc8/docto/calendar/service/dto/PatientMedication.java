package com.ssginc8.docto.calendar.service.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.ssginc8.docto.calendar.repository.dto.PatientMedicationQ;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class PatientMedication {
	@Setter
	@Getter
	@NoArgsConstructor
	public static class Request {
		@NotNull
		private Integer year;

		@NotNull
		@Min(1)
		@Max(12)
		private Integer month;

		public LocalDate getStartDate() {
			return LocalDate.of(year, month, 1);
		}

		public LocalDate getEndDate() {
			return LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
		}
	}

	@Getter
	public static class Response {
		private final List<PatientMedicationItem> patientMedicationItems;

		private Response(List<PatientMedicationItem> patientMedicationItems) {
			this.patientMedicationItems = patientMedicationItems;
		}

		public static Response fromPatientMedicationQ(List<PatientMedicationQ> patientMedicationQList) {
			List<PatientMedicationItem> patientMedicationItems = new ArrayList<>();

			Map<Long, List<PatientMedicationQ>> grouped = patientMedicationQList.stream()
				.collect(Collectors.groupingBy(PatientMedicationQ::getId));

			grouped.forEach((medicationId, patientMedicationQS) -> {
				patientMedicationItems.add(PatientMedicationItem.fromPatientMedicationQS(patientMedicationQS));
			});

			return new Response(patientMedicationItems);
		}
	}

	@Getter
	public static class PatientMedicationItem {
		private final Long medicationId;
		private final String medicationName;
		private final LocalDate startDate;
		private final LocalDate endDate;
		private final NavigableSet<LocalTime> times;
		private final EnumSet<DayOfWeek> days;

		private PatientMedicationItem(Long medicationId, String medicationName, LocalDate startDate, LocalDate endDate, NavigableSet<LocalTime> times,
			EnumSet<DayOfWeek> days) {
			this.medicationId = medicationId;
			this.medicationName = medicationName;
			this.startDate = startDate;
			this.endDate = endDate;
			this.times = times;
			this.days = days;
		}

		public static PatientMedicationItem fromPatientMedicationQS(List<PatientMedicationQ> rows) {
			NavigableSet<LocalTime> times = rows.stream()
				.map(PatientMedicationQ::getTime)
				.collect(Collectors.toCollection(java.util.TreeSet::new));

			EnumSet<DayOfWeek> days = rows.stream()
				.map(PatientMedicationQ::getDay)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));

			return new PatientMedicationItem(
				rows.get(0).getId(),
				rows.get(0).getMedicationName(),
				rows.get(0).getStartDate(),
				rows.get(0).getEndDate(),
				times,
				days
			);
		}
	}
}
