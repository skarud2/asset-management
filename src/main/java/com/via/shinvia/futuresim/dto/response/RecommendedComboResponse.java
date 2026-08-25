package com.via.shinvia.futuresim.dto.response;
import com.via.shinvia.futuresim.service.LeverIntensityCalculator;
import java.math.BigDecimal;
import java.util.List;
public record RecommendedComboResponse(List<Lever> levers, Integer baselineMonthsToGoal, Integer comboMonthsToGoal, Integer diffMonths, BigDecimal assumedReturnRate) {
    public record Lever(LeverIntensityCalculator.LeverType type, BigDecimal intensity, Integer diffMonths, boolean isAssumption) {}
}
