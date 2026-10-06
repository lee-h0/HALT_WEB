package com.halt.halt_web.domain;

/** 응시 상태: 진행 중, 정답, 오답, 이탈(문제 페이지를 벗어남) */
public enum AttemptStatus { IN_PROGRESS, PASSED, FAILED, ABANDONED }
