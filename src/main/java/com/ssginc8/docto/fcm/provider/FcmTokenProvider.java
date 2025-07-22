package com.ssginc8.docto.fcm.provider;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.ssginc8.docto.fcm.entity.FcmToken;
import com.ssginc8.docto.fcm.repository.FcmTokenRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FcmTokenProvider {

	private final FcmTokenRepository fcmTokenRepository;

	public Optional<FcmToken> findLatestTokenByUserId(Long userId) {
		return fcmTokenRepository.findLatestTokenByUserId(userId);
	}

	public Optional<FcmToken> findByToken(String token) {
		return fcmTokenRepository.findByToken(token);
	}

	public FcmToken save(FcmToken fcmToken) {
		return fcmTokenRepository.save(fcmToken);
	}

	public void delete(FcmToken fcmToken) {
		fcmTokenRepository.delete(fcmToken);
	}
}
