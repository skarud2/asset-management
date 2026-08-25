package com.via.shinvia.futuresim.service;

import com.via.shinvia.futuresim.mapper.FutureSimFinancialSnapshotMapper;
import com.via.shinvia.stresstest.mapper.StressTestLoanMapper;
import com.via.shinvia.stresstest.service.AnnualIncomeProvider;
import com.via.shinvia.stresstest.service.LoanBurdenAggregator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFinancialSnapshotServiceTest {

    @Mock private AnnualIncomeProvider annualIncomeProvider;
    @Mock private LoanBurdenAggregator loanBurdenAggregator;
    @Mock private StressTestLoanMapper loanMapper;
    @Mock private FutureSimFinancialSnapshotMapper financialSnapshotMapper;

    @Test
    void 재무프로필_유동자산과_연동_계좌_잔액을_함께_사용한다() {
        Long userId = 1L;
        when(annualIncomeProvider.findAnnualIncome(userId))
                .thenReturn(new AnnualIncomeProvider.Result(true, BigDecimal.ZERO));
        when(financialSnapshotMapper.findLiquidAssetAmountByUserId(userId)).thenReturn(new BigDecimal("99000000"));
        when(financialSnapshotMapper.sumAccountBalanceByUserId(userId)).thenReturn(new BigDecimal("5000000"));
        when(loanMapper.findNormalLoansByUserId(userId)).thenReturn(List.of());
        when(loanBurdenAggregator.aggregate(userId, BigDecimal.ZERO))
                .thenReturn(new LoanBurdenAggregator.Result(BigDecimal.ZERO, BigDecimal.ZERO));

        UserFinancialSnapshotService.Snapshot snapshot = service().getSnapshot(userId);

        assertThat(snapshot.liquidAsset()).isEqualByComparingTo("104000000");
        assertThat(snapshot.netWorth()).isEqualByComparingTo("104000000");
    }

    private UserFinancialSnapshotService service() {
        return new UserFinancialSnapshotService(
                annualIncomeProvider, loanBurdenAggregator, loanMapper, financialSnapshotMapper
        );
    }
}
