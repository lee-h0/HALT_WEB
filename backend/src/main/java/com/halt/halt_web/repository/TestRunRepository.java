package com.halt.halt_web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.halt.halt_web.domain.TestRun;

import java.util.List;

public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    List<TestRun> findByAttemptAttemptIdOrderByCreatedAtAsc(Long attemptId);
}
