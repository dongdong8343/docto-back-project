package com.ssginc8.docto.medication.service;

import com.ssginc8.docto.auth.provider.CurrentUserProvider;
import com.ssginc8.docto.global.error.exception.medicationException.InvalidMedicationDateException;
import com.ssginc8.docto.global.error.exception.medicationException.MedicationAlertDayNotFoundException;
import com.ssginc8.docto.global.error.exception.medicationException.MedicationAlertTimeNotFoundException;
import com.ssginc8.docto.global.error.exception.medicationException.MedicationTakenTimeNotTodayException;
import com.ssginc8.docto.guardian.provider.PatientGuardianProvider;
import com.ssginc8.docto.medication.dto.MedicationCompleteRequest;
import com.ssginc8.docto.medication.dto.MedicationLogResponse;
import com.ssginc8.docto.medication.dto.MedicationScheduleRequest;
import com.ssginc8.docto.medication.dto.MedicationScheduleResponse;
import com.ssginc8.docto.medication.dto.MedicationUpdateRequest;
import com.ssginc8.docto.medication.entity.MedicationAlertDay;
import com.ssginc8.docto.medication.entity.MedicationAlertTime;
import com.ssginc8.docto.medication.entity.MedicationInformation;
import com.ssginc8.docto.medication.entity.MedicationLog;
import com.ssginc8.docto.medication.entity.MedicationStatus;
import com.ssginc8.docto.medication.provider.MedicationProvider;
import com.ssginc8.docto.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicationServiceImpl implements MedicationService {

	private final CurrentUserProvider currentUserProvider;
	private final MedicationProvider medicationProvider;

	@Transactional(readOnly = true)
	@Override
	public Page<MedicationLogResponse> getMedicationLogsByCurrentUser(Pageable pageable) {
		Page<MedicationLog> logs = medicationProvider.getMedicationLogsByCurrentUser(pageable);
		return logs.map(MedicationLogResponse::from);
	}

	@Transactional
	@Override
	public void registerMedicationSchedule(MedicationScheduleRequest request) {
		// 날짜 유효성 검사
		if (request.getStartDate() != null
			&& request.getEndDate() != null
			&& request.getStartDate().isAfter(request.getEndDate())) {
			throw new InvalidMedicationDateException();
		}

		User user = medicationProvider.getUser(request.getUserId());

		MedicationInformation info = MedicationInformation.create(
			user,
			request.getPatientGuardianId(),
			request.getMedicationName(),
			request.getStartDate(),
			request.getEndDate()
		);

		if (request.getTimes() == null || request.getTimes().isEmpty()) {
			throw new MedicationAlertTimeNotFoundException();
		}

		// MealTime 리스트 순회하여 meal+time 저장
		for (MedicationScheduleRequest.MealTime mt : request.getTimes()) {
			String meal = mt.getMeal();              // "morning" | "lunch" | "dinner"
			LocalTime t = mt.getTime();

			MedicationAlertTime at = MedicationAlertTime.create(info, meal, t);
			for (DayOfWeek dow : request.getDays()) {
				at.getAlertDays().add(
					MedicationAlertDay.create(at, dow)
				);
			}
			info.getAlertTimes().add(at);
		}

		medicationProvider.saveMedicationInformation(info);
	}

	@Transactional(readOnly = true)
	@Override
	public List<MedicationScheduleResponse> getMedicationSchedulesByCurrentUser() {
		User user = currentUserProvider.getUserFromUuid();
		List<MedicationInformation> infos = medicationProvider.getMedicationsByUser(user);

		return infos.stream()
			.map(info -> {
				List<MedicationScheduleResponse.MealTime> times = info.getAlertTimes().stream()
					.map(at -> new MedicationScheduleResponse.MealTime(
						at.getMeal(),
						at.getTimeToTake()
					))
					.collect(Collectors.toList());

				List<DayOfWeek> days = info.getAlertTimes().stream()
					.flatMap(at -> at.getAlertDays().stream())
					.map(MedicationAlertDay::getDayOfWeek)
					.distinct()
					.collect(Collectors.toList());

				return MedicationScheduleResponse.builder()
					.medicationId(info.getMedicationId())
					.medicationName(info.getMedicationName())
					.times(times)
					.days(days)
					.startDate(info.getStartDate())
					.endDate(info.getEndDate())
					.build();
			})
			.collect(Collectors.toList());
	}

	@Transactional
	@Override
	public void updateMedicationTime(Long medicationId, MedicationUpdateRequest request) {
		MedicationInformation info = medicationProvider.getMedication(medicationId);

		// 날짜 범위 변경
		if (request.getNewStartDate() != null || request.getNewEndDate() != null) {
			medicationProvider.updateDateRange(
				medicationId,
				request.getNewStartDate(),
				request.getNewEndDate()
			);
		}

		// MealTime 기반 알림시간·요일 일괄 교체
		List<MedicationUpdateRequest.MealTime> times = request.getNewTimes();
		List<DayOfWeek> days = request.getNewDays();

		if (times != null && !times.isEmpty()) {
			info.getAlertTimes().clear();
			for (MedicationUpdateRequest.MealTime mt : times) {
				MedicationAlertTime at = MedicationAlertTime.create(
					info,
					mt.getMeal(),
					mt.getTime()
				);
				if (days != null && !days.isEmpty()) {
					for (DayOfWeek dow : days) {
						at.getAlertDays().add(
							MedicationAlertDay.create(at, dow)
						);
					}
				} else {
					throw new MedicationAlertDayNotFoundException();
				}
				info.getAlertTimes().add(at);
			}
		}
	}

	@Transactional
	@Override
	public void deleteMedicationSchedule(Long medicationId) {
		MedicationInformation info = medicationProvider.getMedication(medicationId);
		info.delete(); // soft delete
	}

	@Transactional
	@Override
	public void completedMedication(Long medicationId, MedicationCompleteRequest request) {
		// 한국 시간대로 변환
		OffsetDateTime utcCompletedAt = request.getCompletedAt();
		ZonedDateTime koreanZonedTime = utcCompletedAt.atZoneSameInstant(ZoneId.of("Asia/Seoul"));
		LocalDateTime completedAt = koreanZonedTime.toLocalDateTime();

		MedicationInformation info = medicationProvider.getMedication(medicationId);
		MedicationAlertTime alertTime = medicationProvider.getMedicationAlertTimeById(request.getMedicationAlertTimeId());

		// 클라이언트에서 보낸 completeAt이 현재 날짜와 다를 경우 유효성 검사
		if (!request.getCompletedAt().toLocalDate().isEqual(LocalDate.now())) {
			throw new MedicationTakenTimeNotTodayException();
		}

		// 이미 해당 알림 시간에 대해 오늘 TAKEN 또는 MISSED 로그가 있는지 확인
		// MISSED 상태라도 사용자가 나중에 약을 복용할 수 있으므로, TAKEN과 MISSED 둘 다 확인
		if (medicationProvider.existsTakenLogForToday(alertTime) || medicationProvider.existsMissedLogForToday(alertTime)) {
			log.debug("DEBUG: 이미 복용 완료 또는 미복용 처리된 약입니다. alertTimeId: {}", alertTime.getMedicationAlertTimeId());
			return;
		}

		MedicationLog log = MedicationLog.create(
			alertTime,
			info,
			MedicationStatus.TAKEN,
			completedAt
		);
		medicationProvider.saveMedicationLog(log);
	}

	@Override
	@Transactional(readOnly = true)
	public MedicationScheduleResponse getMedicationScheduleById(Long medicationId) {
		MedicationInformation info = medicationProvider.getMedication(medicationId);
		// 기존 getMedicationSchedulesByCurrentUser 내부 로직을 재사용
		List<MedicationScheduleResponse.MealTime> times = info.getAlertTimes().stream()
			.map(at -> new MedicationScheduleResponse.MealTime(
				at.getMeal(),
				at.getTimeToTake()
			))
			.collect(Collectors.toList());

		List<DayOfWeek> days = info.getAlertTimes().stream()
			.flatMap(at -> at.getAlertDays().stream())
			.map(MedicationAlertDay::getDayOfWeek)
			.distinct()
			.collect(Collectors.toList());

		return MedicationScheduleResponse.builder()
			.medicationId(info.getMedicationId())
			.medicationName(info.getMedicationName())
			.times(times)
			.days(days)
			.startDate(info.getStartDate())
			.endDate(info.getEndDate())
			.build();
	}
}
