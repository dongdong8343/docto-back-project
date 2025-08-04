package com.ssginc8.docto.qna.controller;

import com.ssginc8.docto.auth.provider.CurrentUserProvider;
import com.ssginc8.docto.qna.dto.QaPostCreateRequest;
import com.ssginc8.docto.qna.dto.QaPostResponse;
import com.ssginc8.docto.qna.dto.QaPostUpdateRequest;
import com.ssginc8.docto.qna.dto.UpdateStatusRequest;
import com.ssginc8.docto.qna.entity.QaStatus;
import com.ssginc8.docto.qna.service.QaPostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/qnas")
@RequiredArgsConstructor
public class QnAController {

	private final CurrentUserProvider currentUserProvider;
	private final QaPostService qaPostService;

	// 게시글 생성
	@PostMapping
	public ResponseEntity<QaPostResponse> createQaPost(
		@RequestBody @Valid QaPostCreateRequest request
	) {
		QaPostResponse response = qaPostService.createQaPost(request);
		return ResponseEntity.ok(response);
	}


	// 게시글 수정
	@PatchMapping("/{qnaId}")
	public ResponseEntity<QaPostResponse> updateQaPost(
		@PathVariable Long qnaId,
		@RequestBody @Valid QaPostUpdateRequest request
	) {QaPostResponse response = qaPostService.updateQaPost(qnaId, request.getContent());
		return ResponseEntity.ok(response);
	}

	// 게시글 삭제
	@DeleteMapping("/{qnaId}")
	public ResponseEntity<Void> deleteQaPost(@PathVariable Long qnaId) {
		qaPostService.deleteQaPost(qnaId);
		return ResponseEntity.noContent().build();
	}

	// 게시글 상세 조회
	@GetMapping("/{qnaId}")
	public ResponseEntity<QaPostResponse> getQaPost(@PathVariable Long qnaId) {
		QaPostResponse response = qaPostService.getQaPost(qnaId);
		return ResponseEntity.ok(response);
	}

	// 예약별 Q&A 조회 (질문 없음 → 204 No Content)
	@GetMapping("/appointment/{appointmentId}")
	public ResponseEntity<QaPostResponse> getByAppointment(
		@PathVariable Long appointmentId
	) {
		QaPostResponse dto = qaPostService.getByAppointment(appointmentId);
		if (dto == null) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(dto);
	}

	// 내가 작성한 Q&A 리스트 (페이징)
	@GetMapping("/user/{userId}/posts")
	public ResponseEntity<Page<QaPostResponse>> getMyQnAs(
		@PathVariable Long userId,
		Pageable pageable
	) {
		Page<QaPostResponse> page = qaPostService.getMyPosts(userId, pageable);
		return ResponseEntity.ok(page);
	}


	@GetMapping("/doctor/posts")
	public ResponseEntity<Page<QaPostResponse>> getDoctorPostsByStatus(
		@RequestParam(defaultValue = "PENDING") QaStatus status,
		Pageable pageable
	) {
		// 현재 로그인한 의사의 uuid 가져오기
		String doctorUuid = currentUserProvider.getUserFromUserId().getUuid();

		Page<QaPostResponse> page = qaPostService.getDoctorPostsByDoctorIdAndStatus(doctorUuid, status, pageable);

		return ResponseEntity.ok(page);
	}



	@PatchMapping("/{qnaId}/status")
	@PreAuthorize("hasRole('DOCTOR')")
	public ResponseEntity<QaPostResponse> updateQaPostStatus(
		@PathVariable Long qnaId,
		@RequestBody @Valid UpdateStatusRequest request
	) {
		QaPostResponse updated = qaPostService.updateQaPostStatus(qnaId, request.getStatus());
		return ResponseEntity.ok(updated);
	}
}

