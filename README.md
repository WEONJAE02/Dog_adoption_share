# PET ROOM

유기견 입양 정보를 등록하고, 조회하며, 사용자 조건에 맞는 반려견을 추천받을 수 있는 유기견 입양 정보 공유 서비스입니다.

회원이 직접 등록한 유기견뿐 아니라 공공데이터(국가동물보호정보시스템)로 전국 보호소의 실제 유기견 정보를 함께 보여주고, 커뮤니티 게시판에서 입양 후기와 정보를 나눌 수 있습니다.

---

## 주요 기능

### 입양 정보
- **전국 보호소 유기견**: 공공데이터포털 API로 전국 보호소의 유기견을 지역(시도)·상태(보호중/공고중)별로 조회. 사진 카드, 상세 정보(공고번호, 특징, 보호소 연락처), 최근 30일 통계(구조 수, 입양률, 안락사율)
- **회원이 등록한 유기견**: 등록·수정·삭제(작성자 본인만), 여러 장 사진 업로드, 검색·필터·정렬, 상태별·품종별 통계 차트
- 두 목록은 상단 선택 버튼으로 하나씩 볼 수 있습니다

### 로그인
- 이메일 회원가입 / 로그인 (세션 방식, 비밀번호 BCrypt 저장)
- 카카오 로그인, 네이버 로그인 (OAuth2)
- 조회는 누구나, 등록·수정·삭제는 로그인 필요

### 커뮤니티 게시판
- 말머리(자유, 정보공유, 입양후기, 질문, 실종·목격), 사진 최대 5장, 검색, 페이지, 조회수
- 댓글과 대댓글(2단계), 작성자만 수정·삭제

### Shop (반려견 용품 중고거래)
- 회원끼리 사료·간식·장난감·의류·식기 등 반려견 용품을 사고팔기 (가격 0원은 "나눔")
- 카테고리, 검색(상품명·설명·지역), 거래 가능만 보기, 정렬(최신·인기·가격), 관심 목록, 내 판매 상품
- 거래 상태(판매중 / 예약중 / 거래완료), 관심(찜), 구매 문의 → 판매자가 문의함에서 답변
- 신고 누적 시 숨김, 회원 탈퇴 시 판매 상품·문의·관심 삭제. 동물 분양·판매는 금지

### 건강체크 (개발 중)
- **내 반려동물**: 이름·종류(개/고양이)·품종·성별·중성화·태어난 해·몸무게·특이사항(지병, 알러지 등)·사진 1장 등록. 1인 최대 10마리, 본인만 조회·수정
- **건강체크**: 반려동물·부위 선택 → 영상(브라우저에서 장면 8장 추출) 또는 사진(최대 5장) → 증상·메모 → 긴급도(바로 병원/빠른 진료/지켜보기)·소견·추천 진료 분야·집에서 할 일·병원에서 물어볼 것
  - 사진은 브라우저에서 긴 변 1024px JPEG로 줄여서 올립니다
  - 응급 증상(호흡 곤란, 경련 등)을 고르면 분석 결과와 상관없이 서버가 "바로 병원"으로 올립니다
  - 하루 10번 제한 (`app.health.daily-limit`). 기록을 지워도 횟수는 줄지 않습니다
  - **AI 분석 (Claude)**: `ANTHROPIC_API_KEY`가 있으면 장면 사진과 반려동물 정보를 Claude(`claude-opus-5-5`)에 보내 정해진 형식(구조화 출력)으로 결과를 받습니다. 키가 없으면 샘플 분석기로 동작하고 비용이 들지 않습니다 (화면에 "샘플"로 표시)
