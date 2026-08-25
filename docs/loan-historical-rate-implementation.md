# 과거 금리인상기 재현 — 작업 정리 (6-3)

> **아직 git에 커밋되지 않은 작업 중(WIP) 상태** — 코드는 존재하지만 스테이징/커밋 필요.

실제 한국은행 기준금리 변동 이력(ECOS)을 그대로 재생해서, 과거 금리인상기 동안 이 대출의 월상환액이 어떻게 변했을지 계산하는 기능. 6-2(계단식 시나리오)에서 만든 `StagedRateSimulator.simulateCustomPath` 엔진을 코드 수정 없이 그대로 재사용.

## 0. 한눈에 보기

**어떤 기능인가**: "2021~2023년 실제 금리인상기처럼 금리가 움직였다면 내 대출 월상환액이 어떻게 변했을까?"를 재현하는 기능. 가상의 시나리오(6-2)가 아니라, 한국은행이 실제로 발표한 기준금리 변동 이력을 그대로 대출에 적용한다.

**DB에서 쓰는 값**: `loan_account`에서 `loanId`로 1건 조회 — 6-2와 동일한 컬럼(`current_balance`, `interest_rate`, `rate_type`, `repayment_type`, `maturity_at`, `loan_status`)을 사용한다. 다만 **금리 시나리오 자체는 DB가 아니라 외부 한국은행 ECOS API**에서 가져온다(일별 기준금리, 통계코드 `722Y001`/`0101000`) — DB에는 저장하지 않고 매 요청마다 실시간 호출.

**어떻게 계산하는가**: ①사용자가 지정한 기간(`startDate`~`endDate`) 동안의 일별 기준금리를 ECOS에서 조회 → ②전날과 값이 달라진 지점(금리 변경일)만 뽑아낸다(`extractRateChangePoints`) → ③그 변경일들을 "몇 개월째에 몇 %로 바뀌었다"는 경로로 변환해 6-2의 `simulateCustomPath` 엔진에 그대로 태운다 → ④엔진이 각 변경 시점의 잔여원금·월상환액·구간이자를 계산.

**화면에 렌더링되는 값** (`historical-rate-replay.html` + `.js`):
- 상단 요약 카드: 대출종류, 금리유형, 초기금리, 초기 월상환액, **금리 변경 횟수**(`changePointCount`), 총 구간이자, 데이터 출처 문구(disclaimer), 상태 배지
- 금리 변경 시점별 테이블: 경과개월 / 적용금리(%) / 잔여원금(원) / 월상환액(원) / 구간이자(원)
- 월상환액 추이 계단형 라인 차트(Chart.js)

## 1. 파일 구조

```
loan/ratesimulation/historical/
├── controller/
│   ├── HistoricalRateReplayController.java       // GET /api/loans/{loanId}/historical-rate-replay
│   └── HistoricalRateReplayViewController.java   // GET /loans/{loanId}/historical-rate-replay (수동 테스트 화면)
├── service/HistoricalRateReplaySimulator.java     // 조회 → 변경점 추출 → StagedRateSimulator 위임
├── dto/
│   ├── request/HistoricalRateReplayRequest.java
│   └── response/{HistoricalRateReplayResponse, RateChangePoint}.java
└── exception/NoHistoricalRateDataException.java   // 조회 기간에 데이터 없음 → 400

client/ecos/
├── EcosApiClient.java          // 한국은행 ECOS StatisticSearch API (통계코드 722Y001, 일별 기준금리 0101000)
├── EcosApiProperties.java
├── EcosApiException.java       // 업스트림 실패 → 502 매핑
└── EcosRestTemplateConfig.java
```

프론트: `templates/loan/historical-rate-replay.html`, `static/js/historical-rate-replay.js`, `static/css/loan-ratesimulation-common.css`(3개 화면 공용, 전용 CSS 없음)

## 2. API

`GET /api/loans/{loanId}/historical-rate-replay?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`

## 3. 데이터 흐름

1. `EcosApiClient.getDailyBaseRate(startDate, endDate)`로 일별 기준금리 시계열 조회
2. `extractRateChangePoints()`로 **전날 대비 값이 바뀐 지점만** 추출(첫 데이터는 시작점으로 무조건 포함, `changeBp`는 bp 단위 변동폭)
3. 변경점을 `CustomRatePathPoint`(월 오프셋, 금리)로 변환해 `StagedRateSimulator.simulateCustomPath()`에 그대로 위임

## 4. 중요 사항

- **외부 API 의존**: ECOS 실데이터 기반 호출이라 응답 지연/실패 가능성이 있음. `EcosApiException` 발생 시 컨트롤러에서 `502 Bad Gateway`로 매핑.
- **데이터 없음 처리**: `ECOS`가 `INFO-200` 코드(조회 결과 없음)를 주면 정상 케이스로 취급해 빈 결과 반환 → 이후 `changePoints`가 비어 있으면 `NoHistoricalRateDataException`(400)으로 변환.
- **엔진 재사용, 수정 없음**: 계단식(6-2)에서 만든 `simulateCustomPath`를 그대로 호출. 새 계산 로직을 추가하지 않았다는 점이 이 기능의 핵심 — 버그가 있다면 6-2 엔진 쪽을 먼저 의심.
- **응답 출처 표기**: `disclaimer` 필드에 `"한국은행 ECOS 실데이터 기반 (통계코드 722Y001)"`을 항상 채워서 반환 — 사용자에게 데이터 출처를 명시하기 위함.
- **보안**: `LoanRateSimulationSecurityConfig`에 이미 `/loans/*/historical-rate-replay` 경로가 등록돼 있어 별도 설정 없이 `permitAll`.
- `EcosApiProperties`에 API 키(`ecos.api.key`)가 `application.yml`에 평문으로 들어가 있음 — 카드 도메인 케이스처럼 민감정보 관리 방식 재검토 여지 있음(현재는 공개 통계 API 키라 위험도는 낮은 편).
