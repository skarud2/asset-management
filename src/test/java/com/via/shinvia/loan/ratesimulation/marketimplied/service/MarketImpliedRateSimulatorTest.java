package com.via.shinvia.loan.ratesimulation.marketimplied.service;

import com.via.shinvia.loan.ratesimulation.marketimplied.dto.response.MarketImpliedRateStep;
import com.via.shinvia.loan.ratesimulation.marketimplied.dto.response.MarketImpliedSimulationResponse;
import com.via.shinvia.loan.ratesimulation.common.entity.LoanAccountSummary;
import com.via.shinvia.loan.ratesimulation.common.exception.InvalidLoanStatusException;
import com.via.shinvia.loan.ratesimulation.common.exception.LoanNotFoundException;
import com.via.shinvia.loan.ratesimulation.common.mapper.LoanAccountSummaryMapper;
import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import com.via.shinvia.loan.ratesimulation.common.service.StagedRateSimulator;
import com.via.shinvia.marketdata.BondYieldCurveProvider;
import com.via.shinvia.marketdata.ForwardRateCurveService;
import com.via.shinvia.marketdata.NoYieldCurveDataException;
import com.via.shinvia.marketdata.YieldCurvePoint;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketImpliedRateSimulatorTest {

    @Mock
    private LoanAccountSummaryMapper loanAccountSummaryMapper;

    @Mock
    private BondYieldCurveProvider bondYieldCurveProvider;

    private final LoanRepaymentCalculator repaymentCalculator = new LoanRepaymentCalculator();

    private MarketImpliedRateSimulator simulator() {
        StagedRateSimulator stagedRateSimulator = new StagedRateSimulator(loanAccountSummaryMapper, repaymentCalculator);
        ForwardRateCurveService forwardRateCurveService = new ForwardRateCurveService(bondYieldCurveProvider);
        return new MarketImpliedRateSimulator(loanAccountSummaryMapper, repaymentCalculator, stagedRateSimulator, forwardRateCurveService);
    }

    private LoanAccountSummary variableRateLoan() {
        LoanAccountSummary loan = new LoanAccountSummary();
        loan.setLoanAccountId(1L);
        loan.setLoanType("주택담보대출");
        loan.setCurrentBalance(new BigDecimal("100000000"));
        loan.setInterestRate(new BigDecimal("4.0"));
        loan.setRateType("변동");
        loan.setRepaymentType("원리금균등");
        loan.setMaturityAt(LocalDate.now().plusMonths(120));
        loan.setLoanStatus("정상");
        return loan;
    }

    @Test
    void 대출을_찾을_수_없으면_LoanNotFoundException을_던진다() {
        when(loanAccountSummaryMapper.findById(999L)).thenReturn(null);

        assertThatThrownBy(() -> simulator().simulate(999L))
                .isInstanceOf(LoanNotFoundException.class);
    }

    @Test
    void 연체중인_대출이면_InvalidLoanStatusException을_던진다() {
        LoanAccountSummary loan = variableRateLoan();
        loan.setLoanStatus("연체");
        when(loanAccountSummaryMapper.findById(1L)).thenReturn(loan);

        assertThatThrownBy(() -> simulator().simulate(1L))
                .isInstanceOf(InvalidLoanStatusException.class);
    }

    @Test
    void 고정금리_대출이면_계산없이_안내_메시지만_반환한다() {
        LoanAccountSummary loan = variableRateLoan();
        loan.setRateType("고정");
        when(loanAccountSummaryMapper.findById(1L)).thenReturn(loan);

        MarketImpliedSimulationResponse response = simulator().simulate(1L);

        assertThat(response.message()).isEqualTo("고정금리 대출이라 시장 금리 변동의 영향을 받지 않아요");
        assertThat(response.path()).isEmpty();
    }

    @Test
    void 수익률곡선_데이터가_없으면_NoYieldCurveDataException을_던진다() {
        when(loanAccountSummaryMapper.findById(1L)).thenReturn(variableRateLoan());
        lenient().when(bondYieldCurveProvider.getYieldCurve(any())).thenReturn(List.of());

        assertThatThrownBy(() -> simulator().simulate(1L))
                .isInstanceOf(NoYieldCurveDataException.class);
    }

    @Test
    void 수익률곡선의_만기_지점을_재산정_시점으로_사용해서_경로를_계산한다() {
        LoanAccountSummary loan = variableRateLoan();
        when(loanAccountSummaryMapper.findById(1L)).thenReturn(loan);
        lenient().when(bondYieldCurveProvider.getYieldCurve(any())).thenReturn(List.of(
                new YieldCurvePoint(12, new BigDecimal("3.0")),
                new YieldCurvePoint(24, new BigDecimal("2.8"))
        ));

        MarketImpliedSimulationResponse response = simulator().simulate(1L);

        assertThat(response.message()).isNull();
        assertThat(response.initialRate()).isEqualByComparingTo("4.0");
        assertThat(response.truncated()).isFalse();

        List<MarketImpliedRateStep> path = response.path();
        // monthOffset=0(현재 시점)은 path에 포함되지 않고 initialRate로만 노출됨
        assertThat(path).extracting(MarketImpliedRateStep::monthOffset).containsExactly(12, 24);

        assertThat(path.get(0).impliedRate()).isEqualByComparingTo("3.0");
        assertThat(path.get(1).impliedRate().doubleValue()).isCloseTo(2.600, offset(0.001));

        assertThat(response.dataSource()).contains("KOFIA");
    }
}
