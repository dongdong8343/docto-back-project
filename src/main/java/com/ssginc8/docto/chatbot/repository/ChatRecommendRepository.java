package com.ssginc8.docto.chatbot.repository;

import com.ssginc8.docto.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRecommendRepository extends JpaRepository<Hospital, Long> {
	List<Hospital> findTop3ByNameContaining(String keyword);
}
