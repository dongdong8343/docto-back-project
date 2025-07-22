package com.ssginc8.docto.chatbot.provider;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ssginc8.docto.chatbot.repository.ChatRecommendRepository;
import com.ssginc8.docto.hospital.entity.Hospital;
import com.ssginc8.docto.chatbot.dto.HospitalSummary;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatbotRecommendProvider {

	private final ChatRecommendRepository chatRecommendRepository;

	@Transactional(readOnly = true)
	public List<HospitalSummary> findHospitalsByKeyword(String keyword) {
		List<Hospital> hospitals = chatRecommendRepository.findTop3ByNameContaining(keyword);

		return hospitals.stream()
			.map(HospitalSummary::from)
			.toList();
	}
}
