# 한계금리 역산 기능 — 작업 정리

## 1. DB
- **`db/init/003_loan_account.sql`** — `loan_account` 테이블 신규 생성 (기존에 없었음)
  - FK 컬럼명은 `card_account` 컨벤션에 맞춰 `connection_id` → `mydata_connection_id`로 조정
  - 로컬 DB(`shinvia-mysql`)에 적용 완료, 테스트 대출 2건 삽입 (`loanId=1` 정상/주담대, `loanId=2` 연체/신용대출)

## 2. 백엔드 — `com.via.shinvia.loan.ratesimulation`

| 구분 | 파일 | 역할 |
|---|---|---|
| Controller | `controller/LoanBreakevenRateController.java` | `GET /api/loans/{loanId}/breakeven-rate`, 예외 → HTTP 상태 매핑 |
| Controller | `controller/LoanBreakevenRateViewController.java` | `GET /loans/breakeven-rate-test` 뷰 서빙 |
| Service | `service/LoanRepaymentCalculator.java` | 상환방식별(원리금균등/원금균등/거치식) 월상환액·총이자 계산 + remainingMonths 산출 |
| Service | `service/BreakevenRateCalculator.java` | 이분탐색으로 한계금리 산출 (현재금리~+10%p, 오차 0.01%p, 최대 30회) |
| Service | `service/LoanBreakevenRateService.java` | 조회→상태검증→계산→응답 조립 오케스트레이션 |
| Entity/Mapper | `entity/LoanAccountSummary.java`, `mapper/LoanAccountSummaryMapper.java`(+xml) | loan_account 최소 조회 전용 (팀원 Mapper 미접촉) |
| Type | `type/RepaymentType.java`, `type/ThresholdType.java` | DB 값 ↔ 계산 로직 매핑 |
| Exception | `exception/*.java` (4종) | LoanNotFound(404), InvalidLoanStatus(400), UnsupportedThresholdType(400), UnsupportedRepaymentType |
| DTO | `dto/request/BreakevenRateRequest.java`, `dto/response/{BreakevenRateResponse,RepaymentCalculationResult}.java` | 요청/응답 모델 |

**공통 인프라**
- `security/LoanRateSimulationSecurityConfig.java` — `/api/loans/**`, `/loans/breakeven-rate-test`를 permitAll로 개방 (로그인 플로우 미구현 상태라 `CardSecurityConfig`와 동일한 방식으로 처리)

**스코프 제한**
- `thresholdType=INCOME_RATIO`는 월소득 데이터 테이블이 없어 이번 라운드에서 미구현 → 호출 시 400 + 안내 메시지 반환 (설계 시 사용자와 협의 후 결정)

## 3. 프론트엔드 (Thymeleaf)

| 파일 | 역할 |
|---|---|
| `templates/loan/breakeven-rate-test.html` | loanId / thresholdType / thresholdValue 입력 폼 + 결과 렌더링 영역 |
| `static/js/loan-breakeven-rate.js` | fetch로 API 호출, 결과·에러 DOM 렌더링 |
| `static/css/loan-breakeven-rate.css` | 기존 `signup.css` 스타일 컨벤션 재사용 |

접속: **`http://localhost:8080/loans/breakeven-rate-test`**

## 4. 요청 1건의 전체 데이터 흐름

시나리오: 화면에서 `loanId=1`, `thresholdType=MONTHLY_PAYMENT_AMOUNT`, `thresholdValue=1300000`으로 "계산하기"를 눌렀을 때.

### 4-1. 화면 진입 (페이지 렌더링)
1. 브라우저가 `GET /loans/breakeven-rate-test` 요청
2. `LoanBreakevenRateViewController.breakevenRateTestPage()`가 뷰 이름 `"loan/breakeven-rate-test"` 반환
3. Thymeleaf가 `templates/loan/breakeven-rate-test.html`을 렌더링. 이 HTML이 `<link>`로 `static/css/loan-breakeven-rate.css`, `<script>`로 `static/js/loan-breakeven-rate.js`를 같이 불러옴
4. 이 시점에는 DB 접근 없음 — 폼과 빈 결과 영역(`#resultArea`, `hidden` 상태)만 그려짐

### 4-2. "계산하기" 클릭 (JS → API 호출)
파일: `static/js/loan-breakeven-rate.js`
1. `form`의 `submit` 이벤트 핸들러가 `event.preventDefault()`로 페이지 새로고침 막음
2. `#loanId`, `#thresholdType`, `#thresholdValue` input의 `.value`를 각각 읽음 → `loanId="1"`, `thresholdType="MONTHLY_PAYMENT_AMOUNT"`, `thresholdValue="1300000"`
3. `fetchBreakevenRate(loanId, thresholdType, thresholdValue)` 호출
   - `URLSearchParams`로 쿼리스트링 조립
   - `fetch('/api/loans/1/breakeven-rate?thresholdType=MONTHLY_PAYMENT_AMOUNT&thresholdValue=1300000')` GET 요청

