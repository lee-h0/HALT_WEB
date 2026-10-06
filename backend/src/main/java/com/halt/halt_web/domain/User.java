package com.halt.halt_web.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

/** 회원 */
@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Nationalized
    @Column(name = "login_id", nullable = false, length = 50, unique = true)
    private String loginId;

    /** BCrypt 해시. 원래 비밀번호는 저장하지 않는다 */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Nationalized
    @Column(name = "nickname", nullable = false, length = 30, unique = true)
    private String nickname;

    @Nationalized
    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    /** 현재 보유 포인트 (포인트 랭킹 기준) */
    @Column(name = "total_points", nullable = false)
    private int totalPoints = 0;

    /** 푼 문제 수 (재풀이 제외) */
    @Column(name = "solved_count", nullable = false)
    private int solvedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private UserRole role = UserRole.USER;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
