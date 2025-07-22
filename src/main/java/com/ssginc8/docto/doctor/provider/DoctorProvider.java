package com.ssginc8.docto.doctor.provider;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ssginc8.docto.doctor.entity.Doctor;
import com.ssginc8.docto.doctor.entity.DoctorSchedule;
import com.ssginc8.docto.doctor.repository.DoctorRepository;
import com.ssginc8.docto.global.error.ErrorCode;
import com.ssginc8.docto.global.error.exception.doctorException.DoctorAlreadyExistsException;
import com.ssginc8.docto.global.error.exception.doctorException.DoctorNotFoundException;
import com.ssginc8.docto.global.error.exception.doctorException.NotDoctorRoleException;
import com.ssginc8.docto.global.error.exception.doctorException.ScheduleNotInDoctorException;
import com.ssginc8.docto.hospital.entity.Hospital;
import com.ssginc8.docto.user.entity.Role;
import com.ssginc8.docto.user.entity.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DoctorProvider {

	private final DoctorRepository doctorRepository;

	@Value("${cloud.default.image.address}")
	private String defaultProfileUrl;

	public Doctor getDoctorByUserId(Long userId) {
		return doctorRepository.findByUserUserId(userId)
			.orElseThrow(DoctorNotFoundException::new);
	}

	@Transactional(readOnly = true)
	public Doctor getDoctorById(Long doctorId) {
		return doctorRepository.findById(doctorId)
			.orElseThrow(DoctorNotFoundException::new);
	}

	public Doctor saveDoctor(Doctor doctor) {
		doctorRepository.save(doctor);
		return doctor;
	}

	public void validateUserIsNotAlreadyDoctor(User user) {
		if (doctorRepository.existsByUser(user)) {
			throw new DoctorAlreadyExistsException(ErrorCode.DOCTOR_ALREADY_EXISTS);
		}
	}


	public List<Doctor> findByHospital(Hospital hospital) {
		return doctorRepository.findByHospital(hospital); // 이 부분이 null 방지 필요
	}

	public Page<Doctor> getAllDoctors(Pageable pageable) {
		return doctorRepository.findByDeletedAtIsNull(pageable);
	}

	public List<Doctor> getDoctorsByHospitalId(Long hospitalId) {
		return doctorRepository.findByHospitalHospitalId(hospitalId);
	}


	public void validateScheduleBelongsToDoctor(DoctorSchedule schedule, Long doctorId) {
		if (!schedule.getDoctor().getDoctorId().equals(doctorId)) {
			throw new ScheduleNotInDoctorException();
		}
	}

	public void validateUserIsDoctor(User user) {
		if (user.getRole() != Role.DOCTOR) {
			throw new NotDoctorRoleException();
		}
	}

	public String getImageUrlOrNull(Doctor doctor) {
		if (doctor.getUser() != null && doctor.getUser().getProfileImage() != null) {
			return doctor.getUser().getProfileImage().getUrl();
		}
		return defaultProfileUrl;
	}
}
