package com.ssginc8.docto.calendar.service.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
		List<PatientMedicationItem> patientMedicationItems;

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
		private final Set<LocalTime> times;
		private final Set<DayOfWeek> days;

		private PatientMedicationItem(Long medicationId, String medicationName, LocalDate startDate, LocalDate endDate, Set<LocalTime> times,
			Set<DayOfWeek> days) {
			this.medicationId = medicationId;
			this.medicationName = medicationName;
			this.startDate = startDate;
			this.endDate = endDate;
			this.times = times;
			this.days = days;
		}

		public static PatientMedicationItem fromPatientMedicationQS(List<PatientMedicationQ> patientMedicationQS) {
			Set<LocalTime> times = new HashSet<>();
			Set<DayOfWeek> days = new HashSet<>();

			patientMedicationQS.forEach(patientMedicationQ -> {
				times.add(patientMedicationQ.getTime());
				days.add(patientMedicationQ.getDay());
			});

			return new PatientMedicationItem(
				patientMedicationQS.get(0).getId(),
				patientMedicationQS.get(0).getMedicationName(),
				patientMedicationQS.get(0).getStartDate(),
				patientMedicationQS.get(0).getEndDate(),
				times,
				days
			);
		}
	}
}
