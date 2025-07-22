package com.ssginc8.docto.hospital.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ssginc8.docto.doctor.entity.Doctor;

import com.ssginc8.docto.doctor.provider.DoctorProvider;
import com.ssginc8.docto.doctor.provider.DoctorScheduleProvider;

import com.ssginc8.docto.file.entity.Category;
import com.ssginc8.docto.file.entity.File;
import com.ssginc8.docto.file.provider.FileProvider;
import com.ssginc8.docto.file.service.FileService;
import com.ssginc8.docto.file.service.dto.UploadFile;
import com.ssginc8.docto.hospital.dto.HospitalRequest;
import com.ssginc8.docto.hospital.dto.HospitalResponse;
import com.ssginc8.docto.hospital.dto.HospitalReviewResponse;
import com.ssginc8.docto.hospital.dto.HospitalScheduleRequest;
import com.ssginc8.docto.hospital.dto.HospitalScheduleResponse;
import com.ssginc8.docto.hospital.dto.HospitalUpdate;
import com.ssginc8.docto.hospital.dto.HospitalWaitingRequest;
import com.ssginc8.docto.hospital.entity.Hospital;
import com.ssginc8.docto.hospital.entity.HospitalSchedule;
import com.ssginc8.docto.hospital.entity.ProvidedService;
import com.ssginc8.docto.hospital.provider.HospitalProvider;
import com.ssginc8.docto.review.provider.ReviewProvider;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor

@lombok.extern.log4j.Log4j2
public class HospitalServiceImpl implements HospitalService {

	private final HospitalProvider hospitalProvider;
	private final ReviewProvider reviewProvider;
	private final DoctorProvider doctorProvider;
	private final DoctorScheduleProvider doctorScheduleProvider;
	private final UserRepository userRepository;
	private final FileProvider fileProvider;
	private final FileService fileService;


	/**
	 *  로그인 사용자의 병원 정보 얻기
	 */
	@Override
	public HospitalResponse getHospitalByAdminId(Long userId) {
		// 1) 관리자(userId) 로 조회한 병원 엔티티
		Hospital hospital = hospitalProvider.findByUserUserId(userId);

		// 2) DB 에서 가져온 fileIds 를 mutable 리스트로 복사
		List<Long> fileIds = hospital.getFileIds() != null
			? new ArrayList<>(hospital.getFileIds())
			: new ArrayList<>();

		// 3) 실제 존재하는 파일만 한 번에 조회
		List<File> files = fileProvider.findAllById(fileIds);

		// 4) URL 매핑
		List<String> imageUrls = files.stream()
			.map(File::getUrl)
			.collect(Collectors.toList());

		// 5) 서비스 이름 조회
		List<String> serviceNames =
			hospitalProvider.findServiceNamesByHospitalId(hospital.getHospitalId());

		// 6) DTO 로 변환할 때, fileIds 도 함께 담아 줍니다
		return HospitalResponse.builder()
			.hospitalId(      hospital.getHospitalId())
			.name(            hospital.getName())
			.address(         hospital.getAddress())
			.phone(           hospital.getPhone())
			.introduction(    hospital.getIntroduction())
			.notice(          hospital.getNotice())
			.latitude(        hospital.getLatitude())
			.longitude(       hospital.getLongitude())
			.businessRegistrationNumber(hospital.getBusinessRegistrationNumber())
			// **기존에 저장된 파일 ID 리스트**
			.fileIds(         fileIds)
			// **실제 존재하는 파일만 매핑된 URL 리스트**
			.imageUrls(       imageUrls)
			.serviceNames(    serviceNames)
			.build();
	}

	@Override
	public Long getHospitalIdByAdminId(Long userId) {
		Hospital hospital = hospitalProvider.findByUserUserId(userId);
		return hospital.getHospitalId();
	}

	@Override
	public Hospital getByUserId(Long userId) {
		return hospitalProvider.findByUserUserId(userId);
	}

