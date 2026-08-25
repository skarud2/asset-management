const SCENARIO_ORDER = ["KEEP", "PARTIAL_REPAYMENT", "REFINANCE", "CASH_HOLDING"];
const SCENARIO_NAMES = {
    KEEP: "현재 유지",
    PARTIAL_REPAYMENT: "부분상환",
    REFINANCE: "대환",
    CASH_HOLDING: "현금보유"
};

document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("scenarioForm");
    const resetButton = document.getElementById("resetButton");

    if (!form || !resetButton) {
        return;
    }

    form.addEventListener("submit", analyzeScenarios);
    resetButton.addEventListener("click", resetForm);
});

// 대출 대응방안 비교 요청
async function analyzeScenarios(event) {
    event.preventDefault();
    hideError();
    hideResult();
    setLoadingState(true);

    const requestData = collectRequestData();

    try {
        const response = await fetch("/api/loan-analysis/scenarios", {
            method: "POST",
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/json"
            },
            body: JSON.stringify(requestData)
        });

        if (!response.ok) {
            const message = await readErrorMessage(response);
            throw new Error(message || `HTTP 오류: ${response.status}`);
        }

        const scenarios = await response.json();
        if (!Array.isArray(scenarios) || scenarios.length === 0) {
            throw new Error("비교할 수 있는 분석 결과가 없습니다.");
        }

        renderResults(scenarios, requestData);
    } catch (error) {
        console.error("대출 대응방안 분석 실패:", error);
        showError(error.message || "분석 중 오류가 발생했습니다.");
    } finally {
        setLoadingState(false);
    }
}

// 입력값 구성
function collectRequestData() {
    return {
        targetLoanAccountId: numberValue("targetLoanAccountId"),
        desiredRepaymentAmount: numberValue("desiredRepaymentAmount"),
        emergencyFundAmount: numberValue("emergencyFundAmount"),
        refinanceInterestRate: numberValue("refinanceInterestRate"),
        refinanceCostAmount: numberValue("refinanceCostAmount"),
        refinancePeriodMonths: numberValue("refinancePeriodMonths")
    };
}

function numberValue(id) {
    const value = document.getElementById(id)?.value;
    return value === "" || value == null ? null : Number(value);
}

// 결과 화면 출력
function renderResults(scenarios, requestData) {
    const ordered = [...scenarios].sort(
        (first, second) => SCENARIO_ORDER.indexOf(first.scenarioType)
            - SCENARIO_ORDER.indexOf(second.scenarioType)
    );
    const recommended = ordered.reduce((best, current) =>
        Number(current.recommendationScore ?? 0) > Number(best.recommendationScore ?? 0)
            ? current
            : best
    );

    renderAnalysisTarget(requestData);
    renderRecommendation(recommended);
    renderComparisonTable(ordered, recommended);
    renderScenarioReasons(ordered);
    highlightRecommendedColumn(recommended.scenarioType);

    const resultArea = document.getElementById("resultArea");
    resultArea.classList.remove("hidden");
    resultArea.scrollIntoView({behavior: "smooth", block: "start"});
}

function renderAnalysisTarget() {
    const select = document.getElementById("targetLoanAccountId");
    const selectedText = select.options[select.selectedIndex]?.text ?? "";
    document.getElementById("analysisTargetText").textContent = `분석 대상: ${selectedText}`;
}

function renderRecommendation(scenario) {
    document.getElementById("recommendedScenarioName").textContent = scenarioName(scenario);
    document.getElementById("recommendedReason").textContent =
        scenario.recommendationReason || "추천 설명이 없습니다.";
    document.getElementById("recommendedScore").textContent =
        `${formatScore(scenario.recommendationScore)}점`;
}

