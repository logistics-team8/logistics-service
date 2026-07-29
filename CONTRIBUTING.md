# 협업 컨벤션


## 브랜치 전략
- main: 항상 배포 가능한 상태 유지 (운영 배포용)
- develop: 팀원들 기능이 합쳐지는 통합 브랜치, 신규 브랜치는 항상 여기서 분기
- feature/서비스명-기능명: 개인 작업 브랜치 (버그 수정·리팩토링·테스트도 이 안에서 처리, 커밋 prefix로 구분)
- 브랜치명은 전부 소문자, 영어 사용 (예: `feature/order-create`, `feature/orderitem-update`)

## 브랜치/기능명 작성 규칙
- feature/서비스명-동사 (영어)
- 동사는 CRUD 기준으로 통일:
    - 생성: create
    - 조회(단건): get
    - 조회(목록): list
    - 수정: update
    - 삭제: delete
- 예: `feature/order-create`, `feature/payment-update`

## Merge 프로세스
1. Issue 생성
2. feature 브랜치 생성 (develop에서 분기)
3. Draft PR 생성
4. 코드 작성 + 테스트 코드 작성 (필요 없는 경우 생략 가능)
5. Ready for review 전환 → 리뷰 요청
6. 리뷰 승인 → PR 생성자가 직접 병합 (Merge commit)
7. 병합된 브랜치 삭제

## 브랜치 보호 규칙
| 브랜치 | 규칙 |
|---|---|
| main | PR을 통해서만 병합 |
| develop | PR을 통해서만 병합, 리뷰 승인 3명 이상 |

## PR 규칙
- 제목: `[서비스명] 작업 내용` (예: `[order] 주문 생성 API 구현`, `[payment] 결제 승인 처리`)
- 여러 모듈 동시 수정 시: `[order,common] ...`
- 머지 방식: Merge commit

## 커밋 컨벤션 (Conventional Commits)
| 타입 | 설명 |
|---|---|
| feat | 새로운 기능 추가 |
| fix | 버그 수정 |
| docs | 문서 수정 |
| style | 코드 포맷팅 (로직 변경 없음) |
| refactor | 코드 구조/로직 개선 (동작은 동일) |
| test | 테스트 코드 추가/수정 |
| chore | 빌드 설정, 의존성 추가 등 |

## PR & Issue 템플릿
- PR 템플릿 적용 완료 (`.github/pull_request_template.md`)
- Issue 템플릿 적용 완료 (`.github/ISSUE_TEMPLATE/task.md`)

## 환경변수
- .env: 실제 비밀값 (커밋 금지, .gitignore 처리됨)
- .env.example: 필요한 키 이름만 명시한 예시 파일 (커밋 대상) — 아직 미작성

## PR 크기 가이드라인
- 하나의 PR엔 관련된 작업만 담기 (여러 기능을 한 PR에 몰아넣지 않기)
- 너무 커지면 리뷰하기 어려워지니, 적당한 단위로 나눠서 올리기

## 코드 포맷
- Spotless 사용 (자동 코드 스타일 정리)
- Gradle 설정: 미완료 (추가 예정)