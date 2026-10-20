package com.halt.halt_web.sandbox;

/**
 * @param exitCode 테스트 명령의 종료 코드. 시간 초과면 -1
 * @param output   테스트 출력 (너무 길면 잘라 낸다)
 */
public record SandboxResult(int exitCode, String output, boolean timedOut, long durationMs) {
}
