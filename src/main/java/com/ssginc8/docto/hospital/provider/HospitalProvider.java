package com.ssginc8.docto.hospital.provider;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ssginc8.docto.file.provider.FileProvider;
import com.ssginc8.docto.file.repository.FileRepository;
import com.ssginc8.docto.global.error.exception.hospitalException.HospitalNotFoundException;
import com.ssginc8.docto.global.error.exception.hospitalException.InvalidHospitalScheduleRequiredFieldsException;
import com.ssginc8.docto.global.error.exception.hospitalException.InvalidHospitalScheduleTimeOrderException;
import com.ssginc8.docto.global.error.exception.hospitalException.ScheduleNotFoundByDayException;
import com.ssginc8.docto.global.error.exception.hospitalException.ScheduleNotFoundException;
import com.ssginc8.docto.global.error.exception.hospitalException.ScheduleNotInHospitalException;
import com.ssginc8.docto.global.error.exception.userException.UserNotFoundException;
import com.ssginc8.docto.hospital.dto.HospitalScheduleRequest;
import com.ssginc8.docto.hospital.entity.Hospital;
import com.ssginc8.docto.hospital.entity.HospitalSchedule;
import com.ssginc8.docto.hospital.entity.ProvidedService;
import com.ssginc8.docto.hospital.repository.HospitalRepository;
import com.ssginc8.docto.hospital.repository.HospitalScheduleRepository;
import com.ssginc8.docto.hospital.repository.ProvidedServiceRepository;

import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HospitalProvider {

	private final HospitalRepository hospitalRepository;
	private final UserRepository userRepository;
	private final HospitalScheduleRepository hospitalScheduleRepository;
	private final ProvidedServiceRepository providedServiceRepository;
	private final FileRepository fileRepo;
	private final FileProvider fileProvider;








	@Transactional(readOnly = true)
	public Hospital getHospitalById(Long hospitalId) {
		return hospitalRepository.findByHospitalIdAndDeletedAtIsNull(hospitalId)
			.orElseThrow(HospitalNotFoundException::new);
	}

	public User getUserById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(UserNotFoundException::new);
	}

	public HospitalSchedule getScheduleByIdOrThrow(Long scheduleId) {
		return hospitalScheduleRepository.findById(scheduleId)
			.orElseThrow(ScheduleNotFoundException::new);
	}





	public void validateScheduleBelongsToHospital(HospitalSchedule schedule, Hospital hospital) {
		if (!schedule.getHospital().getHospitalId().equals(hospital.getHospitalId())) {
			throw new ScheduleNotInHospitalException();
		}
	}

	public void saveHospital(Hospital hospital) {
		hospitalRepository.save(hospital);
	}

	public HospitalSchedule saveHospitalSchedule(HospitalSchedule schedule) {
		hospitalScheduleRepository.save(schedule);
		return schedule;
	}




	public Page<Hospital> findHospitalsWithinRadius(double lat, double lng, double radius, String query, Pageable pageable) {
		return hospitalRepository.findHospitalsWithinRadius(lat, lng, radius,query,  pageable);
	}



	@Transactional
	public void deleteByHospitalScheduleId(Long hospitalScheduleId) {
		HospitalSchedule schedule = hospitalScheduleRepository.findById(hospitalScheduleId)
			.orElseThrow(ScheduleNotFoundException::new);
		hospitalScheduleRepository.delete(schedule);
	}


	public List<ProvidedService> findServicesByHospitalId(Long hospitalId) {
		return providedServiceRepository.findByHospitalHospitalId(hospitalId);
	}


	public void deleteProvidedServicesByHospital(Hospital hospital) {
		providedServiceRepository.deleteAllByHospital(hospital);
	}




	public Page<Hospital> findAll(Pageable pageable) {
		return hospitalRepository.findAllByDeletedAtIsNull(pageable);
	}

	public List<HospitalSchedule> findSchedulesByHospitalId(Long hospitalId) {
		return hospitalScheduleRepository.findByHospitalHospitalId(hospitalId);
	}


	public HospitalSchedule getScheduleByDay(Long hospitalId, DayOfWeek dayOfWeek) {
		return hospitalScheduleRepository.findByHospitalHospitalIdAndDayOfWeek(hospitalId, dayOfWeek)
			.orElseThrow(ScheduleNotFoundByDayException::new);
	}

	public void deleteByHospitalId(Long hospitalId) {
		hospitalScheduleRepository.deleteAllByHospitalHospitalId(hospitalId);
	}

	@Transactional(readOnly = true)
	public Hospital findByUserUserId(Long userId) {
		return hospitalRepository.findByUserUserId(userId)
			.orElseThrow(HospitalNotFoundException::new);
	}






	public String getImageUrl(Long hospitalId) {
		return fileRepo.getFileUrlById((hospitalId));
	}

	public List<String> getServiceNames(Long hospitalId) {
		return providedServiceRepository.findServiceNamesByHospitalId(hospitalId);
	}

	public Page<Hospital> findAllNearby(
		String query,
		double latitude,
		double longitude,
		double radius,
		Pageable pageable
	) {
		// repository 의 커스텀 메서드 호출
		return hospitalRepository.findAllNearby(query, latitude, longitude, radius, pageable);
	}

	/**
	 * 일반 검색
	 */
	public Page<Hospital> findAll(
		Specification<Hospital> spec,
		Pageable pageable
	) {
		return hospitalRepository.findAll(spec, pageable);
	}

	public void saveAll(List<ProvidedService> svs) {
		providedServiceRepository.saveAll(svs);
	}

	public List<String> findServiceNamesByHospitalId(Long hospitalId) {
		return providedServiceRepository.findServiceNamesByHospitalId(hospitalId);
	}





	@Transactional
	public void deleteServiceById(Long serviceId) {
		providedServiceRepository.deleteById(serviceId);
	}


	@Transactional
	public ProvidedService saveService(ProvidedService service) {
		return providedServiceRepository.save(service);
	}


	public void validateHospitalScheduleFields(HospitalScheduleRequest req) {
		if (req.getOpenTime() == null || req.getCloseTime() == null) {
			throw new InvalidHospitalScheduleRequiredFieldsException();
		}

		if (!req.getOpenTime().isBefore(req.getCloseTime())) {
			throw new InvalidHospitalScheduleTimeOrderException();
		}
	}
}

