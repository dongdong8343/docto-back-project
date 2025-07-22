package com.ssginc8.docto.qna.provider;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ssginc8.docto.global.error.exception.commentException.CommentNotFoundException;
import com.ssginc8.docto.notification.dto.QnaNotificationData;
import com.ssginc8.docto.qna.entity.QaComment;
import com.ssginc8.docto.qna.repository.CommentRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CommentProvider {

	private final CommentRepository commentRepository;


	// Id로 댓글 조회
	@Transactional(readOnly = true)
	public QaComment getById(Long commentId) {
		return commentRepository.findById(commentId)
			.orElseThrow((CommentNotFoundException::new)
			);
	}


	// 게시글에 달린 댓글 조회
	@Transactional(readOnly = true)
	public List<QaComment> findByPostId(Long qnaPostId) {
		return commentRepository.findByQnaPostIdQnaPostId(qnaPostId);
	}


	// 댓글 저장
	@Transactional
	public QaComment save(QaComment comment) {
		return commentRepository.save(comment);
	}

	// 댓글 삭제
	@Transactional
	public void deleteById(Long commentId) {
		commentRepository.deleteById(commentId);
	}

	// ID로 Notification 에 필요한 데이터 찾기
	@Transactional(readOnly = true)
	public QnaNotificationData findNotificationDataByQnaCommentId(Long qnaCommentId) {
		return commentRepository.findNotificationDataByQnaCommentId(qnaCommentId);
	}
}
