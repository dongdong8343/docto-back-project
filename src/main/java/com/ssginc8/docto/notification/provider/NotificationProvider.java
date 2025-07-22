package com.ssginc8.docto.notification.provider;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ssginc8.docto.global.error.exception.notificationException.NotificationNotFound;
import com.ssginc8.docto.notification.entity.Notification;
import com.ssginc8.docto.notification.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationProvider {

	private final NotificationRepository notificationRepository;

	@Transactional
	public Notification save(Notification notification) {
		return notificationRepository.save(notification);
	}

	// 특정 알림 조회
	@Transactional(readOnly = true)
	public Notification findById(Long notificationId) {
		return notificationRepository.findById(notificationId)
			.orElseThrow(NotificationNotFound::new);
	}

	// 알림 목록 조회
	@Transactional(readOnly = true)
	public List<Notification> getUserNotifications(Long userId) {
		return notificationRepository.findAllByReceiver_UserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
	}
}
