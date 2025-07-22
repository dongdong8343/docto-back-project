// src/main/java/com/ssginc8/docto/insurance/service/InsuranceDocumentServiceImpl.java
package com.ssginc8.docto.insurance.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import com.ssginc8.docto.file.entity.Category;
import com.ssginc8.docto.file.entity.File;
import com.ssginc8.docto.file.provider.FileProvider;
import com.ssginc8.docto.global.error.exception.fileException.FileUploadFailedException;
import com.ssginc8.docto.hospital.entity.Hospital;
import com.ssginc8.docto.hospital.provider.HospitalProvider;
import com.ssginc8.docto.insurance.entity.InsuranceDocument;
import com.ssginc8.docto.insurance.provider.InsuranceDocumentProvider;
import com.ssginc8.docto.insurance.repository.InsuranceDocumentRepository;
import com.ssginc8.docto.insurance.service.dto.DocumentApproval;
import com.ssginc8.docto.insurance.service.dto.DocumentResponse;
import com.ssginc8.docto.insurance.service.dto.FileDownload;
import com.ssginc8.docto.insurance.service.dto.UserDocumentRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InsuranceDocumentServiceImpl implements InsuranceDocumentService {

	private final InsuranceDocumentRepository repo;
	private final InsuranceDocumentProvider provider;
	private final HospitalProvider          hospitalProvider;
	private final AmazonS3                  amazonS3;
	private final FileProvider              fileProvider;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;  // S3 버킷 이름

	// 1) 환자·보호자: 단건 요청 생성
	@Override
	@Transactional
	public DocumentResponse createRequest(UserDocumentRequest dto) {
		Hospital hosp = hospitalProvider.getHospitalById(dto.getHospitalId());
		InsuranceDocument doc = InsuranceDocument.createRequest(
			dto.getRequesterId(), hosp, dto.getNote(), dto.getType()
		);
		repo.save(doc);
		return toDto(doc);
	}

	// 1-2) 환자·보호자: 본인 요청 페이징 조회
	@Override
	@Transactional(readOnly = true)
	public Page<DocumentResponse> listUserRequests(Long requesterId, Pageable pageable) {
		return repo.findAllByRequesterId(requesterId, pageable)
			.map(this::toDto);
	}

	// 1-3) 환자·보호자: 배치 요청 생성
	@Override
	@Transactional
	public List<DocumentResponse> createRequests(List<UserDocumentRequest> dtos) {
		return dtos.stream()
			.map(this::createRequest)
			.collect(Collectors.toList());
	}

	// 2) 관리자: 파일 첨부
	@Override
	@Transactional
	public DocumentResponse attachFile(Long documentId, MultipartFile multipart) {
		InsuranceDocument doc = provider.getById(documentId);

		String key = "insurance/" + UUID.randomUUID() + "_" + multipart.getOriginalFilename();
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentType(multipart.getContentType());
		meta.setContentLength(multipart.getSize());

		try {
			amazonS3.putObject(bucket, key, multipart.getInputStream(), meta);
		} catch (IOException e) {
			throw new FileUploadFailedException();
		}

		File fileEntity = File.createFile(
			Category.INSURANCE,
			key,
			multipart.getOriginalFilename(),
			amazonS3.getUrl(bucket, key).toString(),
			bucket,
			multipart.getSize(),
			multipart.getContentType()
		);
		fileEntity = fileProvider.saveFile(fileEntity);

		doc.attach(fileEntity);
		repo.save(doc);

		return toDto(doc);
	}

	// 3) 공통: 단건 상태 조회
	@Override
	@Transactional(readOnly = true)
	public DocumentResponse status(Long documentId) {
		return toDto(provider.getById(documentId));
	}

	// 4) 관리자: 전체 요청 페이징 조회
	@Override
	@Transactional(readOnly = true)
	public Page<DocumentResponse> listAll(Pageable pageable) {
		return repo.findAll(pageable)
			.map(this::toDto);
	}

	// 5) 관리자: 승인/반려 처리
	@Override
	@Transactional
	public void approve(Long documentId, DocumentApproval dto) {
		InsuranceDocument doc = provider.getById(documentId);
		if (Boolean.TRUE.equals(dto.getApproved())) {
			doc.approve();
		} else {
			doc.reject(dto.getReason());
		}
		repo.save(doc);
	}

	// 6) 공통: 파일 다운로드
	@Override
	@Transactional(readOnly = true)
	public FileDownload downloadFile(Long documentId) {
		InsuranceDocument doc = provider.getById(documentId);
		File file = doc.getFile();
		if (file == null) {
			throw new IllegalStateException("첨부된 파일이 없습니다.");
		}

		S3Object s3Object = amazonS3.getObject(file.getBucketName(), file.getFileName());
		try (S3ObjectInputStream is = s3Object.getObjectContent()) {
			byte[] data = is.readAllBytes();
			return FileDownload.builder()
				.data(data)
				.size(file.getFileSize())
				.contentType(file.getFileType())
				.originalName(file.getOriginalName())
				.build();
		} catch (IOException e) {
			throw new RuntimeException("파일 다운로드 오류", e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Page<DocumentResponse> listByHospital(Long hospitalId, Pageable pageable) {
		Page<InsuranceDocument> page = repo.findAllByHospitalHospitalId(hospitalId, pageable);
		return page.map(DocumentResponse::from);  // 같은 트랜잭션 안에서 프록시가 초기화됨
	}

	/** 엔티티 → DTO 변환 헬퍼 */
	private DocumentResponse toDto(InsuranceDocument doc) {
		return DocumentResponse.builder()
			.documentId(doc.getDocumentId())
			.status(doc.getStatus())
			.rejectionReason(doc.getRejectionReason())
			.downloadUrl(doc.getFile() != null ? doc.getFile().getUrl() : null)
			.hospitalId(doc.getHospital().getHospitalId())
			.hospitalName(doc.getHospital().getName())
			.type(doc.getType())
			.build();
	}
}
