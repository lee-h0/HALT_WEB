
CREATE DATABASE HALT_WEB_DB COLLATE Korean_Wansung_CI_AS;
GO

/* ---------------------------------------------------------------------
   초기화가 필요할 때만 사용 (모든 데이터가 삭제됨ㅋㅋ) - 웬만하면 건들 ㄴㄴ
   ---------------------------------------------------------------------
DROP TABLE IF EXISTS daily_recommendations;
DROP TABLE IF EXISTS solution_unlocks;
DROP TABLE IF EXISTS user_problem_best;
DROP TABLE IF EXISTS test_runs;
DROP TABLE IF EXISTS prompt_logs;
DROP TABLE IF EXISTS attempt_files;
DROP TABLE IF EXISTS attempts;
DROP TABLE IF EXISTS problem_files;
DROP TABLE IF EXISTS problems;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS difficulty_settings;
*/
select *
from attempts

SELECT * FROM user_problem_best;
SELECT user_id, nickname, total_points, solved_count FROM users;

select *
from users;


SELECT TOP 5 * FROM prompt_logs ORDER BY prompt_id DESC;
SELECT TOP 3 attempt_id, tokens_used, prompt_count, file_change_count FROM attempts ORDER BY attempt_id DESC;

/*항상 이것부터 실행 후 DB작업*/
USE HALT_WEB_DB;
GO

/* 1. difficulty_settings : 난이도별 고정값 */
CREATE TABLE difficulty_settings (
    difficulty   VARCHAR(10) NOT NULL,
    token_limit  INT         NOT NULL,   -- AI 사용량 100%에 해당하는 토큰 수
    max_points   INT         NOT NULL,   -- 만점
    min_points   INT         NOT NULL,   -- 최하점 (한도 초과 시 지급)
    unlock_cost  INT         NOT NULL,   -- 풀이 열람 시 차감 포인트
    CONSTRAINT PK_difficulty_settings PRIMARY KEY (difficulty),
    CONSTRAINT CK_difficulty_settings_name   CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT CK_difficulty_settings_points CHECK (min_points >= 0 AND max_points >= min_points),
    CONSTRAINT CK_difficulty_settings_limits CHECK (token_limit > 0 AND unlock_cost >= 0)
);
GO

/* 2. users : 회원 */
CREATE TABLE users (
    user_id        BIGINT IDENTITY(1,1) NOT NULL,
    login_id       NVARCHAR(50)  NOT NULL,
    password_hash  VARCHAR(100)  NOT NULL,                 -- BCrypt 해시
    nickname       NVARCHAR(30)  NOT NULL,
    email          NVARCHAR(100) NOT NULL,
    total_points   INT           NOT NULL CONSTRAINT DF_users_total_points DEFAULT 0,  -- 현재 보유 포인트
    solved_count   INT           NOT NULL CONSTRAINT DF_users_solved_count DEFAULT 0,  -- 푼 문제 수 (재풀이 제외)
    role           VARCHAR(10)   NOT NULL CONSTRAINT DF_users_role DEFAULT 'USER',
    created_at     DATETIME2(0)  NOT NULL CONSTRAINT DF_users_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_users PRIMARY KEY (user_id),
    CONSTRAINT UQ_users_login_id UNIQUE (login_id),
    CONSTRAINT UQ_users_nickname UNIQUE (nickname),
    CONSTRAINT UQ_users_email    UNIQUE (email),
    CONSTRAINT CK_users_points   CHECK (total_points >= 0 AND solved_count >= 0),
    CONSTRAINT CK_users_role     CHECK (role IN ('USER', 'ADMIN'))
);
GO

select *
from users

-- 랭킹 조회용 (포인트 순, 푼 문제 수 순)
CREATE INDEX IX_users_total_points ON users (total_points DESC) INCLUDE (nickname);
CREATE INDEX IX_users_solved_count ON users (solved_count DESC) INCLUDE (nickname);
GO

/* 3. problems : 문제 */
CREATE TABLE problems (
    problem_id         BIGINT IDENTITY(1,1) NOT NULL,
    slug               VARCHAR(50)    NOT NULL,   -- Git 문제 폴더 이름. 서버 시작 시 같은 폴더를 중복 등록하지 않기 위한 키
    title              NVARCHAR(100)  NOT NULL,
    difficulty         VARCHAR(10)    NOT NULL,
    description        NVARCHAR(MAX)  NOT NULL,   -- 설명, 할 일, 기술 메모 (마크다운)
    constraints_text   NVARCHAR(MAX)  NULL,       -- 지킬 조건
    out_of_scope_text  NVARCHAR(MAX)  NULL,       -- 안 해도 되는 것
    test_command       NVARCHAR(200)  NOT NULL,   -- 예: python3 -m pytest -q
    is_active          BIT            NOT NULL CONSTRAINT DF_problems_is_active DEFAULT 1,
    created_at         DATETIME2(0)   NOT NULL CONSTRAINT DF_problems_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_problems PRIMARY KEY (problem_id),
    CONSTRAINT UQ_problems_slug UNIQUE (slug),
    CONSTRAINT FK_problems_difficulty FOREIGN KEY (difficulty) REFERENCES difficulty_settings (difficulty)
);
GO

