package com.via.shinvia.lifecycle.scenario.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleBaseStateDto;
import com.via.shinvia.lifecycle.common.dto.LifecycleLoanDto;
import com.via.shinvia.lifecycle.common.model.LifestyleLevel;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleScenarioResultDto;
import com.via.shinvia.lifecycle.scenario.model.LifecycleScenarioRecord;
import com.via.shinvia.lifecycle.survey.dto.*;
import com.via.shinvia.lifecycle.survey.service.LifecycleSurveyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "finance.api.service-key=test-key"
})
@Sql(scripts = "/db/lifecycle_reference_init.sql")
@Transactional
class LifecycleSimulationServiceTest {

    @Autowired
    private LifecycleSimulationService lifecycleSimulationService;

    @Autowired
    private LifecycleSurveyService lifecycleSurveyService;

    @Autowired
    private com.via.shinvia.lifecycle.scenario.mapper.LifecycleScenarioMapper lifecycleScenarioMapper;

    @Test
    void testSimulationWithAllSevenEvents() {
        Long userId = 1L;

        LifecycleScenarioRecord scenario = new LifecycleScenarioRecord();
        scenario.setUserId(userId);
        scenario.setScenarioName("Full 7 Events Scenario");
        scenario.setBaseDate(LocalDate.now());
        scenario.setStatus("ACTIVE");
        lifecycleScenarioMapper.insertScenario(scenario);
        Long scenarioId = scenario.getLifecycleScenarioId();

        // 1. Marriage
        MarriageSurveyRequest marriageRequest = MarriageSurveyRequest.builder()
                .targetDate(LocalDate.of(2027, 6, 1))
                .lifestyleLevel(LifestyleLevel.AVERAGE)
                .regionSido("Seoul")
                .regionSigungu("Gangnam")
                .userContributionRate(new BigDecimal("0.5"))
                .familySupportAmount(new BigDecimal("5000000"))
                .build();
        lifecycleSurveyService.saveMarriageSurvey(scenarioId, marriageRequest);

        // 2. Childbirth
        ChildbirthSurveyRequest childbirthRequest = ChildbirthSurveyRequest.builder()
                .targetDate(LocalDate.of(2028, 3, 1))
                .lifestyleLevel(LifestyleLevel.AVERAGE)
                .regionSido("Seoul")
                .regionSigungu("Gangnam")
                .postpartumCare(true)
                .build();
        lifecycleSurveyService.saveChildbirthSurvey(scenarioId, childbirthRequest);

        // 3. Vehicle
        VehicleSurveyRequest vehicleRequest = VehicleSurveyRequest.builder()
                .targetDate(LocalDate.of(2028, 9, 1))
                .vehiclePrice(new BigDecimal("35000000"))
                .cashPaymentAmount(new BigDecimal("15000000"))
                .loanAmount(new BigDecimal("20000000"))
                .loanPeriodMonths(48)
                .build();
        lifecycleSurveyService.saveVehicleSurvey(scenarioId, vehicleRequest);

        // 4. Monthly Rent
        MonthlyRentSurveyRequest rentRequest = MonthlyRentSurveyRequest.builder()
                .targetDate(LocalDate.of(2029, 3, 1))
                .desiredDeposit(new BigDecimal("30000000"))
                .desiredMonthlyRent(new BigDecimal("800000"))
                .monthlyManagementFee(new BigDecimal("100000"))
                .build();
        lifecycleSurveyService.saveMonthlyRentSurvey(scenarioId, rentRequest);

        // 5. Jeonse
        JeonseSurveyRequest jeonseRequest = JeonseSurveyRequest.builder()
                .targetDate(LocalDate.of(2031, 3, 1))
                .desiredJeonseAmount(new BigDecimal("350000000"))
                .ownFundAmount(new BigDecimal("150000000"))
                .desiredLoanAmount(new BigDecimal("200000000"))
                .build();
        lifecycleSurveyService.saveJeonseSurvey(scenarioId, jeonseRequest);

        // 6. Home Purchase
        HomePurchaseSurveyRequest homeRequest = HomePurchaseSurveyRequest.builder()
                .targetDate(LocalDate.of(2035, 1, 1))
                .desiredPurchasePrice(new BigDecimal("700000000"))
                .ownFundAmount(new BigDecimal("300000000"))
                .desiredArea(new BigDecimal("84"))
                .loanPeriodMonths(360)
                .regionSido("Seoul")
                .regionSigungu("Mapo")
                .build();
        lifecycleSurveyService.saveHomePurchaseSurvey(scenarioId, homeRequest);

        // 7. Repayment
        RepaymentSurveyRequest repaymentRequest = RepaymentSurveyRequest.builder()
                .targetDate(LocalDate.of(2037, 1, 1))
                .repaymentAmount(new BigDecimal("50000000"))
                .build();
        lifecycleSurveyService.saveRepaymentSurvey(scenarioId, repaymentRequest);

        LifecycleLoanDto existingLoan = LifecycleLoanDto.builder()
                .loanAccountId(1001L)
                .loanType("CREDIT")
                .currentBalance(new BigDecimal("30000000"))
                .interestRate(new BigDecimal("0.055"))
                .rateType("FIXED")
                .repaymentType("원리금균등상환")
                .maturityAt(LocalDate.now().plusYears(5))
                .build();

        LifecycleBaseStateDto baseState = LifecycleBaseStateDto.builder()
                .baseDate(LocalDate.now())
                .annualIncome(new BigDecimal("60000000"))
                .liquidAssetAmount(new BigDecimal("100000000"))
                .loans(List.of(existingLoan))
                .build();

        // When
        LifecycleScenarioResultDto result = lifecycleSimulationService.simulate(
                userId,
                "test@test.com",
                scenarioId,
                baseState
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEventSnapshots()).hasSize(7);
        assertThat(result.getFinalState()).isNotNull();
    }
}