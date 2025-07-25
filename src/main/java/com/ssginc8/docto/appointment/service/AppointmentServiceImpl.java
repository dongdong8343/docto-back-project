package com.ssginc8.docto.appointment.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.hibernate.query.SortDirection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssginc8.docto.appointment.dto.AppointmentDailyCountResponse;
import com.ssginc8.docto.appointment.dto.AppointmentListResponse;
import com.ssginc8.docto.appointment.dto.AppointmentRequest;
import com.ssginc8.docto.appointment.dto.AppointmentResponse;
import com.ssginc8.docto.appointment.dto.AppointmentSearchCondition;
import com.ssginc8.docto.appointment.dto.TimeSlotDto;
import com.ssginc8.docto.appointment.entity.Appointment;
import com.ssginc8.docto.appointment.entity.AppointmentStatus;
import com.ssginc8.docto.appointment.entity.AppointmentType;
import com.ssginc8.docto.appointment.entity.PaymentType;
import com.ssginc8.docto.appointment.provider.AppointmentProvider;
import com.ssginc8.docto.auth.provider.CurrentUserProvider;
import com.ssginc8.docto.doctor.entity.Doctor;
import com.ssginc8.docto.doctor.entity.DoctorSchedule;
import com.ssginc8.docto.doctor.provider.DoctorProvider;
import com.ssginc8.docto.appointment.validator.AppointmentValidator;
import com.ssginc8.docto.doctor.provider.DoctorScheduleProvider;
import com.ssginc8.docto.global.error.exception.appointmentException.IsSuspendedUserException;
import com.ssginc8.docto.global.error.exception.appointmentException.NotCanceledException;
import com.ssginc8.docto.global.error.exception.appointmentException.RoleNotFoundException;
import com.ssginc8.docto.global.event.appointment.AppointmentRequestEvent;
import com.ssginc8.docto.global.event.appointment.AppointmentStatusChangedEvent;
import com.ssginc8.docto.guardian.entity.PatientGuardian;
import com.ssginc8.docto.guardian.provider.PatientGuardianProvider;
import com.ssginc8.docto.hospital.entity.Hospital;
import com.ssginc8.docto.hospital.provider.HospitalProvider;
import com.ssginc8.docto.hospital.service.HospitalService;
import com.ssginc8.docto.notification.service.NotificationService;
import com.ssginc8.docto.patient.entity.Patient;
import com.ssginc8.docto.patient.provider.PatientProvider;
import com.ssginc8.docto.qna.dto.QaPostCreateRequest;
import com.ssginc8.docto.qna.provider.QaPostProvider;
import com.ssginc8.docto.qna.service.QaPostService;

import com.ssginc8.docto.review.provider.ReviewProvider;

