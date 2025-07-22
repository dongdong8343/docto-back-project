package com.ssginc8.docto.medication.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ssginc8.docto.medication.entity.MedicationAlertTime;

public interface MedicationAlertTimeRepository extends JpaRepository<MedicationAlertTime, Long> {

	Optional<MedicationAlertTime> findByMedicationAlertTimeIdAndDeletedAtIsNull(Long medicationAlertTimeId);

}
