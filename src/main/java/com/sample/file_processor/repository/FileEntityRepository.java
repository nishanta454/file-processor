package com.sample.file_processor.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sample.file_processor.domain.entity.FileEntity;

public interface FileEntityRepository extends JpaRepository<FileEntity, UUID> {
}