package com.sample.file_processor.service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.sample.file_processor.domain.enums.FileStatus;
import com.sample.file_processor.repository.FileEntityRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncFileProcessorService {
	private final FileEntityRepository uploadedFileRepository;

	private final RestTemplate restTemplate = new RestTemplate();

	@Value("${downstream.api.url}")
	private String downstreamApiUrl;

	@Async
	@Transactional
	public void processFileAsync(UUID fileId) {
		var file = uploadedFileRepository.findById(fileId).orElseThrow();
		file.setStatus(FileStatus.IN_PROGRESS);
		uploadedFileRepository.save(file);

		try {
			var fileText = new String(file.getFileContent(), StandardCharsets.UTF_8);
			var lines = fileText.split("\\r?\\n");

			for (var line : lines) {
				Map<String, String> payload = new HashMap<>();
                payload.put("message", line);
                
                var headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);
                
                restTemplate.postForObject(downstreamApiUrl, request, String.class);
			}

			file.setStatus(FileStatus.PROCESSED);

		} catch (Exception ex) {
			file.setStatus(FileStatus.FAILED);
			log.error("exception while processing file", ex);
		}

		uploadedFileRepository.save(file);
	}
}
