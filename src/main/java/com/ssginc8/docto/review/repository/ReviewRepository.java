package com.ssginc8.docto.review.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ssginc8.docto.review.entity.Review;



@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

	Page<Review> findByHospitalHospitalIdAndDeletedAtIsNullAndReportCountLessThan(Long hospitalId, int maxCount, Pageable pageable);

	Page<Review> findByDeletedAtIsNullAndReportCountLessThan(int maxCount, Pageable pageable);



	@EntityGraph(attributePaths = {
		"user",
		"hospital",
		"doctor",
		"appointment",
		"keywords"
	})
	Optional<Review> findWithGraphByReviewId(Long reviewId);

	Page<Review> findByUserUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
		Long userId,
		Pageable pageable
	);

  	/**
	 * appointment Id로 review 가져오기
	 */
	@Query("""
		SELECT r.appointment.appointmentId
		FROM Review r
		WHERE r.appointment.appointmentId IN :appointmentIds
	""")
	Set<Long> findAppointmentIdsWithReview(@Param("appointmentIds") List<Long> appointmentIds);
}

