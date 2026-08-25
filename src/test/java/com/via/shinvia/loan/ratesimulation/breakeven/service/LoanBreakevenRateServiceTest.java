package com.via.shinvia.loan.ratesimulation.breakeven.service;

import com.via.shinvia.loan.ratesimulation.breakeven.dto.request.BreakevenRateRequest;
import com.via.shinvia.loan.ratesimulation.common.entity.LoanAccountSummary;
import com.via.shinvia.loan.ratesimulation.common.exception.InvalidLoanStatusException;
import com.via.shinvia.loan.ratesimulation.common.exception.LoanNotFoundException;
import com.via.shinvia.loan.ratesimulation.breakeven.exception.UnsupportedThresholdTypeException;
import com.via.shinvia.loan.ratesimulation.common.mapper.LoanAccountSummaryMapper;
import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import com.via.shinvia.loan.ratesimulation.breakeven.type.ThresholdType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanBreakevenRateServiceTest {

    @Mock
    private LoanAccountSummaryMapper loanAccountSummaryMapper;

    private final LoanRepaymentCalculator repaymentCalculator = new LoanRepaymentCalculator();
    private final BreakevenRateCalculator breakevenRateCalculator = new BreakevenRateCalculator(repaymentCalculator);

    private LoanBreakevenRateService service() {
        return new LoanBreakevenRateService(loanAccountSummaryMapper, repaymentCalculator, breakevenRateCalculator);
    }

    private LoanAccountSummary loanWithStatus(String status) {
        LoanAccountSummary loan = new LoanAccountSummary();
        loan.setLoanAccountId(1L);
        loan.setLoanType("주택담보대출");
        loan.setCurrentBalance(new BigDecimal("100000000"));
        loan.setInterestRate(new BigDecimal("4.0"));
        loan.setRateType("변동");
        loan.setRepaymentType("원리금균등");
        loan.setMaturityAt(LocalDate.now().plusMonths(120));
        loan.setLoanStatus(status);
        return loan;
    }

    @Test
    void 대출을_찾을_수_없으면_LoanNotFoundException을_던진다() {
        when(loanAccountSummaryMapper.findById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service().calculate(
                new BreakevenRateRequest(999L, ThresholdType.MONTHLY_PAYMENT_AMOUNT, new BigDecimal("1000000"))
        )).isInstanceOf(LoanNotFoundException.class);
    }

    @Test
    void 완제된_대출이면_InvalidLoanStatusException을_던진다() {
        when(loanAccountSummaryMapper.findById(1L)).thenReturn(loanWithStatus("완제"));

        assertThatThrownBy(() -> service().calculate(
                new BreakevenRateRequest(1L, ThresholdType.MONTHLY_PAYMENT_AMOUNT, new BigDecimal("1000000"))
        )).isInstanceOf(InvalidLoanStatusException.class)
                .hasMessageContaining("완제되었거나 연체 중인 대출은 시뮬레이션할 수 없어요");
    }

    @Test
    void 연체중인_대출이면_InvalidLoanStatusException을_던진다() {
        when(loanAccountSummaryMapper.findById(1L)).thenReturn(loanWithStatus("연체"));

        assertThatThrownBy(() -> service().calculate(
                new BreakevenRateRequest(1L, ThresholdType.MONTHLY_PAYMENT_AMOUNT, new BigDecimal("1000000"))
        )).isInstanceOf(InvalidLoanStatusException.class);
    }

    @Test
    void INCOME_RATIO는_아직_지원하지_않아_UnsupportedThresholdTypeException을_던진다() {
        assertThatThrownBy(() -> service().calculate(
                new BreakevenRateRequest(1L, ThresholdType.INCOME_RATIO, new BigDecimal("40"))
        )).isInstanceOf(UnsupportedThresholdTypeException.class);
    }

    @Test
    void 정상_대출이면_한계금리_계산결과를_반환한다() {
        when(loanAccountSummaryMapper.findById(1L)).thenReturn(loanWithStatus("정상"));

        var response = service().calculate(
                new BreakevenRateRequest(1L, ThresholdType.MONTHLY_PAYMENT_AMOUNT, new BigDecimal("1300000"))
        );

        assertThat(response.loanId()).isEqualTo(1L);
        assertThat(response.alreadyExceeded()).isFalse();
        assertThat(response.breakevenRate()).isNotNull();
    }
}
