package com.ssginc8.docto.medication.repository;

import java.time.LocalDate;

import com.ssginc8.docto.medication.entity.MedicationAlertTime;
import com.ssginc8.docto.medication.entity.MedicationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicationLogRepository extends JpaRepository<MedicationLog, Long> {

	@EntityGraph(attributePaths = "medication")
	Page<MedicationLog> findByMedication_User_UserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

	/**
	 * 특정 MedicationAlertTime에 대해 주어진 날짜에 TAKEN (복용 완료) 상태의 MedicationLog가 존재하는지 확인.
	 * loggedAt 필드를 사용하여 날짜를 비교합니다.
	 */
	@Query("""
		SELECT COUNT(m) > 0
		FROM MedicationLog m
		WHERE m.medicationAlertTime = :alertTime
			AND FUNCTION('DATE', m.loggedAt) = :date
			AND m.status = 'TAKEN'
	""")
	boolean existsTakenLogByAlertTimeAndDate(
		@Param("alertTime")MedicationAlertTime alertTime,
		@Param("date")LocalDate date
	);

	/**
	 * 특정 MedicationAlertTime에 대해 주어진 날짜에 MISSED (미복용) 상태의 MedicationLog가 존재하는지 확인.
	 * loggedAt 필드를 사용하여 날짜를 비교합니다.
	 */
	@Query("""
       SELECT COUNT(m) > 0
       FROM MedicationLog m
       WHERE m.medicationAlertTime = :alertTime
          AND FUNCTION('DATE', m.loggedAt) = :date
          AND m.status = 'MISSED'
    """)
	boolean existsMissedLogByAlertTimeAndDate(
		@Param("alertTime") MedicationAlertTime alertTime,
		@Param("date") LocalDate date
	);
}
