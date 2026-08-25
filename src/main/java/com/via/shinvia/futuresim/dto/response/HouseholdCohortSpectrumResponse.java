package com.via.shinvia.futuresim.dto.response;

import java.math.BigDecimal;
import java.util.List;

// 목표 설정(2단계) 가구원수 구성비 막대 시각화용. cohorts는 1인~5인이상 5개 구간을
// household_size_code 순으로 담는다. userHouseholdSizeIndex는 그중 사용자가 속한 구간의 인덱스(0~4)다.
// AgeCohortSpectrumResponse와 달리 distributionPct(전체 가구 중 비중)를 함께 내려서
// 막대의 폭(구성비)과 명도(순자산 중앙값)를 각각 다른 데이터로 인코딩할 수 있게 한다.
public record HouseholdCohortSpectrumResponse(
        List<CohortPoint> cohorts,
        int userHouseholdSizeIndex,
        String surveyYear
) {
    public record CohortPoint(String label, BigDecimal medianNetWorth, BigDecimal distributionPct) {
    }
}
