package com.ssginc8.docto.cs.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssginc8.docto.cs.entity.CsMessage;

public interface CsMessageRepository extends JpaRepository<CsMessage, Long> {

	@Query("SELECT m FROM CsMessage m "
		+ "WHERE m.csRoom.csRoomId = :csRoomId "
		+ "AND m.createdAt < :before "
		+ "AND m.csRoom.deletedAt IS NULL "
		+ "ORDER BY m.createdAt DESC")  // 오래된 순 → 이후 reverse
	List<CsMessage> findCsMessageBefore(
		@Param("csRoomId") Long csRoomId,
		@Param("before") LocalDateTime before,
		Pageable pageable
	);
}