import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.provider.UserProvider;
import com.ssginc8.docto.user.service.UserService;
import com.ssginc8.docto.appointment.util.TimeSlotUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

	private final CurrentUserProvider currentUserProvider;
	private final AppointmentProvider appointmentProvider;
	private final UserProvider userProvider;
	private final PatientProvider patientProvider;
	private final PatientGuardianProvider patientGuardianProvider;
	private final HospitalProvider hospitalProvider;
	private final DoctorProvider doctorProvider;
	private final DoctorScheduleProvider doctorScheduleProvider;
	private final QaPostProvider qaPostProvider;
	private final ReviewProvider reviewProvider;

	private final QaPostService qaPostService;
	private final UserService userService;
	private final HospitalService hospitalService;

	private final AppointmentValidator appointmentValidator;
	private final ApplicationEventPublisher applicationEventPublisher;

	/**
	 * 진료 예약 리스트 조회
	 * 검색 필터링 조건: hospitalId, doctorId, patientGuardianId
	 * 병원, 의사, 환자, 보호자 입장에서 검색 가능
	 */
	@Override
	public Page<AppointmentListResponse> getAppointmentList(Pageable pageable, AppointmentSearchCondition condition) {

		Page<Appointment> appointments = appointmentProvider.getAppointmentListByCondition(pageable, condition);

		return appointments.map(AppointmentListResponse::fromEntity);
	}

	/**
	 * Token의 정보로 Appointment List 가져오기
	 */
	@Override
	public Page<AppointmentListResponse> getAppointmentsByLoginUser(Pageable pageable, LocalDate date) {
		// 1. 로그인한 사용자 가져오기
		User loginUser = currentUserProvider.getUserFromUuid();

		Page<Appointment> appointments;

		// 2. 역할에 따라 분기 처리
		switch (loginUser.getRole()) {
			case PATIENT:
				appointments = appointmentProvider.getAppointmentsByPatient(loginUser.getUserId(), pageable);
				break;

			case GUARDIAN:
				appointments = appointmentProvider.getAppointmentsByGuardian(loginUser.getUserId(), pageable);
				break;

			case DOCTOR:
				return appointmentProvider.getAppointmentsByDoctor(loginUser.getUserId(), pageable, date)
					.map(AppointmentListResponse::fromEntity);

			case HOSPITAL_ADMIN:
				return appointmentProvider.getAppointmentsByHospital(loginUser.getUserId(), pageable, date)
					.map(AppointmentListResponse::fromEntity);
			default:
				throw new RoleNotFoundException();
		}

		// 리뷰가 작성된 appointmentId만 추출
		List<Long> appointmentIds = appointments.stream()
			.map(Appointment::getAppointmentId)
			.toList();

		Set<Long> reviewedAppointmentIds = reviewProvider.getReviewedAppointmentIds(appointmentIds);

		// 리뷰 여부 포함해서 DTO 변환
		return appointments.map(app ->
			AppointmentListResponse.fromEntity(
				app,
				reviewedAppointmentIds.contains(app.getAppointmentId())
			)
		);
	}

	/**
	 * 진료 예약의 단건 조회
	 */
	@Transactional(readOnly = true)
	@Override
	public AppointmentResponse getAppointmentDetail(Long appointmentId) {
		Appointment appointment = appointmentProvider.getAppointmentById(appointmentId);
		String qaPost = qaPostProvider.getQaPostByAppointment(appointment);

		return AppointmentResponse.fromEntity(appointment, qaPost);
	}

	/**
	 * 진료 예약 접수
	 * 접수 시 STATUS.REQUESTED
	 * 병원 확인 후 승인 -> CONFIRMED => 이 과정은 updateAppointmentStatus로 관리
	 * 			  거절 -> CANCELED
	 */
	@Transactional
	@Override
	public void requestAppointment(AppointmentRequest request) {
		// 1. 보호자 (user)와 환자 (patient) 조회
		User guardian = userProvider.getUserById(request.getUserId());

		if (guardian.getIsSuspended()) {
			throw new IsSuspendedUserException();
		}

		Patient patient = patientProvider.getActivePatient(request.getPatientId());

		// 2. 보호자-환자 관계 유효성 검사
		PatientGuardian patientGuardian = patientGuardianProvider.validateAndGetPatientGuardian(guardian, patient);

		// 3. 병원과 의사 조회
		Hospital hospital = hospitalProvider.getHospitalById(request.getHospitalId());
		Doctor doctor = doctorProvider.getDoctorById(request.getDoctorId());

		// 4. request 데이터 검증
		// 1) validate appointmentTime
		appointmentValidator.validateAppointmentTime(doctor, patientGuardian, request.getAppointmentTime());

		// 2) 수납 방식과 예약 타입 enum 값들 유효성 검사
		AppointmentType appointmentType = AppointmentType.from(request.getAppointmentType());
		PaymentType paymentType = PaymentType.from(request.getPaymentType());

		// 3) 중복 예약 방지
		// 동일 환자 예약 제한 (30분) : 다른 병원, 다른 의사라도
		appointmentValidator.validateDuplicateAppointment(patient, request.getAppointmentTime());

		// 한 병원의 한 의사가 30분 단위로 받을 수 있는 환자 수 제한
		appointmentValidator.validateDoctorSlotCapacity(doctor, request.getAppointmentTime());

		// 5. Appointment 엔티티 생성 및 저장
		Appointment appointment = Appointment.create(
			patientGuardian,
			hospital,
			doctor,
			request.getAppointmentTime(),
			appointmentType,
			request.getSymptom(),
			AppointmentStatus.REQUESTED,
			paymentType
		);

		appointmentProvider.save(appointment);

		// 6. 질문(QaPost) 저장
		if (request.getQuestion() != null && !request.getQuestion().isBlank()) {
			QaPostCreateRequest qaPostCreateRequest = QaPostCreateRequest.builder()
				.appointmentId(appointment.getAppointmentId())
				.content(request.getQuestion())
				.build();

			qaPostService.createQaPost(qaPostCreateRequest);
		}

		applicationEventPublisher.publishEvent(new AppointmentRequestEvent(appointment));
	}

	/**
	 * 진료 상태 업데이트
	 */
	@Transactional
	@Override
	public AppointmentResponse updateAppointmentStatus(Long appointmentId, String statusStr) {
		Appointment appointment = appointmentProvider.getAppointmentById(appointmentId);
		String qaContent = qaPostProvider.getQaPostByAppointment(appointment);

		AppointmentStatus newStatus = AppointmentStatus.from(statusStr);
		appointment.changeStatus(newStatus);

		appointmentProvider.save(appointment);

		applicationEventPublisher.publishEvent(new AppointmentStatusChangedEvent(appointment, false));

		return AppointmentResponse.fromEntity(appointment, qaContent);
	}

	/**
	 * 재예약: 기존 예약에서 예약 시간 변경
	 */
	@Transactional
	@Override
	public AppointmentResponse rescheduleAppointment(Long appointmentId, LocalDateTime newTime) {
		Appointment original = appointmentProvider.getAppointmentById(appointmentId);

		User guardian = original.getPatientGuardian().getUser();

		if (guardian.getIsSuspended()) {
			throw new IsSuspendedUserException();
		}

		original.changeStatus(AppointmentStatus.CANCELED);
		String qaContent = qaPostProvider.getQaPostByAppointment(original);

		appointmentValidator.validateAppointmentTime(original.getDoctor(), original.getPatientGuardian(), newTime);
		appointmentValidator.validateDuplicateAppointment(original.getPatientGuardian().getPatient(), newTime);
		appointmentValidator.validateDoctorSlotCapacity(original.getDoctor(), newTime);

		Appointment newAppointment = Appointment.create(
			original.getPatientGuardian(),
			original.getHospital(),
			original.getDoctor(),
			newTime,
			original.getAppointmentType(),
			original.getSymptom(),
			AppointmentStatus.REQUESTED,
			original.getPaymentType()
		);

		appointmentProvider.save(newAppointment);
		applicationEventPublisher.publishEvent(new AppointmentRequestEvent(newAppointment));

		return AppointmentResponse.fromEntity(newAppointment, qaContent);
	}

	/**
	 * 예약 취소
	 */
	@Transactional
	public void cancelAppointment(Long appointmentId) {
		// 1. 예약 조회
		Appointment appointment = appointmentProvider.getAppointmentById(appointmentId);

		// 2. 예약 취소 가능 상태인지 확인
		if (appointment.getStatus() != AppointmentStatus.REQUESTED &&
			appointment.getStatus() != AppointmentStatus.CONFIRMED) {
			throw new NotCanceledException();
		}

		// 3. User 엔티티 조회
		User user = appointment.getPatientGuardian().getUser();

		// 4. 현재 시간과 예약 시간 계산
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime appointmentTime = appointment.getAppointmentTime();

		// 5. 패널티 부여 여부 판단 (예약 시간 1시간 이내 취소 시)
		boolean isPenalty = now.isAfter(appointmentTime.minusHours(1));

		// 6. 예약 상태 변경 및 패널티 부여 로직
		if (isPenalty) {
			user.addPenalty(1L); // User 엔티티의 addPenalty 메서드 호출

			// 예약 상태를 NO_SHOW로 변경 (1시간 이내 취소 시)
			appointment.changeStatus(AppointmentStatus.NO_SHOW);
		} else {
			// 1시간 이내 취소가 아닌 경우 CANCELED로 변경
			appointment.changeStatus(AppointmentStatus.CANCELED);
		}

		// 7. 변경된 Appointment 엔티티 저장
		appointmentProvider.save(appointment);


		// 8. 알림 이벤트 발행
		applicationEventPublisher.publishEvent(new AppointmentStatusChangedEvent(appointment, isPenalty));
	}

	@Override
	public Page<AppointmentListResponse> getActiveAppointmentsByLoginUser(Pageable pageable, LocalDate date) {
		List<AppointmentStatus> activeStatuses = List.of(AppointmentStatus.REQUESTED, AppointmentStatus.CONFIRMED);
		return getAppointmentsByLoginUserAndStatus(pageable, date, activeStatuses, SortDirection.ASCENDING);
	}

	@Override
	public Page<AppointmentListResponse> getInactiveAppointmentsByLoginUser(Pageable pageable, LocalDate date) {
		List<AppointmentStatus> inactiveStatuses = List.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELED, AppointmentStatus.NO_SHOW);
		return getAppointmentsByLoginUserAndStatus(pageable, date, inactiveStatuses, SortDirection.DESCENDING);
	}

	/**
	 * 특정 상태를 가진 예약 리스트 가져오는 공통 로직
	 */
	private Page<AppointmentListResponse> getAppointmentsByLoginUserAndStatus(
		Pageable pageable,
		LocalDate date,
		List<AppointmentStatus> targetStatuses,
		SortDirection sortDirection
	)
	{
		User loginUser = currentUserProvider.getUserFromUuid();
		Page<Appointment> appointments;

		switch (loginUser.getRole()) {
			case PATIENT:
				appointments = (sortDirection == SortDirection.ASCENDING) ?
					appointmentProvider.getAppointmentsByPatientAndStatusIn(loginUser.getUserId(), targetStatuses, pageable) :
					appointmentProvider.getAppointmentsByPatientAndStatusInDesc(loginUser.getUserId(), targetStatuses, pageable);
				break;
			case GUARDIAN:
				appointments = (sortDirection == SortDirection.ASCENDING) ?
					appointmentProvider.getAppointmentsByGuardianAndStatusIn(loginUser.getUserId(), targetStatuses, pageable) :
					appointmentProvider.getAppointmentsByGuardianAndStatusInDesc(loginUser.getUserId(), targetStatuses, pageable);
				break;
			case DOCTOR:
				return (sortDirection == SortDirection.ASCENDING) ?
					appointmentProvider.getAppointmentsByDoctorAndStatusIn(loginUser.getUserId(), targetStatuses, pageable)
						.map(AppointmentListResponse::fromEntity) :
					appointmentProvider.getAppointmentsByDoctorAndStatusInDesc(loginUser.getUserId(), targetStatuses, pageable)
						.map(AppointmentListResponse::fromEntity);
			case HOSPITAL_ADMIN:
				return (sortDirection == SortDirection.ASCENDING) ?
					appointmentProvider.getAppointmentsByHospitalAndStatusIn(loginUser.getUserId(), targetStatuses, pageable, date)
						.map(AppointmentListResponse::fromEntity) :
					appointmentProvider.getAppointmentsByHospitalAndStatusInDesc(loginUser.getUserId(), targetStatuses, pageable, date)
						.map(AppointmentListResponse::fromEntity);
			default:
				throw new RoleNotFoundException();
		}

		// 리뷰 여부 확인 로직은 동일하게 적용
		List<Long> appointmentIds = appointments.stream()
			.map(Appointment::getAppointmentId)
			.toList();

		Set<Long> reviewedAppointmentIds = reviewProvider.getReviewedAppointmentIds(appointmentIds);

		// 리뷰 여부 포함해서 DTO 변환
		return appointments.map(app ->
			AppointmentListResponse.fromEntity(
				app,
				reviewedAppointmentIds.contains(app.getAppointmentId())
			)
		);
	}

	@Override
	public List<TimeSlotDto> getAvailableTimeSlots(Long doctorId, Long patientId, LocalDate date) {
		Doctor doctor = doctorProvider.getDoctorById(doctorId);
		DoctorSchedule schedule = doctorScheduleProvider.getScheduleByDoctorAndDay(doctor, date.getDayOfWeek());
		Patient patient = patientProvider.getActivePatient(patientId);

		LocalDateTime baseDate = date.atStartOfDay();

		List<TimeSlotDto> slots = TimeSlotUtil.getTimeSlotsWithAvailability(
			schedule, doctor, baseDate, appointmentProvider
		);

		return slots.stream()
			.map(slot -> {
				// LocalDateTime으로 변환
				LocalDateTime slotStart = baseDate.with(slot.getStart());
				LocalDateTime slotEnd = baseDate.with(slot.getEnd());

				boolean overlaps =
					appointmentProvider.existsByPatientAndTimeRange(
						patientId,
						slotStart.minusMinutes(15),
						slotEnd.plusMinutes(15)
					);

				return new TimeSlotDto(slot.getStart(), slot.getEnd(), slot.isAvailable() && !overlaps);
			}).toList();
	}

	@Override
	public List<AppointmentDailyCountResponse> getDailyAppointmentCounts(LocalDate start, LocalDate end) {
		User loginUser = currentUserProvider.getUserFromUuid();
		Hospital hospital = hospitalService.getByUserId(loginUser.getUserId());
		return appointmentProvider.countAppointmentsByDateRange(hospital.getHospitalId(), start, end);
	}
}
