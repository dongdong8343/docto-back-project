package com.ssginc8.docto.review.provider;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ssginc8.docto.global.error.exception.reviewException.ReviewNotFoundException;
import com.ssginc8.docto.review.entity.Review;
import com.ssginc8.docto.review.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewProvider {

	private final ReviewRepository reviewRepository;

	// 내가 쓴 리뷰 조회
	@Transactional(readOnly = true)
	public Page<Review> getMyReviews(Long userId, Pageable pageable) {
		return reviewRepository.findByUserUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, pageable);
	}


	// 병원 전체 리뷰 조회
	@Transactional(readOnly = true)
	public Page<Review> getHospitalReviews(Long hospitalId, Pageable pageable) {
		return reviewRepository.findByHospitalHospitalIdAndDeletedAtIsNullAndReportCountLessThan(
			hospitalId, 3, pageable
		);
	}

	// admin 전체 리뷰 조회
	@Transactional(readOnly = true)
	public Page<Review> getAllReviews(Pageable pageable) {
		return reviewRepository.findByDeletedAtIsNullAndReportCountLessThan(3, pageable);
	}



	//리뷰 단건 조회
	@Transactional(readOnly = true)
	public Review getById(Long reviewId) {
		return reviewRepository.findWithGraphByReviewId(reviewId)
			.orElseThrow(ReviewNotFoundException::new);
	}



	//리뷰 저장
	@Transactional
	public Review save(Review review) {
		return reviewRepository.save(review);
	}



	//키워드 삭제
	@Transactional
	public void deleteByReviewId(Long reviewId) {
		Review review = reviewRepository.findWithGraphByReviewId(reviewId)
			.orElseThrow(ReviewNotFoundException::new);
		review.getKeywords().clear();
	}

	public Review getReviewById(Long reviewId) {
		return reviewRepository.findById(reviewId)
			.orElseThrow(ReviewNotFoundException::new);
	}
  
  /**
	 * appointment Id로 review 가져오기
	 */
	public Set<Long> getReviewedAppointmentIds(List<Long> appointmentIds) {
		return reviewRepository.findAppointmentIdsWithReview(appointmentIds);
	}
}
