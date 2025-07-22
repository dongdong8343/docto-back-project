// src/main/java/com/ssginc8/docto/insurance/repo/InsuranceDocumentRepository.java
package com.ssginc8.docto.insurance.repository;

import com.ssginc8.docto.insurance.entity.InsuranceDocument;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


/**
 * InsuranceDocument 전용 JPA Repository
 */
public interface InsuranceDocumentRepository extends JpaRepository<InsuranceDocument, Long> {

	/** 환자·보호자: 본인이 생성한 요청 페이징 조회 */
	Page<InsuranceDocument> findAllByRequesterId(Long requesterId, Pageable pageable);


	Page<InsuranceDocument> findAllByHospitalHospitalId(Long hospitalId, Pageable pageable);

	/** 관리자: 전체 요청 페이징 조회 */
	Page<InsuranceDocument> findAll(Pageable pageable);


}


