package com.halt.halt_web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.halt.halt_web.domain.ProblemFile;

import java.util.List;

public interface ProblemFileRepository extends JpaRepository<ProblemFile, Long> {

    /** 채점용: 숨김 테스트를 포함한 전체 파일 */
    List<ProblemFile> findByProblemProblemId(Long problemId);

    /** 응시자에게 보낼 공개 파일만 */
    List<ProblemFile> findByProblemProblemIdAndHiddenFalse(Long problemId);
}
