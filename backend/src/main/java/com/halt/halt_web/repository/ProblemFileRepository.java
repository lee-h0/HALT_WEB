package com.halt.halt_web.repository;

import com.halt.halt_web.domain.ProblemFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProblemFileRepository extends JpaRepository<ProblemFile, Long> {

    /** 채점용: 숨김 테스트를 포함한 전체 파일 */
    List<ProblemFile> findByProblemProblemId(Long problemId);

    /** 응시자에게 보낼 공개 파일만 */
    List<ProblemFile> findByProblemProblemIdAndHiddenFalse(Long problemId);

    /** 문제 폴더를 다시 읽을 때 기존 파일을 모두 지운다 (바로 DELETE 문이 실행된다) */
    @Modifying
    @Query("delete from ProblemFile f where f.problem.problemId = :problemId")
    void deleteAllByProblemId(@Param("problemId") Long problemId);
}
