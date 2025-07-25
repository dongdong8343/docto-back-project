package com.ssginc8.docto.guardian.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssginc8.docto.file.provider.FileProvider;
import com.ssginc8.docto.global.event.EmailSendEvent;
import com.ssginc8.docto.global.error.exception.guardianException.InvalidGuardianStatusException;
import com.ssginc8.docto.global.error.exception.guardianException.InvalidInviteCodeException;
import com.ssginc8.docto.global.properties.ImageDefaultProperties;
import com.ssginc8.docto.guardian.dto.GuardianInviteResponse;
import com.ssginc8.docto.guardian.dto.GuardianResponse;
import com.ssginc8.docto.guardian.dto.PatientSummaryResponse;
import com.ssginc8.docto.guardian.dto.PendingInviteResponse;
import com.ssginc8.docto.guardian.entity.PatientGuardian;
import com.ssginc8.docto.guardian.entity.Status;
import com.ssginc8.docto.guardian.provider.PatientGuardianProvider;
import com.ssginc8.docto.patient.entity.Patient;
import com.ssginc8.docto.patient.provider.PatientProvider;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.provider.UserProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientGuardianServiceImpl implements PatientGuardianService {

	private final PatientGuardianProvider patientGuardianProvider;
	private final PatientProvider patientProvider;
	private final UserProvider userProvider;
	private final ApplicationEventPublisher eventPublisher;
	private final FileProvider fileProvider;
	private final ImageDefaultProperties imageDefaultProperties;

	@Override
	public GuardianInviteResponse inviteGuardian(Long patientId, String guardianEmail) {
		// 1) 환자·보호자 로드
		Patient patient = patientProvider.getActivePatient(patientId);
		User guardian = userProvider.loadUserByEmailOrException(guardianEmail);

		// 2) 기존 PENDING 매핑 중 가장 최근 것 조회
		PatientGuardian pg = patientGuardianProvider
			.findLatestPendingMapping(guardian, patient);

		// 3) 항상 새 inviteCode 생성
		String inviteCode = generateInviteCode(patientId, guardian.getUserId());

		if (pg != null) {
			// 재초대: 기존 엔티티에만 코드 덮어쓰기
			pg.updateInviteCode(inviteCode);
		} else {
			// 신규 초대: 엔티티 생성 후 코드 설정
			pg = PatientGuardian.create(guardian, patient, LocalDateTime.now());
			pg.updateInviteCode(inviteCode);
		}

		// 4) **명시적으로 저장**해서 JPA가 변경을 놓치지 않도록 함
		patientGuardianProvider.save(pg);

		// 5) 이메일 발송 이벤트 — 이 이벤트 구독자에서 실제 메일을 보냄
		eventPublisher.publishEvent(
			EmailSendEvent.guardianInvite(guardianEmail, inviteCode)
		);

		// (기존 GuardianInviteEvent도 필요하다면 여기에 추가 발행)

		return new GuardianInviteResponse(inviteCode);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PendingInviteResponse> getPendingInvites(Long patientId) {
		return patientGuardianProvider.getPendingInvitesByPatientId(patientId)
			.stream()
			.map(pg -> new PendingInviteResponse(
				pg.getPatientGuardianId(),
				pg.getUser().getName(),
				pg.getUser().getEmail(),
				pg.getInviteCode()
			))
			.collect(Collectors.toList());
	}

	@Override
	public void updateStatusByInviteCode(String inviteCode, String statusStr) {
		// Provider.findByInviteCode(inviteCode)가 엔티티 또는 예외를 던집니다.
		PatientGuardian pg = patientGuardianProvider.findByInviteCode(inviteCode);
		pg.updateStatus(parseStatus(statusStr));
	}

	@Override
	public void updateStatus(Long requestId, String inviteCode, String statusStr) {
		PatientGuardian pg = patientGuardianProvider.getById(requestId);
		if (!pg.getInviteCode().equals(inviteCode)) {
			throw new InvalidInviteCodeException();
		}
		pg.updateStatus(parseStatus(statusStr));
	}

	@Override
	public void deleteMapping(Long guardianId, Long patientId) {
		patientGuardianProvider.deleteMapping(guardianId, patientId);
	}

	@Override
	public void deleteMappingByMappingId(Long mappingId) {
		PatientGuardian pg = patientGuardianProvider.getById(mappingId);
		pg.delete(); // BaseTimeEntity.soft-delete
	}

	@Override
	@Transactional(readOnly = true)
	public List<PatientSummaryResponse> getAllAcceptedMappings(Long guardianId) {
		return patientGuardianProvider.getAllAcceptedMappings(guardianId)
			.stream()
			.map(pg -> {
				Patient patient = pg.getPatient();
				User user = patient.getUser();
				PatientSummaryResponse dto = PatientSummaryResponse.of(
					patient.getPatientId(),
					user.getName(),
					decryptRRN(patient.getResidentRegistrationNumber()),
					user.getAddress()
				);
				Long fileId = user.getProfileImage() != null
					? user.getProfileImage().getFileId()
					: null;
				String url = fileProvider.getFileUrlById(fileId);
				dto.setProfileImageUrl(
					(url != null && !url.isBlank()) ? url : imageDefaultProperties.getAddress());
				return dto;
			})
			.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<GuardianResponse> getGuardiansByPatientId(Long patientId) {
		return patientGuardianProvider.getAllAcceptedGuardiansByPatientId(patientId)
			.stream()
			.map(pg -> GuardianResponse.from(pg.getPatientGuardianId(), pg.getUser().getName()))
			.collect(Collectors.toList());
	}

	// ────────────────────────────────────────────────────────────────────────────
	// Utility
	// ────────────────────────────────────────────────────────────────────────────

	private Status parseStatus(String statusStr) {
		try {
			return Status.valueOf(statusStr);
		} catch (IllegalArgumentException e) {
			throw new InvalidGuardianStatusException();
		}
	}

	// 변경 후: 해시 입력에만 타임스탬프를 추가하고, 코드에는 포함시키지 않습니다.
	private String generateInviteCode(Long patientId, Long userId) {
		// 1) 코드의 고정 부분
		String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String prefix = String.format("inv-%d-%d-%s", patientId, userId, date);

		// 2) 해시 입력에는 현재 밀리초 타임스탬프를 추가
		String raw = prefix + "-" + System.currentTimeMillis();

		// 3) 최종 해시(6자) 생성
		String hash = sha256(raw).substring(0, 6);

		// 4) 반환: prefix + "-" + 해시
		return prefix + "-" + hash;
	}

	private String sha256(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] h = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : h)
				sb.append(String.format("%02x", b));
			return sb.toString();
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", ex);
		}
	}

	private String decryptRRN(String encrypted) {
		return com.ssginc8.docto.global.util.AESUtil.decrypt(encrypted);
	}
}
