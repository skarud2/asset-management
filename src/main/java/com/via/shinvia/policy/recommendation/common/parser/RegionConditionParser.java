package com.via.shinvia.policy.recommendation.common.parser;

import com.via.shinvia.policy.recommendation.common.dto.RecommendationUserDTO;
import com.via.shinvia.policy.recommendation.common.model.ConditionEvaluation;
import com.via.shinvia.policy.recommendation.common.model.ConditionType;
import com.via.shinvia.policy.recommendation.common.util.RegionNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
// 지원지역·세부 거주기간 조건 해석 기능
public class RegionConditionParser {

    private static final Pattern RESIDENCE_PERIOD_A = Pattern.compile(
            "(\\d+)\\s*(년|개월)\\s*이상[^.]{0,40}(?:거주|주소)"
    );

    private static final Pattern RESIDENCE_PERIOD_B = Pattern.compile(
            "(?:거주|주소)[^.]{0,40}(\\d+)\\s*(년|개월)\\s*이상"
    );

    private static final Set<String> LOCAL_AREAS = Set.of(
            "수원시", "용인시", "고양시", "화성시", "성남시", "부천시", "남양주시", "안산시", "평택시", "안양시",
            "시흥시", "파주시", "김포시", "의정부시", "광주시", "하남시", "광명시", "군포시", "양주시", "오산시",
            "이천시", "안성시", "구리시", "의왕시", "포천시", "양평군", "여주시", "동두천시", "과천시", "가평군", "연천군",
            "춘천시", "원주시", "강릉시", "동해시", "태백시", "속초시", "삼척시", "홍천군", "횡성군", "영월군", "평창군", "정선군", "철원군", "화천군", "양구군", "인제군", "고성군", "양양군",
            "청주시", "충주시", "제천시", "보은군", "옥천군", "영동군", "증평군", "진천군", "괴산군", "음성군", "단양군",
            "천안시", "공주시", "보령시", "아산시", "서산시", "논산시", "계룡시", "당진시", "금산군", "부여군", "서천군", "청양군", "홍성군", "예산군", "태안군",
            "전주시", "군산시", "익산시", "정읍시", "남원시", "김제시", "완주군", "진안군", "무주군", "장수군", "임실군", "순창군", "고창군", "부안군",
            "목포시", "여수시", "순천시", "나주시", "광양시", "담양군", "곡성군", "구례군", "고흥군", "보성군", "화순군", "장흥군", "강진군", "해남군", "영암군", "무안군", "함평군", "영광군", "장성군", "완도군", "진도군", "신안군",
            "포항시", "경주시", "김천시", "안동시", "구미시", "영주시", "영천시", "상주시", "문경시", "경산시", "의성군", "청송군", "영양군", "영덕군", "청도군", "고령군", "성주군", "칠곡군", "예천군", "봉화군", "울진군", "울릉군",
            "창원시", "진주시", "통영시", "사천시", "김해시", "밀양시", "거제시", "양산시", "의령군", "함안군", "창녕군", "남해군", "하동군", "산청군", "함양군", "거창군", "합천군",
            "제주시", "서귀포시",
            "종로구", "용산구", "성동구", "광진구", "동대문구", "중랑구", "성북구", "강북구", "도봉구", "노원구", "은평구", "서대문구", "마포구", "양천구", "강서구", "구로구", "금천구", "영등포구", "동작구", "관악구", "서초구", "강남구", "송파구", "강동구", "해운대구", "사하구", "금정구", "연제구", "수영구", "사상구", "기장군", "부산진구", "미추홀구", "연수구", "남동구", "부평구", "계양구", "강화군", "옹진군", "광산구", "유성구", "대덕구", "울주군", "달서구", "달성군", "군위군"
    );

    public List<ConditionEvaluation> evaluate(
            String supportRegion,
            String detailText,
            RecommendationUserDTO user
    ) {
        List<ConditionEvaluation> results = new ArrayList<>();

        evaluateSido(supportRegion, detailText, user, results);
        evaluateSigungu(detailText, user, results);
        evaluateResidencePeriod(detailText, user, results);

        return results;
    }