/* 4. problem_files : 문제 초기 파일과 숨김 테스트 */
CREATE TABLE problem_files (
    file_id     BIGINT IDENTITY(1,1) NOT NULL,
    problem_id  BIGINT         NOT NULL,
    file_path   NVARCHAR(260)  NOT NULL,          -- 예: src/server.js
    content     NVARCHAR(MAX)  NOT NULL,
    is_hidden   BIT            NOT NULL CONSTRAINT DF_problem_files_is_hidden DEFAULT 0,  -- 1: 숨김 테스트 (응시자에게 보내지 않음)
    CONSTRAINT PK_problem_files PRIMARY KEY (file_id),
    CONSTRAINT UQ_problem_files_path UNIQUE (problem_id, file_path),
    CONSTRAINT FK_problem_files_problem FOREIGN KEY (problem_id) REFERENCES problems (problem_id) ON DELETE CASCADE
);
GO

/* 5. attempts : 응시 기록 */
CREATE TABLE attempts (
    attempt_id         BIGINT IDENTITY(1,1) NOT NULL,
    user_id            BIGINT        NOT NULL,
    problem_id         BIGINT        NOT NULL,
    status             VARCHAR(15)   NOT NULL CONSTRAINT DF_attempts_status DEFAULT 'IN_PROGRESS',
    started_at         DATETIME2(0)  NOT NULL CONSTRAINT DF_attempts_started_at DEFAULT SYSDATETIME(),
    submitted_at       DATETIME2(0)  NULL,
    elapsed_sec        INT           NOT NULL CONSTRAINT DF_attempts_elapsed_sec DEFAULT 0,
    tokens_used        INT           NOT NULL CONSTRAINT DF_attempts_tokens_used DEFAULT 0,
    prompt_count       INT           NOT NULL CONSTRAINT DF_attempts_prompt_count DEFAULT 0,        -- 지시 횟수 (표시용)
    file_change_count  INT           NOT NULL CONSTRAINT DF_attempts_file_change_count DEFAULT 0,   -- 파일 변경 횟수 (표시용)
    score              INT           NULL,        -- 오답이거나 풀이를 먼저 열람한 문제면 0
    CONSTRAINT PK_attempts PRIMARY KEY (attempt_id),
    CONSTRAINT FK_attempts_user    FOREIGN KEY (user_id)    REFERENCES users (user_id),
    CONSTRAINT FK_attempts_problem FOREIGN KEY (problem_id) REFERENCES problems (problem_id),
    CONSTRAINT CK_attempts_status  CHECK (status IN ('IN_PROGRESS', 'PASSED', 'FAILED', 'ABANDONED')),
    CONSTRAINT CK_attempts_counts  CHECK (elapsed_sec >= 0 AND tokens_used >= 0 AND prompt_count >= 0 AND file_change_count >= 0),
    CONSTRAINT CK_attempts_score   CHECK (score IS NULL OR score >= 0)
);
GO

-- 대시보드 "이어서 응시": 회원의 가장 최근 응시 조회
CREATE INDEX IX_attempts_user_started ON attempts (user_id, started_at DESC) INCLUDE (problem_id, status);
CREATE INDEX IX_attempts_problem_status ON attempts (problem_id, status);
-- 회원 1명당 진행 중인 응시는 1건만 허용 (새 응시를 시작하기 전에 이전 응시를 ABANDONED로 바꿔야 함)
CREATE UNIQUE INDEX UX_attempts_one_in_progress ON attempts (user_id) WHERE status = 'IN_PROGRESS';
GO

/* 6. attempt_files : 제출 시점의 풀이 파일 */
CREATE TABLE attempt_files (
    attempt_id  BIGINT         NOT NULL,
    file_path   NVARCHAR(260)  NOT NULL,
    content     NVARCHAR(MAX)  NOT NULL,
    CONSTRAINT PK_attempt_files PRIMARY KEY (attempt_id, file_path),
    CONSTRAINT FK_attempt_files_attempt FOREIGN KEY (attempt_id) REFERENCES attempts (attempt_id) ON DELETE CASCADE
);
GO

/* 7. prompt_logs : AI 지시 기록 */
CREATE TABLE prompt_logs (
    prompt_id      BIGINT IDENTITY(1,1) NOT NULL,
    attempt_id     BIGINT         NOT NULL,
    prompt_text    NVARCHAR(MAX)  NOT NULL,
    response_text  NVARCHAR(MAX)  NULL,
    model          VARCHAR(50)    NOT NULL,       -- 예: Gemini Flash 계열 모델명
    input_tokens   INT            NOT NULL CONSTRAINT DF_prompt_logs_input_tokens DEFAULT 0,
    output_tokens  INT            NOT NULL CONSTRAINT DF_prompt_logs_output_tokens DEFAULT 0,
    created_at     DATETIME2(0)   NOT NULL CONSTRAINT DF_prompt_logs_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_prompt_logs PRIMARY KEY (prompt_id),
    CONSTRAINT FK_prompt_logs_attempt FOREIGN KEY (attempt_id) REFERENCES attempts (attempt_id) ON DELETE CASCADE,
    CONSTRAINT CK_prompt_logs_tokens CHECK (input_tokens >= 0 AND output_tokens >= 0)
);
GO

