package com.via.shinvia.futuresim.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.via.shinvia.futuresim.entity.FuturesimPlanSnapshot;
import com.via.shinvia.futuresim.mapper.PlanSnapshotMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// 5단계("레버 조합해보기")에서 "다음"/저장 클릭 시 현재 계획을 이름 붙여 저장한다. 사용자당 이름별로
// 1행만 유지하고(futuresim_plan_snapshot.uk_user_plan_name), 같은 이름으로 다시 저장하면 upsert로
// 덮어쓴다 — 이름을 바꾸면 별도 행으로 쌓여서 여러 계획을 저장/목록/불러오기할 수 있다.
@Service
public class PlanSnapshotService {

    private static final String DEFAULT_PLAN_NAME_PREFIX = "계획 ";

    private final PlanSnapshotMapper mapper;
    // 이 프로젝트(Spring Boot 4)에는 com.fasterxml.jackson.databind.ObjectMapper 빈이 자동 등록돼 있지
    // 않아서(Jackson 3 계열만 auto-configure됨), 여기서는 직접 만들어 쓴다 — 레코드 몇 개를 JSON 배열로
    // 바꾸는 단순한 용도라 커스텀 모듈 등록 없이 기본 설정으로 충분하다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlanSnapshotService(PlanSnapshotMapper mapper) {
        this.mapper = mapper;
    }

    public String save(
            Long userId,
            String planName,
            BigDecimal goalAmount,
            String goalPresetKey,
            String benchmarkType,
            String benchmarkLabel,
            BigDecimal benchmarkMedianNetWorth,
            BigDecimal assumedReturnRate,
            List<LeverIntensityCalculator.LeverSelection> selections,
            int baselineMonthsToGoal,
            int projectedMonthsToGoal,
            int diffMonths,
            String loanImpactJson,
            BigDecimal finalNetWorth
    ) {
        String finalPlanName = planName == null || planName.isBlank()
                ? DEFAULT_PLAN_NAME_PREFIX + LocalDate.now()
                : planName.trim();
        mapper.upsert(
                userId, finalPlanName, goalAmount, goalPresetKey, benchmarkType, benchmarkLabel, benchmarkMedianNetWorth,
                assumedReturnRate, toJson(selections), baselineMonthsToGoal, projectedMonthsToGoal,
                diffMonths, loanImpactJson, finalNetWorth
        );
        return finalPlanName;
    }

    // 목록 화면 — 최근 수정한 순서.
    public List<FuturesimPlanSnapshot> list(Long userId) {
        return mapper.findAllByUserId(userId);
    }

    // 불러오기 — 본인 소유가 아니면(다른 사용자의 id를 URL로 넣어 접근하는 경우 포함) null.
    public FuturesimPlanSnapshot getOwned(Long userId, Long id) {
        return mapper.findByIdAndUserId(id, userId);
    }

    private String toJson(List<LeverIntensityCalculator.LeverSelection> selections) {
        try {
            return objectMapper.writeValueAsString(selections);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("선택한 레버 목록을 JSON으로 변환하지 못했습니다", e);
        }
    }
}
