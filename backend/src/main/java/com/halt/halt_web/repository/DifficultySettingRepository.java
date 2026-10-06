package com.halt.halt_web.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.halt.halt_web.domain.DifficultySetting;

public interface DifficultySettingRepository extends JpaRepository<DifficultySetting, String> {
}