	/**
	 *  병원 검색
	 */
	@Override
	public Page<HospitalResponse> searchHospitals(
		String query,
		String sortBy,
		Double latitude,
		Double longitude,
		Double radius,
		Pageable pageable
	) {
		// 1) 이름 검색 Specification (글로벌/이름순/리뷰순에도 적용)
		Specification<Hospital> spec = Specification.where(null);
		if (StringUtils.hasText(query)) {
			spec = spec.and((root, cq, cb) ->
				cb.like(cb.lower(root.get("name")), "%" + query.toLowerCase() + "%")
			);
		}

		// 2) 거리순 분기: latitude, longitude, radius 모두 필요
		if ("DISTANCE".equalsIgnoreCase(sortBy)
			&& latitude != null && longitude != null && radius != null
		) {
			return hospitalProvider
				.findAllNearby(
					query,
					latitude,
					longitude,
					radius,
					PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
				)
				.map(HospitalResponse::from);
		}

		// 3) 글로벌(이름순) 또는 리뷰 많은 순
		Sort sort = Sort.by("name");  // 기본 이름순
		if ("REVIEW_COUNT".equalsIgnoreCase(sortBy)) {
			sort = Sort.by(Sort.Direction.DESC, "reviewCount");
		}

		Page<Hospital> page = hospitalProvider.findAll(
			spec,
			PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort)
		);
		return page.map(HospitalResponse::from);
	}

	/**
	 *  위치기반 병원 리스트 조회
	 */
	@Override
	public Page<HospitalResponse> getHospitalsWithinRadius(
		double lat, double lng, double radius, String query, Pageable pageable) {

		Page<Hospital> hospitals = hospitalProvider.findHospitalsWithinRadius(
			lat, lng, radius, query, pageable
		);
		return hospitals.map(hospital -> {
			String imageUrl = hospitalProvider.getImageUrl(hospital.getHospitalId());
			List<String> serviceNames = hospitalProvider.getServiceNames(hospital.getHospitalId());
			return HospitalResponse.from(hospital, Collections.singletonList(imageUrl), serviceNames);
		});
	}




	/**
	 * 병원아이디 상세 조회
	 *
	 */
	@Override
	public HospitalResponse getHospitalId(Long hospitalId) {

		Hospital hospital = hospitalProvider.getHospitalById(hospitalId);
		List<ProvidedService> services = hospitalProvider.findServicesByHospitalId(hospitalId);

		List<String> serviceNames = (services == null || services.isEmpty())
			? Collections.emptyList()
			: services.stream()
			.map(ProvidedService::getServiceName)
			.collect(Collectors.toList());

		List<String> imageUrls = fileProvider.getFileUrlsByIds(hospital.getFileIds()); // ✅ 이미지 URL 가져오기\
		List<Long>   fileIds   = hospital.getFileIds();
		log.info("병원 fileId = {}", hospital.getFileIds());
		return HospitalResponse.builder()
			.hospitalId(hospital.getHospitalId())
			.name(hospital.getName())
			.address(hospital.getAddress())
			.latitude(hospital.getLatitude())
			.longitude(hospital.getLongitude())
			.phone(hospital.getPhone())
			.introduction(hospital.getIntroduction())
			.notice(hospital.getNotice())
			.waiting(hospital.getWaiting())
			.serviceNames(serviceNames)
			.imageUrls(imageUrls)
			.fileIds(fileIds)
			.build();

	}



	/**
	 * 병원 정보 등록
	 *
	 */

	@Override
	public Long saveHospital(Long userId, HospitalRequest dto) {
		User user = hospitalProvider.getUserById(userId);

		// 1) Hospital 생성: fileIds는 mutable ArrayList 로 초기화
		Hospital hospital = Hospital.create(
			user,
			dto.getName(),
			dto.getAddress(),
			dto.getPhone(),
			dto.getIntroduction(),
			dto.getBusinessRegistrationNumber(),
			dto.getLatitude(),
			dto.getLongitude(),
			dto.getNotice(),
			new ArrayList<>()
		);

		// 2) 파일 업로드 & ID 리스트에 추가
		List<Long> fileIds = new ArrayList<>();
		if (dto.getFiles() != null) {
			for (MultipartFile mf : dto.getFiles()) {
				if (mf.isEmpty()) continue;
				UploadFile.Result res = fileService.uploadImage(
					UploadFile.Command.builder()
						.file(mf)
						.category(Category.HOSPITAL)
						.build()
				);
				File fileEntity = File.createFile(
					res.getCategory(),
					res.getFileName(),
					res.getOriginalFileName(),
					res.getUrl(),
					res.getBucket(),
					res.getFileSize(),
					res.getFileType()
				);
				File saved = fileProvider.saveFile(fileEntity);
				fileIds.add(saved.getFileId());
			}
		}
		hospital.getFileIds().addAll(fileIds);

		// 3) hospital 저장
		hospitalProvider.saveHospital(hospital);

		// 4) 서비스 등록
		if (dto.getServiceName() != null && !dto.getServiceName().isEmpty()) {
			List<ProvidedService> services = dto.getServiceName().stream()
				.filter(s -> !s.isBlank())
				// Collectors.toCollection으로도 ArrayList 생성이 가능합니다.
				.map(s -> ProvidedService.create(s, hospital))
				.collect(Collectors.toList());
			hospitalProvider.saveAll(services);
		}

		return hospital.getHospitalId();
	}


	@Override
	public Long updateHospital(Long hospitalId, HospitalUpdate dto) {
		Hospital hospital = hospitalProvider.getHospitalById(hospitalId);

		// 1) 기본 정보 업데이트
		hospital.updateFromValues(
			dto.getName(),
			dto.getPhone(),
			dto.getAddress(),
			dto.getIntroduction(),
			dto.getNotice()
		);

		// 2) 삭제할 파일 처리
		if (dto.getDeletedFileIds() != null) {
			for (Long fileIdToDelete : dto.getDeletedFileIds()) {
				File file = fileProvider.getFileById(fileIdToDelete);
				fileService.deleteFile(file.getFileName());
				fileProvider.deleteFileById(fileIdToDelete);
			}
		}

		// 3) 신규 파일 업로드 및 ID 수집
		List<Long> newFileIds = new ArrayList<>();
		if (dto.getExistingFileIds() != null) {
			newFileIds.addAll(dto.getExistingFileIds());
		}
		if (dto.getFiles() != null) {
			for (MultipartFile mf : dto.getFiles()) {
				if (mf.isEmpty()) continue;
				UploadFile.Result res = fileService.uploadImage(
					UploadFile.Command.builder()
						.file(mf)
						.category(Category.HOSPITAL)
						.build()
				);
				File f = File.createFile(
					res.getCategory(),
					res.getFileName(),
					res.getOriginalFileName(),
					res.getUrl(),
					res.getBucket(),
					res.getFileSize(),
					res.getFileType()
				);
				File saved = fileProvider.saveFile(f);
				newFileIds.add(saved.getFileId());
			}
		}

		// 4) fileIds 컬렉션을 교체
		hospital.getFileIds().clear();
		hospital.getFileIds().addAll(newFileIds);

		// 5) 서비스 목록 — diff 로직 적용
		//    (기존에 남길 것만 유지, 새로 추가된 것만 insert, 빠진 것만 delete)
		List<ProvidedService> oldServices = hospitalProvider.findServicesByHospitalId(hospitalId);
		Set<String> newNames = dto.getServiceNames() == null
			? Collections.emptySet()
			: dto.getServiceNames().stream()
			.filter(s -> !s.isBlank())
			.collect(Collectors.toSet());

		// 삭제할 서비스
		oldServices.stream()
			.filter(sv -> !newNames.contains(sv.getName()))
			.forEach(sv -> hospitalProvider.deleteServiceById(sv.getId()));

		// 추가할 서비스
		newNames.stream()
			.filter(name -> oldServices.stream().noneMatch(sv -> sv.getName().equals(name)))
			.map(name -> ProvidedService.create(name, hospital))
			.forEach(hospitalProvider::saveService);

		return hospitalId;
	}
	/**
	 * 병원 삭제
	 *
	 */
	@Override
	@Transactional
	public void deleteHospital(Long hospitalId) {
		// 1. 병원 조회
		Hospital hospital = hospitalProvider.getHospitalById(hospitalId);

		// 2. 병원이 가진 모든 의사 조회
		List<Doctor> doctors = doctorProvider.findByHospital(hospital);

		// 3. 각 의사의 스케줄 삭제
		for (Doctor doctor : doctors) {
			doctorScheduleProvider.deleteByDoctorId(doctor.getDoctorId());
		}

		// 4. 의사 삭제
		for (Doctor doctor : doctors) {
			doctor.delete();
		}

		// 5. 병원의 서비스 삭제
		hospitalProvider.deleteProvidedServicesByHospital(hospital);

		// 6. 병원 스케줄 삭제
		hospitalProvider.deleteByHospitalId(hospitalId);

		// 7. 병원 삭제
		hospital.delete();
	}






	/**
	 * 병원 전체 리스트(어드민)
	 *
	 */
	@Override
	public Page<HospitalResponse> getHospitals(Pageable pageable) {
		Page<Hospital> hospitals = hospitalProvider.findAll(pageable);

		return hospitals.map(hospital -> {
			List<String> serviceNames = hospitalProvider.findServicesByHospitalId(hospital.getHospitalId())
				.stream()
				.map(ProvidedService::getServiceName)
				.collect(Collectors.toList());

			return HospitalResponse.builder()
				.hospitalId(hospital.getHospitalId())
				.name(hospital.getName())
				.address(hospital.getAddress())
				.notice(hospital.getNotice())
				.introduction(hospital.getIntroduction())
				.latitude(hospital.getLatitude())
				.longitude(hospital.getLongitude())
				.phone(hospital.getPhone())
				.waiting(hospital.getWaiting())
				.serviceNames(serviceNames)
				.build();
		});
	}




	/**
	 * 병원 영업시간 등록
	 *
	 */
	@Override
	public void saveSchedules(Long hospitalId, List<HospitalScheduleRequest> schedules) {
		Hospital hospital = hospitalProvider.getHospitalById(hospitalId);

		List<HospitalScheduleRequest> savedDto = new ArrayList<>();

		for (HospitalScheduleRequest scheduleDTO : schedules) {

			hospitalProvider.validateHospitalScheduleFields(scheduleDTO);

			HospitalSchedule schedule = HospitalSchedule.create(
				hospital,
				scheduleDTO.getDayOfWeek(),
				scheduleDTO.getOpenTime(),
				scheduleDTO.getCloseTime(),
				scheduleDTO.getLunchStart(),
				scheduleDTO.getLunchEnd()
			);

			HospitalSchedule saved = hospitalProvider.saveHospitalSchedule(schedule);

			savedDto.add(new HospitalScheduleRequest(saved));
		}


	}



	/**
	 * 병원 영업시간 조회
	 */

