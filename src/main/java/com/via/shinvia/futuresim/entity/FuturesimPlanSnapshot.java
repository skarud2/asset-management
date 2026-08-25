package com.via.shinvia.futuresim.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// futuresim_plan_snapshot 1행 — 이름을 붙여 저장한 5단계 실행 계획. 목록/불러오기 화면에서 쓴다.
@Getter
@Setter
@NoArgsConstructor
public class FuturesimPlanSnapshot {

    private Long id;

    private Long userId;

    private String planName;

    private BigDecimal goalAmount;

    private String goalPresetKey;

    private String benchmarkType;

    private String benchmarkLabel;

    private BigDecimal benchmarkMedianNetWorth;

    private BigDecimal assumedReturnRate;

    private String selectedLeversJson;

    private String loanImpactJson;

    private int baselineMonthsToGoal;

    private int projectedMonthsToGoal;

    private int diffMonths;

    private BigDecimal finalNetWorth;

    private LocalDateTime updatedAt;
}
