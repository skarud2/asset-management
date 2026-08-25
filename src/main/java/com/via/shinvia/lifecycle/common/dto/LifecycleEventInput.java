package com.via.shinvia.lifecycle.common.dto;

import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifecycleEventInput {

    // DB에 저장된 생애주기 이벤트 식별자
    private Long lifecycleEventId;

    // 이벤트 종류
    // 결혼, 출산, 차량구매, 전세 등
    private LifecycleEventType eventType;

    // 이벤트 실행 순서
    // 예: 결혼 1 → 출산 2 → 전세 3
    private Integer eventOrder;

    // 사용자가 희망하는 이벤트 발생일
    private LocalDate targetDate;

    // 이벤트 생활수준
    // 실속형, 평균형, 여유형, 프리미엄, 직접입력
    private LifestyleLevel lifestyleLevel;

    // 반복 출산 이벤트별 비용 구성을 결과 스냅샷까지 유지한다.
    private Integer childOrder;
    private Boolean repurchaseCarSeat;
    private Boolean repurchaseStroller;
    private Boolean repurchaseCrib;
    private Boolean repurchaseOtherSetup;
    private Boolean postpartumCare;
    private String childbirthRegionSido;
    private String childbirthRegionSigungu;

    // 이벤트 전체 예상비용
    // 예: 결혼 총 예상비용 4,500만원
    private BigDecimal estimatedCost;

    // 전체 비용 중 사용자가 실제 부담해야 하는 금액
    // 가족지원, 복지 등을 고려한 사용자 부담금
    private BigDecimal userRequiredAmount;

    // 가족·공공 지원을 차감하기 전 사용자의 비용 분담액
    private BigDecimal userContributionAmount;

    // 이벤트 이후 매월 추가되는 지출
    // 예: 출산 후 월 양육비, 차량 구매 후 유지비
    private BigDecimal additionalMonthlyExpense;

    // 이벤트 발생 시 들어오는 일회성 현금
    // 예: 가족지원금 등
    private BigDecimal cashInflowAmount;

    // 가족 또는 부모 등 민간 지원금. 공공 복지 혜택과 구분한다.
    private BigDecimal familySupportAmount;

    // 결혼 비용 차트와 상세 보고서에 사용하는 항목별 산출 금액
    private BigDecimal marriageHallCost;
    private BigDecimal marriageMealCost;
    private BigDecimal marriageFurnitureCost;
    private BigDecimal marriageHoneymoonCost;
    private BigDecimal postpartumCareCost;
    private BigDecimal infantCarSeatCost;
    private BigDecimal infantStrollerCost;
    private BigDecimal infantCribCost;
    private BigDecimal infantOtherSetupCost;

    // 이벤트로 새롭게 발생하는 대출금액
    // 예: 자동차대출, 전세대출, 주택담보대출
    private BigDecimal newLoanAmount;

    // 이벤트로 새롭게 취득하는 자산가치
    // 예: 차량 4천만원, 주택 8억원
    private BigDecimal acquiredAssetAmount;

    // 해당 이벤트에서 이용 가능한 복지지원 목록
    private List<LifecycleSupportDto> supports;

    // 해당 이벤트와 관련된 금융상품 추천 목록
    private List<LifecycleProductDto> recommendedProducts;

    // 자가 보유 중 전세/월세 이사 시 기존 주택 보유 여부 (true: 보유, false: 매각)
    private Boolean keepExistingHome;

    // 대출 상환 이벤트 시 특정 대상 대출 계좌 식별자
    private Long targetLoanAccountId;

    // 대출 상환 액션 (FULL, PARTIAL 등)
    private String repaymentAction;

    // 신규 발생 대출의 희망 만기 기간(개월)
    private Integer loanPeriodMonths;

    // 신규 발생 대출의 적용 금리(%)
    private BigDecimal loanInterestRate;

    // 신규 대출 상환방식 (EQUAL_PAYMENT, EQUAL_PRINCIPAL, BULLET)
    private String loanRepaymentType;

    // 결과화면에서 실제 산출값을 그대로 표시하기 위한 부대비용
    private BigDecimal taxAmount;
    private BigDecimal brokerageFeeAmount;
    private BigDecimal registrationFeeAmount;
}

