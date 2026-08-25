# 계단식(경로형) 금리 시나리오 — 작업 정리 (6-2)

재산정 주기(예: 6개월)마다 금리가 계단식으로 오른다고 가정했을 때, 시점별 월상환액·잔여원금이 어떻게 변하는지 계산하는 기능. `한계금리 역산`과 같은 커밋(93d6e8d)에서 함께 구현됨.

## 0. 한눈에 보기

**어떤 기능인가**: "금리가 6개월마다 0.25%p씩 계속 오르면 내 대출이 어떻게 되나?" 같은 질문에 답하는 시뮬레이터. 사용자가 재산정 주기와 스텝당 금리 인상폭을 입력하면, 그 시나리오대로 금리가 계단식으로 올랐을 때 시점별 월상환액·잔여원금·이자를 계산해서 보여준다.

**DB에서 쓰는 값**: `loan_account` 테이블 1건을 `loanId`로 조회해서 아래 컬럼만 사용한다.

| 컬럼 | 의미 | 용도 |
| --- | --- | --- |
| `current_balance` | 현재 대출 잔액 | 계산 시작 시점의 원금 |
| `interest_rate` | 현재 적용금리 | 계단식 인상의 시작 금리 |
| `rate_type` | 고정/변동 | 고정금리면 계산 자체를 생략 |
| `repayment_type` | 원리금균등/원금균등/거치식 | 상환방식별로 월상환액 계산 공식이 다름 |
| `maturity_at` | 만기일 | 오늘부터 만기까지 남은 개월 수(`remainingMonths`) 산출 |
| `loan_status` | 정상/완제/연체 | `정상`이 아니면 계산 거부(400) |

**어떻게 계산하는가**: DB 값을 하나도 갱신하지 않는 **순수 조회 + 메모리 계산**이다. ①`remainingMonths`(만기까지 개월 수)와 초기 월상환액을 구한다 → ②`repricingCycleMonths`(재산정 주기)가 지날 때마다 잔여원금을 원리금균등/원금균등/거치식 공식으로 재계산하고, 금리에 `stepDeltaPercent`를 더한다 → ③새 금리·새 잔여원금으로 월상환액을 다시 계산 → ④만기를 넘어서는 시점에 도달하면 멈춘다(`truncated=true`). 이 과정을 `stepCount`번 반복해 시점별 스냅샷(`StagedRateStep`) 리스트를 만든다.

**화면에 렌더링되는 값** (`staged-rate-simulation.html` + `.js`):
- 상단 요약 카드: 대출종류, 금리유형, 초기금리, 초기 월상환액, 총 구간이자, 상태 배지(계산완료/만기로 일부만 계산됨/고정금리)
- 재산정 시점별 테이블: 경과개월 / 적용금리(%) / 잔여원금(원) / 월상환액(원) / 해당 구간 이자(원)
- 월상환액 추이를 보여주는 계단형(step) 라인 차트(Chart.js)

## 1. 파일 구조

```
loan/ratesimulation/
├── staged/
│   ├── controller/
│   │   ├── StagedRateSimulationController.java       // GET /api/loans/{loanId}/staged-rate-simulation
│   │   └── StagedRateSimulationViewController.java    // GET /loans/{loanId}/staged-rate-simulation (수동 테스트 화면)
│   └── dto/
│       ├── request/StagedRateSimulationRequest.java
│       └── response/StagedRateSimulationResponse.java
└── common/
    ├── service/StagedRateSimulator.java                // 핵심 엔진 (simulate + simulateCustomPath)
    ├── dto/response/StagedRateStep.java                 // 재산정 시점 1개 스냅샷
    └── dto/request/CustomRatePathPoint.java             // simulateCustomPath 입력 (monthOffset, rate)
```

프론트: `templates/loan/staged-rate-simulation.html`, `static/js/staged-rate-simulation.js`, `static/css/loan-ratesimulation-common.css`(3개 화면 공용) + `static/css/staged-rate-simulation.css`(이 화면 전용 추가 스타일)

## 2. API

`GET /api/loans/{loanId}/staged-rate-simulation`

| 파라미터 | 기본값 | 설명 |
| --- | --- | --- |
| `repricingCycleMonths` | 6 | 재산정 주기(개월) |
| `stepDeltaPercent` | (필수) | 재산정마다 오르는 금리폭 |
| `stepCount` | 5 | 재산정 반복 횟수 |

## 3. 핵심 로직 — `StagedRateSimulator`

- `simulate(request)`: 고정 주기(`repricingCycleMonths`)마다 `stepDeltaPercent`만큼 금리를 올리며 매 시점의 잔여원금·월상환액을 재계산.
- `simulateCustomPath(...)`: **정규 간격이 아니라 외부에서 주어진 임의의 `(monthOffset, rate)` 시퀀스를 그대로 재생**하는 범용 버전. 이후 6-3(과거금리 재현), 6-4(시장내재금리)가 엔진을 수정 없이 그대로 재사용함 — 이 기능의 핵심 설계 포인트.

## 4. 중요 사항

- **고정금리 대출**: 재산정 개념이 없으므로 계산을 생략하고 `path`는 빈 리스트, `message`에 안내 문구만 채워 반환.
- **truncated**: 다음 재산정 시점이 만기를 넘어서면 그 지점에서 멈추고 `truncated=true`로 표시(마지막 구간이 만기 이후로 넘어가는 걸 방지).
- **totalSegmentInterest**: 구간 이자는 `직전 월상환액 × 구간개월수 − 원금감소분`으로 근사 계산(정확한 일할 이자 계산 아님, 시뮬레이션용 근사치).
- **`simulateCustomPath`의 재사용 규칙**: `ratePath`는 `monthOffset` 오름차순 정렬 + 첫 원소는 반드시 `monthOffset=0`이어야 함. 같은 달에 금리가 두 번 바뀌는 경우(`segmentMonths<=0`)는 별도 스텝을 만들지 않고 다음 구간에 적용할 값만 갱신.
- **보안**: 로그인/MyData 동의 플로우가 아직 없어서 `LoanRateSimulationSecurityConfig`(`/api/loans/**` 전체)가 `permitAll`로 열어둔 상태. 인증 플로우 생기면 이 설정 파일만 고치면 됨.
- 로컬 테스트: `http://localhost:8080/loans/{loanId}/staged-rate-simulation`
