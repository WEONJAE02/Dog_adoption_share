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
    user_id    BIGINT
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