### 4-3. 컨트롤러 진입 (요청 파싱)
파일: `controller/LoanBreakevenRateController.java`
1. `@PathVariable Long loanId` → `1L`
2. `@RequestParam ThresholdType thresholdType` → Spring이 문자열 `"MONTHLY_PAYMENT_AMOUNT"`를 enum `ThresholdType.MONTHLY_PAYMENT_AMOUNT`로 자동 변환
3. `@RequestParam BigDecimal thresholdValue` → `BigDecimal("1300000")`
4. 세 값을 묶어 `new BreakevenRateRequest(loanId, thresholdType, thresholdValue)` 레코드 생성
5. `loanBreakevenRateService.calculate(request)` 호출

### 4-4. 서비스 — 조회 + 검증
파일: `service/LoanBreakevenRateService.java`
1. `request.thresholdType() != MONTHLY_PAYMENT_AMOUNT` 체크 → 통과 (INCOME_RATIO였다면 여기서 `UnsupportedThresholdTypeException` 던지고 끝)
2. `loanAccountSummaryMapper.findById(1L)` 호출 → MyBatis가 아래 SQL 실행

   파일: `mapper/LoanAccountSummaryMapper.xml`
   ```sql
   SELECT loan_account_id, loan_type, current_balance, interest_rate,
          rate_type, repayment_type, maturity_at, data_as_of_at, loan_status
   FROM loan_account
   WHERE loan_account_id = 1
   ```
   MySQL(`via_sys.loan_account`, localhost:3309)에서 실행되고, 결과 row가 `map-underscore-to-camel-case` 설정에 따라 `entity/LoanAccountSummary.java` 객체로 매핑됨 (`loan_type` → `loanType` 등)

   예시 결과값: `loanType="주택담보대출"`, `currentBalance=100000000.00`, `interestRate=4.200000`, `rateType="변동"`, `repaymentType="원리금균등"`, `maturityAt=2036-08-04`, `loanStatus="정상"`

3. `loan == null`이면 → `LoanNotFoundException` (404). 여기서는 row가 있으므로 통과
4. `loan.getLoanStatus()`가 `"정상"`이 아니면 → `InvalidLoanStatusException` (400). 통과
5. `repaymentCalculator.calculateRemainingMonths(loan.getMaturityAt())` 호출
   - 파일: `service/LoanRepaymentCalculator.java`
   - `ChronoUnit.MONTHS.between(LocalDate.now(), maturityAt)` → 예: 오늘이 2026-08-04면 만기 2036-08-04까지 `120`개월

### 4-5. 서비스 — 이분탐색 호출
`service/LoanBreakevenRateService.java`가 아래 인자로 `breakevenRateCalculator.search(...)` 호출:
- `principal = loan.getCurrentBalance()` → 100,000,000 (⚠ `principal_amount`가 아니라 `current_balance` 사용)
- `currentRatePercent = loan.getInterestRate()` → 4.2
- `remainingMonths = 120`
- `repaymentTypeDbValue = loan.getRepaymentType()` → `"원리금균등"`
- `thresholdMonthlyPayment = request.thresholdValue()` → 1,300,000

파일: `service/BreakevenRateCalculator.java`
1. `monthlyPaymentAt(...)`로 현재 금리(4.2%)에서의 월상환액 계산 → 내부적으로 `LoanRepaymentCalculator.calculate()` 호출 (아래 4-6 참고) → `currentMonthlyPayment = 1,021,983.75`
2. `1,021,983.75 >= 1,300,000`? → 아니오 → `alreadyExceeded` 아님, 다음 단계로
3. `hi = 4.2 + 10.0 = 14.2`에서의 월상환액 계산 → threshold(1,300,000)보다 큼 → "10%p 올려도 도달 안 함" 케이스 아님, 탐색 진행
4. `lo=4.2, hi=14.2`에서 시작해 최대 30회 이분탐색:
   - 매 반복마다 `mid = (lo+hi)/2`에서 `LoanRepaymentCalculator.calculate()`를 다시 호출해 월상환액을 구하고, threshold보다 작으면 `lo=mid`, 크거나 같으면 `hi=mid`
   - `hi - lo < 0.01%p`가 되면 종료
5. 최종 `mid ≈ 9.60` → `breakevenRate = 9.60`
6. `Result(currentMonthlyPayment=1,021,983.75, breakevenRate=9.60, alreadyExceeded=false, excessAmount=null)` 반환

