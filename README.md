<div align="center">

# 🚛 logistics-service

### 물류 관리와 배송을 위한 Spring Cloud 기반 MSA 플랫폼

</div>

---

## 프로젝트 소개

`logistics-service`는 생산 업체와 수령 업체 사이의 주문, 재고, 허브 이동, 배송과 알림을 여러 서비스가 협력해 처리하는 B2B 물류 플랫폼입니다. 각 도메인은 독립된 Spring Boot 애플리케이션과 PostgreSQL 스키마를 사용하며, 외부 요청은 API Gateway를 통해 전달됩니다.

현재 서비스 간 통신은 Eureka 기반 서비스 디스커버리와 OpenFeign REST 호출을 사용합니다. Gateway는 JWT와 Redis 세션을 검증해 사용자 문맥을 전달하고, 도메인 서비스는 공통 응답 형식과 OpenAPI 문서를 제공합니다. 아홉 JVM 서비스에는 Zipkin 분산 추적이 적용되어 있습니다.

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 4.0.7, Spring Security 7, Spring Data JPA |
| MSA | Spring Cloud 2025.1.2, Gateway, Config Server, Eureka, OpenFeign |
| Database | PostgreSQL 17, Redis 7.4 |
| API 문서 | springdoc-openapi 3.0.3, Gateway 통합 Swagger UI |
| AI와 알림 | Gemini 3.1 Flash Lite, Slack API |
| Observability | Spring Boot Actuator, Micrometer Tracing, Zipkin 3.6.1 |
| DevOps | Gradle, Docker, Docker Compose, GitHub Actions, OCIR, OCI Compute, Caddy |

## 시스템 아키텍처

주요 요청 흐름은 다음과 같습니다.

1. API Gateway가 JWT와 Redis 세션을 검증하고 사용자 문맥을 헤더로 전달합니다.
2. Gateway가 Eureka에서 서비스를 찾아 외부 API 요청을 라우팅합니다.
3. 도메인 서비스는 OpenFeign을 통해 필요한 다른 서비스의 내부 API를 호출합니다.
4. Config Server가 프로필별 공통 설정과 서비스별 설정을 제공합니다.
5. 모든 JVM 서비스가 trace context를 전파하고 Zipkin으로 span을 전송합니다.

### Infrastructure

| 구성 요소 | 포트 | 역할 |
| --- | ---: | --- |
| API Gateway | 8080 | 외부 요청 라우팅, JWT와 Redis 세션 검증, 사용자 문맥 전달, 통합 Swagger |
| Config Server | 8888 | `config-repo`의 공통 설정과 서비스별 설정 제공 |
| Eureka Server | 8761 | 서비스 등록과 디스커버리 |
| PostgreSQL | 5432 | 서비스별 스키마를 사용하는 관계형 데이터 저장소 |
| Redis | 6379 | 사용자 세션, Hub 캐시, 배송 담당자 순번 관리 |
| Zipkin | 9411 | 서비스 간 분산 추적 수집과 조회 |

### Microservices

| 서비스 | 포트 | 역할 | 담당자 |
| --- | ---: | --- | --- |
| User | 8081 | 회원가입, 인증, 사용자와 권한, Redis 세션 관리 | 이강석 |
| Hub | 8082 | 허브, 허브 간 경로, 최단 경로와 배송 계획 관리 | 안병규 |
| Company/Product | 8083 | 업체, 상품, 재고와 재고 처리 이력 관리 | 권순혁 |
| Delivery | 8084 | 배송, 배송 경로 이력과 배송 담당자 관리 | 이재형 |
| Order | 8085 | 주문 생성, 조회, 변경, 취소와 재고 및 배송 연동 | 홍태규 |
| Notification | 8086 | Gemini 배송 시한 계산, Slack 발송과 이력 관리 | 김서인 |

## 프로젝트 구조

