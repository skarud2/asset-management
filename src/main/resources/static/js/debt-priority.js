document.addEventListener("DOMContentLoaded", loadDebtPriorities);

// 부채 상환 우선순위 조회
async function loadDebtPriorities() {
    const loadingArea = document.getElementById("loadingArea");
    const errorArea = document.getElementById("errorArea");
    const priorityList = document.getElementById("priorityList");

    if (!loadingArea || !errorArea || !priorityList) {
        return;
    }

    try {
        const response = await fetch("/api/loan-analysis/debt-priority", {
            method: "GET",
            headers: {"Accept": "application/json"}
        });

        if (!response.ok) {
            throw new Error(`HTTP 오류: ${response.status}`);
        }

        const priorities = await response.json();
        loadingArea.classList.add("hidden");

        if (!Array.isArray(priorities) || priorities.length === 0) {
            priorityList.innerHTML = `
                <div class="result-message empty">
                    분석할 대출 정보가 없습니다.
                </div>`;
            updateSummary([]);
            return;
        }

        const sortedPriorities = [...priorities].sort(
            (first, second) => Number(first.priorityRank ?? 0) - Number(second.priorityRank ?? 0)
        );

        updateSummary(sortedPriorities);
        priorityList.innerHTML = sortedPriorities.map(createPriorityRow).join("");
    } catch (error) {
        console.error("부채 상환 우선순위 조회 실패:", error);
        loadingArea.classList.add("hidden");
        errorArea.classList.remove("hidden");
        updateSummary([]);
    }
}

// 분석 요약 출력
function updateSummary(priorities) {
    const loanCount = document.getElementById("loanCount");
    const topLoanType = document.getElementById("topLoanType");
    const topPriorityScore = document.getElementById("topPriorityScore");

    if (!priorities.length) {
        loanCount.textContent = "0건";
        topLoanType.textContent = "-";
        topPriorityScore.textContent = "-";
        return;
    }

    const firstLoan = priorities[0];
    loanCount.textContent = `${priorities.length}건`;
    topLoanType.textContent = displayText(firstLoan.loanType);
    topPriorityScore.textContent = formatScore(firstLoan.priorityScore);
}

// 우선순위 행 생성
function createPriorityRow(loan) {
    const rank = Number(loan.priorityRank ?? 0);
    const firstClass = rank === 1 ? " first" : "";

    return `
        <article class="priority-row${firstClass}">
            <div class="rank-cell">
                <span class="mobile-label">순위</span>
                <strong>${rank > 0 ? `${rank}위` : "-"}</strong>
            </div>

            <div class="loan-cell">
                <div class="loan-heading">
                    <h3>${escapeHtml(displayText(loan.loanType))}</h3>
                    <span>${escapeHtml(displayText(loan.rateType))}</span>
                    <span>${escapeHtml(displayText(loan.loanStatus))}</span>
                </div>
                <p>${escapeHtml(displayText(loan.reason))}</p>
            </div>

            <div class="amount-cell value-cell">
                <span class="mobile-label">잔액</span>
                <strong>${formatCurrency(loan.currentBalance)}</strong>
            </div>

            <div class="rate-cell value-cell">
                <span class="mobile-label">금리</span>
                <strong>${formatRate(loan.interestRate)}</strong>
            </div>

            <div class="score-cell value-cell">
                <span class="mobile-label">RPS</span>
                <strong>${formatScore(loan.priorityScore)}</strong>
            </div>
        </article>`;
}

// 금액 표시
function formatCurrency(value) {
    if (value === null || value === undefined || value === "") {
        return "-";
    }

    const number = Number(value);

    if (!Number.isFinite(number)) {
        return "-";
    }

    return `${new Intl.NumberFormat("ko-KR", {
        maximumFractionDigits: 0
    }).format(number)}원`;
}

// 금리 표시
function formatRate(value) {
    if (value === null || value === undefined || value === "") {
        return "-";
    }

    const number = Number(value);

    if (!Number.isFinite(number)) {
        return "-";
    }

    return `${number.toFixed(2)}%`;
}

// 점수 표시
function formatScore(value) {
    if (value === null || value === undefined || value === "") {
        return "-";
    }

    const number = Number(value);

    if (!Number.isFinite(number)) {
        return "-";
    }

    return number.toFixed(2);
}

function displayText(value) {
    const text = String(value ?? "").trim();
    return text || "-";
}

// 동적 문자의 HTML 이스케이프
function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
