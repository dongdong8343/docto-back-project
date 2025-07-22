package com.ssginc8.docto.patient.repository;

import com.ssginc8.docto.patient.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
	Optional<Patient> findByPatientIdAndDeletedAtIsNull(Long patientId);

	Page<Patient> findByDeletedAtIsNull(Pageable pageable);

	Optional<Patient> findByUser_UserIdAndUser_DeletedAtIsNullAndDeletedAtIsNull(Long userUserId);

}
