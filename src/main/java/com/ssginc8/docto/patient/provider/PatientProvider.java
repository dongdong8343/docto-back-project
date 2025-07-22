package com.ssginc8.docto.patient.provider;

import com.ssginc8.docto.global.error.exception.patientException.PatientNotFoundException;
import com.ssginc8.docto.patient.entity.Patient;
import com.ssginc8.docto.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PatientProvider {

	private final PatientRepository patientRepository;

	@Transactional(readOnly = true)
	public Patient getActivePatient(Long id) {
		return patientRepository.findByPatientIdAndDeletedAtIsNull(id)
			.orElseThrow(PatientNotFoundException::new);
	}

	@Transactional(readOnly = true)
	public Page<Patient> getAllActivePatients(Pageable pageable) {
		return patientRepository.findByDeletedAtIsNull(pageable);
	}

	@Transactional
	public Patient savePatient(Patient patient) {
		return patientRepository.save(patient);
	}

	@Transactional(readOnly = true)
	public Patient getPatientByUserId(Long userId) {
		return patientRepository.findByUser_UserIdAndUser_DeletedAtIsNullAndDeletedAtIsNull(userId)
			.orElseThrow(PatientNotFoundException::new);
	}
}