@Override
public List<HospitalScheduleResponse> getSchedules(Long hospitalId) {

	hospitalProvider.getHospitalById(hospitalId); // 존재 여부 체크

	return hospitalProvider.findSchedulesByHospitalId(hospitalId).stream()
		.map(schedule -> new HospitalScheduleResponse(
			schedule.getHospitalScheduleId(),
			schedule.getDayOfWeek(),
			schedule.getOpenTime(),
			schedule.getCloseTime(),
			schedule.getLunchStart(),
			schedule.getLunchEnd()
		))
		.collect(Collectors.toList());
}




	@Override
	@Transactional
	public void updateHospitalSchedule(Long hospitalId, List<HospitalScheduleRequest> scheduleDtos) {
		// 1. 병원 조회
		Hospital hospital = hospitalProvider.getHospitalById(hospitalId);

		for (HospitalScheduleRequest dto : scheduleDtos) {
			if (dto.getHospitalScheduleId() != null) {
				// 2.1 기존 스케줄 업데이트
				HospitalSchedule schedule = hospitalProvider.getScheduleByIdOrThrow(dto.getHospitalScheduleId());
				hospitalProvider.validateScheduleBelongsToHospital(schedule, hospital);

				schedule.updateSchedule(
					dto.getDayOfWeek(),
					dto.getOpenTime(),
					dto.getCloseTime(),
					dto.getLunchStart(),
					dto.getLunchEnd()
				);
			} else {
				// 2.2 신규 스케줄 생성
				HospitalSchedule newSchedule = HospitalSchedule.create(
					hospital,
					dto.getDayOfWeek(),
					dto.getOpenTime(),
					dto.getCloseTime(),
					dto.getLunchStart(),
					dto.getLunchEnd()
				);
				hospitalProvider.saveHospitalSchedule(newSchedule);
			}
		}
	}
	/**
	 * 병원 영업시간 삭제
	 *
	 */
	@Override
	public void deleteHospitalSchedules(Long hospitalScheduleId) {

		HospitalSchedule schedule = hospitalProvider.getScheduleByIdOrThrow(hospitalScheduleId); // 없으면 예외 던짐
		hospitalProvider.deleteByHospitalScheduleId(schedule.getHospitalScheduleId());
	}

	/**
	 * 병원 웨이팅 등록
	 *
	 */
	@Override
	public Long saveHospitalWaiting(Long hospitalId, HospitalWaitingRequest hospitalWaitingRequest) {

		Hospital hospital = hospitalProvider.getHospitalById(hospitalId);

		hospital.updateWaiting(hospitalWaitingRequest.getWaiting());

		hospitalProvider.saveHospital(hospital);


		return hospital.getHospitalId();
	}
	/**
	 * 병원 웨이팅 조회
	 *
	 */
	@Override

	public Long getHospitalWaiting(Long hospitalId) {

		Hospital hospital = hospitalProvider.getHospitalById(hospitalId);

		return hospital.getWaiting();
	}

	/**
	 * 병원 웨이팅 수정
	 *
	 */
	@Override
	@Transactional
	public void updateHospitalWaiting(Long hospitalId, HospitalWaitingRequest hospitalWaiting) {

		Hospital hospital = hospitalProvider.getHospitalById(hospitalId);

		hospital.updateWaiting(hospitalWaiting.getWaiting());


	}



	// 병원별 전체 리뷰 조회
	@Override
	@Transactional(readOnly = true)
	public Page<HospitalReviewResponse> getReviews(Long hospitalId, Pageable pageable) {

		hospitalProvider.getHospitalById(hospitalId);

		return reviewProvider.getHospitalReviews(hospitalId, pageable)
			.map(HospitalReviewResponse::fromEntity);

	}

}


