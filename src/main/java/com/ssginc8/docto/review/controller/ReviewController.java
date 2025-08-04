package com.ssginc8.docto.review.controller;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.ssginc8.docto.auth.provider.CurrentUserProvider;
import com.ssginc8.docto.review.dto.ReviewAllListResponse;
import com.ssginc8.docto.review.dto.ReviewCreateRequest;
import com.ssginc8.docto.review.dto.ReviewMyListResponse;
import com.ssginc8.docto.review.dto.ReviewResponse;
import com.ssginc8.docto.review.dto.ReviewUpdateRequest;
import com.ssginc8.docto.review.service.ReviewService;
import com.ssginc8.docto.user.entity.User;

import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ReviewController {

	private final CurrentUserProvider currentUserProvider;
	 private final ReviewService reviewService;


	// 리뷰 생성
	@PostMapping("/reviews")
	public ResponseEntity<Long> create(@RequestBody ReviewCreateRequest dto) {
		Long id = reviewService.createReview(dto, dto.getUserId());
		//리뷰 아이디 반환
		return ResponseEntity.ok(id);
	}


	// 리뷰 수정
	@PatchMapping("/reviews/{reviewId}")
	public ResponseEntity<ReviewResponse> update(
		@PathVariable Long reviewId,
		@RequestBody ReviewUpdateRequest dto) {
		ReviewResponse updated = reviewService.updateReview(dto, reviewId);
		return ResponseEntity.ok(updated);
	}


	// 리뷰 삭제
	@DeleteMapping("/reviews/{reviewId}")
	public ResponseEntity<Void> delete(@PathVariable Long reviewId) {
		reviewService.deleteReview(reviewId);
		return ResponseEntity.noContent().build();
	}


	// 나의 리뷰 조회
	@GetMapping("/users/me/reviews")
	public ResponseEntity<Page<ReviewMyListResponse>> getMyReviews(
		Pageable pageable
	) {// 서비스에 바로 UUID 전달해서 User 조회
		User me = currentUserProvider.getUserFromUserId();

		// 조회된 User의 userId로 조회
		Page<ReviewMyListResponse> page = reviewService.getMyReviews(me.getUserId(), pageable);
		return ResponseEntity.ok(page);
	}



	// admin 전체 리뷰 조회
	@GetMapping("/admin/reviews")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Page<ReviewAllListResponse>> getAllReviews(Pageable pageable) {
		Page<ReviewAllListResponse> page = reviewService.getAllReviews(pageable);
		return ResponseEntity.ok(page);
	}


	// 신고 엔드포인트
	@PostMapping("/reviews/{reviewId}/report")
	public ResponseEntity<Void> report(
		@PathVariable Long reviewId,
		@RequestParam String reason) {
		reviewService.reportReview(reviewId, reason);
		return ResponseEntity.ok().build();
	}


}