- **추천 주치의 (샘플 데이터)**: 결과의 진료 분야와 내 지역(시도·시군구), 긴급도로 샘플 수의사 26명 중 상위 3명을 추천하고 이유를 함께 보여줍니다. AI는 진료 분야만 정하고, 순위는 코드의 점수로 정합니다
  - 점수(최대 100): 주 분야 일치 50 / 함께 보는 분야 25, 같은 시도 10 + 같은 시군구 10, 해당 분야 진료 건수 최대 15, 평점 최대 10, 응급일 때 24시간 진료 5
  - 수의사·병원은 모두 가상 데이터("펫룸 ○○")이며 화면에 "샘플"로 표시합니다
- **지난 체크와 비교**: 같은 반려동물·같은 부위의 바로 이전 체크와 나란히 보여주고, 긴급도로 "나아졌어요 / 비슷해요 / 더 급해졌어요"를 표시합니다. 기록 목록은 반려동물·부위로 거를 수 있습니다
- 다음 단계: 공공데이터 실제 동물병원 위치

### AI 기능 (OpenAI)
- 유기견 정보 기반 입양 홍보 문구 생성
- 사용자 조건(활동량, 알레르기, 크기 등) 기반 입양 추천
- 사용자가 직접 입력한 API Key를 브라우저(localStorage)에 저장해 사용합니다

---

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| Frontend | HTML, CSS, JavaScript (단일 페이지 `src/index.html`), Fetch API, Chart.js, Kakao Map SDK |
| Backend | Java 17, Spring Boot 3.5, Spring Web, Spring JDBC (JdbcTemplate), Spring Security, OAuth2 Client |
| Database | MySQL 8 |
| 외부 API | 카카오/네이버 로그인, 공공데이터포털 구조동물 조회 서비스, OpenAI API |
| Build | Gradle |

---

## 로컬에서 실행하기

### 1. 준비물
- **JDK 17** (`build.gradle`이 Java 17 툴체인을 사용합니다. JDK 21만 있으면 빌드가 실패하니 17을 설치하세요)
- **Docker** (MySQL 실행용)
- **Python 3** 또는 아무 정적 파일 서버 (프론트 실행용)

### 2. MySQL 실행
비밀번호는 원하는 값으로 바꾸고, 아래 3번의 `DB_PASSWORD`에 같은 값을 넣으세요.

```bash
docker run -d --name petroom-mysql \
  -p 127.0.0.1:3307:3306 \
  -e MYSQL_ROOT_PASSWORD=원하는비밀번호 \
  -e MYSQL_DATABASE=petdb \
  -e TZ=Asia/Seoul \
  mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
```

테이블은 서버가 처음 뜰 때 `src/main/resources/schema.sql`로 자동 생성됩니다.

### 3. 환경변수 설정
```bash
cp .env.example .env.local
```
`.env.local`을 열어 값을 채웁니다. 이 파일은 `.gitignore`에 등록돼 있어 커밋되지 않습니다.