```text
logistics-service/
├── libs/
│   └── common/                         # 공통 응답, 오류, 보안 principal, 유틸리티
├── infra/
│   ├── config-server/                  # 8888: 중앙 설정 제공
│   ├── eureka-server/                  # 8761: 서비스 등록과 발견
│   ├── gateway/                        # 8080: 외부 요청 진입점
│   └── postgres/init/                  # PostgreSQL 스키마 초기화 SQL
├── apps/
│   ├── user-service/                   # 8081, users 스키마
│   ├── hub-service/                    # 8082, hubs 스키마
│   ├── company-product-service/        # 8083, company_products 스키마
│   ├── delivery-service/               # 8084, deliveries 스키마
│   ├── order-service/                  # 8085, orders 스키마
│   └── notification-service/           # 8086, notifications 스키마
├── config-repo/                        # Config Server 설정 원본
├── deploy/                             # OCI Dev Compose, Caddy, 배포와 롤백 스크립트
├── .github/workflows/                  # 모듈 CI, ARM64 이미지 빌드와 OCI Dev 배포
├── docker-compose.yml                  # 로컬 인프라와 Gateway 구성
└── Dockerfile                          # Gradle 모듈별 멀티 스테이지 이미지 빌드
```

## 데이터 저장 구조

로컬 환경은 하나의 `logistics` 데이터베이스 안에서 서비스별 PostgreSQL 스키마를 분리합니다. 서비스는 다른 서비스의 스키마를 직접 조회하지 않고 내부 API로 데이터를 교환합니다.

| 스키마 | 주요 Aggregate와 테이블 |
| --- | --- |
| `users` | User, `p_users` |
| `hubs` | Hub, HubRoute, `p_hubs`, `p_hub_routes` |
| `company_products` | Company, Product, StockTransaction, `p_companies`, `p_products`, `p_stock_transactions` |
| `deliveries` | Delivery, DeliveryRouteHistory, DeliveryManager, `p_deliveries`, `p_delivery_route_histories`, `p_delivery_managers` |
| `orders` | Order, OrderItem, `p_orders`, `p_order_items` |
| `notifications` | SlackMessage, AiRequestLog, `p_slack_messages`, `p_ai_request_log` |

## 실행 방법

### 요구사항

- JDK 21
- Docker와 Docker Compose
- 저장소에 포함된 Gradle Wrapper

### 1. 로컬 인프라 실행

루트 Compose에서 PostgreSQL, Redis, Zipkin, Eureka와 Config Server를 먼저 실행합니다.

```bash
docker compose up -d --build postgres redis zipkin eureka-server config-server
```

루트 `docker-compose.yml`에는 Gateway가 정의되어 있지만 여섯 도메인 서비스는 포함되어 있지 않습니다. 아래 로컬 개발 절차에서는 도메인 서비스와 Gateway를 Gradle로 실행합니다. 전체 서비스 컨테이너 구성은 OCI Dev 배포용 `deploy/compose.dev.yml`에 있습니다.

### 2. 환경 변수 설정

Gateway와 User 서비스는 서로 다른 Base64 인코딩 HMAC 키를 사용합니다. 실제 키, 토큰과 비밀번호는 저장소에 커밋하지 않습니다.

```bash
export JWT_ACCESS_SECRET="<Base64로 인코딩한 Access Token HMAC 키>"
export JWT_REFRESH_SECRET="<Base64로 인코딩한 Refresh Token HMAC 키>"
```

