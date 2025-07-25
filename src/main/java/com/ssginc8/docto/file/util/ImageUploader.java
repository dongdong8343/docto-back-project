package com.ssginc8.docto.file.util;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.ssginc8.docto.file.entity.Category;
import com.ssginc8.docto.file.entity.File;
import com.ssginc8.docto.file.service.FileService;
import com.ssginc8.docto.file.service.dto.UploadFile;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ImageUploader {

	private final FileService fileService;

	// 프로필 사진이 있는 경우 -> s3에 저장
	public File uploadProfileImage(MultipartFile profileImage) {

		File savedProfileImage = null;

		if (Objects.nonNull(profileImage)) {
			UploadFile.Command fileCommand = UploadFile.Command.builder()
				.file(profileImage)
				.category(Category.USER)
				.build();

			UploadFile.Result fileResult = fileService.uploadImage(fileCommand);

			savedProfileImage = File.createFile(fileResult.getCategory(), fileResult.getFileName(),
				fileResult.getOriginalFileName(),
				fileResult.getUrl(), fileResult.getBucket(), fileResult.getFileSize(),
				fileResult.getFileType());
		}

		return savedProfileImage;
	}
}