### 4-6. 계산 엔진 (매 이분탐색 반복마다 재사용됨)
파일: `service/LoanRepaymentCalculator.java` — `calculate(principal, annualRatePercent, remainingMonths, repaymentTypeDbValue)`
1. `RepaymentType.fromDbValue("원리금균등")` → enum `EQUAL_PRINCIPAL_INTEREST`
2. `r = annualRatePercent / 12 / 100` (월이자율), `n = remainingMonths`
3. `calculateEqualPrincipalInterest(p, r, n)`:
   - `M = p × r × (1+r)^n / ((1+r)^n − 1)`
   - `총이자 = M × n − p`
4. `BigDecimal`로 반올림(소수점 2자리, HALF_UP) 후 `RepaymentCalculationResult(monthlyPayment, totalInterest)` 반환

(원금균등이면 `calculateEqualPrincipal`로 매달 원금 `P/n` + 잔여원금×이자율을 1회차 기준으로, 거치식은 거치기간 컬럼이 없어 원리금균등과 동일 로직 사용)

### 4-7. 응답 조립
`service/LoanBreakevenRateService.java`로 돌아와서:
1. `marginPercent = breakevenRate(9.60) − currentRate(4.2) = 5.40`
2. `buildMessage(...)` → `"기준금리가 지금보다 5.40%p 더 오르면 월상환액이 1,300,000원을 넘어요"`
3. `dto/response/BreakevenRateResponse.java` 레코드로 조립:
   ```json
   {
     "loanId": 1,
     "loanType": "주택담보대출",
     "rateType": "변동",
     "currentRate": 4.200000,
     "currentMonthlyPayment": 1021983.75,
     "breakevenRate": 9.60,
     "marginPercent": 5.40,
     "thresholdType": "MONTHLY_PAYMENT_AMOUNT",
     "thresholdValue": 1300000,
     "alreadyExceeded": false,
     "excessAmount": null,
     "message": "기준금리가 지금보다 5.40%p 더 오르면 월상환액이 1,300,000원을 넘어요"
   }
   ```
4. 컨트롤러가 `ResponseEntity.ok(response)`로 감싸고, Spring(Jackson)이 위 JSON으로 직렬화해 HTTP 응답 전송

### 4-8. 프론트엔드 렌더링 (JS → DOM)
파일: `static/js/loan-breakeven-rate.js`
1. `fetchBreakevenRate()`의 `response.json()`으로 위 JSON을 파싱해 `data` 객체로 받음
2. `renderResult(data)` 호출 — `data`의 각 필드를 그대로 DOM에 꽂아 넣음:
   - `data.loanType`, `data.rateType` → `#resultLoanType`, `#resultRateType`
   - `data.currentRate` → `#resultCurrentRate`
   - `data.currentMonthlyPayment` → `formatNumber()`(Intl.NumberFormat 'ko-KR')로 천단위 콤마 포맷 → `#resultCurrentMonthlyPayment`
   - `data.breakevenRate` (null이면 "없음") → `#resultBreakevenRate`
   - `data.marginPercent` → `#resultMarginPercent`
   - `data.alreadyExceeded` (boolean → "예"/"아니오") → `#resultAlreadyExceeded`
   - `data.excessAmount` → `#resultExcessAmount`
   - `data.message` → `#resultMessage`
   - `JSON.stringify(data, null, 2)` 전체 원본 → `#resultRawJson` (접힌 `<details>` 안)
3. `resultArea.classList.remove('hidden')`으로 결과 섹션 표시

**요약하면**: `loan_account` 테이블(현재 잔액·금리·상환방식·만기일) → `LoanAccountSummaryMapper`가 SELECT → `LoanAccountSummary` 객체 → `LoanBreakevenRateService`가 상태 검증 후 `LoanRepaymentCalculator`(순수 수식 계산)와 `BreakevenRateCalculator`(그 수식을 반복 호출하는 이분탐색)를 조합 → `BreakevenRateResponse` JSON → JS `fetch`가 받아서 DOM에 필드별로 꽂아넣는 구조입니다.

## 5. 테스트
- 신규 단위테스트 12개 (`LoanRepaymentCalculatorTest` 4, `BreakevenRateCalculatorTest` 3, `LoanBreakevenRateServiceTest` 5) — 전부 통과
- 기존 전체 스위트(`ShinviaApplicationTests` 포함) 회귀 없음 확인
- curl + 브라우저(Claude in Chrome)로 정상/alreadyExceeded/미도달/상태에러/미지원타입/404 6개 시나리오 실제 동작 확인

## 6. 알아두면 좋은 것
- 응답의 `loanType`/`rateType`은 DB에 저장된 한글 원문 그대로 반환 (명세 예시의 영문 "MORTGAGE"와 다름)
- INCOME_RATIO를 실제로 구현하려면 월소득 테이블/컬럼 설계부터 먼저 필요