| 환경 변수 | 필요 시점 | 기본값 또는 용도 |
| --- | --- | --- |
| `JWT_ACCESS_SECRET` | Gateway, User 실행 | Access Token 서명과 검증 키, 기본값 없음 |
| `JWT_REFRESH_SECRET` | Gateway, User 실행 | Refresh Token 서명과 검증 키, 기본값 없음 |
| `SLACK_BOT_TOKEN` | Notification 실행과 실제 발송 | Slack Bot Token, 기본값 없음 |
| `GEMINI_API_KEY` | Notification 실행과 AI 호출 | Gemini API Key, 기본값 없음 |
| `NAVER_MAPS_API_KEY_ID` | Hub 기본 경로 초기화 사용 시 | Naver Maps API Key ID |
| `NAVER_MAPS_API_KEY` | Hub 기본 경로 초기화 사용 시 | Naver Maps API Key |
| `HUB_ROUTE_DEFAULT_DATA_ENABLED` | Hub 기본 경로 초기화 사용 시 | 기본값 `false` |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | DB 접속 변경 시 | 기본값 `localhost`, `5432`, `logistics` |
| `DB_USERNAME`, `DB_PASSWORD` | DB 계정 변경 시 | 로컬 기본값 `logistics` |
| `REDIS_HOST`, `REDIS_PORT` | Redis 접속 변경 시 | 기본값 `localhost`, `6379` |
| `CONFIG_SERVER_URL` | Config Server 주소 변경 시 | 기본값 `http://localhost:8888` |
| `ZIPKIN_ENDPOINT` | Zipkin 주소 변경 시 | 로컬 기본값 `http://localhost:9411/api/v2/spans` |

Notification 서비스를 실행하려면 `SLACK_BOT_TOKEN`과 `GEMINI_API_KEY`도 별도로 설정해야 합니다. Hub 기본 경로 초기화를 활성화할 때만 Naver Maps 키가 필요합니다.

### 3. 빌드와 테스트

```bash
./gradlew build
./gradlew test
```

특정 모듈만 확인할 때는 다음과 같이 모듈 경로를 지정합니다.

```bash
./gradlew :apps:hub-service:build
```

### 4. Gateway와 도메인 서비스 실행

각 명령은 별도 터미널에서 실행합니다. 애플리케이션은 기본적으로 `local` 프로필과 `http://localhost:8888`의 Config Server를 사용합니다.

```bash
./gradlew :infra:gateway:bootRun
./gradlew :apps:user-service:bootRun
./gradlew :apps:hub-service:bootRun
./gradlew :apps:company-product-service:bootRun
./gradlew :apps:delivery-service:bootRun
./gradlew :apps:order-service:bootRun
./gradlew :apps:notification-service:bootRun
```

필요한 서비스만 실행할 수 있지만 서비스 간 호출이 포함된 기능은 해당 의존 서비스도 Eureka에 등록되어 있어야 합니다.

### 5. 접속 URL

| 대상 | URL |
| --- | --- |
| API Gateway | http://localhost:8080 |
| 통합 Swagger UI | http://localhost:8080/swagger-ui.html |
| Eureka Dashboard | http://localhost:8761 |
| Config Server Health | http://localhost:8888/actuator/health |
| Zipkin UI | http://localhost:9411 |
| User | http://localhost:8081 |
| Hub | http://localhost:8082 |
| Company/Product | http://localhost:8083 |
| Delivery | http://localhost:8084 |
| Order | http://localhost:8085 |
| Notification | http://localhost:8086 |

## API 경계

외부 클라이언트는 Gateway의 공개 경로를 사용하고, `/internal/**` 경로는 서비스 간 호출에만 사용합니다. 자세한 요청과 응답 명세는 통합 Swagger UI에서 확인할 수 있습니다.

| 서비스 | Gateway 공개 경로 |
| --- | --- |
| User | `/api/v1/users/**`, `/api/v1/auth/**`, `/api/v1/admin/**` |
| Hub | `/api/v1/hubs/**`, `/api/v1/hub-routes/**` |
| Company/Product | `/api/v1/companies/**`, `/api/v1/products/**` |
| Delivery Manager | `/api/v1/delivery/**` |
| Order | `/api/v1/orders/**` |
| Notification | `/api/slack-messages/**` |

Delivery 본체 컨트롤러는 `/api/v1/deliveries/**`를 제공하지만 현재 Gateway의 Delivery predicate는 `/api/v1/delivery/**`이므로 해당 경로를 Gateway에서 라우팅하지 않습니다. 내부 API는 `/internal/v1/**` 또는 Notification의 `/internal/notifications/**`에 있으며 Gateway 공개 경로에 포함되지 않습니다.