// 방안별 비교표 출력
function renderComparisonTable(scenarios, recommended) {
    const rows = [
        {label: "변경 전 대출잔액", field: "beforeBalance", format: formatCurrency},
        {label: "변경 후 대출잔액", field: "afterBalance", format: formatCurrency, lower: true},
        {label: "변경 전 금리", field: "beforeInterestRate", format: formatRate},
        {label: "변경 후 금리", field: "afterInterestRate", format: formatRate, lower: true},
        {label: "변경 전 월상환액", field: "beforeMonthlyPayment", format: formatCurrency},
        {label: "변경 후 월상환액", field: "afterMonthlyPayment", format: formatCurrency, lower: true},
        {label: "상환금액", field: "repaymentAmount", format: formatCurrency},
        {label: "중도상환수수료", field: "prepaymentFeeAmount", format: formatCurrency, lower: true},
        {label: "대환 부대비용", field: "refinanceCostAmount", format: formatCurrency, lower: true},
        {label: "예상 이자 절감액", field: "estimatedInterestSaving", format: formatSignedCurrency, higher: true},
        {label: "비용 차감 후 순효과", field: "netBenefitAmount", format: formatSignedCurrency, higher: true, signed: true},
        {label: "남는 현금", field: "remainingCashAmount", format: formatCurrency, higher: true},
        {label: "현금 유지 가능기간", field: "liquidityMonths", format: formatMonths, higher: true},
        {label: "추천점수", field: "recommendationScore", format: value => `${formatScore(value)}점`, higher: true}
    ];

    document.getElementById("comparisonTableBody").innerHTML = rows.map(row => {
        const bestIndexes = findBestIndexes(scenarios, row);
        const cells = scenarios.map((scenario, index) => {
            const value = scenario[row.field];
            const classes = [];

            if (scenario.scenarioType === recommended.scenarioType) classes.push("recommended-column");
            if (bestIndexes.includes(index) && value != null) classes.push("best-value");
            if (row.signed && Number(value) > 0) classes.push("value-positive");
            if (row.signed && Number(value) < 0) classes.push("value-negative");

            return `<td class="${classes.join(" ")}">${row.format(value)}</td>`;
        }).join("");

        return `<tr><th scope="row">${escapeHtml(row.label)}</th>${cells}</tr>`;
    }).join("");
}

function findBestIndexes(scenarios, row) {
    if (!row.higher && !row.lower) return [];
    const values = scenarios.map(scenario => Number(scenario[row.field] ?? 0));
    const best = row.higher ? Math.max(...values) : Math.min(...values);
    return values.map((value, index) => value === best ? index : -1).filter(index => index >= 0);
}

function renderScenarioReasons(scenarios) {
    document.getElementById("scenarioReasonList").innerHTML = scenarios.map(scenario => `
        <div class="scenario-reason-item">
            <div class="scenario-reason-name">${escapeHtml(scenarioName(scenario))}</div>
            <div>${escapeHtml(scenario.recommendationReason || "-")}</div>
            <div class="scenario-reason-score">${formatScore(scenario.recommendationScore)}점</div>
        </div>`).join("");
}

function highlightRecommendedColumn(scenarioType) {
    document.querySelectorAll(".scenario-result-table thead th[data-scenario]").forEach(header => {
        header.classList.toggle("recommended-column", header.dataset.scenario === scenarioType);
    });
}

function scenarioName(scenario) {
    return scenario.scenarioName || SCENARIO_NAMES[scenario.scenarioType] || scenario.scenarioType || "-";
}

function setLoadingState(loading) {
    document.getElementById("loadingArea").classList.toggle("hidden", !loading);
    const submitButton = document.getElementById("submitButton");
    submitButton.disabled = loading;
    submitButton.textContent = loading ? "분석 중" : "대응방안 비교하기";
}

function hideResult() {
    document.getElementById("resultArea").classList.add("hidden");
}

function showError(message) {
    document.getElementById("errorMessage").textContent = message;
    document.getElementById("errorArea").classList.remove("hidden");
}

function hideError() {
    document.getElementById("errorArea").classList.add("hidden");
}

function resetForm() {
    document.getElementById("scenarioForm").reset();
    hideResult();
    hideError();
}

async function readErrorMessage(response) {
    try {
        const body = await response.json();
        return body.message || body.detail || body.error;
    } catch (_) {
        return null;
    }
}

function formatCurrency(value) {
    const number = Number(value);
    return Number.isFinite(number)
        ? `${new Intl.NumberFormat("ko-KR", {maximumFractionDigits: 0}).format(number)}원`
        : "-";
}

function formatSignedCurrency(value) {
    const number = Number(value);
    if (!Number.isFinite(number)) return "-";
    const amount = `${new Intl.NumberFormat("ko-KR", {maximumFractionDigits: 0}).format(Math.abs(number))}원`;
    return number > 0 ? `+${amount}` : number < 0 ? `-${amount}` : amount;
}

function formatRate(value) {
    const number = Number(value);
    return Number.isFinite(number) ? `${number.toFixed(2)}%` : "-";
}

function formatMonths(value) {
    const number = Number(value);
    return Number.isFinite(number) ? `${number.toFixed(1)}개월` : "-";
}

function formatScore(value) {
    const number = Number(value);
    return Number.isFinite(number) ? number.toFixed(2) : "-";
}

function escapeHtml(value) {
    return String(value ?? "-")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
