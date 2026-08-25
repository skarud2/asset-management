package com.via.shinvia.futuresim.service;

import com.via.shinvia.futuresim.entity.KosisAgeGroupNetWorth;
import com.via.shinvia.futuresim.entity.KosisHouseholdNetWorth;
import com.via.shinvia.futuresim.exception.InvalidAgeException;
import com.via.shinvia.futuresim.exception.InvalidHouseholdSizeException;
import com.via.shinvia.futuresim.mapper.AgeGroupNetWorthBenchmarkMapper;
import com.via.shinvia.futuresim.mapper.HouseholdNetWorthBenchmarkMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

// "지금 내 상태" 단계에서 사용자 가구원수/나이에 맞는 KOSIS 순자산 벤치마크를 조회한다.
// household_size가 null(자기입력 안 한 사용자)이면 '전체' 벤치마크로 폴백하고 isDefault=true를 반환한다.
// null이 아닌데 매칭되는 라벨이 없으면(예: "10인") 잘못된 값으로 보고 예외를 던진다.
@Service
public class HouseholdNetWorthBenchmarkService {

    private static final String DEFAULT_HOUSEHOLD_SIZE_LABEL = "전체";

    private final HouseholdNetWorthBenchmarkMapper benchmarkMapper;
    private final AgeGroupNetWorthBenchmarkMapper ageGroupBenchmarkMapper;

    public HouseholdNetWorthBenchmarkService(
            HouseholdNetWorthBenchmarkMapper benchmarkMapper,
            AgeGroupNetWorthBenchmarkMapper ageGroupBenchmarkMapper
    ) {
        this.benchmarkMapper = benchmarkMapper;
        this.ageGroupBenchmarkMapper = ageGroupBenchmarkMapper;
    }

    public Result getBenchmark(String householdSize) {
        boolean useDefault = (householdSize == null);
        String lookupLabel = useDefault ? DEFAULT_HOUSEHOLD_SIZE_LABEL : householdSize;

        KosisHouseholdNetWorth row = benchmarkMapper.findLatestByLabel(lookupLabel);

        if (row == null) {
            if (useDefault) {
                throw new IllegalStateException(
                        "KOSIS 가구원수별 순자산 벤치마크 기본값('전체')이 없습니다. 시드 데이터를 확인해주세요."
                );
            }
            throw new InvalidHouseholdSizeException(householdSize);
        }

        return Result.from(row, useDefault);
    }

    // 가구원수 구성비 막대(1인~5인이상) 5개 구간을 household_size_code 순으로 전부 반환한다.
    // 2단계 스펙트럼 시각화용 — household_distribution_pct(전체 가구 중 비중)까지 함께 내려간다.
    public List<Result> getAllHouseholdBenchmarks() {
        return benchmarkMapper.findAllLatest().stream()
                .map((row) -> Result.from(row, false))
                .toList();
    }

    // userAge를 5구간(29세 이하/30~39세/40~49세/50~59세/60세 이상) 중 하나로 매핑해 벤치마크를 조회한다.
    // 사용자 나이는 app_user.birth_date에서 계산해서 넘겨준다 (UserMapper.findByUserId(userId).getBirthDate() 참고,
    // 예: Period.between(birthDate, LocalDate.now()).getYears()). 이 서비스는 계산된 나이(int)만 입력받는다.
    public AgeGroupResult getAgeGroupBenchmark(int userAge) {
        if (userAge < 0) {
            throw new InvalidAgeException(userAge);
        }

        String ageGroupLabel = resolveAgeGroupLabel(userAge);
        KosisAgeGroupNetWorth row = ageGroupBenchmarkMapper.findLatestByLabel(ageGroupLabel);

        if (row == null) {
            throw new IllegalStateException(
                    "KOSIS 연령대별 순자산 벤치마크 데이터가 없습니다: " + ageGroupLabel
            );
        }

        return AgeGroupResult.from(row);
    }

    // 연령대 스펙트럼(29세 이하~60세 이상) 5개 구간을 age_group_code 순으로 전부 반환한다.
    // "지금 내 상태"의 단일 연령대 비교(getAgeGroupBenchmark)와 달리, 2단계 스펙트럼 시각화용.
    public List<AgeGroupResult> getAllAgeGroupBenchmarks() {
        return ageGroupBenchmarkMapper.findAllLatest().stream()
                .map(AgeGroupResult::from)
                .toList();
    }

    private String resolveAgeGroupLabel(int userAge) {
        if (userAge < 30) {
            return "29세 이하";
        }
        if (userAge < 40) {
            return "30~39세";
        }
        if (userAge < 50) {
            return "40~49세";
        }
        if (userAge < 60) {
            return "50~59세";
        }
        return "60세 이상";
    }

    public record Result(
            String householdSizeCode,
            String householdSizeLabel,
            String surveyYear,
            BigDecimal avgIncome,
            BigDecimal medianIncome,
            BigDecimal avgAsset,
            BigDecimal medianAsset,
            BigDecimal avgDebt,
            BigDecimal medianDebt,
            BigDecimal avgNetWorth,
            BigDecimal medianNetWorth,
            BigDecimal avgDebtRepayment,
            BigDecimal medianDebtRepayment,
            BigDecimal avgHouseholdHeadAge,
            BigDecimal householdDistributionPct,
            boolean isDefault
    ) {
        private static Result from(KosisHouseholdNetWorth row, boolean isDefault) {
            return new Result(
                    row.getHouseholdSizeCode(),
                    row.getHouseholdSizeLabel(),
                    row.getSurveyYear(),
                    row.getAvgIncome(),
                    row.getMedianIncome(),
                    row.getAvgAsset(),
                    row.getMedianAsset(),
                    row.getAvgDebt(),
                    row.getMedianDebt(),
                    row.getAvgNetWorth(),
                    row.getMedianNetWorth(),
                    row.getAvgDebtRepayment(),
                    row.getMedianDebtRepayment(),
                    row.getAvgHouseholdHeadAge(),
                    row.getHouseholdDistributionPct(),
                    isDefault
            );
        }
    }

    public record AgeGroupResult(
            String ageGroupCode,
            String ageGroupLabel,
            String surveyYear,
            BigDecimal avgIncome,
            BigDecimal medianIncome,
            BigDecimal avgAsset,
            BigDecimal medianAsset,
            BigDecimal avgDebt,
            BigDecimal medianDebt,
            BigDecimal avgNetWorth,
            BigDecimal medianNetWorth,
            BigDecimal avgDebtRepayment,
            BigDecimal medianDebtRepayment,
            BigDecimal avgHouseholdHeadAge,
            BigDecimal householdDistributionPct
    ) {
        private static AgeGroupResult from(KosisAgeGroupNetWorth row) {
            return new AgeGroupResult(
                    row.getAgeGroupCode(),
                    row.getAgeGroupLabel(),
                    row.getSurveyYear(),
                    row.getAvgIncome(),
                    row.getMedianIncome(),
                    row.getAvgAsset(),
                    row.getMedianAsset(),
                    row.getAvgDebt(),
                    row.getMedianDebt(),
                    row.getAvgNetWorth(),
                    row.getMedianNetWorth(),
                    row.getAvgDebtRepayment(),
                    row.getMedianDebtRepayment(),
                    row.getAvgHouseholdHeadAge(),
                    row.getHouseholdDistributionPct()
            );
        }
    }
}
