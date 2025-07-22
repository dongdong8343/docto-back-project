package com.ssginc8.docto.fcm.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssginc8.docto.fcm.entity.FcmToken;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

	Optional<FcmToken> findByToken(String token);

	@Query(value = """
		SELECT f
		FROM FcmToken f
		WHERE f.user.userId = :userId
		ORDER BY f.updatedAt DESC, f.fcmId DESC LIMIT 1
	""", nativeQuery = false)
	Optional<FcmToken> findLatestTokenByUserId(@Param("userId") Long userId);
}
