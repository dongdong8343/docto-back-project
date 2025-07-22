package com.ssginc8.docto.medication.repository;

import com.ssginc8.docto.medication.entity.MedicationInformation;
import com.ssginc8.docto.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicationInformationRepository extends JpaRepository<MedicationInformation, Long> {
	Optional<MedicationInformation> findByMedicationIdAndDeletedAtIsNull(Long medicationId);

	List<MedicationInformation> findByUserAndDeletedAtIsNull(User user);

	@Modifying
	@Query("UPDATE MedicationInformation m SET m.startDate = :startDate, m.endDate = :endDate WHERE m.medicationId = :id AND m.deletedAt IS NULL")
	int updateDateRange(@Param("id") Long id,
		@Param("startDate") LocalDate startDate,
		@Param("endDate") LocalDate endDate);

}
