package com.ssginc8.docto.qna.repository;

import com.ssginc8.docto.notification.dto.QnaNotificationData;
import com.ssginc8.docto.qna.entity.QaComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CommentRepository extends JpaRepository<QaComment, Long> {

	List<QaComment> findByQnaPostIdQnaPostId(Long qnaPostId);

	@Query("""
		SELECT new com.ssginc8.docto.notification.dto.QnaNotificationData(
			u, h.name, pg.patient.user.name, a.appointmentTime
		)
		FROM QaComment qc
		JOIN qc.qnaPostId qp
		JOIN qp.appointment a
		JOIN a.hospital h
		JOIN a.patientGuardian pg
		JOIN pg.user u
		WHERE qc.qnaCommentId = :qnaCommentId
	""")
	QnaNotificationData findNotificationDataByQnaCommentId(@Param("qnaCommentId") Long qnaCommentId);
}
