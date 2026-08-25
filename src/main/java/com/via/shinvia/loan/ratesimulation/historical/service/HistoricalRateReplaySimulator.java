package com.via.shinvia.loan.ratesimulation.historical.service;

import com.via.shinvia.client.ecos.EcosApiClient;
import com.via.shinvia.client.ecos.EcosDailyRate;
import com.via.shinvia.loan.ratesimulation.common.dto.request.CustomRatePathPoint;
import com.via.shinvia.loan.ratesimulation.historical.dto.request.HistoricalRateReplayRequest;
import com.via.shinvia.loan.ratesimulation.historical.dto.response.HistoricalRateReplayResponse;
import com.via.shinvia.loan.ratesimulation.historical.dto.response.RateChangePoint;
import com.via.shinvia.loan.ratesimulation.common.entity.LoanAccountSummary;
import com.via.shinvia.loan.ratesimulation.common.exception.InvalidLoanStatusException;
import com.via.shinvia.loan.ratesimulation.common.exception.LoanNotFoundException;
import com.via.shinvia.loan.ratesimulation.historical.exception.NoHistoricalRateDataException;
import com.via.shinvia.loan.ratesimulation.common.mapper.LoanAccountSummaryMapper;
import com.via.shinvia.loan.ratesimulation.common.service.LoanRepaymentCalculator;
import com.via.shinvia.loan.ratesimulation.common.service.StagedRateSimulator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// 실제 한국은행 기준금리 변동 이력(ECOS)을 그대로 재생해서, 과거 금리인상기에 이 대출의
// 월상환액이 어떻게 변했을지 계산. 조회 -> 변경점 추출 -> StagedRateSimulator 엔진 재사용
@Service
public class HistoricalRateReplaySimulator {

    private static final String NORMAL_LOAN_STATUS = "정상";
    private static final String DISCLAIMER = "한국은행 ECOS 실데이터 기반 (통계코드 722Y001)";
    private static final BigDecimal BP_PER_PERCENT = BigDecimal.valueOf(100);

    private final LoanAccountSummaryMapper loanAccountSummaryMapper;
    private final LoanRepaymentCalculator repaymentCalculator;
    private final StagedRateSimulator stagedRateSimulator;
    private final EcosApiClient ecosApiClient;

    public HistoricalRateReplaySimulator(
            LoanAccountSummaryMapper loanAccountSummaryMapper,
            LoanRepaymentCalculator repaymentCalculator,
            StagedRateSimulator stagedRateSimulator,
            EcosApiClient ecosApiClient
    ) {
        this.loanAccountSummaryMapper = loanAccountSummaryMapper;
        this.repaymentCalculator = repaymentCalculator;
        this.stagedRateSimulator = stagedRateSimulator;
        this.ecosApiClient = ecosApiClient;
    }

    public HistoricalRateReplayResponse replay(HistoricalRateReplayRequest request) {
        LoanAccountSummary loan = loanAccountSummaryMapper.findById(request.loanId());
        if (loan == null) {
            throw new LoanNotFoundException(request.loanId());
        }
        if (!NORMAL_LOAN_STATUS.equals(loan.getLoanStatus())) {
            throw new InvalidLoanStatusException();
        }

        List<EcosDailyRate> dailySeries = ecosApiClient.getDailyBaseRate(request.startDate(), request.endDate());
        List<RateChangePoint> changePoints = extractRateChangePoints(dailySeries);

        if (changePoints.isEmpty()) {
            throw new NoHistoricalRateDataException(request.startDate(), request.endDate());
        }

        LocalDate baseDate = changePoints.get(0).date();
        List<CustomRatePathPoint> ratePath = changePoints.stream()
                .map(point -> new CustomRatePathPoint(
                        (int) ChronoUnit.MONTHS.between(baseDate, point.date()),
                        point.newRate()
                ))
                .toList();

        int remainingMonths0 = repaymentCalculator.calculateRemainingMonths(loan.getMaturityAt());

        StagedRateSimulator.CustomPathSimulationResult result = stagedRateSimulator.simulateCustomPath(
                loan.getCurrentBalance(), remainingMonths0, loan.getRepaymentType(), ratePath
        );

        return new HistoricalRateReplayResponse(
                loan.getLoanAccountId(),
                loan.getLoanType(),
                loan.getRateType(),
                request.startDate(),
                request.endDate(),
                ratePath.get(0).rate(),
                result.initialMonthlyPayment(),
                result.path(),
                result.totalSegmentInterest(),
                changePoints.size(),
                result.truncated(),
                DISCLAIMER
        );
    }

    // 날짜순 일별 금리 시계열에서, 전날과 값이 달라진 지점만 추출 (첫 데이터는 시작점으로 무조건 포함)
    public List<RateChangePoint> extractRateChangePoints(List<EcosDailyRate> dailySeries) {
        List<RateChangePoint> points = new ArrayList<>();
        BigDecimal previousRate = null;

        for (EcosDailyRate daily : dailySeries) {
            if (previousRate == null) {
                points.add(new RateChangePoint(daily.date(), daily.rate(), 0));
            } else if (daily.rate().compareTo(previousRate) != 0) {
                int changeBp = daily.rate().subtract(previousRate)
                        .multiply(BP_PER_PERCENT)
                        .setScale(0, RoundingMode.HALF_UP)
                        .intValue();
                points.add(new RateChangePoint(daily.date(), daily.rate(), changeBp));
            }
            previousRate = daily.rate();
        }

        return points;
    }
}
