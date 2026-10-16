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

-- 용품 중고거래 (Shop). price 0 은 나눔
CREATE TABLE IF NOT EXISTS shop_item (
    item_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    category    VARCHAR(20)  NOT NULL,
    title       VARCHAR(100) NOT NULL,
    price       INT          NOT NULL DEFAULT 0,
    description TEXT         NOT NULL,
    trade_area  VARCHAR(50),
    status      VARCHAR(10)  NOT NULL DEFAULT '판매중',
    view_count  INT          NOT NULL DEFAULT 0,
    hidden      TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME,
    FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE IF NOT EXISTS shop_item_image (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id   BIGINT       NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    FOREIGN KEY (item_id) REFERENCES shop_item (item_id)
);

-- 관심(찜). 한 사람이 한 상품에 한 번
CREATE TABLE IF NOT EXISTS shop_item_like (
    item_id    BIGINT   NOT NULL,
    user_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (item_id, user_id),
    FOREIGN KEY (item_id) REFERENCES shop_item (item_id),
    FOREIGN KEY (user_id) REFERENCES users (user_id)
);

-- 구매 문의 (보낸 사람 → 판매자)
CREATE TABLE IF NOT EXISTS shop_inquiry (
    inquiry_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id     BIGINT        NOT NULL,
    sender_id   BIGINT        NOT NULL,
    receiver_id BIGINT        NOT NULL,
    message     VARCHAR(1000) NOT NULL,
    contact     VARCHAR(100),
    reply       VARCHAR(1000),
    created_at  DATETIME      NOT NULL,
    replied_at  DATETIME,
    FOREIGN KEY (item_id) REFERENCES shop_item (item_id),
    FOREIGN KEY (sender_id) REFERENCES users (user_id),
    FOREIGN KEY (receiver_id) REFERENCES users (user_id)
);

-- 내 반려동물 (건강체크 대상). 입양 대상 유기견(dog)과는 별개
CREATE TABLE IF NOT EXISTS my_pet (
    pet_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    name       VARCHAR(20)  NOT NULL,
    species    VARCHAR(10)  NOT NULL,
    breed      VARCHAR(30),
    gender     VARCHAR(10),
    neutered   TINYINT(1),
    birth_year INT,
    weight_kg  DECIMAL(5,2),
    notes      VARCHAR(500),
    image_name VARCHAR(255),
    created_at DATETIME     NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES users (user_id)
);

-- 건강체크 (사진·영상 프레임 → 분석 결과). result_json 에 분석 결과 전체를 보관
CREATE TABLE IF NOT EXISTS health_check (
    check_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    pet_id      BIGINT        NOT NULL,
    user_id     BIGINT        NOT NULL,
    body_part   VARCHAR(20)   NOT NULL,
    source      VARCHAR(10)   NOT NULL,
    symptoms    VARCHAR(500),
    memo        VARCHAR(1000),
    urgency     VARCHAR(20)   NOT NULL,
    specialty   VARCHAR(20),
    summary     VARCHAR(1000),
    result_json TEXT          NOT NULL,
    analyzer    VARCHAR(50)   NOT NULL,
    created_at  DATETIME      NOT NULL,
    FOREIGN KEY (pet_id) REFERENCES my_pet (pet_id),
    FOREIGN KEY (user_id) REFERENCES users (user_id)
);

CREATE TABLE IF NOT EXISTS health_check_frame (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    check_id  BIGINT       NOT NULL,
    seq       INT          NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    FOREIGN KEY (check_id) REFERENCES health_check (check_id)
);

-- 건강체크 사용 횟수 (하루 제한용). 체크 기록을 지워도 남아서 제한을 우회할 수 없다. 회원 탈퇴 시에만 삭제
CREATE TABLE IF NOT EXISTS health_check_usage (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_usage_user_time (user_id, created_at),
    FOREIGN KEY (user_id) REFERENCES users (user_id)
);

-- 주치의 추천용 샘플 수의사 (가상 인물·가상 병원). 실제 병원 데이터가 아니다
CREATE TABLE IF NOT EXISTS vet (
    vet_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(20)  NOT NULL,
    clinic_name     VARCHAR(50)  NOT NULL,
    sido            VARCHAR(20)  NOT NULL,
    sigungu         VARCHAR(20)  NOT NULL,
    specialty       VARCHAR(20)  NOT NULL,
    sub_specialties VARCHAR(100),
    career_years    INT          NOT NULL,
    cases_count     INT          NOT NULL,
    rating          DECIMAL(2,1) NOT NULL,
    review_count    INT          NOT NULL,
    open_24h        TINYINT(1)   NOT NULL DEFAULT 0,
    intro           VARCHAR(300),
    is_sample       TINYINT(1)   NOT NULL DEFAULT 1
);
