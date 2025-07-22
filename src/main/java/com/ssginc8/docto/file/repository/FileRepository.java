package com.ssginc8.docto.file.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssginc8.docto.file.entity.File;

public interface FileRepository extends JpaRepository<File, Long> {

	@Query("SELECT f.url FROM File f WHERE f.fileId IN :fileIds")
	List<String> getFileUrlsByIds(@Param("fileIds") List<Long> fileIds);



	@Query("SELECT f.url FROM File f WHERE f.fileId = :fileId")
	String getFileUrlById(@Param("fileId") Long fileId);
}
