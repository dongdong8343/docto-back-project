package com.ssginc8.docto.file.entity;

import org.hibernate.annotations.DynamicUpdate;

import com.ssginc8.docto.global.base.BaseTimeEntity;
import com.ssginc8.docto.hospital.entity.Hospital;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@DynamicUpdate
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "tbl_file")
@Entity
public class File extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long fileId;

	@Enumerated(EnumType.STRING)
	private Category category;

	@Column(nullable = false)
	private String fileName;

	@Column(nullable = false)
	private String originalName;

	@Column(nullable = false)
	private String url;

	@Column(nullable = false)
	private String bucketName;

	@Column(nullable = false)
	private Long fileSize;

	@Column(nullable = false)
	private String fileType;

	private File(Category category, String fileName, String originalName, String url, String bucketName,
		Long fileSize, String fileType) {
		this.category = category;
		this.fileName = fileName;
		this.originalName = originalName;
		this.url = url;
		this.bucketName = bucketName;
		this.fileSize = fileSize;
		this.fileType = fileType;
	}

	public static File createFile(Category category, String fileName, String originalName, String url,
		String bucketName,
		Long fileSize, String fileType) {
		return new File(category, fileName, originalName, url, bucketName, fileSize, fileType);
	}

	public void updateFile(String fileName, String originalName, String url,
		String bucketName, Long fileSize, String fileType) {
		this.fileName = fileName;
		this.originalName = originalName;
		this.url = url;
		this.bucketName = bucketName;
		this.fileSize = fileSize;
		this.fileType = fileType;
	}
}
