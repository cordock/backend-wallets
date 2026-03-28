# Sentbe Wallets

대규모 트래픽 상황을 가정한 월렛 출금 API 과제 구현입니다.  
핵심 목표는 다음 3가지입니다.

- 월렛 잔액 관리 및 입출금 내역 저장
- 동일 `transactionId` 기반 멱등성 보장
- 동시 요청 환경에서 잔액 무결성 보장

## 목차

- [1. 기술 스택](#1-기술-스택)
- [2. 실행 방법](#2-실행-방법)
- [3. DB 세팅 및 초기 데이터](#3-db-세팅-및-초기-데이터)
- [4. API 명세](#4-api-명세)
- [5. 동시성 제어 설계 결정](#5-동시성-제어-설계-결정)
- [6. 트랜잭션/예외 처리 설계 결정](#6-트랜잭션예외-처리-설계-결정)
- [7. 동시성 테스트](#7-동시성-테스트)

## 1. 기술 스택

- Java 21
- Spring Boot 3.5.7
- Spring Data JPA + QueryDSL
- PostgreSQL 15 (Docker Compose)
- Swagger(OpenAPI)
- JUnit5 (통합 테스트)

## 2. 실행 방법

### 2.1 DB 실행 (PostgreSQL)

```bash
docker compose up -d
```

### 2.2 API 서버 실행

```bash
./gradlew bootRun
```

또는

```bash
./gradlew bootJar
java -jar build/libs/*.jar
```

### 2.3 테스트 실행

```bash
./gradlew test
```

## 3. DB 세팅 및 초기 데이터

### 3.1 `application.yml` 핵심 설정

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create
  sql:
    init:
      mode: always
```

애플리케이션 시작 시 테이블을 생성하고 `data.sql`을 자동 실행합니다.

### 3.2 초기 데이터

초기 데이터 파일: [data.sql](src/main/resources/data.sql)

- `wallet` 1건
- `wallet_transaction` 11건 (페이징 테스트)

### 3.3 설정 선택 배경 및 한계

| 구분 | 선택 | 이유 |
|---|---|---|
| 과제 검증 편의성 | `ddl-auto=create` + `sql.init.mode=always` | 1회 실행만으로 스키마/초기데이터 즉시 재현 가능 |
| 구현 우선순위 | 테스트 DB 별도 분리 미적용 | 인프라 복잡도보다 동시성/멱등성 핵심 로직 구현을 우선 |

#### 현재 구성의 한계

- 앱 재시작 시 데이터가 초기화됨
- 로컬/테스트 환경 완전 미분리 시 데이터 오염 가능

#### 실무 확장 시 개선 방향

- 프로파일별 DB(또는 스키마) 분리 (`local`, `test`, `prod`)
- `ddl-auto`를 `validate/update`로 전환
- Flyway/Liquibase 및 환경별 시드 전략 적용

## 4. API 명세

Swagger UI:

- `http://localhost:8080/swagger-ui.html`

OpenAPI JSON:

- `http://localhost:8080/v3/api-docs`

### 4.1 출금

- `POST /wallets/{id}/withdrawals`

요청 예시:

```json
{
  "amount": 1000,
  "transactionId": "TX_20260328_00001"
}
```

### 4.2 입출금 내역 조회 (페이지네이션)

- `GET /wallets/{id}/transactions?page=0&size=10`

정렬:

- `id DESC`

응답은 공통 응답 래퍼(`CustomResponse`)를 통해 내려가며, 페이지 응답의 경우 `pageInfo`가 포함됩니다.

## 5. 동시성 제어 설계 결정

### 5.1 핵심 전략 요약

| 구분 | 적용 기술 | 선택 이유 |
|---|---|---|
| 잔액 무결성 | Pessimistic Write Lock | Lost Update 방지 및 강한 일관성(Strong Consistency) 확보 |
| 중복 방지 | Composite Unique Key | `wallet_id + transaction_id` 기반 DB 레벨 최종 방어선 구축 |
| 멱등성 보장 | Double-Check Locking | 락 획득 전/후 재검증으로 TOCTOU 레이스 컨디션 해결 |

### 5.2 상세 설계 근거

- 비관적 락(Pessimistic Lock)  
  낙관적 락은 충돌 시 재시도가 필요하고, 재시도 시점에는 이미 다른 트랜잭션이 정산을 완료해 데이터 상태가 바뀌어 있을 수 있어 처리 복잡도와 불확실성이 커집니다. 본 과제는 정합성 우선을 목표로, `FOR UPDATE` 기반의 비관적 락으로 동일 월렛에 대한 동시 갱신을 직렬화했습니다.

- DB 유니크 키(Composite Unique Key)  
  애플리케이션 로직만으로는 경쟁 상황에서 중복 삽입을 완전히 차단하기 어려워, `(wallet_id, transaction_id)` 유니크 제약을 최종 방어선으로 사용했습니다.

- Double-Check Locking  
  멱등성 검증은 `check-then-act` 구조라 레이스가 생길 수 있으므로, 락 획득 전 1차 확인 후 락 획득 뒤 2차 확인으로 `TOCTOU` 구간을 줄였습니다.

### 5.3 트레이드오프

- 장점
  - 잔액 무결성과 멱등성 보장에 유리
  - 분산 환경에서도 DB 레벨 제약으로 일관된 결과 확보
- 단점
  - 락 경합 시 지연시간 증가 가능
  - 트랜잭션/커넥션 사용량 증가로 처리량 저하 가능

### 5.4 우려사항 및 향후 대책

- 우려사항
  - 고부하 구간에서 락 대기와 DB 병목 가능성
- 향후 대책
  - 1단계: Redis 분산락으로 동일 walletId 요청을 DB 밖에서 직렬화해 락 경합 자체를 줄임
  - 2단계: 트래픽이 더 커지면 walletId 기준으로 요청을 큐에 넣고 순차 처리하는 구조 검토

## 6. 트랜잭션/예외 처리 설계 결정

### 6.1 트랜잭션 전파 전략 (`REQUIRES_NEW`)

- 목적  
  출금 실패(잔액 부족 등) 시에도 해당 내역을 멱등성 재현의 근거로 남기기 위함입니다.

- 구조  
  메인 비즈니스 로직이 롤백되더라도, 실패 이력 저장 로직은 독립된 트랜잭션에서 커밋되도록 분리했습니다.

### 6.2 리스크 관리 및 향후 대책
| 우려사항 | 대응 전략 및 향후 개선 방향                                                                                  |
|---|---------------------------------------------------------------------------------------------------|
| 커넥션 풀 고갈 | - `REQUIRES_NEW` 경로에서 스레드당 커넥션 2개 점유 발생, 동시 요청 증가 시 풀 고갈 리스크. <br>- 대응: 실패 이력 캐시 선기록/후 DB 비동기 적재  |
| DB 병목 현상 | - 고부하 구간에서 비관적 락 경합으로 대기 시간 증가 가능. <br>- 대응: 인덱스/쿼리 튜닝, 읽기/쓰기 부하 분리 우선 적용, 필요 시 메시지 큐 기반 비동기화 확장. |

## 7. 동시성 테스트

테스트 클래스:  
`src/test/java/com/sentbe/wallets/domain/wallet/service/WalletConcurrencyIntegrationTest.java`

### 7.1 테스트 시나리오 요약

모든 테스트는 `SoftAssertions`를 사용하여 응답 결과와 DB 상태를 동시에 전수 검증합니다.

| 테스트 시나리오 | 스레드 수 | 초기 잔액 | 요청 금액(건당) | 기대 결과 (성공/실패) |
|---|---:|---:|---:|---|
| 멱등성 성공 (동일 ID 중복 요청) | 100 | 100,000 | 1,000 | 성공 1건 반영, 나머지는 동일 결과 반환 |
| 멱등성 실패 (동일 ID 중복 요청) | 20 | 100,000 | 101,000 | 실패 1건 반영, 나머지는 동일 결과 반환 |
| 잔액 정합성 (충분한 잔액) | 100 | 100,000 | 1,000 | 성공 100건, 최종 잔액 0원 |
| 잔액 제한 (부족한 잔액) | 20 | 5,000 | 1,000 | 성공 5건 / 실패 15건, 잔액 음수 방지 |

> 시나리오 2, 4는 `REQUIRES_NEW`로 인해 커넥션을 2개 점유하므로 스레드 수를 20개로 제한했습니다.

### 7.2 테스트 결과

> 동시성 제어 미적용 결과는 브랜치 `repro/race-condition-without-lock`에서 재현했습니다.

### 시나리오 1. 동일 transactionId 동시 요청 — 성공 멱등성

공통 조건 및 기대 결과는 `7.1 테스트 시나리오 요약`을 따릅니다.

| 구분 | 결과 요약 | 판정 |
|---|---|----|
| 동시성 제어 전 | 응답 수 불일치 (기대 100, 실제 69) | 실패 |
| 동시성 제어 후 | 성공=100, 예상외실패=0, 적재(SUCCESS)=1, 최종잔액=99,000 | 성공 |

<details>
<summary>근거 로그</summary>

**동시성 제어 전**
```text
Multiple Failures (1 failure)
-- failure 1 --
Expected size: 100 but was: 69 in:
[WalletWithdrawalResDto[walletId=1, transactionId=TX_20260328_00000, status=SUCCESS, amount=1000, remainBalance=99000, errorCode=null],
    WalletWithdrawalResDto[walletId=1, transactionId=TX_20260328_00000, status=SUCCESS, amount=1000, remainBalance=99000, errorCode=null],
    WalletWithdrawalResDto[walletId=1, transactionId=TX_20260328_00000, status=SUCCESS, amount=1000, remainBalance=99000, errorCode=null],
    (생략)
```

**동시성 제어 후**
```text
2026-03-29T00:17:11.745+09:00  INFO 1108 --- [    Test worker] w.d.w.s.WalletConcurrencyIntegrationTest : 성공=100, 잔액부족실패=0, 예상외실패=0, 적재(SUCCESS)=1, 적재(FAILED)=0, 최종잔액=99000
```
</details>

---

### 시나리오 2. 동일 transactionId 동시 요청 — 실패 멱등성

공통 조건 및 기대 결과는 `7.1 테스트 시나리오 요약`을 따릅니다.

> 잔액 부족 실패는 락 획득 자체가 불필요한 경로입니다.  
> 어떤 스레드가 먼저 실행되더라도 `잔액 부족 -> 실패`로 귀결되므로  
> 동시성 제어 전/후 결과가 동일합니다.  
> 단, 멱등성 보장(실패 이력 1건 저장 + 동일 실패 재응답)은 `(wallet_id, transaction_id)` 유니크 제약에 의존합니다.

| 구분 | 결과 요약                                                | 판정 |
|---|------------------------------------------------------|----|
| 동시성 제어 전 | 성공=0, 잔액부족실패=20, 예상외실패=0, 적재(FAILED)=1, 최종잔액=100,000 | 성공 |
| 동시성 제어 후 | 동일                                                   | 성공 |

<details>
<summary>근거 로그</summary>

```text
2026-03-29T00:17:49.709+09:00  INFO 1387 --- [    Test worker] w.d.w.s.WalletConcurrencyIntegrationTest : 성공=0, 잔액부족실패=20, 예상외실패=0, 적재(SUCCESS)=0, 적재(FAILED)=1, 최종잔액=100000
```

</details>

---

### 시나리오 3. 서로 다른 transactionId + 충분한 잔액

공통 조건 및 기대 결과는 `7.1 테스트 시나리오 요약`을 따릅니다.

| 구분 | 결과 요약 | 판정 |
|---|---|----|
| 동시성 제어 전 | 최종 잔액 불일치 (기대 0, 실제 99,000) | 실패 |
| 동시성 제어 후 | 성공=100, 예상외실패=0, 적재(SUCCESS)=100, 최종잔액=0 | 성공 |

<details>
<summary>근거 로그</summary>

**동시성 제어 전**
```text
Multiple Failures (1 failure)
-- failure 1 --
expected: 0L
 but was: 99000L
```

**동시성 제어 후**
```text
2026-03-29T00:18:17.929+09:00  INFO 1644 --- [    Test worker] w.d.w.s.WalletConcurrencyIntegrationTest : 성공=100, 잔액부족실패=0, 예상외실패=0, 적재(SUCCESS)=100, 적재(FAILED)=0, 최종잔액=0
```
</details>

---

### 시나리오 4. 서로 다른 transactionId + 제한된 잔액

공통 조건 및 기대 결과는 `7.1 테스트 시나리오 요약`을 따릅니다.

| 구분 | 결과 요약 | 판정 |
|---|---|----|
| 동시성 제어 전 | 성공=20, 실패=0, 최종잔액=4,000 (초과 출금 발생) | 실패 |
| 동시성 제어 후 | 성공=5, 잔액부족실패=15, 예상외실패=0, 최종잔액=0 | 성공 |

<details>
<summary>근거 로그</summary>

**동시성 제어 전**
```text
Multiple Failures (4 failures)
-- failure 1 --
expected: 5L
 but was: 20L
at WalletConcurrencyIntegrationTest.Given_DifferentTransactionIds_When_ConcurrentWithdrawalsWithLimitedBalance_Then_BalanceNeverNegative(WalletConcurrencyIntegrationTest.java:233)
-- failure 2 --
expected: 15L
 but was: 0L
at WalletConcurrencyIntegrationTest.Given_DifferentTransactionIds_When_ConcurrentWithdrawalsWithLimitedBalance_Then_BalanceNeverNegative(WalletConcurrencyIntegrationTest.java:234)
-- failure 3 --
expected: 0L
 but was: 4000L
at WalletConcurrencyIntegrationTest.Given_DifferentTransactionIds_When_ConcurrentWithdrawalsWithLimitedBalance_Then_BalanceNeverNegative(WalletConcurrencyIntegrationTest.java:237)
-- failure 4 --
expected: 5000L
 but was: 24000L
at WalletConcurrencyIntegrationTest.Given_DifferentTransactionIds_When_ConcurrentWithdrawalsWithLimitedBalance_Then_BalanceNeverNegative(WalletConcurrencyIntegrationTest.java:238)
org.assertj.core.error.AssertJMultipleFailuresError: 
(생략)
```

**동시성 제어 후**
```text
2026-03-29T00:18:51.282+09:00  INFO 1903 --- [    Test worker] w.d.w.s.WalletConcurrencyIntegrationTest : 성공=5, 잔액부족실패=15, 예상외실패=0, 적재(SUCCESS)=5, 적재(FAILED)=15, 최종잔액=0
```
</details>
