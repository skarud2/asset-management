package com.via.shinvia.futuresim.event;

import com.via.shinvia.loan.ratesimulation.common.dto.request.CustomRatePathPoint;
import com.via.shinvia.marketdata.ForwardRateCurveService;
import com.via.shinvia.marketdata.ForwardRatePoint;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// RateChangeEvent(SIMPLE/STAGED/MARKET_IMPLIED)를 loan/ratesimulation의
// StagedRateSimulator.simulateCustomPath()가 그대로 받을 수 있는 List<CustomRatePathPoint>로 통일해서
// 만들어준다. 실제 상환 스케줄 계산은 하지 않고 "금리 경로"만 만든다 — loan/ratesimulation은
// 읽기 전용으로 참조만 한다(수정 없음).
@Service
public class RateChangeEventResolver {

    private final ForwardRateCurveService forwardRateCurveService;

    public RateChangeEventResolver(ForwardRateCurveService forwardRateCurveService) {
        this.forwardRateCurveService = forwardRateCurveService;
    }

    public List<CustomRatePathPoint> resolve(RateChangeEvent event, BigDecimal currentRatePercent) {
        return switch (event.mode()) {
            case SIMPLE -> resolveSimple(event, currentRatePercent);
            case STAGED -> resolveStaged(event, currentRatePercent);
            case MARKET_IMPLIED -> resolveMarketImplied(currentRatePercent);
        };
    }

    // 1개월 시점에 한 번에 delta만큼 오른 뒤 계속 유지 — "만약 지금 당장 X%p 오른다면"을 보는 가장 단순한 시나리오.
    private List<CustomRatePathPoint> resolveSimple(RateChangeEvent event, BigDecimal currentRatePercent) {
        BigDecimal delta = event.simpleDeltaPercent() != null ? event.simpleDeltaPercent() : BigDecimal.ZERO;
        return List.of(
                new CustomRatePathPoint(0, currentRatePercent),
                new CustomRatePathPoint(1, currentRatePercent.add(delta))
        );
    }

    // repricingCycleMonths마다 stepDeltaPercent씩 stepCount번 계단식으로 오른다.
    private List<CustomRatePathPoint> resolveStaged(RateChangeEvent event, BigDecimal currentRatePercent) {
        int cycle = event.repricingCycleMonths();
        BigDecimal stepDelta = event.stepDeltaPercent();
        int stepCount = event.stepCount();

        List<CustomRatePathPoint> path = new ArrayList<>();
        path.add(new CustomRatePathPoint(0, currentRatePercent));
        BigDecimal rate = currentRatePercent;
        for (int step = 1; step <= stepCount; step++) {
            rate = rate.add(stepDelta);
            path.add(new CustomRatePathPoint(cycle * step, rate));
        }
        return path;
    }

    // 채권 스팟금리 곡선에서 부트스트랩한 선도금리(ForwardRateCurveService)를 그대로 경로로 쓴다.
    private List<CustomRatePathPoint> resolveMarketImplied(BigDecimal currentRatePercent) {
        List<ForwardRatePoint> forwardRates = forwardRateCurveService.calculateForwardRates(LocalDate.now());

        List<CustomRatePathPoint> path = new ArrayList<>();
        path.add(new CustomRatePathPoint(0, currentRatePercent));
        for (ForwardRatePoint point : forwardRates) {
            path.add(new CustomRatePathPoint(point.monthOffset(), point.impliedRate()));
        }
        return path;
    }
}
