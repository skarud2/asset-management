# 시장 내재 금리 시뮬레이션 — 작업 정리 (6-4)

> **아직 git에 커밋되지 않은 작업 중(WIP) 상태** — 코드는 존재하지만 스테이징/커밋 필요.

채권 수익률곡선(spot rate curve)에서 부트스트래핑으로 뽑아낸 내재 선도금리(forward rate)를 대출에 적용해, "시장이 기대하는 금리 경로대로 흘러가면" 월상환액이 어떻게 변할지 계산하는 기능. 6-2에서 만든 `StagedRateSimulator.simulateCustomPath` 엔진을 여기서도 코드 수정 없이 재사용.

## 0. 한눈에 보기

**어떤 기능인가**: 6-3이 "과거에 실제로 있었던 금리 변동"을 재현한다면, 이 기능은 "지금 채권시장이 앞으로의 금리를 어떻게 예상하고 있는지"를 대출에 적용해 **미래** 월상환액을 전망한다. 채권 수익률곡선에는 미래 기대 금리(내재 선도금리)가 이미 반영돼 있다는 금융 이론을 이용.

**DB에서 쓰는 값**: 두 테이블을 조회한다.

| 테이블 | 컬럼 | 용도 |
| --- | --- | --- |
| `loan_account` | `current_balance`, `interest_rate`, `rate_type`, `repayment_type`, `maturity_at`, `loan_status` | 6-2/6-3과 동일 — 대출 원금·상환방식·만기 등 |
| `bond_yield_curve` | `as_of_date`(기준일), `tenor_months`(만기 개월수), `yield_rate`(수익률 %) | 오늘 기준 채권 수익률곡선 — **KOFIA 정식 연동 전이라 수동 입력값**(예: 3개월물 3.200%, 12개월물 3.000% 등) |

**어떻게 계산하는가**: ①`bond_yield_curve`에서 오늘 날짜의 만기별 수익률(spot rate)을 만기 오름차순으로 조회 → ②인접한 두 만기 구간 사이의 "내재 선도금리"를 부트스트래핑 공식 `(1+s2)^(T2/12) = (1+s1)^(T1/12) × (1+f)^((T2-T1)/12)`로 역산(가장 짧은 만기 구간은 그 자체의 수익률을 그대로 씀) → ③대출의 현재 금리를 시작점(0개월)으로 놓고, 각 만기 지점의 내재 선도금리를 "몇 개월째 몇 %로 바뀐다"는 경로로 변환 → ④6-2의 `simulateCustomPath` 엔진에 그대로 위임해 월상환액을 계산.

**화면에 렌더링되는 값** (`market-implied-simulation.html` + `.js`):
- 상단 요약 카드: 대출종류, 금리유형, 기준일(`asOfDate`), 초기금리, 데이터 출처 문구(수동 입력값임을 명시), 상태 배지(계산완료/고정금리)
- 만기 지점별 테이블: 경과개월 / 내재 선도금리(%) / 월상환액(원) — 6-2·6-3과 달리 잔여원금·구간이자 컬럼은 없음(`MarketImpliedRateStep`에 해당 필드 자체가 없음)
- 월상환액 추이 계단형 라인 차트(Chart.js)

## 1. 파일 구조

```
loan/ratesimulation/marketimplied/
├── controller/
│   ├── MarketImpliedRateController.java       // GET /api/loans/{loanId}/market-implied-simulation
│   └── MarketImpliedRateViewController.java   // GET /loans/{loanId}/market-implied-simulation (수동 테스트 화면)
├── service/MarketImpliedRateSimulator.java     // 선도금리 계산 → StagedRateSimulator 위임
└── dto/response/{MarketImpliedRateStep, MarketImpliedSimulationResponse}.java

marketdata/
├── ForwardRateCurveService.java     // 수익률곡선 부트스트래핑으로 구간별 내재 선도금리 계산
├── BondYieldCurveProvider.java      // 인터페이스
├── DbBondYieldCurveProvider.java    // 현재 유일한 구현체 — bond_yield_curve 테이블(수동 입력값) 조회
├── YieldCurvePoint.java / ForwardRatePoint.java
└── NoYieldCurveDataException.java   // 곡선 데이터 없음 → 400
```