| 변수 | 필수 | 설명 |
|---|---|---|
| `DB_USERNAME`, `DB_PASSWORD` | 필수 | MySQL 계정 (위 Docker 기준 `root` / 정한 비밀번호) |
| `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET` | 선택 | 카카오 REST API 키, 클라이언트 시크릿 |
| `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET` | 선택 | 네이버 애플리케이션 Client ID / Secret |
| `ANTHROPIC_API_KEY` | 선택 | 건강체크 AI 분석용 Claude API 키. 비워 두면 샘플 결과만 나옵니다. 사용한 만큼 과금되니 [Anthropic 콘솔](https://console.anthropic.com)에서 월 사용 한도를 걸어 두세요 |
| `HEALTH_AI_MODEL`, `HEALTH_DAILY_LIMIT` | 선택 | 건강체크 모델(기본 `claude-opus-5-5`), 회원당 하루 횟수(기본 10) |
| `ANIMAL_API_KEY` | 선택 | 공공데이터포털 "국가동물보호정보시스템 구조동물 조회 서비스" 인증키 (Decoding) |
| `DB_URL`, `UPLOAD_DIR`, `PRIVATE_UPLOAD_DIR`, `FRONTEND_URL` | 선택 | 기본값 대신 다른 DB, 공개·비공개 업로드 폴더, 프론트 주소를 쓸 때 |

선택 항목을 비워 두면 해당 기능만 꺼집니다. (소셜 로그인 버튼은 안내 문구만, 전국 보호소 영역은 안내 문구만 표시)

### 4. 백엔드 실행 (포트 8081)
```bash
set -a && . ./.env.local && set +a
./gradlew bootRun
```

### 5. 프론트 실행 (포트 5500)
다른 터미널에서:
```bash
cd src
python3 -m http.server 5500 --bind 127.0.0.1
```

### 6. 접속
http://localhost:5500/index.html

업로드한 사진은 두 폴더에 나눠 저장됩니다(둘 다 커밋되지 않음).
- `uploads/`: 유기견·커뮤니티·Shop 사진. `/uploads/파일명`으로 누구나 볼 수 있습니다
- `private-uploads/`: 내 반려동물 사진과 건강체크 장면. 공개 주소가 없고, 로그인한 본인만 `/pets/{id}/image`, `/health-checks/{id}/frames/{n}`으로 받을 수 있습니다. 예전에 `uploads/`에 저장된 반려동물·건강체크 사진은 서버를 시작할 때 자동으로 옮겨집니다

### 소셜 로그인 키를 직접 발급받을 때
각 개발자 콘솔에 아래 주소를 등록해야 합니다.

| 제공자 | 등록 위치 | 주소 |
|---|---|---|
| 카카오 | 앱 설정 > 플랫폼 키 > REST API 키 > 카카오 로그인 리다이렉트 URI | `http://localhost:8081/login/oauth2/code/kakao` |
| 네이버 | API 설정 > 로그인 오픈 API 서비스 환경 > PC 웹 | 서비스 URL `http://localhost:5500`, Callback URL `http://localhost:8081/login/oauth2/code/naver` |

- 카카오: 카카오 로그인 활성화, 동의항목에서 닉네임 설정, 클라이언트 시크릿 활성화가 필요합니다.
- 네이버: 검수 전("개발 중")에는 앱 등록자와 멤버관리에 추가한 계정만 로그인할 수 있습니다.

---

## API 요약

| 구분 | 메서드 | 경로 | 로그인 |
|---|---|---|---|
| 인증 | POST | `/auth/signup`, `/auth/login`, `/auth/logout` | - |
| 인증 | GET | `/auth/me` (로그인 상태 + 사용 가능한 소셜 로그인) | - |
| 소셜 로그인 | GET | `/oauth2/authorization/{kakao\|naver}` | - |
| 회원 등록 유기견 | GET | `/dogs`, `/dogs/{id}`, `/dogs/statistics` | - |
| 회원 등록 유기견 | POST / PUT / DELETE | `/dogs`, `/dogs/{id}` (multipart: `dog` JSON + `images`) | 필요 (수정·삭제는 작성자만) |
| 전국 보호소 유기견 | GET | `/shelter/regions`, `/shelter/animals`, `/shelter/stats` | - |
| 게시판 | GET | `/posts`, `/posts/categories`, `/posts/{id}` | - |
| 게시판 | POST / PUT / DELETE | `/posts`, `/posts/{id}` (multipart: `post` JSON + `images`) | 필요 (수정·삭제는 작성자만) |
| 댓글 | POST / DELETE | `/posts/{id}/comments`, `/posts/{id}/comments/{commentId}` | 필요 |
| Shop | GET | `/shop/meta`, `/shop/items`, `/shop/items/{id}` | - (`filter=liked\|mine`은 필요) |
| Shop | POST / PUT / DELETE | `/shop/items`, `/shop/items/{id}` (multipart: `item` JSON + `images`) | 필요 (수정·삭제는 판매자만) |
| Shop | PUT / POST / DELETE | `/shop/items/{id}/status`, `/shop/items/{id}/like` | 필요 |
| 구매 문의 | POST | `/shop/items/{id}/inquiries` | 필요 |
| 내 반려동물 | GET / POST / PUT / DELETE | `/pets`, `/pets/{id}` (multipart: `pet` JSON + `image`) | 필요 (본인만) |
| 건강체크 | GET | `/health-checks/meta`, `/health-checks?petId=&bodyPart=`, `/health-checks/{id}`, `/health-checks/{id}/compare` | 필요 (본인만) |
| 건강체크 | POST / DELETE | `/health-checks` (multipart: `check` JSON + `frames`), `/health-checks/{id}` | 필요 (본인만) |
| 주치의 추천 | GET | `/vets/recommend?specialty=&sido=&sigungu=&urgency=`, `/vets/regions` | - |
| 구매 문의 | GET / POST | `/inquiries/shop/received`, `/inquiries/shop/sent`, `/inquiries/shop/{id}/reply` | 필요 |

---

## 프로젝트 구조

```
src/
├── index.html                      # 프론트엔드 (단일 페이지)
└── main/
    ├── java/org/likelion/hsu/db_project/
    │   ├── Config/                 # Security, CORS, 소셜 로그인, 정적 파일, DB 마이그레이션
    │   ├── Controller/             # Auth, Dog, Post(게시판), Shelter(공공데이터)
    │   ├── Service/                # 비즈니스 로직, 이미지 저장, 공공데이터 연동
    │   ├── Repository/             # JdbcTemplate 기반 DB 접근
    │   ├── Entity/  Dto/  Security/
    └── resources/
        ├── application.properties
        └── schema.sql              # 테이블 정의 (기동 시 자동 생성)
```

---

## 협업 규칙

- `main`은 PR로만 합칩니다(보호 규칙 설정됨). 기능별 브랜치(`feature/...`)를 만들어 PR을 올려 주세요.
- API 키, 비밀번호는 `.env.local`에만 둡니다. 코드, 이슈, PR, 메신저에 붙여 넣지 마세요.
- 공공데이터 자료를 화면에 쓸 때는 출처(농림축산식품부 국가동물보호정보시스템)를 표기합니다.

---

## 담당 역할

이 프로젝트에서 저는 프론트엔드 파트를 담당했습니다.

### 화면 UI 구현

- PET ROOM 메인 화면 구성
- 유기견 목록 카드 UI 구현
- 검색, 필터, 정렬 툴바 구현
- 등록/수정 모달 UI 구현
- 상세 조회 모달 및 이미지 갤러리 UI 구현
- 반응형 레이아웃 적용

### API 연동

- `fetch` 기반 REST API 연동
- 유기견 목록 조회 API 연동
- 유기견 상세 조회 API 연동
- 유기견 등록, 수정, 삭제 API 연동
- 통계 조회 API 연동
- FormData를 활용한 이미지 업로드 요청 처리

### 데이터 시각화

- Chart.js를 활용한 상태별 유기견 현황 그래프 구현
- Chart.js를 활용한 품종별 유기견 현황 그래프 구현
- 데이터 변경 후 차트 자동 갱신 처리

### 사용자 경험 개선

- 이미지 미리보기 기능 구현
- 상세 이미지 갤러리 및 썸네일 전환 기능 구현
- 삭제 전 확인 메시지 처리
- 등록, 수정, 삭제 후 Toast 메시지 출력
- 검색/필터 변경 시 즉시 목록 갱신
- 이미지 캐싱 문제를 줄이기 위한 쿼리 파라미터 처리

### AI 기능 연동

- OpenAI API를 활용한 입양 홍보 문구 생성 기능 구현
- 사용자 조건 기반 AI 추천 기능 구현
- 추천 결과와 추천 이유를 카드 형태로 표시
- API Key를 localStorage에 저장하여 데모 환경에서 활용 가능하도록 처리
