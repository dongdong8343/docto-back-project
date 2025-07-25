package com.ssginc8.docto.patient.service;

import com.ssginc8.docto.file.provider.FileProvider;
import com.ssginc8.docto.global.properties.ImageDefaultProperties;
import com.ssginc8.docto.global.util.AESUtil;
import com.ssginc8.docto.patient.dto.PatientRequest;
import com.ssginc8.docto.patient.dto.PatientResponse;
import com.ssginc8.docto.patient.entity.Patient;
import com.ssginc8.docto.patient.provider.PatientProvider;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.provider.UserProvider;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ssginc8.docto.global.error.exception.patientException.RRNEncryptionFailedException;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

	private final PatientProvider patientProvider;
	private final UserProvider userProvider;
	private final FileProvider fileProvider;
	private final ImageDefaultProperties imageDefaultProperties;

	@Override
	@Transactional
	public Long createPatient(PatientRequest dto) {
		User user = userProvider.getUserById(dto.getUserId());
		String encryptedRRN = encryptRRN(dto.getResidentRegistrationNumber());
		Patient patient = Patient.create(user, encryptedRRN);
		Patient savedPatient = patientProvider.savePatient(patient);
		return savedPatient.getPatientId();
	}

	@Override
	@Transactional(readOnly = true)
	public Page<PatientResponse> getAllPatients(Pageable pageable) {
		return patientProvider.getAllActivePatients(pageable)
			.map(patient -> {
				PatientResponse dto = PatientResponse.from(patient);

				// 1) User 엔티티에서 FileId 꺼내오기
				Long fileId = patient.getUser().getProfileImage() != null
					? patient.getUser().getProfileImage().getFileId()
					: null;

				// 2) S3 URL 조회 (null 이면 default)
				String url = fileProvider.getFileUrlById(fileId);
				dto.setProfileImageUrl(
					(url != null && !url.isBlank()) ? url : imageDefaultProperties.getAddress()
				);

				return dto;
			});
	}

	@Override
	@Transactional
	public void deletePatient(Long patientId) {
		Patient patient = patientProvider.getActivePatient(patientId);
		patient.delete(); // BaseTimeEntity의 soft delete
	}

	@Override
	@Transactional(readOnly = true)
	public PatientResponse getPatientByUserId(Long userId) {
		Patient patient = patientProvider.getPatientByUserId(userId);
		return PatientResponse.from(patient);
	}

	private String encryptRRN(String rrn) {
		try {
			return AESUtil.encrypt(rrn);
		} catch (Exception e) {
			throw new RRNEncryptionFailedException();
		}
	}
}
