package com.via.shinvia.config.navigation;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceMenuProvider {

    private final List<ServiceMenuCategory> loanMenus = List.of(
            category("diagnosis", "금융 진단센터", "monitoring",
                    item("마이데이터 조회", "계좌, 카드, 대출 현황을 한눈에 확인합니다.","/mydata/result"),
                    item("개인 스트레스 DSR 분석", "내 금융정보를 기준으로 상환 여력을 점검합니다.", "/stress-test/personal")),
            category("loans", "대출", "payments",
                    item("대출상품 추천", "현재 조건에 맞는 대출상품과 예상 조건을 확인합니다.", "/loans/recommendations"),
                    item("금리상승 취약도", "금리 상승 시 상환 부담이 얼마나 커지는지 분석합니다.", "/loans/breakeven-rate-test"),
                    item("과거 금리인상기 재현", "과거 금리 변화를 현재 대출에 적용해 비교합니다.", "/loans/1/historical-rate-replay"),
                    item("시장 내재 금리 시나리오", "시장에 반영된 미래 금리 흐름으로 부담을 예상합니다.", "/loans/1/market-implied-simulation"),
                    item("계단식 금리 시나리오", "단계별 금리 변동에 따른 상환액 변화를 확인합니다.", "/loans/1/staged-rate-simulation"),
                    item("부채 상환 우선순위", "보유 대출 중 먼저 상환할 대상을 분석합니다.", "/loan-analysis/debt-priority"),
                    item("대출 대응방안 비교", "유지, 부분상환, 대환 등 대응방안을 비교합니다.", "/loan-analysis/scenarios")),
            category("policy-products", "정책금융상품", "account_balance",
                    item("서민금융상품", "생활 안정과 금융 부담 완화를 위한 상품을 확인합니다.", "/policy-support"),
                    item("자산형성상품", "목돈 마련과 자산 형성을 지원하는 상품을 확인합니다.", "/asset-products"),
                    item("사회연대금융", "사회적 금융 지원상품과 이용 조건을 확인합니다.", "/social-finance"),
                    item("복합지원", "금융과 복지를 연계한 지원정보를 확인합니다.", "/welfare-support"),
                    item("맞춤 금융지원상품 추천", "개인 조건에 맞는 지원상품을 추천받습니다.", "/policy/recommendation")),
            category("policy-guide", "금융정책 안내", "policy",
                    item("스트레스 DSR", "스트레스 금리와 DSR 적용 기준을 알아봅니다.", "/financial-policy/stress-dsr"),
                    item("가계대출·DSR 규제", "가계대출과 DSR 규제 내용을 확인합니다.", "/financial-policy/household-dsr"),
                    item("주택담보대출 규제", "주택담보대출의 주요 제한과 조건을 확인합니다.", "/financial-policy/mortgage-regulation"),
                    item("전세대출 규제", "전세대출 관련 규제와 이용 조건을 확인합니다.", "/financial-policy/jeonse-regulation"),
                    item("정책대출 제도", "정책대출의 종류와 지원 기준을 살펴봅니다.", "/financial-policy/policy-loan"))
    );

    private final List<ServiceMenuCategory> assetMenus = List.of(
            category("life-plan", "금융 라이프 플랜", "timeline",
                    item("금융 라이프 플랜", "결혼, 주거, 출산 등 미래 계획을 금융 시나리오로 구성합니다.", "/lifecycle/survey")),
            category("future-simulation", "미래금융 시뮬레이터", "model_training",
                    item("미래금융 시뮬레이터", "미래 소득과 지출 변화에 따른 자산 흐름을 예측합니다.", "/futuresim")),
            category("surplus-fund", "여유자금 운용", "savings",
                    item("여유자금 운용", "사용 가능한 여유자금의 운용 방안을 비교합니다.", "/surplus-funds/guide")),
            category("personal-report", "개인화 리포트", "summarize",
                    item("개인화 리포트", "나의 금융 현황과 계획을 한눈에 확인합니다.", "/report"))
    );

    public List<ServiceMenuCategory> loanMenus() {
        return loanMenus;
    }

    public List<ServiceMenuCategory> assetMenus() {
        return assetMenus;
    }

    private static ServiceMenuCategory category(String id, String name, String icon, ServiceMenuItem... items) {
        return new ServiceMenuCategory(id, name, icon, List.of(items));
    }

    private static ServiceMenuItem item(String name, String description, String path) {
        return new ServiceMenuItem(name, description, path, true);
    }
}
