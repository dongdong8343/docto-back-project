package com.ssginc8.docto.file.provider;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ssginc8.docto.file.entity.File;
import com.ssginc8.docto.file.repository.FileRepository;
import com.ssginc8.docto.global.error.exception.fileException.FileNotFoundException;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
@Component
public class FileProvider {
	private final FileRepository fileRepo;

	public File saveFile(File file) {
		return fileRepo.save(file);
	}

  /**
	 * 파일 ID로 S3 URL을 조회합니다.
	 * @param fileId tbl_file PK
	 * @return URL 문자열 또는 null
	 */
	public String getFileUrlById(Long fileId) {
		if (fileId == null) {
			return null;
		}
		// repository에 선언된 getFileUrlById(@Param("fileId") Long) 호출
		String url = fileRepo.getFileUrlById(fileId);
		if (url == null || url.isBlank()) {
			// 해당 ID로 저장된 파일을 찾을 수 없을 때 예외 던짐
			throw new FileNotFoundException();
		}
		return url;
	}
  
	public List<String> getFileUrlsByIds(List<Long> fileIds) {
		if (fileIds == null || fileIds.isEmpty()) {
			return Collections.emptyList();
		}

	
		return fileRepo.getFileUrlsByIds(fileIds)
			.stream()
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	public File getFileById(Long fileId) {
		return fileRepo.findById(fileId)
			.orElseThrow(() -> new EntityNotFoundException("File not found: " + fileId));
	}

	public List<File> findAllById(List<Long> fileIds) {
		if (fileIds == null || fileIds.isEmpty()) {
			return Collections.emptyList();
		}
		return fileRepo.findAllById(fileIds);
	}

	@Transactional
	public void deleteFileById(Long fileId) {
		fileRepo.deleteById(fileId);
	}
}
