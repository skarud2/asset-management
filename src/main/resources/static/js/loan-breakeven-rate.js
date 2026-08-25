const form = document.querySelector('#breakevenForm');
const submitButton = document.querySelector('#submitButton');
const resetButton = document.querySelector('#resetButton');
const errorMessage = document.querySelector('#errorMessage');
const resultArea = document.querySelector('#resultArea');
const resultMessage = document.querySelector('#resultMessage');
const resultExcessTile = document.querySelector('#resultExcessTile');
const resultStatusBadge = document.querySelector('#resultStatusBadge');

resetButton.addEventListener('click', () => {
    form.reset();
    errorMessage.classList.add('hidden');
    resultArea.classList.add('hidden');
});

form.addEventListener('submit', async (event) => {
    event.preventDefault();

    errorMessage.classList.add('hidden');
    resultArea.classList.add('hidden');
    submitButton.disabled = true;

    const loanId = document.querySelector('#loanId').value;
    const thresholdType = document.querySelector('#thresholdType').value;
    const thresholdValue = document.querySelector('#thresholdValue').value;

    try {
        const data = await fetchBreakevenRate(loanId, thresholdType, thresholdValue);
        renderResult(data);
    } catch (error) {
        errorMessage.textContent = error.message;
        errorMessage.classList.remove('hidden');
    } finally {
        submitButton.disabled = false;
    }
});

async function fetchBreakevenRate(loanId, thresholdType, thresholdValue) {
    const params = new URLSearchParams({thresholdType, thresholdValue});
    const response = await fetch(`/api/loans/${loanId}/breakeven-rate?${params}`);
    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.message || `요청 처리 중 오류가 발생했습니다. (HTTP ${response.status})`);
    }

    return data;
}

const LOAN_TYPE_LABELS = {
    MORTGAGE_LOAN: '주택담보대출',
    CREDIT_LOAN: '신용대출',
    JEONSE_LOAN: '전세자금대출',
    STUDENT_LOAN: '학자금대출'
};

const RATE_TYPE_LABELS = {
    FIXED: '고정금리',
    VARIABLE: '변동금리'
};

function translateLoanType(type) {
    return LOAN_TYPE_LABELS[type] || type;
}

function translateRateType(type) {
    return RATE_TYPE_LABELS[type] || type;
}

function renderResult(data) {
    document.querySelector('#resultLoanType').textContent = translateLoanType(data.loanType);
    document.querySelector('#resultRateType').textContent = translateRateType(data.rateType);
    document.querySelector('#resultCurrentRate').textContent = data.currentRate;
    document.querySelector('#resultCurrentMonthlyPayment').textContent = formatNumber(data.currentMonthlyPayment);
    document.querySelector('#resultBreakevenRate').textContent =
        data.breakevenRate === null ? '없음' : `${data.breakevenRate}%`;
    document.querySelector('#resultMarginPercent').textContent =
        data.marginPercent === null ? '-' : `+${data.marginPercent}%p`;
    resultMessage.textContent = data.message;
    document.querySelector('#resultRawJson').textContent = JSON.stringify(data, null, 2);

    if (data.alreadyExceeded) {
        resultExcessTile.classList.remove('hidden');
        document.querySelector('#resultExcessAmount').textContent = formatNumber(data.excessAmount);
        setBadge('badge-critical', '이미 초과');
    } else {
        resultExcessTile.classList.add('hidden');
        if (data.breakevenRate === null) {
            setBadge('badge-warning', '10%p 내 도달 안함');
        } else {
            setBadge('badge-good', '안전 범위');
        }
    }

    resultArea.classList.remove('hidden');
}

function setBadge(className, label) {
    resultStatusBadge.className = `badge ${className}`;
    resultStatusBadge.textContent = label;
}

function formatNumber(value) {
    return new Intl.NumberFormat('ko-KR').format(value);
}
