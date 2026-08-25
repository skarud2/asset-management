package com.via.shinvia.loan.recommendation.service;

import com.via.shinvia.loan.recommendation.dto.LoanRecommendationRequest;
import com.via.shinvia.loan.recommendation.dto.LoanRecommendationResult;
import com.via.shinvia.loan.recommendation.mapper.LoanRecommendationMapper;
import com.via.shinvia.loan.recommendation.model.CreditCandidateRow;
import com.via.shinvia.loan.recommendation.model.ExcludedLoanProduct;
import com.via.shinvia.loan.recommendation.model.HousingCandidateRow;
import com.via.shinvia.loan.recommendation.model.LoanCostEstimate;
import com.via.shinvia.loan.recommendation.model.LoanPurpose;
import com.via.shinvia.loan.recommendation.model.LoanRecommendationSaveRow;
import com.via.shinvia.loan.recommendation.model.OptionChoice;
import com.via.shinvia.loan.recommendation.model.RecommendedLoanProduct;
import com.via.shinvia.loan.recommendation.model.RepaymentCalculationMethod;
import com.via.shinvia.loan.recommendation.model.UserFinancialProfileView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanRecommendationService {

    private static final int SAVE_LIMIT = 5;
    private static final BigDecimal MAX_REQUEST_AMOUNT =
            BigDecimal.valueOf(10_000_000_000L);
    private static final int MAX_TERM_MONTHS = 600;

    private final LoanRecommendationMapper mapper;
    private final LoanCostCalculator costCalculator;

    public UserFinancialProfileView findProfile(String loginEmail) {
        if (loginEmail == null || loginEmail.isBlank()) {
            return null;
        }
        return mapper.findProfileByLoginEmail(loginEmail.trim());
    }

    public List<OptionChoice> findRateTypeOptions() {
        return mapper.findRateTypeOptions();
    }

    public List<OptionChoice> findRepaymentTypeOptions() {
        return mapper.findRepaymentTypeOptions();
    }

    public List<OptionChoice> findCollateralTypeOptions() {
        return mapper.findCollateralTypeOptions();
    }

    @Transactional
    public LoanRecommendationResult recommend(
            String loginEmail,
            LoanRecommendationRequest request
    ) {
        LoanPurpose purpose = LoanPurpose.fromCode(normalizeUpper(request.getLoanPurpose()));
        request.normalize(purpose.getLoanType());
        RepaymentCalculationMethod calculationMethod =
                RepaymentCalculationMethod.fromCode(request.getCalculationMethod());
        validateRequest(request);

        UserFinancialProfileView profile = requireProfile(loginEmail);
        EvaluationResult evaluation;

        if ("CREDIT".equals(purpose.getLoanType())) {
            if (profile.getCreditScore() == null) {
                throw new IllegalArgumentException(
                        "신용대출 추천에는 금융 프로필의 신용점수가 필요합니다."
                );
            }
            evaluation = evaluateCredit(
                    mapper.findCreditCandidates(),
                    profile,
                    request,
                    purpose,
                    calculationMethod
            );
        } else {
            evaluation = evaluateHousing(
                    mapper.findHousingCandidates(purpose.getLoanType()),
                    request,
                    purpose,
                    calculationMethod
            );
        }

        List<RecommendedLoanProduct> ranked = rankRecommendations(
                evaluation.eligibleProducts()
        );
        List<RecommendedLoanProduct> top = ranked.stream()
                .limit(SAVE_LIMIT)
                .toList();

        mapper.deactivateRecommendations(
                profile.getUserFinancialProfileId(),
                purpose.getLoanType()
        );

        for (RecommendedLoanProduct item : top) {
            mapper.upsertRecommendation(toSaveRow(
                    profile.getUserFinancialProfileId(),
                    purpose,
                    request,
                    calculationMethod,
                    item
            ));
        }

        return new LoanRecommendationResult(
                purpose.getCode(),
                purpose.getLabel(),
                purpose.getLoanType(),
                request.getRequestedAmount(),
                request.getTermMonths(),
                calculationMethod.getLabel(),
                evaluation.sourceProductCount(),
                ranked.size(),
                evaluation.excludedProducts().size(),
                top,
                evaluation.excludedProducts()
        );
    }

    private void validateRequest(LoanRecommendationRequest request) {
        if (request.getRequestedAmount() == null
                || request.getRequestedAmount().signum() <= 0) {
            throw new IllegalArgumentException("희망 대출금액은 0원보다 커야 합니다.");
        }
        if (request.getRequestedAmount().compareTo(MAX_REQUEST_AMOUNT) > 0) {
            throw new IllegalArgumentException("희망 대출금액은 100억 원 이하로 입력해 주세요.");
        }
        if (request.getTermMonths() == null
                || request.getTermMonths() < 1
                || request.getTermMonths() > MAX_TERM_MONTHS) {
            throw new IllegalArgumentException("상환기간은 1개월 이상 600개월 이하로 입력해 주세요.");
        }
    }

    private UserFinancialProfileView requireProfile(String loginEmail) {
        UserFinancialProfileView profile = findProfile(loginEmail);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "로그인 사용자와 연결된 금융 프로필이 없습니다. "
                            + "user_financial_profile 데이터를 먼저 등록해 주세요."
            );
        }
        return profile;
    }

    private EvaluationResult evaluateCredit(
            List<CreditCandidateRow> sourceRows,
            UserFinancialProfileView profile,
            LoanRecommendationRequest request,
            LoanPurpose purpose,
            RepaymentCalculationMethod calculationMethod
    ) {
        Map<Long, List<CreditCandidateRow>> rowsByProduct = groupByProduct(
                sourceRows,
                CreditCandidateRow::getCatalogProductId
        );
        List<RecommendedLoanProduct> eligible = new ArrayList<>();
        List<ExcludedLoanProduct> excluded = new ArrayList<>();

        for (List<CreditCandidateRow> rows : rowsByProduct.values()) {
            CreditCandidateRow first = rows.get(0);
            Set<String> reasons = commonExclusionReasons(
                    first.getMaxLimitAmount(),
                    first.getMaxPeriodMonths(),
                    request
            );
            // 마이너스한도대출은 실제 사용잔액에 따라 이자가 달라
            // 일반 분할상환 대출과 동일한 비용 기준으로 비교하지 않는다.
            if ("마이너스한도대출".equals(first.getCreditProductTypeName())) {
                reasons.add(
                        "마이너스한도대출은 실제 사용잔액에 따라 이자가 달라 "
                                + "일반 대출과 동일한 월 납입액·총상환액 기준으로 비교하지 않습니다."
                );
            }

            CreditRateResolution rateResolution = rows.stream()
                    .map(row -> resolveDirectCreditRate(row, profile.getCreditScore()))
                    .filter(resolution -> resolution.rate() != null)
                    .min(Comparator.comparing(CreditRateResolution::rate))
                    .orElse(null);

            if (rateResolution == null) {
                reasons.add(
                        "현재 신용점수 " + profile.getCreditScore()
                                + "점 구간의 공시금리가 없어 개인화 비용을 계산할 수 없습니다."
                );
            }

            if (!reasons.isEmpty()) {
                excluded.add(toExcluded(first, reasons));
                continue;
            }

            LoanCostEstimate estimate = costCalculator.calculate(
                    request.getRequestedAmount(),
                    rateResolution.rate(),
                    request.getTermMonths(),
                    calculationMethod
            );

            String baseReason = purpose.getLabel()
                    + " 목적의 신용대출 중 현재 신용점수 "
                    + profile.getCreditScore() + "점에 해당하는 "
                    + rateResolution.basis()
                    + "가 확인된 상품입니다.";

            eligible.add(RecommendedLoanProduct.builder()
                    .catalogProductId(first.getCatalogProductId())
                    .productName(first.getProductName())
                    .loanType(first.getLoanType())
                    .institutionName(first.getInstitutionName())
                    .joinWay(first.getJoinWay())
                    .recommendedRate(rateResolution.rate())
                    .catalogMinRate(first.getCatalogMinRate())
                    .catalogMaxRate(first.getCatalogMaxRate())

                    .maxLimitAmount(first.getMaxLimitAmount())
                    .maxPeriodMonths(first.getMaxPeriodMonths())
                    .requestedAmount(request.getRequestedAmount())
                    .requestedTermMonths(request.getTermMonths())
                    .estimatedMonthlyPayment(estimate.monthlyPayment())
                    .averageMonthlyPayment(estimate.averageMonthlyPayment())
                    .estimatedTotalInterest(estimate.totalInterest())
                    .estimatedTotalCost(estimate.totalCost())
                    .paymentLabel(estimate.paymentLabel())
                    .calculationMethodLabel(calculationMethod.getLabel())
                    .rateBasis(rateResolution.basis())
                    .targetDescription(first.getTargetDescription())
                    .reason(baseReason)
                    .qualificationNote(buildQualificationNote(
                            first.getMaxLimitAmount(),
                            first.getMaxPeriodMonths()
                    ))
                    .preferenceMatchCount(0)
                    .requestedPreferenceCount(0)
                    .build());
        }

        return new EvaluationResult(rowsByProduct.size(), eligible, excluded);
    }

    private EvaluationResult evaluateHousing(
            List<HousingCandidateRow> sourceRows,
            LoanRecommendationRequest request,
            LoanPurpose purpose,
            RepaymentCalculationMethod calculationMethod
    ) {
        Map<Long, List<HousingCandidateRow>> rowsByProduct = groupByProduct(
                sourceRows,
                HousingCandidateRow::getCatalogProductId
        );
        List<RecommendedLoanProduct> eligible = new ArrayList<>();
        List<ExcludedLoanProduct> excluded = new ArrayList<>();
        int requestedPreferenceCount = requestedPreferenceCount(request, purpose.getLoanType());

        for (List<HousingCandidateRow> rows : rowsByProduct.values()) {
            HousingCandidateRow first = rows.get(0);
            Set<String> reasons = commonExclusionReasons(
                    first.getMaxLimitAmount(),
                    first.getMaxPeriodMonths(),
                    request
            );

            List<HousingCandidateRow> matchingRows = rows.stream()
                    .filter(row -> matchesAllRequestedPreferences(
                            row,
                            request,
                            purpose.getLoanType()
                    ))
                    .toList();

            if (matchingRows.isEmpty()) {
                reasons.addAll(preferenceExclusionReasons(
                        rows,
                        request,
                        purpose.getLoanType()
                ));
            }

            HousingCandidateRow selected = matchingRows.stream()
                    .filter(row -> resolveHousingRate(row) != null)
                    .min(Comparator.comparing(this::resolveHousingRate))
                    .orElse(null);

            if (!matchingRows.isEmpty() && selected == null) {
                reasons.add("선택 조건에 맞는 옵션에 비교 가능한 공시금리가 없습니다.");
            }

            if (!reasons.isEmpty()) {
                excluded.add(toExcluded(first, reasons));
                continue;
            }

            BigDecimal representativeRate = resolveHousingRate(selected);
            LoanCostEstimate estimate = costCalculator.calculate(
                    request.getRequestedAmount(),
                    representativeRate,
                    request.getTermMonths(),
                    calculationMethod
            );
            int matchCount = requestedPreferenceCount;

            String baseReason = buildHousingBaseReason(
                    purpose,
                    matchCount,
                    requestedPreferenceCount
            );

            eligible.add(RecommendedLoanProduct.builder()
                    .catalogProductId(selected.getCatalogProductId())
                    .productName(selected.getProductName())
                    .loanType(selected.getLoanType())
                    .institutionName(selected.getInstitutionName())
                    .joinWay(selected.getJoinWay())
                    .recommendedRate(representativeRate)
                    .catalogMinRate(selected.getCatalogMinRate())
                    .catalogMaxRate(selected.getCatalogMaxRate())
                    .optionMinRate(selected.getOptionMinRate())
                    .optionMaxRate(selected.getOptionMaxRate())
                    .maxLimitAmount(selected.getMaxLimitAmount())
                    .maxPeriodMonths(selected.getMaxPeriodMonths())
                    .requestedAmount(request.getRequestedAmount())
                    .requestedTermMonths(request.getTermMonths())
                    .estimatedMonthlyPayment(estimate.monthlyPayment())
                    .averageMonthlyPayment(estimate.averageMonthlyPayment())
                    .estimatedTotalInterest(estimate.totalInterest())
                    .estimatedTotalCost(estimate.totalCost())
                    .paymentLabel(estimate.paymentLabel())
                    .calculationMethodLabel(calculationMethod.getLabel())
                    .rateBasis(housingRateBasis(selected))
                    .targetDescription(selected.getTargetDescription())
                    .reason(baseReason)
                    .qualificationNote(buildQualificationNote(
                            selected.getMaxLimitAmount(),
                            selected.getMaxPeriodMonths()
                    ))
                    .preferenceMatchCount(matchCount)
                    .requestedPreferenceCount(requestedPreferenceCount)
                    .rateTypeCode(selected.getRateTypeCode())
                    .rateTypeName(selected.getRateTypeName())
                    .repaymentTypeCode(selected.getRepaymentTypeCode())
                    .repaymentTypeName(selected.getRepaymentTypeName())
                    .collateralTypeCode(selected.getCollateralTypeCode())
                    .collateralTypeName(selected.getCollateralTypeName())
                    .loanLimitText(selected.getLoanLimitText())
                    .build());
        }

        return new EvaluationResult(rowsByProduct.size(), eligible, excluded);
    }

    private <T> Map<Long, List<T>> groupByProduct(
            List<T> rows,
            Function<T, Long> idExtractor
    ) {
        return rows.stream().collect(Collectors.groupingBy(
                idExtractor,
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }

    private Set<String> commonExclusionReasons(
            BigDecimal maxLimitAmount,
            Integer maxPeriodMonths,
            LoanRecommendationRequest request
    ) {
        Set<String> reasons = new LinkedHashSet<>();

        if (maxLimitAmount != null
                && maxLimitAmount.compareTo(request.getRequestedAmount()) < 0) {
            reasons.add(
                    "공시된 최대한도 " + formatWon(maxLimitAmount)
                            + "이 희망금액 " + formatWon(request.getRequestedAmount())
                            + "보다 적습니다."
            );
        }

        if (maxPeriodMonths != null
                && maxPeriodMonths < request.getTermMonths()) {
            reasons.add(
                    "공시된 최대기간 " + maxPeriodMonths
                            + "개월이 희망기간 " + request.getTermMonths()
                            + "개월보다 짧습니다."
            );
        }

        return reasons;
    }

    private Set<String> preferenceExclusionReasons(
            List<HousingCandidateRow> rows,
            LoanRecommendationRequest request,
            String loanType
    ) {
        Set<String> reasons = new LinkedHashSet<>();

        if (request.getPreferredRateTypeCode() != null
                && rows.stream().noneMatch(row -> Objects.equals(
                        request.getPreferredRateTypeCode(),
                        row.getRateTypeCode()
                ))) {
            reasons.add("선택한 금리유형을 제공하지 않습니다.");
        }

        if (request.getPreferredRepaymentTypeCode() != null
                && rows.stream().noneMatch(row -> Objects.equals(
                        request.getPreferredRepaymentTypeCode(),
                        row.getRepaymentTypeCode()
                ))) {
            reasons.add("선택한 상품 상환방식을 제공하지 않습니다.");
        }

        if ("MORTGAGE".equals(loanType)
                && request.getPreferredCollateralTypeCode() != null
                && rows.stream().noneMatch(row -> Objects.equals(
                        request.getPreferredCollateralTypeCode(),
                        row.getCollateralTypeCode()
                ))) {
            reasons.add("선택한 담보유형을 제공하지 않습니다.");
        }

        if (reasons.isEmpty()) {
            reasons.add("선택한 금리·상환·담보 조건을 동시에 충족하는 상품 옵션이 없습니다.");
        }
        return reasons;
    }

    private boolean matchesAllRequestedPreferences(
            HousingCandidateRow row,
            LoanRecommendationRequest request,
            String loanType
    ) {
        if (request.getPreferredRateTypeCode() != null
                && !Objects.equals(
                        request.getPreferredRateTypeCode(),
                        row.getRateTypeCode()
                )) {
            return false;
        }

        if (request.getPreferredRepaymentTypeCode() != null
                && !Objects.equals(
                        request.getPreferredRepaymentTypeCode(),
                        row.getRepaymentTypeCode()
                )) {
            return false;
        }

        return !"MORTGAGE".equals(loanType)
                || request.getPreferredCollateralTypeCode() == null
                || Objects.equals(
                        request.getPreferredCollateralTypeCode(),
                        row.getCollateralTypeCode()
                );
    }

    private CreditRateResolution resolveDirectCreditRate(
            CreditCandidateRow row,
            int score
    ) {
        if (score >= 901) {
            return new CreditRateResolution(
                    row.getRateOver900(),
                    "901점 이상 구간 공시금리"
            );
        }
        if (score >= 801) {
            return new CreditRateResolution(
                    row.getRate801To900(),
                    "801~900점 구간 공시금리"
            );
        }
        if (score >= 701) {
            return new CreditRateResolution(
                    row.getRate701To800(),
                    "701~800점 구간 공시금리"
            );
        }
        if (score >= 601) {
            return new CreditRateResolution(
                    row.getRate601To700(),
                    "601~700점 구간 공시금리"
            );
        }
        if (score >= 501) {
            return new CreditRateResolution(
                    row.getRate501To600(),
                    "501~600점 구간 공시금리"
            );
        }
        if (score >= 401) {
            return new CreditRateResolution(
                    row.getRate401To500(),
                    "401~500점 구간 공시금리"
            );
        }
        if (score >= 301) {
            return new CreditRateResolution(
                    row.getRate301To400(),
                    "301~400점 구간 공시금리"
            );
        }
        return new CreditRateResolution(
                row.getRate300OrBelow(),
                "300점 이하 구간 공시금리"
        );
    }

    private BigDecimal resolveHousingRate(HousingCandidateRow row) {
        BigDecimal averageRate = row.getOptionAverageRate();
        BigDecimal minRate = row.getOptionMinRate();
        BigDecimal maxRate = row.getOptionMaxRate();

        // 평균금리가 옵션 금리범위 안에 있을 때만 사용
        if (averageRate != null && isRateWithinRange(
                averageRate,
                minRate,
                maxRate
        )) {
            return averageRate;
        }

        // 평균금리가 범위를 벗어나거나 없으면 옵션 최저·최고금리 대표값 사용
        BigDecimal optionRate = representativeRate(
                minRate,
                maxRate
        );

        if (optionRate != null) {
            return optionRate;
        }

        // 옵션 금리도 없으면 카탈로그 금리 fallback
        return representativeRate(
                row.getCatalogMinRate(),
                row.getCatalogMaxRate()
        );
    }

    private boolean isRateWithinRange(
            BigDecimal rate,
            BigDecimal minRate,
            BigDecimal maxRate
    ) {
        if (rate == null) {
            return false;
        }

        if (minRate != null && rate.compareTo(minRate) < 0) {
            return false;
        }

        if (maxRate != null && rate.compareTo(maxRate) > 0) {
            return false;
        }

        return true;
    }

    private String housingRateBasis(HousingCandidateRow row) {
        if (row.getOptionAverageRate() != null
                && isRateWithinRange(
                row.getOptionAverageRate(),
                row.getOptionMinRate(),
                row.getOptionMaxRate()
        )) {
            return "선택 옵션 평균 공시금리";
        }

        if (row.getOptionMinRate() != null
                || row.getOptionMaxRate() != null) {
            return "선택 옵션 금리범위 대표값";
        }

        return "상품 카탈로그 대표금리";
    }

    private BigDecimal representativeRate(
            BigDecimal minRate,
            BigDecimal maxRate
    ) {
        if (minRate != null && maxRate != null) {
            return minRate.add(maxRate)
                    .divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
        }
        return minRate != null ? minRate : maxRate;
    }

    private int requestedPreferenceCount(
            LoanRecommendationRequest request,
            String loanType
    ) {
        int count = 0;
        if (request.getPreferredRateTypeCode() != null) count++;
        if (request.getPreferredRepaymentTypeCode() != null) count++;
        if ("MORTGAGE".equals(loanType)
                && request.getPreferredCollateralTypeCode() != null) {
            count++;
        }
        return count;
    }

    private String buildHousingBaseReason(
            LoanPurpose purpose,
            int matchCount,
            int requestedPreferenceCount
    ) {
        if (requestedPreferenceCount == 0) {
            return purpose.getLabel()
                    + " 목적에 맞는 활성 공시상품 중 비교 가능한 금리가 있는 상품입니다.";
        }

        return purpose.getLabel() + " 목적에 맞는 상품이며, 선택한 상품 조건 "
                + requestedPreferenceCount + "개를 모두 충족했습니다."
                + " 확인된 조건 일치 수는 " + matchCount + "개입니다.";
    }

    private String buildQualificationNote(
            BigDecimal maxLimitAmount,
            Integer maxPeriodMonths
    ) {
        List<String> notes = new ArrayList<>();
        if (maxLimitAmount == null) {
            notes.add("숫자형 최대한도가 공시되지 않아 희망금액 충족 여부는 금융기관 확인이 필요합니다.");
        }
        if (maxPeriodMonths == null) {
            notes.add("숫자형 최대기간이 공시되지 않아 희망기간 가능 여부는 금융기관 확인이 필요합니다.");
        }
        notes.add("직업·재직기간 등 자유문장 가입요건은 자동 탈락조건으로 사용하지 않았습니다.");
        return String.join(" ", notes);
    }
    private List<RecommendedLoanProduct> rankRecommendations(
            List<RecommendedLoanProduct> products
    ) {
        List<RecommendedLoanProduct> sorted = new ArrayList<>(products);

        sorted.sort(Comparator
                .comparing(
                        RecommendedLoanProduct::getEstimatedTotalCost,
                        Comparator.nullsLast(BigDecimal::compareTo)
                )
                .thenComparing(
                        RecommendedLoanProduct::getEstimatedMonthlyPayment,
                        Comparator.nullsLast(BigDecimal::compareTo)
                )
                .thenComparing(
                        RecommendedLoanProduct::getRecommendedRate,
                        Comparator.nullsLast(BigDecimal::compareTo)
                )
                .thenComparing(
                        RecommendedLoanProduct::getInstitutionName,
                        Comparator.nullsLast(String::compareTo)
                )
                .thenComparing(RecommendedLoanProduct::getCatalogProductId));

        List<RecommendedLoanProduct> ranked = new ArrayList<>();

        for (int index = 0; index < sorted.size(); index++) {
            int rank = index + 1;
            RecommendedLoanProduct item = sorted.get(index);

            String badge = switch (rank) {
                case 1 -> "총상환액 최소";
                case 2 -> "낮은 총상환액 후보";
                default -> "비교조건 충족";
            };

            String costReason = rank == 1
                    ? " 동일한 희망금액·기간·계산방식으로 비교한 상품 중 예상 총상환액이 가장 낮습니다."
                    : " 동일한 조건으로 비교한 상품 중 예상 총상환액 " + rank + "위입니다.";

            ranked.add(item.toBuilder()
                    .rank(rank)
                    .badge(badge)
                    .reason(item.getReason() + costReason)
                    .build());
        }

        return ranked;
    }

    private ExcludedLoanProduct toExcluded(
            CreditCandidateRow row,
            Set<String> reasons
    ) {
        return ExcludedLoanProduct.builder()
                .catalogProductId(row.getCatalogProductId())
                .productName(row.getProductName())
                .institutionName(row.getInstitutionName())
                .targetDescription(row.getTargetDescription())
                .reasons(List.copyOf(reasons))
                .build();
    }

    private ExcludedLoanProduct toExcluded(
            HousingCandidateRow row,
            Set<String> reasons
    ) {
        return ExcludedLoanProduct.builder()
                .catalogProductId(row.getCatalogProductId())
                .productName(row.getProductName())
                .institutionName(row.getInstitutionName())
                .targetDescription(row.getTargetDescription())
                .reasons(List.copyOf(reasons))
                .build();
    }

    private LoanRecommendationSaveRow toSaveRow(
            Long profileId,
            LoanPurpose purpose,
            LoanRecommendationRequest request,
            RepaymentCalculationMethod calculationMethod,
            RecommendedLoanProduct item
    ) {
        return LoanRecommendationSaveRow.builder()
                .catalogProductId(item.getCatalogProductId())
                .userFinancialProfileId(profileId)
                .productName(item.getProductName())
                .loanType(item.getLoanType())
                .minRate(item.getCatalogMinRate())
                .maxRate(item.getCatalogMaxRate())
                .maxLimitAmount(item.getMaxLimitAmount())
                .maxPeriodMonths(item.getMaxPeriodMonths())
                .targetDescription(abbreviate(item.getReason(), 300))
                .institutionName(item.getInstitutionName())
                .recommendationRank(item.getRank())
                .recommendedRate(item.getRecommendedRate())
                .loanPurpose(purpose.getCode())
                .requestedAmount(request.getRequestedAmount())
                .requestedTermMonths(request.getTermMonths())
                .calculationMethod(calculationMethod.getCode())
                .estimatedMonthlyPayment(item.getEstimatedMonthlyPayment())
                .estimatedTotalInterest(item.getEstimatedTotalInterest())
                .estimatedTotalCost(item.getEstimatedTotalCost())
                .build();
    }

    private String formatWon(BigDecimal amount) {
        return String.format("%,d원", amount.setScale(0, RoundingMode.HALF_UP).longValue());
    }

    private String normalizeUpper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record CreditRateResolution(BigDecimal rate, String basis) {
    }

    private record EvaluationResult(
            int sourceProductCount,
            List<RecommendedLoanProduct> eligibleProducts,
            List<ExcludedLoanProduct> excludedProducts
    ) {
    }
}
