<div align="center">
  
# 🚛 logistics-service
### 물류 관리 및 배송 시스템을 위한 MSA 기반 플랫폼


<br>

![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.7-6DB33F?logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-6DB33F?logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)

</div>

---

## 🛠 기술스택

| 구분 | 기술 |
|---|---|
| Backend | Java 21, Spring Boot 4.0.7, Spring Security7,Spring Cloud 2025.1.2, Spring Data JPA |
| Database | PostgreSQL 17, Redis 7.4-alpine |
| AI | Gemini gemini-3.5-flash  |
| DevOps | Docker, Docker Compose, 추후 작성 |

<br>

## 🏗 시스템 아키텍처
<img width="11043" height="7866" alt="image" src="https://github.com/user-attachments/assets/a8a2ec3b-756e-43cc-b124-4da7b7614840" />


<br><br>


## 서비스 구성
### Infrastructure
| 서비스 | 포트 | 역할 |
|---|---|---|
| Eureka Server | 8761 | 서비스 등록 및 디스커버리 |
| Config Server | 8888 | 서비스별 설정 중앙 관리 |
| API Gateway | 8080 | 요청 라우팅, JWT 인증·인가, 사용자 정보 헤더 전달 |


### Micro Service
| 서비스 | 포트 | 역할 | 담당자 |
|---|---|---|---|
| User | 8081 | 회원가입, 로그인, 사용자·권한·세션 관리 | 이강석 |
| Hub | 8082 | 허브 정보 및 허브 간 이동 경로 관리 | 안병규 |
| Company/Product | 8083 | 업체 및 상품 정보·재고 관리 | 권순혁 |
| Delivery | 8084 | 배송 정보 및 배송 담당자 관리 | 이재형 |
| Order | 8085 | 주문 생성·조회·취소 및 상품 재고 차감 요청 | 홍태규 |
| Notification | 8086 | Slack 알림 생성 및 발송 이력 관리 | 김서인 |

<br>

## 📂 프로젝트 구조
```
logistics-service/
├── libs/
│   └── common/                         # 공통 응답·오류·범용 유틸의 위치
├── infra/
│   ├── config-server/                  # 8888: 설정 제공
│   ├── eureka-server/                  # 8761: 서비스 등록·발견
│   ├── gateway/                        # 8080: 외부 요청 진입점
│   └── postgres/init/                  # 로컬 DB 스키마 초기화 SQL
├── apps/
│   ├── user-service/                   # 8081, users
│   ├── hub-service/                    # 8082, hubs
│   ├── company-product-service/        # 8083, company_products
│   ├── delivery-service/               # 8084, deliveries
│   ├── order-service/                  # 8085, orders
│   └── notification-service/           # 8086, notifications
├── config-repo/                        # Config Server가 제공하는 설정 원본
└── docker-compose.yml                  # 로컬 PostgreSQL 실행
```

<br>

## 💾 ERD
이미지

<br>

## 🚀 실행 방법
### 요구사항
- Java 21 이상
- Docker & Docker Compose

### 인프라 설정
```
docker-compose up -d
```

### 환경변수 설정
```
환경 변수 설정
```

### 서비스 빌드
```
빌드
```

### 3.서비스 실행
```
실행
```

### 4. URL
| 서비스 | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
<br>

## 📚 주요 기능
### 1. User
- 회원가입, 로그인, 로그아웃, 토큰 재발급
- JWT와 Redis를 활용한 인증 및 세션 관리
- 회원 가입 및 승인 시 Hub, Company 서비스 연동
- 사용자 정보 조회·수정 및 회원 탈퇴
- 관리자 회원 승인·거절 및 사용자 검색
- 배송 담당자 승인 시 Delivery 서비스 연동

### 2. Company & Product
- 업체 등록·조회·수정 및 삭제
- 업체별 상품 등록·조회·수정 및 삭제
- 상품 재고 관리
- 허브와 연계한 업체 소속 정보 관리

### 3. Hub
- 허브 등록·조회·수정 및 삭제
- 허브 간 이동 경로 관리
- 출발·도착 허브를 기반으로 배송 경로 제공

### 4. Order
- 주문 생성·조회·수정 및 취소
- 주문 상품 정보와 수량 관리
- 상품 서비스와 연동한 재고 차감 및 복구
- 주문 상태 및 처리 이력 관리
- 배송 및 알림 서비스 연동?

### 5. Notification
- 주문 및 배송 정보를 기반으로 알림 메시지 생성
- Slack을 통한 배송 알림 발송
- 알림 발송 상태와 발송 이력 관리
- Gemini gemini-3.5-flash를 활용한 알림 메시지 생성

### 6. Delivery
- 배송 생성·조회 및 상태 관리
- 허브 배송 및 업체 배송 담당자 관리
- 배송 담당자 순번 배정

<br>


## 🚨 트러블 슈팅
- 내용
<br>
