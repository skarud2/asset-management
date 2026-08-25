package com.via.shinvia.futuresim.dto.response;

import java.math.BigDecimal;
import java.util.List;

// 목표 설정(2단계) 연령대 스펙트럼 시각화용. cohorts는 29세 이하~60세 이상 5개 구간을
// age_group_code 순으로 담는다. userAgeGroupIndex는 그중 사용자가 속한 구간의 인덱스(0~4)다.
public record AgeCohortSpectrumResponse(
        List<CohortPoint> cohorts,
        int userAgeGroupIndex,
        Integer userAge,
        String surveyYear,
        BigDecimal currentNetWorth
) {
    public record CohortPoint(String label, BigDecimal medianNetWorth) {
    }
}
