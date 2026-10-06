package com.halt.halt_web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.halt.halt_web.domain.SolutionUnlock;
import com.halt.halt_web.domain.UserProblemId;

public interface SolutionUnlockRepository extends JpaRepository<SolutionUnlock, UserProblemId> {
}
