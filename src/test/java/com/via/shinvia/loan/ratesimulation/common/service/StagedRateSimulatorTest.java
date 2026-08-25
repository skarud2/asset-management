package com.via.shinvia.loan.ratesimulation.common.service;

import com.via.shinvia.loan.ratesimulation.common.dto.request.CustomRatePathPoint;
import com.via.shinvia.loan.ratesimulation.staged.dto.request.StagedRateSimulationRequest;
import com.via.shinvia.loan.ratesimulation.staged.dto.response.StagedRateSimulationResponse;
import com.via.shinvia.loan.ratesimulation.common.dto.response.StagedRateStep;
import com.via.shinvia.loan.ratesimulation.common.entity.LoanAccountSummary;
import com.via.shinvia.loan.ratesimulation.common.exception.InvalidLoanStatusException;
import com.via.shinvia.loan.ratesimulation.common.exception.LoanNotFoundException;
import com.via.shinvia.loan.ratesimulation.common.mapper.LoanAccountSummaryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StagedRateSimulatorTest {

    @Mock
    private LoanAccountSummaryMapper loanAccountSummaryMapper;

    private final LoanRepaymentCalculator repaymentCalculator = new LoanRepaymentCalculator();

    private StagedRateSimulator simulator() {
        return new StagedRateSimulator(loanAccountSummaryMapper, repaymentCalculator);
    }

    private LoanAccountSummary variableRateLoan(int remainingMonths) {
        LoanAccountSummary loan = new LoanAccountSummary();
        loan.setLoanAccountId(1L);
        loan.setLoanType("주택담보대출");
        loan.setCurrentBalance(new BigDecimal("100000000"));
        loan.setInterestRate(new BigDecimal("4.0"));
        loan.setRateType("변동");
        loan.setRepaymentType("원리금균등");
        loan.setMaturityAt(LocalDate.now().plusMonths(remainingMonths));
        loan.setLoanStatus("정상");
        return loan;
    }

    @Test
    void 대출을_찾을_수_없으면_LoanNotFoundException을_던진다() {
        when(loanAccountSummaryMapper.findById(999L)).thenReturn(null);

        assertThatThrownBy(() -> simulator().simulate(
                new StagedRateSimulationRequest(999L, 12, new BigDecimal("0.5"), 3)
        )).isInstanceOf(LoanNotFoundException.class);
    }

    @Test
    void 연체중인_대출이면_InvalidLoanStatusException을_던진다() {
        LoanAccountSummary loan = variableRateLoan(120);
        loan.setLoanStatus("연체");
        when(loanAccountSummaryMapper.findById(1L)).thenReturn(loan);

        assertThatThrownBy(() -> simulator().simulate(
                new StagedRateSimulationRequest(1L, 12, new BigDecimal("0.5"), 3)
        )).isInstanceOf(InvalidLoanStatusException.class);
    }

    @Test
    void 고정금리_대출이면_계산없이_안내_메시지만_반환한다() {
        LoanAccountSummary loan = variableRateLoan(120);
        loan.setRateType("고정");
        when(loanAccountSummaryMapper.findById(1L)).thenReturn(loan);

        StagedRateSimulationResponse response = simulator().simulate(
                new StagedRateSimulationRequest(1L, 12, new BigDecimal("0.5"), 3)
        );

        assertThat(response.message()).isEqualTo("고정금리 대출이라 재산정에 따른 금리 변동이 없어요");
        assertThat(response.path()).isEmpty();
        assertThat(response.initialMonthlyPayment()).isNull();
    }

    @Test
    void 삼_스텝_시나리오는_스텝마다_금리가_델타만큼_오르고_잔액이_감소한다() {
        when(loanAccountSummaryMapper.findById(1L)).thenReturn(variableRateLoan(120));

        StagedRateSimulationResponse response = simulator().simulate(
                new StagedRateSimulationRequest(1L, 12, new BigDecimal("0.5"), 3)
        );

        assertThat(response.truncated()).isFalse();
        List<StagedRateStep> path = response.path();
        assertThat(path).hasSize(4); // step0 + 3 steps

        assertThat(path.get(0).monthOffset()).isEqualTo(0);
        assertThat(path.get(0).appliedRate().doubleValue()).isCloseTo(4.0, offset(0.001));
        assertThat(path.get(0).segmentInterest()).isNull();

        for (int i = 1; i <= 3; i++) {
            StagedRateStep step = path.get(i);
            double expectedRate = 4.0 + 0.5 * i;
            assertThat(step.monthOffset()).isEqualTo(12 * i);
            assertThat(step.appliedRate().doubleValue()).isCloseTo(expectedRate, offset(0.001));
            assertThat(step.segmentInterest()).isNotNull();
            // 잔액은 매 스텝 감소해야 한다
            assertThat(step.remainingBalance().doubleValue())
                    .isLessThan(path.get(i - 1).remainingBalance().doubleValue());
        }

        // 참조값 (python으로 독립 계산): step1 잔액 91,699,504.87 / 월상환 1,034,149.66
        assertThat(path.get(1).remainingBalance().doubleValue()).isCloseTo(91699504.87, offset(1.0));
        assertThat(path.get(1).monthlyPayment().doubleValue()).isCloseTo(1034149.66, offset(1.0));
    }

    @Test
    void 대출_만기가_stepCount_이전에_도래하면_truncated가_true이고_중간에_끊긴다() {
        // 잔여기간 20개월인 대출에 12개월 주기로 5스텝을 요청 -> 2번째 재산정(24개월)은 만기를 넘어서 진입 못함
        when(loanAccountSummaryMapper.findById(1L)).thenReturn(variableRateLoan(20));

        StagedRateSimulationResponse response = simulator().simulate(
                new StagedRateSimulationRequest(1L, 12, new BigDecimal("0.5"), 5)
        );

        assertThat(response.truncated()).isTrue();
        // step0 + 1번째 재산정(12개월, 20개월 미만이라 진입 가능)까지만 있어야 함
        assertThat(response.path()).hasSize(2);
        assertThat(response.path().get(1).monthOffset()).isEqualTo(12);
    }

    @Test
    void simulateCustomPath는_임의_시점의_변경점_시퀀스를_그대로_반영한다() {
        List<CustomRatePathPoint> ratePath = List.of(
                new CustomRatePathPoint(0, new BigDecimal("4.0")),
                new CustomRatePathPoint(12, new BigDecimal("4.5")),
                new CustomRatePathPoint(24, new BigDecimal("5.0")),
                new CustomRatePathPoint(36, new BigDecimal("5.5"))
        );

        StagedRateSimulator.CustomPathSimulationResult result = simulator().simulateCustomPath(
                new BigDecimal("100000000"), 120, "원리금균등", ratePath
        );

        assertThat(result.truncated()).isFalse();
        assertThat(result.path()).hasSize(4);
        // simulate()의 "삼 스텝" 테스트와 동일한 조건(12개월 간격, +0.5%p)이므로 같은 참조값으로 대조
        assertThat(result.path().get(1).remainingBalance().doubleValue()).isCloseTo(91699504.87, offset(1.0));
        assertThat(result.path().get(1).monthlyPayment().doubleValue()).isCloseTo(1034149.66, offset(1.0));
    }

    @Test
    void simulateCustomPath는_변경점이_만기_이전에_도래하면_truncated로_중단한다() {
        List<CustomRatePathPoint> ratePath = List.of(
                new CustomRatePathPoint(0, new BigDecimal("5.0")),
                new CustomRatePathPoint(24, new BigDecimal("5.5"))
        );

        StagedRateSimulator.CustomPathSimulationResult result = simulator().simulateCustomPath(
                new BigDecimal("20000000"), 20, "원리금균등", ratePath
        );

        assertThat(result.truncated()).isTrue();
        assertThat(result.path()).hasSize(1);
    }

    @Test
    void simulateCustomPath는_같은_달에_두_번_바뀌어도_유실되지_않고_최신_금리로_반영된다() {
        // monthOffset=0에서 4.0% -> 4.25%로 한 번 더 바뀌고, 6개월 뒤 4.5%로 바뀌는 경우
        // 0->6 구간은 (유실된 4.0%가 아니라) 병합된 4.25%로 계산되어야 한다
        List<CustomRatePathPoint> ratePath = List.of(
                new CustomRatePathPoint(0, new BigDecimal("4.0")),
                new CustomRatePathPoint(0, new BigDecimal("4.25")),
                new CustomRatePathPoint(6, new BigDecimal("4.5"))
        );

        StagedRateSimulator.CustomPathSimulationResult result = simulator().simulateCustomPath(
                new BigDecimal("100000000"), 120, "원리금균등", ratePath
        );

        assertThat(result.path()).hasSize(2);
        assertThat(result.path().get(1).monthOffset()).isEqualTo(6);
        assertThat(result.path().get(1).appliedRate()).isEqualByComparingTo("4.5");
        // 4.0%가 아니라 병합된 4.25%로 6개월치 잔여원금이 계산되어야 함
        assertThat(result.path().get(1).remainingBalance().doubleValue())
                .isCloseTo(95942974.58, offset(1.0));
    }
}
