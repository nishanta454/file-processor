package com.sample.file_processor.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sample.file_processor.domain.entity.FileEntity;
import com.sample.file_processor.domain.enums.FileStatus;
import com.sample.file_processor.repository.FileEntityRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileProcessorService {

	private final FileEntityRepository fileEntityRepository;

	private final AsyncFileProcessorService asyncFileProcessorService;

	@Transactional
	public UUID uploadFile(String filename, byte[] content) {
		var file = FileEntity.builder().fileName(filename).fileContent(content).status(FileStatus.IN_QUEUE)
				.createdAt(LocalDateTime.now()).build();

		fileEntityRepository.save(file);

		asyncFileProcessorService.processFileAsync(file.getId());

		return file.getId();
	}

	public FileStatus getFileStatus(UUID fileId) {
		var file = fileEntityRepository.findById(fileId).orElseThrow();
		return file.getStatus();
	}
}
