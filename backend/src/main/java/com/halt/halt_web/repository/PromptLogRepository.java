package com.halt.halt_web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.halt.halt_web.domain.PromptLog;

import java.util.List;

public interface PromptLogRepository extends JpaRepository<PromptLog, Long> {

    List<PromptLog> findByAttemptAttemptIdOrderByCreatedAtAsc(Long attemptId);
}