프론트: `templates/loan/market-implied-simulation.html`, `static/js/market-implied-simulation.js`, `static/css/loan-ratesimulation-common.css`(3개 화면 공용, 전용 CSS 없음)

## 2. API

`GET /api/loans/{loanId}/market-implied-simulation` — 별도 요청 파라미터 없음(기준일 `asOfDate`는 서버가 오늘 날짜로 고정).

## 3. 데이터 흐름

1. `ForwardRateCurveService.calculateForwardRates(asOfDate)` — 수익률곡선을 만기(tenor) 오름차순 정렬 후, 구간별로 `(1+s2)^(T2/12) = (1+s1)^(T1/12) * (1+f)^((T2-T1)/12)` 부트스트래핑 공식으로 내재 선도금리 계산
2. 대출의 현재 금리를 `monthOffset=0` 시작점으로 놓고, 각 선도금리 지점을 `CustomRatePathPoint`로 변환
3. `StagedRateSimulator.simulateCustomPath()`에 그대로 위임

## 4. 중요 사항

- **데이터 출처가 임시값**: `DbBondYieldCurveProvider`는 KOFIA 채권정보센터 오픈API 정식 연동 **전** 단계라 `bond_yield_curve` 테이블의 **수동 입력값**을 조회하는 구현체다. 응답의 `dataSource` 필드에 `"내부 참고용 수익률곡선 (KOFIA 채권정보센터 승인 전 — 수동 입력값 기반)"`을 항상 명시해 실데이터가 아님을 표시함. **KOFIA API 승인 나면 `DbBondYieldCurveProvider`를 그 API 호출 구현체로 교체하면 됨** (인터페이스로 분리해둔 이유).
- **[버그 수정] `as_of_date` 정확일치 조회 → 최근일자 조회로 변경 (2026-08-06)**: 원래 `BondYieldCurveMapper.findByAsOfDate`가 `WHERE as_of_date = #{asOfDate}`로 **오늘 날짜와 정확히 일치**하는 행만 찾았음. `db/init/004_bond_yield_curve.sql` 시드데이터는 `CURDATE()`로 DB를 처음 띄운 날짜에 고정되기 때문에, 하루만 지나도 `NoYieldCurveDataException`("기준일에 해당하는 수익률곡선 데이터가 없어요")이 발생하는 버그가 있었음. 실제 채권시장 데이터는 주말·공휴일엔 갱신되지 않으므로, `WHERE as_of_date = (SELECT MAX(as_of_date) FROM bond_yield_curve WHERE as_of_date <= #{asOfDate})`로 바꿔서 **오늘 이하 날짜 중 가장 최근 곡선**을 쓰도록 수정함(`BondYieldCurveMapper.xml`). 시드데이터를 매일 갱신하지 않아도 동작함.
- **재산정 시점 정의가 6-2/6-3과 다름**: 고정 주기(6-2)나 실제 금리변경일(6-3) 대신, **수익률곡선의 만기(tenor) 지점 자체**를 재산정 시점으로 사용. 설계상 의도된 차이이며 실수 아님.
- **고정금리 대출**: 6-2와 동일한 패턴으로 계산을 생략하고 `message`에 안내 문구만 채워 반환.
- **곡선 데이터 없음**: `bond_yield_curve`에 해당 기준일 데이터가 없으면 `NoYieldCurveDataException` → 400.
- **엔진 재사용, 수정 없음**: `simulateCustomPath`를 그대로 호출 — 계산 버그가 있다면 6-2 엔진 쪽부터 의심.
- **보안**: `LoanRateSimulationSecurityConfig`에 `/loans/*/market-implied-simulation` 경로가 이미 등록되어 `permitAll`.