CREATE INDEX IX_prompt_logs_attempt ON prompt_logs (attempt_id, created_at);
GO

/* 8. test_runs : 테스트 실행과 제출 채점 결과 */
CREATE TABLE test_runs (
    run_id        BIGINT IDENTITY(1,1) NOT NULL,
    attempt_id    BIGINT         NOT NULL,
    run_type      VARCHAR(10)    NOT NULL,        -- TEST: 테스트 버튼, SUBMIT: 제출 버튼
    passed        BIT            NOT NULL,
    passed_count  INT            NOT NULL,
    total_count   INT            NOT NULL,
    output_text   NVARCHAR(MAX)  NULL,
    created_at    DATETIME2(0)   NOT NULL CONSTRAINT DF_test_runs_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_test_runs PRIMARY KEY (run_id),
    CONSTRAINT FK_test_runs_attempt FOREIGN KEY (attempt_id) REFERENCES attempts (attempt_id) ON DELETE CASCADE,
    CONSTRAINT CK_test_runs_type   CHECK (run_type IN ('TEST', 'SUBMIT')),
    CONSTRAINT CK_test_runs_counts CHECK (passed_count >= 0 AND total_count >= passed_count)
);
GO

CREATE INDEX IX_test_runs_attempt ON test_runs (attempt_id, created_at);
GO

/* 9. user_problem_best : 회원·문제별 최고 기록 1건 */
CREATE TABLE user_problem_best (
    user_id          BIGINT        NOT NULL,
    problem_id       BIGINT        NOT NULL,
    best_attempt_id  BIGINT        NOT NULL,
    best_score       INT           NOT NULL,
    first_solved_at  DATETIME2(0)  NOT NULL CONSTRAINT DF_user_problem_best_first_solved_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_user_problem_best PRIMARY KEY (user_id, problem_id),
    CONSTRAINT FK_user_problem_best_user    FOREIGN KEY (user_id)         REFERENCES users (user_id),
    CONSTRAINT FK_user_problem_best_problem FOREIGN KEY (problem_id)      REFERENCES problems (problem_id),
    CONSTRAINT FK_user_problem_best_attempt FOREIGN KEY (best_attempt_id) REFERENCES attempts (attempt_id),
    CONSTRAINT CK_user_problem_best_score   CHECK (best_score >= 0)
);
GO

-- 다른 사람 풀이 목록: 문제별 최고 기록을 점수 순으로 조회
CREATE INDEX IX_user_problem_best_problem ON user_problem_best (problem_id, best_score DESC);
GO

/* 10. solution_unlocks : 못 푼 상태에서 포인트를 내고 풀이를 연 기록 */
CREATE TABLE solution_unlocks (
    user_id       BIGINT        NOT NULL,
    problem_id    BIGINT        NOT NULL,
    points_spent  INT           NOT NULL,
    unlocked_at   DATETIME2(0)  NOT NULL CONSTRAINT DF_solution_unlocks_unlocked_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_solution_unlocks PRIMARY KEY (user_id, problem_id),
    CONSTRAINT FK_solution_unlocks_user    FOREIGN KEY (user_id)    REFERENCES users (user_id),
    CONSTRAINT FK_solution_unlocks_problem FOREIGN KEY (problem_id) REFERENCES problems (problem_id),
    CONSTRAINT CK_solution_unlocks_points  CHECK (points_spent >= 0)
);
GO

/* 11. daily_recommendations : AI 추천 오늘의 문제 (선택) */
CREATE TABLE daily_recommendations (
    user_id     BIGINT         NOT NULL,
    rec_date    DATE           NOT NULL,
    problem_id  BIGINT         NOT NULL,
    reason      NVARCHAR(300)  NULL,
    CONSTRAINT PK_daily_recommendations PRIMARY KEY (user_id, rec_date),
    CONSTRAINT FK_daily_recommendations_user    FOREIGN KEY (user_id)    REFERENCES users (user_id),
    CONSTRAINT FK_daily_recommendations_problem FOREIGN KEY (problem_id) REFERENCES problems (problem_id)
);
GO

/* 초기 데이터 : 난이도별 고정값
   포인트는 추천안(1~30점 범위). token_limit은 잠정값이며 Gemini 무료 한도 확인 후 조정한다. */
INSERT INTO difficulty_settings (difficulty, token_limit, max_points, min_points, unlock_cost) VALUES
    ('EASY',   20000, 10, 1,  5),
    ('MEDIUM', 40000, 20, 2, 10),
    ('HARD',   60000, 30, 3, 15);
GO
