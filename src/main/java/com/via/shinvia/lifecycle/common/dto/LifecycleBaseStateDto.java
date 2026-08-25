package com.via.shinvia.lifecycle.common.dto;

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
public class LifecycleBaseStateDto {
    // 시뮬레이션 대상 회원 식별자
    private Long userId;
    // 시뮬레이션 시작 기준일
    private LocalDate baseDate;
    // 생년월일  app_user.birth_date에서 조회
    private LocalDate birthDate;
    // 회원 성별 app_user.gender에서 조회(없긴한데 나중에 테이블에 추가하는거 괜찮을듯)
    private String gender;
    //연소득  user_financial_profile.annual_income에서 조회
    private BigDecimal annualIncome;
    // 소득 유형 예: 근로소득, 사업소득, 기타
    private String incomeType;
    // 현재 고용 상태 예: 재직, 자영업, 무직
    private String employmentStatus;
    // 현재 신용점수
    private Integer creditScore;
    // 현재 사용할 수 있는 유동자산 현금성 자산 및 사용자 입력 유동자산
    private BigDecimal liquidAssetAmount;
    // 현재 월 생활비 생애주기 기본 설문에서 입력
    private BigDecimal monthlyLivingExpense;
    // 현재 주거 형태 FAMILY, MONTHLY_RENT, JEONSE, OWN
    private String currentHousingType;
    // 현재 월 주거비 월세, 관리비 등 매월 발생하는 주거비
    private BigDecimal monthlyHousingExpense;
    // 사용자의 현재 산업군 미래 소득 상승률 기준을 찾을 때 사용
    private String industryCode;
    // 사용자가 선택한 미래 소득 상승 시나리오 CONSERVATIVE, BASE, OPTIMISTIC, CUSTOM
    private String salaryGrowthScenario;
    // 실제 시뮬레이션에 적용할 연평균 급여 상승률 예: 0.031 = 연 3.1%
    private BigDecimal annualSalaryGrowthRate;
    // 사용자가 현재 보유한 대출 목록
    private List<LifecycleLoanDto> loans;
}