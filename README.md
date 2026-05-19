PET ROOM - 유기견 입양 정보 공유 서비스

유기견 정보를 등록하고 관리할 수 있는 입양 정보 공유 서비스입니다.
Spring Boot 기반 REST API와 MySQL을 사용하여 유기견 기본 정보, 상세 정보, 이미지 데이터를 관리하고, 검색·필터링·정렬·통계 기능을 제공합니다.

⸻

프로젝트 소개

유기견 입양 정보를 체계적으로 관리하고, 사용자가 원하는 조건으로 유기견 정보를 빠르게 조회할 수 있도록 구현한 서비스입니다.

단순 CRUD 기능을 넘어 유기견 기본 정보, 상세 정보, 이미지 정보를 분리하여 관리하고, 다중 이미지 업로드와 통계 조회 기능까지 포함했습니다.

⸻

주요 기능

* 유기견 정보 등록, 조회, 수정, 삭제
* 다중 이미지 업로드 및 대표 이미지 처리
* 이름, 품종, 구조 장소 기반 검색
* 입양 상태별 필터링
* 최신순, 이름순, 나이순 정렬
* 입양 상태별/품종별 통계 조회
* OpenAI API 기반 소개 문구 생성 데모

⸻

기술 스택

Backend

* Java 17
* Spring Boot 3.5.6
* Spring Web
* Spring JDBC / JdbcTemplate
* MySQL
* Lombok
* Gradle

Frontend

* HTML
* CSS
* JavaScript
* Chart.js
* Kakao Map SDK
* OpenAI API

⸻

담당 역할

* Spring Boot 기반 REST API 구현
* MySQL 테이블 설계
* JdbcTemplate 기반 Repository 구현
* 유기견 CRUD 기능 구현
* MultipartFile 기반 다중 이미지 업로드 구현
* 유기견 기본 정보, 상세 정보, 이미지 정보 분리 저장 구조 설계
* 검색, 필터링, 정렬 기능 구현
* 입양 상태별/품종별 통계 API 구현
* 프론트엔드 화면과 백엔드 API 연동
* GitHub 공개를 위한 환경변수 및 민감정보 관리 정리

⸻

주요 구현 내용

1. 유기견 CRUD API

유기견 정보를 등록, 조회, 수정, 삭제할 수 있는 REST API를 구현했습니다.

등록 및 수정 시 유기견 기본 정보뿐만 아니라 구조 장소, 메모, 이미지 정보를 함께 처리할 수 있도록 구성했습니다.

2. 다중 이미지 업로드

MultipartFile을 사용하여 여러 장의 이미지를 업로드할 수 있도록 구현했습니다.

* UUID 기반 파일명 생성
* 첫 번째 이미지를 대표 이미지로 설정
* 이미지 메타데이터 DB 저장
* 서버 업로드 디렉터리에 실제 파일 저장

3. 데이터 분리 설계

유기견 관련 데이터를 역할에 따라 분리하여 관리했습니다.

dog         : 유기견 기본 정보
dog_detail  : 구조 장소, 메모 등 상세 정보
dog_image   : 유기견 이미지 정보

이를 통해 기본 정보, 상세 정보, 이미지 정보를 독립적으로 관리할 수 있도록 설계했습니다.

4. 검색, 필터링, 정렬

목록 조회 API에서 Query Parameter를 활용하여 다양한 조회 조건을 처리했습니다.

* 검색: 이름, 품종, 구조 장소
* 필터링: 입양 상태
* 정렬: 최신순, 이름순, 나이순

5. 통계 조회

입양 상태별, 품종별 유기견 수를 집계하는 통계 API를 구현했습니다.

프론트엔드에서는 해당 데이터를 Chart.js와 연동하여 시각화할 수 있도록 구성했습니다.

⸻

API 요약

기능	Method	URL
유기견 등록	POST	/dogs
유기견 목록 조회	GET	/dogs
유기견 상세 조회	GET	/dogs/{id}
유기견 수정	PUT	/dogs/{id}
유기견 삭제	DELETE	/dogs/{id}
통계 조회	GET	/dogs/statistics

⸻

실행 방법

git clone https://github.com/사용자명/레포지토리명.git
cd 레포지토리명
./gradlew bootRun

실행 전 MySQL 데이터베이스 생성과 환경변수 설정이 필요합니다.

CREATE DATABASE petdb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_mysql_password
FILE_UPLOAD_DIR=./uploads

기본 실행 주소:

http://localhost:8081

⸻

환경변수 관리

본 프로젝트는 DB 계정 정보와 업로드 경로를 코드에 직접 작성하지 않고 환경변수로 관리합니다.

spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
file.upload-dir=${FILE_UPLOAD_DIR:./uploads}

⸻

프로젝트를 통해 경험한 점

* Spring Boot 기반 REST API 설계 및 구현
* MultipartFile을 활용한 다중 이미지 업로드 처리
* JdbcTemplate 기반 SQL 작성 및 데이터 접근 계층 구현
* MySQL 테이블 분리 설계 경험
* 검색, 필터링, 정렬 조건을 반영한 조회 로직 구현
* 통계 데이터를 API로 제공하고 프론트엔드에서 활용하는 흐름 경험
* GitHub 공개를 고려한 민감정보 관리 및 환경변수 설정 경험

⸻

개선 예정

* JPA 기반 리팩터링
* Swagger/OpenAPI 문서화
* 공통 예외 처리 및 에러 응답 포맷 통일
* 파일 확장자, MIME 타입, 파일 크기 검증 강화
* 이미지 삭제 시 실제 서버 파일도 함께 삭제하도록 개선
* 테스트 코드 추가
* AI API Key를 백엔드 환경변수로 관리하는 구조로 개선

⸻

보안 관련 안내

본 프로젝트에는 DB 비밀번호, API Key 등의 민감정보를 직접 포함하지 않습니다.

OpenAI API 연동 기능은 데모 목적의 기능이며, 현재는 사용자가 직접 API Key를 입력하는 방식으로 구성되어 있습니다. 실서비스 환경에서는 백엔드 서버에서 API Key를 환경변수로 관리하는 방식이 적절합니다.
