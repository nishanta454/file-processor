package com.sample.file_processor.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sample.file_processor.domain.enums.FileStatus;
import com.sample.file_processor.service.FileProcessorService;
import com.sample.file_processor.service.RateLimiterService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileProcessorController {

	private final FileProcessorService fileProcessorService;
	
	private final RateLimiterService rateLimiterService;

	@PostMapping("/upload")
	public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request)
			throws Exception {
		String clientIP = getClientIP(request);

		if (!rateLimiterService.tryConsume(clientIP)) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many requests - please wait");
		}

		UUID contentId = fileProcessorService.uploadFile(file.getOriginalFilename(), file.getBytes());
		return ResponseEntity.ok().body(contentId);
	}

	@GetMapping("/status/{contentId}")
	public ResponseEntity<?> getFileStatus(@PathVariable UUID contentId) {
		try {
			FileStatus status = fileProcessorService.getFileStatus(contentId);
			return ResponseEntity.ok().body(status);
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
		}
	}

	private String getClientIP(HttpServletRequest request) {
		String xfHeader = request.getHeader("X-Forwarded-For");
		if (xfHeader == null) {
			return request.getRemoteAddr();
		}
		return xfHeader.split(",")[0];
	}
}
