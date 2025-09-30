package com.ssginc8.docto.calendar.service.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.ssginc8.docto.calendar.repository.dto.PatientGuardianMedicationQ;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PatientGuardianMedications {
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
		private List<GuardianGroup> patientMedicationItems;

		private Response(List<GuardianGroup> guardianGroups) {
			this.patientMedicationItems = guardianGroups;
		}

		public static Response fromRows(List<PatientGuardianMedicationQ> rows) {
			log.info(rows.toString());
			List<GuardianGroup> patientMedicationItems = new ArrayList<>();

			Map<Long, List<PatientGuardianMedicationQ>> grouped = rows.stream()
				.collect(Collectors.groupingBy(PatientGuardianMedicationQ::getMedicationId));

			grouped.forEach(((medicationId, patientGuardianMedicationQS) -> {
				patientMedicationItems.add(
					GuardianGroup.fromGrouped(medicationId, patientGuardianMedicationQS));
			}));

			return new Response(patientMedicationItems);
		}
	}

	@Getter
	public static class GuardianGroup {
		private final Long patientGuardianId;
		private final List<MedicationItem> medicationItems;

		private GuardianGroup(Long patientGuardianId,
			List<MedicationItem> medicationItems) {
			this.patientGuardianId = patientGuardianId;
			this.medicationItems = medicationItems;
		}

		public static GuardianGroup fromGrouped(Long patientGuardianId,
			List<PatientGuardianMedicationQ> rows) {
			List<MedicationItem> medicationItems = new ArrayList<>();

			Map<Long, List<PatientGuardianMedicationQ>> grouped = rows.stream()
				.collect(
					Collectors.groupingBy(PatientGuardianMedicationQ::getMedicationId));

			grouped.forEach((medicationId, patientMedicationQS) -> medicationItems.add(
				MedicationItem.fromGroupedRows(patientMedicationQS)));

			return new GuardianGroup(patientGuardianId, medicationItems);
		}
	}

	@Getter
	public static class MedicationItem {
		private final Long medicationId;
		private final String medicationName;
		private final LocalDate startDate;
		private final LocalDate endDate;
		private final NavigableSet<LocalTime> times;
		private final EnumSet<DayOfWeek> days;

		private MedicationItem(Long medicationId, String medicationName, LocalDate startDate, LocalDate endDate,
			NavigableSet<LocalTime> times, EnumSet<DayOfWeek> days) {
			this.medicationId = medicationId;
			this.medicationName = medicationName;
			this.startDate = startDate;
			this.endDate = endDate;
			this.times = times;
			this.days = days;
		}

		public static MedicationItem fromGroupedRows(
			List<PatientGuardianMedicationQ> rows) {
			NavigableSet<LocalTime> times = rows.stream()
				.map(PatientGuardianMedicationQ::getTime)
				.collect(Collectors.toCollection(java.util.TreeSet::new));

			EnumSet<DayOfWeek> days = rows.stream()
				.map(PatientGuardianMedicationQ::getDay)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));

			return new MedicationItem(rows.get(0).getMedicationId(),
				rows.get(0).getMedicationName(),
				rows.get(0).getStartDate(), rows.get(0).getEndDate(),
				times, days);
		}
	}
}