    private void evaluateSido(
            String supportRegion,
            String detailText,
            RecommendationUserDTO user,
            List<ConditionEvaluation> results
    ) {
        String rawArea = RegionNormalizer.normalizeText(supportRegion);
        if (rawArea.isBlank() || rawArea.contains("전국") || "없음".equals(rawArea)) {
            return;
        }

        String detail = RegionNormalizer.normalizeText(detailText);
        boolean businessLocationBased = isBusinessLocationCondition(detail);
        boolean residenceLocationBased = isResidenceLocationCondition(detail);

        // 사업장 소재지가 자격조건인데 설문에는 사업장 주소가 없다.
        // 거주지와 사업장 소재지를 같은 값으로 간주하면 잘못 탈락시킬 수 있다.
        if (businessLocationBased && !residenceLocationBased) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.REGION,
                    "사업장 소재지 기준은 현재 거주지 정보만으로 확인할 수 없습니다."
            ));
            return;
        }

        String userSido = RegionNormalizer.normalizeSido(user.getResidenceSido());
        if (userSido.isBlank()) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.REGION,
                    "거주지역 정보 확인이 필요합니다."
            ));
            return;
        }

        boolean match = false;
        for (String token : rawArea.split("[,/·\\s]+")) {
            String productSido = RegionNormalizer.normalizeSido(token);
            if (!productSido.isBlank() && productSido.equals(userSido)) {
                match = true;
                break;
            }
        }

        results.add(match
                ? ConditionEvaluation.satisfied(ConditionType.REGION, "지원 지역 조건과 일치합니다.")
                : ConditionEvaluation.notSatisfied(ConditionType.REGION, "지원 지역 조건과 일치하지 않습니다."));

        if (businessLocationBased) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.REGION,
                    "거주지역은 확인되었지만 근무지·사업장 소재지 조건은 별도 확인이 필요합니다."
            ));
        }
    }

    private void evaluateSigungu(
            String detailText,
            RecommendationUserDTO user,
            List<ConditionEvaluation> results
    ) {
        String text = RegionNormalizer.normalizeText(detailText);
        String userSigungu = RegionNormalizer.normalizeSigungu(user.getResidenceSigungu());

        if (isBusinessLocationCondition(text) && !isResidenceLocationCondition(text)) {
            return;
        }

        if (text.isBlank() || userSigungu.isBlank()) {
            return;
        }

        Set<String> mentioned = new LinkedHashSet<>();
        for (String area : LOCAL_AREAS) {
            if (text.contains(area)) {
                mentioned.add(area);
                continue;
            }

            // 남양주처럼 '시'가 생략된 원문 대응
            String shortName = area.replaceAll("(시|군|구)$", "");
            if (shortName.length() >= 3 && text.contains(shortName)) {
                mentioned.add(area);
            }
        }

        if (mentioned.isEmpty()) {
            return;
        }

        boolean match = mentioned.stream().anyMatch(area -> area.equals(userSigungu));
        results.add(match
                ? ConditionEvaluation.satisfied(ConditionType.REGION, "세부 시·군·구 지원지역에 포함됩니다.")
                : ConditionEvaluation.notSatisfied(ConditionType.REGION, "세부 시·군·구 지원지역에 포함되지 않습니다."));
    }

    private void evaluateResidencePeriod(
            String detailText,
            RecommendationUserDTO user,
            List<ConditionEvaluation> results
    ) {
        String text = RegionNormalizer.normalizeText(detailText);
        Integer requiredMonths = parseResidenceMonths(text);

        if (requiredMonths == null) {
            return;
        }

        if (user.getResidenceMonths() == null) {
            results.add(ConditionEvaluation.needsConfirmation(
                    ConditionType.RESIDENCE_PERIOD,
                    "지역 거주기간 확인이 필요합니다."
            ));
            return;
        }

        results.add(user.getResidenceMonths() >= requiredMonths
                ? ConditionEvaluation.satisfied(ConditionType.RESIDENCE_PERIOD, "지역 거주기간 조건을 충족합니다.")
                : ConditionEvaluation.notSatisfied(ConditionType.RESIDENCE_PERIOD, "지역 거주기간 조건을 충족하지 않습니다."));
    }

    private boolean isBusinessLocationCondition(String text) {
        return text.contains("사업장")
                || text.contains("사업자등록")
                || text.contains("소재 기업")
                || text.contains("소재한 기업")
                || text.contains("소재하고 영업")
                || text.contains("도내 기업")
                || text.contains("관내 기업")
                || text.contains("소상공인")
                || text.contains("중소기업");
    }

    private boolean isResidenceLocationCondition(String text) {
        return text.contains("거주")
                || text.contains("주민등록")
                || text.contains("주소지")
                || text.contains("전입");
    }

    private Integer parseResidenceMonths(String text) {
        Matcher matcher = RESIDENCE_PERIOD_A.matcher(text);
        if (!matcher.find()) {
            matcher = RESIDENCE_PERIOD_B.matcher(text);
            if (!matcher.find()) {
                return null;
            }
        }

        int amount = Integer.parseInt(matcher.group(1));
        return "년".equals(matcher.group(2)) ? amount * 12 : amount;
    }
}
