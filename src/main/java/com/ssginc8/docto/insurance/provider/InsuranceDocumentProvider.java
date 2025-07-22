// src/main/java/com/ssginc8/docto/insurance/provider/InsuranceDocumentProvider.java
package com.ssginc8.docto.insurance.provider;

import java.util.List;



import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import com.ssginc8.docto.insurance.entity.InsuranceDocument;
import com.ssginc8.docto.global.error.exception.insuranceException.DocumentNotFoundException;
import com.ssginc8.docto.insurance.repository.InsuranceDocumentRepository;

/**
 * Provider 계층: 순수 엔티티 조회 책임
 *
 * • 단건 조회
 * • 전체 조회 (페이징/비페이징)
 */
@Component
@RequiredArgsConstructor
public class InsuranceDocumentProvider {

	private final InsuranceDocumentRepository repo;

	/** 단건 조회 (없으면 404) */
	public InsuranceDocument getById(Long id) {
		return repo.findById(id)
			.orElseThrow(DocumentNotFoundException::new);
	}

	/** 관리자용 전체 목록 조회 (페이징) */
	public Page<InsuranceDocument> getAll(Pageable pageable) {
		return repo.findAll(pageable);
	}

	/** 관리자용 전체 목록 조회 (비페이징) */
	public List<InsuranceDocument> getAll() {
		return repo.findAll();
	}


}
