package com.ssginc8.docto.cs.controller;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import com.ssginc8.docto.auth.provider.CurrentUserProvider;
import com.ssginc8.docto.cs.dto.CsMessageRequest;
import com.ssginc8.docto.cs.service.CsService;
import com.ssginc8.docto.user.entity.User;
import com.ssginc8.docto.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequiredArgsConstructor
@Log4j2
public class WebSocketChatController {

	private final CurrentUserProvider currentUserProvider;
	private final CsService csService;

	/**
	 * 클라이언트가 /app/chat.sendMessage 로 메세지를 보낼 때
	 */
	@MessageMapping("/chat.sendMessage")
	public void sendMessage(@Payload CsMessageRequest chatMessage) {
		// 1. JWT 토큰에서 현재 로그인한 사용자의 정보를 가져옴
		User user = currentUserProvider.getUserFromUuid();

		// 2. CsMessageRequest에 csRoomId가 포함되어 있는지 확인
		if (chatMessage.getCsRoomId() == null) {
			log.error("CsRoomId is null in CsMessageRequest for user {}", user.getUserId());
			return;
		}

		csService.createMessage(chatMessage.getCsRoomId(), user.getUserId(), chatMessage.getContent());

		log.info("Message received for csRoomId: {}, userId: {}, content: {}",
			chatMessage.getCsRoomId(), user.getUserId(), chatMessage.getContent());
	}

	/**
	 * WebSocket 클라이언트 연결 시 발생하는 이벤트 처리
	 */
	@EventListener
	public void handleWebSocketConnectListener(SessionConnectedEvent event) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
		log.info("WebSocket Session 연결됨: sessionId={}, user={}",
			headerAccessor.getSessionId(), headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous");

		if (headerAccessor.getUser() != null && headerAccessor.getUser().getName() != null) {
			headerAccessor.getSessionAttributes().put("principalName", headerAccessor.getUser().getName());
			log.info("Stored principalName {} in WebSocket session attributes.", headerAccessor.getUser().getName());
		}
	}
}
