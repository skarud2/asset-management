package com.via.shinvia.marketdata;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// 수익률곡선(spot rate)에서 부트스트래핑으로 구간별 내재 선도금리를 계산한다.
// (1+s2)^(T2/12) = (1+s1)^(T1/12) * (1+f)^((T2-T1)/12)
// f = [(1+s2)^(T2/12) / (1+s1)^(T1/12)]^(12/(T2-T1)) - 1
@Service
public class ForwardRateCurveService {

    private static final int RATE_SCALE = 3;

    private final BondYieldCurveProvider bondYieldCurveProvider;

    public ForwardRateCurveService(BondYieldCurveProvider bondYieldCurveProvider) {
        this.bondYieldCurveProvider = bondYieldCurveProvider;
    }

    public List<ForwardRatePoint> calculateForwardRates(LocalDate asOfDate) {
        List<YieldCurvePoint> curve = bondYieldCurveProvider.getYieldCurve(asOfDate).stream()
                .sorted(Comparator.comparingInt(YieldCurvePoint::tenorMonths))
                .toList();

        List<ForwardRatePoint> forwardRates = new ArrayList<>();
        YieldCurvePoint previous = null;

        for (YieldCurvePoint point : curve) {
            if (previous == null) {
                // 가장 짧은 만기 구간은 부트스트래핑할 더 짧은 구간이 없으므로, 그 자체의 spot rate가 선도금리
                forwardRates.add(new ForwardRatePoint(point.tenorMonths(), point.yieldRate()));
            } else {
                BigDecimal forwardRate = computeForwardRate(previous, point);
                forwardRates.add(new ForwardRatePoint(point.tenorMonths(), forwardRate));
            }
            previous = point;
        }

        return forwardRates;
    }

    private BigDecimal computeForwardRate(YieldCurvePoint shorter, YieldCurvePoint longer) {
        double s1 = shorter.yieldRate().doubleValue() / 100;
        double s2 = longer.yieldRate().doubleValue() / 100;
        double t1Years = shorter.tenorMonths() / 12.0;
        double t2Years = longer.tenorMonths() / 12.0;
        double periodYears = t2Years - t1Years;

        double growthLonger = Math.pow(1 + s2, t2Years);
        double growthShorter = Math.pow(1 + s1, t1Years);
        double forward = Math.pow(growthLonger / growthShorter, 1 / periodYears) - 1;

        return BigDecimal.valueOf(forward * 100).setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }
}
