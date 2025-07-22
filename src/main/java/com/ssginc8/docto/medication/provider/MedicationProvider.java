package com.ssginc8.docto.medication.provider;

import com.ssginc8.docto.medication.entity.*;
import com.ssginc8.docto.medication.repository.MedicationAlertDayRepository;
import com.ssginc8.docto.medication.repository.MedicationAlertTimeRepository;
import com.ssginc8.docto.medication.repository.MedicationInformationRepository;
import com.ssginc8.docto.medication.repository.MedicationLogRepository;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.provider.UserProvider;
import com.ssginc8.docto.global.error.exception.medicationException.MedicationNotFoundException;
import com.ssginc8.docto.user.service.UserServiceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MedicationProvider {

	private final MedicationInformationRepository medicationInformationRepository;
	private final MedicationAlertDayRepository medicationAlertDayRepository;
	private final MedicationAlertTimeRepository medicationAlertTimeRepository;
	private final MedicationLogRepository medicationLogRepository;
	private final UserProvider userProvider;
	private final UserServiceImpl userService;

	@Transactional(readOnly = true)
	public Page<MedicationLog> getMedicationLogsByCurrentUser(Pageable pageable) {
		User currentUser = userService.getUserFromUuid();
		return medicationLogRepository.findByMedication_User_UserIdAndDeletedAtIsNull(currentUser.getUserId(), pageable);
	}

	@Transactional(readOnly = true)
	public User getUser(Long userId) {
		return userProvider.getUserById(userId);
	}

	@Transactional(readOnly = true)
	public List<MedicationInformation> getMedicationsByUser(User user) {
		return medicationInformationRepository.findByUserAndDeletedAtIsNull(user);
	}

	@Transactional
	public void saveMedicationInformation(MedicationInformation medicationInformation) {
		medicationInformationRepository.save(medicationInformation);
	}

	@Transactional(readOnly = true)
	public User getCurrentUserFromToken() {
		return userService.getUserFromUuid();
	}

	@Transactional(readOnly = true)
	public MedicationInformation getMedication(Long medicationId) {
		return medicationInformationRepository.findByMedicationIdAndDeletedAtIsNull(medicationId)
			.orElseThrow(MedicationNotFoundException::new);
	}

	@Transactional(readOnly = true)
	public List<MedicationAlertTime> findAlertTimesDayAndTime(DayOfWeek day, LocalTime time) {
		return medicationAlertDayRepository.findAlertTimesByDayAndTime(day, time);
	}

	@Transactional
	public void updateDateRange(Long medicationId, LocalDate newStart, LocalDate newEnd) {
		int updated = medicationInformationRepository.updateDateRange(medicationId, newStart, newEnd);
		if (updated == 0) {
			throw new com.ssginc8.docto.global.error.exception.medicationException.MedicationNotFoundException();
		}
		// 필요 시 영속성 컨텍스트를 clear하거나, Service에서 재조회
	}

	@Transactional(readOnly = true)
	public MedicationAlertTime getMedicationAlertTimeById(Long alertTimeId) {
		return medicationAlertTimeRepository.findByMedicationAlertTimeIdAndDeletedAtIsNull(alertTimeId)
			.orElseThrow(MedicationNotFoundException::new);
	}

	/**
	 * 특정 MedicationAlertTime에 대해 오늘 TAKEN (복용 완료) 로그가 존재하는지 확인
	 */
	@Transactional(readOnly = true)
	public boolean existsTakenLogForToday(MedicationAlertTime alertTime) {
		return medicationLogRepository.existsTakenLogByAlertTimeAndDate(alertTime, LocalDate.now());
	}
	/**
	 * 특정 MedicationAlertTime에 대해 오늘 MISSED (미복용) 로그가 존재하는지 확인
	 */
	@Transactional(readOnly = true)
	public boolean existsMissedLogForToday(MedicationAlertTime alertTime) {
		return medicationLogRepository.existsMissedLogByAlertTimeAndDate(alertTime, LocalDate.now());
	}

	/**
	 * MedicationLog 저장
	 */
	@Transactional
	public void saveMedicationLog(MedicationLog medicationLog) {
		medicationLogRepository.save(medicationLog);
	}

}
