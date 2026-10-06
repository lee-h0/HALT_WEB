package com.halt.halt_web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.halt.halt_web.domain.AttemptFile;
import com.halt.halt_web.domain.AttemptFileId;

import java.util.List;

public interface AttemptFileRepository extends JpaRepository<AttemptFile, AttemptFileId> {

    List<AttemptFile> findByIdAttemptId(Long attemptId);
}
