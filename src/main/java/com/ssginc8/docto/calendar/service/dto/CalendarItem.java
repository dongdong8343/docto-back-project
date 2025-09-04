package com.ssginc8.docto.calendar.service.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.querydsl.core.Tuple;

import lombok.Getter;

@Getter
public class CalendarItem {
	private final LocalDate date;
	private final LocalTime time;
	private final String title;
	private final Long relatedId;
	private final ItemType itemType;
	private final List<DayOfWeek> days;

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	private final LocalDate startDate; 

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	private final LocalDate endDate;   

	private CalendarItem(LocalDate date, LocalTime time, String title, Long relatedId, ItemType itemType, List<DayOfWeek> days, LocalDate startDate, LocalDate endDate) {
		this.date = date;
		this.time = time;
		this.title = title;
		this.relatedId = relatedId;
		this.itemType = itemType;
		this.days = days;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public static List<CalendarItem> mergeAndSort(List<Tuple> medicationTuples, List<Tuple> appointmentTuples, CalendarRequest request) {
		List<CalendarItem> calendarItems = fromMedicationTuples(medicationTuples, request);
		calendarItems.addAll(fromAppointmentTuples(appointmentTuples));

		calendarItems.sort(Comparator
			.comparing(CalendarItem::getDate)
			.thenComparing(CalendarItem::getTime));

		return calendarItems;
	}

	public static List<CalendarItem> fromMedicationTuples(List<Tuple> tuples, CalendarRequest request) {
		List<CalendarItem> items = new ArrayList<>();
		YearMonth yearMonth = YearMonth.of(request.getYear(), request.getMonth());

		// medicationId 기준으로 그룹핑
		Map<Long, List<Tuple>> grouped = tuples.stream()
			.collect(Collectors.groupingBy(t -> t.get(0, Long.class)));

		for (Map.Entry<Long, List<Tuple>> entry : grouped.entrySet()) {
			List<Tuple> group = entry.getValue();
			Tuple base = group.get(0);

			Long id = base.get(0, Long.class);
			String title = base.get(1, String.class);
			LocalTime time = base.get(2, LocalTime.class);
			LocalDate startDate = base.get(6, LocalDate.class);
			LocalDate endDate = base.get(7, LocalDate.class);

			// 해당 약의 전체 요일 수집
			List<DayOfWeek> days = group.stream()
				.map(t -> DayOfWeek.valueOf(t.get(3, String.class).toUpperCase()))
				.collect(Collectors.toList());

			for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
				LocalDate date = LocalDate.of(request.getYear(), request.getMonth(), day);
				if (!days.contains(date.getDayOfWeek()) || date.isBefore(startDate) || date.isAfter(endDate)) {
					continue;
				}

				// 여러 요일 모두 담은 CalendarItem 생성
				items.add(new CalendarItem(date, time, title, id, ItemType.MEDICATION, days, startDate, endDate));
			}
		}

		return items;
	}

	public static List<CalendarItem> fromAppointmentTuples(List<Tuple> tuples) {
		List<CalendarItem> items = new ArrayList<>();
		for (Tuple tuple : tuples) {
			items.add(fromAppointmentTuple(tuple));
		}
		return items;
	}

	private static CalendarItem fromAppointmentTuple(Tuple tuple) {
		Long id = tuple.get(0, Long.class);
		String title = tuple.get(1, String.class);
		LocalDateTime dateTime = tuple.get(2, LocalDateTime.class);

		return new CalendarItem(dateTime.toLocalDate(), dateTime.toLocalTime(), title, id, ItemType.APPOINTMENT, null, null, null);
	}
}