## 주요 기능

### User

- 회원가입, 로그인, 로그아웃과 Access Token 재발급
- JWT 발급과 검증, Redis 기반 로그인 세션과 Refresh Token 재사용 방지
- 내 정보 조회, 수정과 회원 탈퇴
- 관리자 사용자 생성, 조회, 검색, 수정과 삭제
- 가입 승인, 거절과 승인 대기 사용자 조회
- 사용자 역할에 필요한 Hub 또는 Company 검증과 배송 담당자 승인 시 Delivery 서비스 동기화

### Company/Product

- 업체와 상품 등록, 단건 조회, 검색, 수정과 논리 삭제
- 생산 업체, 수령 업체와 소속 Hub 정보 관리
- 주문 생성과 취소를 위한 상품 다건 조회, 재고 차감과 복원 내부 API
- 주문 ID와 처리 유형을 기준으로 재고 변경을 중복 처리하지 않는 StockTransaction 기록

### Hub

- Hub와 HubRoute 등록, 단건 조회, 검색, 수정과 논리 삭제
- 우선순위 큐 기반 Dijkstra 최단 경로 탐색
- Hub, HubRoute와 최단 경로 응답의 Redis 캐시와 변경 시 캐시 무효화
- Naver Geocoding과 Directions를 이용한 기본 Hub 경로 거리와 시간 초기화
- Delivery 서비스가 사용하는 순서형 허브 이동 계획 내부 API

### Order

- `Idempotency-Key` 기반 주문 생성 중복 방지와 요청 해시 검증
- 사용자, 수령 업체와 상품 다건 검증
- Product 서비스 재고 차감 후 Delivery 서비스 배송 생성 요청
- 배송 생성 실패 시 재고 복원과 주문 실패 상태 처리
- 주문 단건 조회, 검색, 수정과 논리 삭제
- 전체 주문과 개별 주문 상품 취소, 재고 복원과 연결 배송 취소

### Delivery

- 주문 ID 기준 멱등 배송 생성과 Hub 배송 계획 조회
- 배송 목록 검색, 단건 상세와 순서형 배송 경로 이력 조회
- 배송 상태와 구간별 경로 상태 전이, 주문 취소에 따른 배송 취소
- 권한 범위를 적용한 배송 논리 삭제
- Hub 배송 담당자와 업체 배송 담당자 조회, 수정과 논리 삭제
- Redis 분산 락과 마지막 순번을 이용한 배송 담당자 순번 배정

### Notification

- 주문 알림 내부 요청을 Application Event로 비동기 처리
- Gemini 3.1 Flash Lite를 이용한 최종 발송 시한 계산과 AI 요청 이력 저장
- 사용자 서비스에서 Slack ID를 조회해 메시지 생성과 발송
- Slack 발송 성공, 실패 상태와 재시도 이력 관리
- Slack 메시지 단건 조회와 조건 검색

Order 서비스의 `NotificationPort`와 `NotificationFeignClient`는 현재 구현이 비어 있으므로 주문 생성에서 Notification 호출까지의 연결은 완료된 기능으로 포함하지 않습니다.

## CI/CD

- GitHub Actions가 변경 모듈별 Gradle 빌드와 테스트를 실행합니다.
- Dev Candidate workflow가 아홉 JVM 서비스의 `linux/arm64` 이미지를 빌드해 OCIR에 digest 기준으로 게시합니다.
- 이미지 digest와 commit SHA를 후보 manifest로 만들고 OCI Object Storage에 저장합니다.
- OCI Compute의 self-hosted runner가 `deploy/compose.dev.yml`과 Caddy를 사용해 후보를 배포하고 실패 시 이전 버전으로 롤백합니다.
- 현재 OCI 환경은 Dev와 시연용 단일 배포 대상이며 별도의 고가용성 Production 환경은 포함하지 않습니다.
