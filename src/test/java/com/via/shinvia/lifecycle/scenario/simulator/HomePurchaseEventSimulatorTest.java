package com.via.shinvia.lifecycle.scenario.simulator;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventInput;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.common.model.LifecycleEventType;
import com.via.shinvia.lifecycle.reference.service.LifecycleReferenceService;
import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomePurchaseEventSimulatorTest {

    @Mock
    private LifecycleReferenceService referenceService;

    @Mock
    private LoanRepaymentCalculator loanRepaymentCalculator;

    @InjectMocks
    private HomePurchaseEventSimulator simulator;

    @Test
    void keepsFundingShortageAsNegativeCashSoNetAssetIsNotOverstated() {
        when(referenceService.getNationalRate(
                LifecycleEventType.HOME_PURCHASE,
                "ACQUISITION_TAX_RATE",
                null
        )).thenReturn(BigDecimal.ZERO);

        LifecycleFinancialStateDto before = LifecycleFinancialStateDto.builder()
                .cashAsset(new BigDecimal("10"))
                .depositAsset(BigDecimal.ZERO)
                .realEstateAsset(BigDecimal.ZERO)
                .totalDebt(BigDecimal.ZERO)
                .monthlyDebtPayment(BigDecimal.ZERO)
                .build();
        LifecycleEventInput input = LifecycleEventInput.builder()
                .eventType(LifecycleEventType.HOME_PURCHASE)
                .acquiredAssetAmount(new BigDecimal("100"))
                .estimatedCost(new BigDecimal("100"))
                .userRequiredAmount(new BigDecimal("30"))
                .newLoanAmount(BigDecimal.ZERO)
                .additionalMonthlyExpense(BigDecimal.ZERO)
                .build();

        var result = simulator.simulate(before, input);

        assertEquals(0, new BigDecimal("-20").compareTo(result.getAfterState().getCashAsset()));
        assertEquals(0, new BigDecimal("20").compareTo(result.getFundingShortage()));
        assertEquals(0, new BigDecimal("100").compareTo(result.getAfterState().getRealEstateAsset()));
    }
}
