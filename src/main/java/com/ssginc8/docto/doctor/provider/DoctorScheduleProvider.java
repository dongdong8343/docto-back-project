package com.ssginc8.docto.doctor.provider;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ssginc8.docto.doctor.dto.DoctorScheduleRequest;
import com.ssginc8.docto.doctor.entity.Doctor;
import com.ssginc8.docto.doctor.entity.DoctorSchedule;
import com.ssginc8.docto.doctor.repository.DoctorScheduleRepository;
import com.ssginc8.docto.global.error.exception.doctorException.DoctorLunchTimeConflictException;
import com.ssginc8.docto.global.error.exception.doctorException.DoctorScheduleDuplicateDayException;
import com.ssginc8.docto.global.error.exception.doctorException.DoctorScheduleNotFoundException;
import com.ssginc8.docto.global.error.exception.doctorException.HospitalScheduleNotFoundException;
import com.ssginc8.docto.global.error.exception.doctorException.InvalidDoctorScheduleLunchException;
import com.ssginc8.docto.global.error.exception.doctorException.InvalidDoctorScheduleLunchOrderException;
import com.ssginc8.docto.global.error.exception.doctorException.InvalidDoctorScheduleLunchRangeException;
import com.ssginc8.docto.global.error.exception.doctorException.InvalidDoctorScheduleOutOfHoursException;
import com.ssginc8.docto.global.error.exception.doctorException.InvalidDoctorScheduleRequiredFieldsException;
import com.ssginc8.docto.global.error.exception.doctorException.InvalidDoctorScheduleTimeException;
import com.ssginc8.docto.global.error.exception.doctorException.InvalidDoctorScheduleTimeOrderException;
import com.ssginc8.docto.hospital.entity.Hospital;
import com.ssginc8.docto.hospital.entity.HospitalSchedule;
import com.ssginc8.docto.hospital.provider.HospitalProvider;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DoctorScheduleProvider {

	private final DoctorScheduleRepository doctorScheduleRepository;
	private final HospitalProvider hospitalProvider;


	public void validateDoctorSchedule(Doctor doctor, LocalDateTime appointmentTime) {
		DayOfWeek day = DayOfWeek.from(appointmentTime);
		DoctorSchedule schedule = doctorScheduleRepository.findByDoctorAndDayOfWeek(doctor, day)
			.orElseThrow(DoctorScheduleNotFoundException::new);

		LocalTime time = appointmentTime.toLocalTime();

		if (time.isBefore(schedule.getStartTime()) || time.isAfter(schedule.getEndTime())) {
			throw new InvalidDoctorScheduleOutOfHoursException();
		}

		if (!time.isBefore(schedule.getLunchStart()) && time.isBefore(schedule.getLunchEnd())) {
			throw new InvalidDoctorScheduleLunchException();
		}
	}



		// 1) 필수 필드 Null 검증
		public void validateRequiredFields(DoctorScheduleRequest req) {
			if (req.getDayOfWeek() == null ||
				req.getStartTime() == null ||
				req.getEndTime() == null) {
				throw new InvalidDoctorScheduleRequiredFieldsException();
			}
		}

		// 2) 진료·점심시간 범위 검증
		public void validateTimeRanges(DoctorScheduleRequest req) {
			LocalTime start      = req.getStartTime();
			LocalTime end        = req.getEndTime();
			LocalTime lunchStart = req.getLunchStart();
			LocalTime lunchEnd   = req.getLunchEnd();

			// 1) 진료 시작 < 종료
			if (!start.isBefore(end)) {
				throw new InvalidDoctorScheduleTimeOrderException();
			}

			// 2) 점심시간이 둘 다 설정된 경우에만 추가 검증
			if (lunchStart != null && lunchEnd != null) {
				// 2-1) 점심 시작 < 점심 종료
				if (!lunchStart.isBefore(lunchEnd)) {
					throw new InvalidDoctorScheduleLunchOrderException();
				}
				// 2-2) 점심이 진료 시간 범위 안에 있는지
				if (lunchStart.isBefore(start) || lunchEnd.isAfter(end)) {
					throw new InvalidDoctorScheduleLunchRangeException();
				}
			}

		}




		// 4) 요청 리스트 내 중복 요일 검증
		public void validateNoDuplicateDay(List<DoctorScheduleRequest> reqList) {
			Set<DayOfWeek> seen = new HashSet<>();
			for (DoctorScheduleRequest req : reqList) {
				if (!seen.add(req.getDayOfWeek())) {
					throw new DoctorScheduleDuplicateDayException();
				}
			}
		}







	public DoctorSchedule saveDoctorSchedule(DoctorSchedule doctorSchedule) {
		return doctorScheduleRepository.save(doctorSchedule);
	}

	public DoctorSchedule getDoctorScheduleById(Long scheduleId) {
		return doctorScheduleRepository.findById(scheduleId)
			.orElseThrow(DoctorScheduleNotFoundException::new);
	}

	public void validateWithinHospitalSchedule(Doctor doctor, DoctorScheduleRequest scheduleRequest) {
		Hospital hospital = doctor.getHospital();
		HospitalSchedule hospitalSchedule = hospitalProvider.getScheduleByDay(hospital.getHospitalId(), scheduleRequest.getDayOfWeek());

		if (hospitalSchedule == null) {
			throw new HospitalScheduleNotFoundException();
		}

		if (scheduleRequest.getStartTime().isBefore(hospitalSchedule.getOpenTime()) ||
			scheduleRequest.getEndTime().isAfter(hospitalSchedule.getCloseTime())) {
			throw new InvalidDoctorScheduleTimeException();
		}

		if (scheduleRequest.getLunchStart() != null && scheduleRequest.getLunchEnd() != null) {
			if (scheduleRequest.getLunchStart().isBefore(hospitalSchedule.getLunchStart()) ||
				scheduleRequest.getLunchEnd().isAfter(hospitalSchedule.getLunchEnd())) {
				throw new DoctorLunchTimeConflictException();
			}
		}
	}

	public List<DoctorSchedule> getSchedulesByDoctorId(Long doctorId) {
		return doctorScheduleRepository.findAllByDoctorDoctorId(doctorId);
	}

	public void deleteDoctorSchedule(DoctorSchedule schedule) {
		doctorScheduleRepository.delete(schedule);
	}

	public void deleteByDoctorId(Long doctorId) {
		List<DoctorSchedule> schedules = doctorScheduleRepository.findAllByDoctorDoctorId(doctorId);
		doctorScheduleRepository.deleteAll(schedules);
	}

	public DoctorSchedule getScheduleByDoctorAndDay(Doctor doctor, DayOfWeek dayOfWeek) {
		return doctorScheduleRepository.findByDoctorAndDayOfWeek(doctor, dayOfWeek)
			.orElseThrow(DoctorScheduleNotFoundException::new);
	}
}
