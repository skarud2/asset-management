package com.via.shinvia.lifecycle.scenario.service;

import com.via.shinvia.lifecycle.common.dto.LifecycleEventResult;
import com.via.shinvia.lifecycle.common.dto.LifecycleFinancialStateDto;
import com.via.shinvia.lifecycle.scenario.dto.LifecycleFeasibilityDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class LifecycleFeasibilityService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal DSR_CAUTION = new BigDecimal("30");
    private static final BigDecimal DSR_DEFER = new BigDecimal("40");

    public LifecycleFeasibilityDto assess(LifecycleEventResult result) {
        if (result == null) {
            return ready("판단할 이벤트 정보가 없습니다.");
        }

        LifecycleFinancialStateDto before = result.getBeforeState();
        LifecycleFinancialStateDto after = result.getAfterState();
        BigDecimal shortage = nvl(result.getFundingShortage());
        BigDecimal afterSaving = after != null
                ? nvl(after.getMonthlySavingCapacity())
                : ZERO;
        BigDecimal afterDsr = after != null ? nvl(after.getDsr()) : ZERO;

        if (afterSaving.signum() < 0) {
            if (shortage.signum() > 0) {
                return deferred(
                        "초기자금이 부족하고 월 적자가 예상됩니다.",
                        "준비기간을 늘리는 것만으로는 해결하기 어려우므로 비용이나 대출 규모를 함께 낮춰야 합니다.",
                        shortage,
                        null
                );
            }
            return deferred(
                    "이벤트 이후 매월 적자가 예상됩니다.",
                    "고정지출이나 대출 규모를 낮춘 뒤 진행 시점을 다시 검토하세요.",
                    ZERO,
                    null
            );
        }

        if (shortage.signum() > 0) {
            Integer delayMonths = calculateDelayMonths(
                    shortage,
                    before != null ? before.getMonthlySavingCapacity() : ZERO
            );
            String message = delayMonths != null
                    ? "이벤트 전 월 저축여력을 유지하면 초기 부족자금 마련에 약 " + delayMonths + "개월이 필요합니다."
                    : "현재 저축여력으로는 부족자금을 해소하기 어려워 비용이나 계획 시점 조정이 필요합니다.";
            return deferred("필요한 초기자금이 부족합니다.", message, shortage, delayMonths);
        }

        if (afterDsr.compareTo(DSR_DEFER) >= 0) {
            return deferred(
                    "대출 상환부담이 높은 수준입니다.",
                    "DSR을 낮출 수 있도록 자기자금을 늘리거나 대출 규모를 줄이는 방안을 권장합니다.",
                    ZERO,
                    null
            );
        }

        if (afterDsr.compareTo(DSR_CAUTION) >= 0) {
            return caution("진행은 가능하지만 대출 상환부담을 점검해야 합니다.");
        }

        return ready("이 이벤트를 반영한 뒤에도 월 저축여력과 대출 상환부담이 안정적인 수준입니다.");
    }

    private Integer calculateDelayMonths(BigDecimal shortage, BigDecimal monthlySaving) {
        BigDecimal saving = nvl(monthlySaving);
        if (saving.signum() <= 0) {
            return null;
        }
        return shortage.divide(saving, 0, RoundingMode.CEILING).intValue();
    }

    private LifecycleFeasibilityDto ready(String message) {
        return LifecycleFeasibilityDto.builder()
                .status("READY")
                .title("계획 진행 가능")
                .message(message)
                .cashGap(ZERO)
                .build();
    }

    private LifecycleFeasibilityDto caution(String message) {
        return LifecycleFeasibilityDto.builder()
                .status("CAUTION")
                .title("주의해서 진행")
                .message(message)
                .cashGap(ZERO)
                .build();
    }

    private LifecycleFeasibilityDto deferred(
            String title,
            String message,
            BigDecimal cashGap,
            Integer delayMonths
    ) {
        return LifecycleFeasibilityDto.builder()
                .status("DEFER")
                .title(title)
                .message(message)
                .cashGap(cashGap)
                .recommendedDelayMonths(delayMonths)
                .build();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : ZERO;
    }
}
