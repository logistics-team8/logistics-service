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
| Database | PostgreSQL 17, Redis |
| AI | 미확정 |
| DevOps | Docker, Docker Compose, 추후 작성 |

<br>

## 🏗 시스템 아키텍처
<img width="11043" height="7866" alt="image" src="https://github.com/user-attachments/assets/a8a2ec3b-756e-43cc-b124-4da7b7614840" />


<br><br>


## 서비스 구성
### Infrastructure
| 서비스 | 포트 | 역할 |
|---|---|---|
| Eureka Server | 8761 | 내용 |
| Config Server | 8888 | 내용 |
| API Gateway | 8080 | 라우팅, JWT 인증 |


### Micro Service
| 서비스 | 포트 | 역할 | 담당자 |
|---|---|---|---|
| User | 0000 | 내용 | 이강석 |
| Company/Product | 0000 | 내용 | 권순혁 |
| Hub | 0000 | 내용 | 안병규 |
| Order | 0000 | 내용 | 홍태규 |
| Notification | 0000 | 내용 | 김서인 |
| Delivery | 0000 | 내용 | 이재형 |

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
- 내용

### 2. Company & Product
- 내용

### 3. Hub
- 내용

### 4. Order
- 내용

### 5. Notification
- 내용

### 6. Delivery
- 내용

<br>


## 🚨 트러블 슈팅
- 내용
<br>
