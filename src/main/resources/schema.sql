-- 서버 기동 시 실행된다. 이미 있는 테이블은 건드리지 않는다.
-- dog / dog_detail / dog_image 는 Repository의 SQL을 기준으로 작성한 정의다.

CREATE TABLE IF NOT EXISTS users (
    user_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider    VARCHAR(20)  NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    email       VARCHAR(255),
    password    VARCHAR(100),
    nickname    VARCHAR(50)  NOT NULL,
    created_at  DATETIME     NOT NULL,
    terms_agreed_at DATETIME,
    deleted_at  DATETIME,
    UNIQUE KEY uk_users_provider (provider, provider_id)
);

CREATE TABLE IF NOT EXISTS dog (
    dog_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    breed      VARCHAR(100),
    age        INT NOT NULL DEFAULT 0,
    status     VARCHAR(30),
    image_name VARCHAR(255),
    gender     VARCHAR(20),
    reg_date   DATETIME,
    user_id    BIGINT,
    hidden     TINYINT(1) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS dog_detail (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    dog_id      BIGINT NOT NULL,
    found_place VARCHAR(255),
    memo        TEXT,
    FOREIGN KEY (dog_id) REFERENCES dog (dog_id)
);

CREATE TABLE IF NOT EXISTS dog_image (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    dog_id      BIGINT NOT NULL,
    file_name   VARCHAR(255),
    file_path   VARCHAR(500),
    upload_date DATETIME,
    FOREIGN KEY (dog_id) REFERENCES dog (dog_id)
);

-- 커뮤니티 게시판
CREATE TABLE IF NOT EXISTS board_post (
    post_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    category   VARCHAR(20)  NOT NULL,
    title      VARCHAR(100) NOT NULL,
    content    TEXT         NOT NULL,
    view_count INT          NOT NULL DEFAULT 0,
    hidden     TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE IF NOT EXISTS board_post_image (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id   BIGINT       NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    FOREIGN KEY (post_id) REFERENCES board_post (post_id)
);

-- parent_id 가 있으면 대댓글 (2단계까지만 사용)
CREATE TABLE IF NOT EXISTS board_comment (
    comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    parent_id  BIGINT,
    content    VARCHAR(500) NOT NULL,
    deleted    TINYINT(1)   NOT NULL DEFAULT 0,
    hidden     TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL,
    FOREIGN KEY (post_id) REFERENCES board_post (post_id),
    FOREIGN KEY (user_id) REFERENCES users (user_id)
);

-- 회원 등록 유기견에 대한 입양 문의 (보낸 사람 → 등록자)
CREATE TABLE IF NOT EXISTS adoption_inquiry (
    inquiry_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    dog_id      BIGINT        NOT NULL,
    sender_id   BIGINT        NOT NULL,
    receiver_id BIGINT        NOT NULL,
    message     VARCHAR(1000) NOT NULL,
    contact     VARCHAR(100),
    reply       VARCHAR(1000),
    created_at  DATETIME      NOT NULL,
    replied_at  DATETIME,
    FOREIGN KEY (dog_id) REFERENCES dog (dog_id),
    FOREIGN KEY (sender_id) REFERENCES users (user_id),
    FOREIGN KEY (receiver_id) REFERENCES users (user_id)
);

-- 신고 (대상: dog / post / comment). 같은 사람이 같은 대상을 두 번 신고할 수 없다
CREATE TABLE IF NOT EXISTS report (
    report_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(10)  NOT NULL,
    target_id   BIGINT       NOT NULL,
    reporter_id BIGINT       NOT NULL,
    reason      VARCHAR(30)  NOT NULL,
    detail      VARCHAR(500),
    created_at  DATETIME     NOT NULL,
    UNIQUE KEY uk_report_once (target_type, target_id, reporter_id),
    FOREIGN KEY (reporter_id) REFERENCES users (user_id)
);
