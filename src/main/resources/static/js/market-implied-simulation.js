const form = document.querySelector('#marketImpliedForm');
const submitButton = document.querySelector('#submitButton');
const resetButton = document.querySelector('#resetButton');
const errorMessage = document.querySelector('#errorMessage');
const resultArea = document.querySelector('#resultArea');
const resultMessage = document.querySelector('#resultMessage');
const resultStatusBadge = document.querySelector('#resultStatusBadge');
const chartSection = document.querySelector('#chartSection');
const resultTableBody = document.querySelector('#resultTableBody');
const resultDataSource = document.querySelector('#resultDataSource');

let monthlyPaymentChart = null;

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

    try {
        const data = await fetchMarketImpliedSimulation(loanId);
        renderResult(data);
    } catch (error) {
        errorMessage.textContent = error.message;
        errorMessage.classList.remove('hidden');
    } finally {
        submitButton.disabled = false;
    }
});

async function fetchMarketImpliedSimulation(loanId) {
    const response = await fetch(`/api/loans/${loanId}/market-implied-simulation`);
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
    document.querySelector('#resultAsOfDate').textContent = data.asOfDate;
    document.querySelector('#resultInitialRate').textContent = data.initialRate;
    resultDataSource.textContent = data.dataSource;

    const isFixedRate = data.path.length === 0;

    resultMessage.classList.toggle('hidden', !data.message);
    resultMessage.textContent = data.message || '';

    chartSection.classList.toggle('hidden', isFixedRate);

    if (isFixedRate) {
        setBadge('badge-neutral', '고정금리');
    } else if (data.truncated) {
        setBadge('badge-warning', '만기로 일부만 계산됨');
    } else {
        setBadge('badge-good', '계산 완료');
    }

    if (!isFixedRate) {
        renderTable(data.path);
        renderChart(data.path);
    }

    resultArea.classList.remove('hidden');
}

function setBadge(className, label) {
    resultStatusBadge.className = `badge ${className}`;
    resultStatusBadge.textContent = label;
}

function renderTable(path) {
    resultTableBody.innerHTML = '';

    for (const step of path) {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${step.monthOffset}</td>
            <td>${step.impliedRate}%</td>
            <td>${formatNumber(step.monthlyPayment)}원</td>
        `;
        resultTableBody.appendChild(row);
    }
}

function renderChart(path) {
    const ctx = document.querySelector('#monthlyPaymentChart');

    if (monthlyPaymentChart) {
        monthlyPaymentChart.destroy();
    }

    monthlyPaymentChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: path.map((step) => `${step.monthOffset}개월`),
            datasets: [{
                label: '월상환액',
                data: path.map((step) => step.monthlyPayment),
                stepped: 'before',
                borderColor: '#2a78d6',
                backgroundColor: 'rgba(42, 120, 214, 0.12)',
                pointBackgroundColor: '#2a78d6',
                pointRadius: 4,
                borderWidth: 2,
                fill: true
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    labels: {
                        color: '#52514e',
                        font: {family: 'system-ui'}
                    }
                }
            },
            scales: {
                x: {
                    grid: {color: '#e1e0d9'},
                    ticks: {color: '#898781'}
                },
                y: {
                    beginAtZero: false,
                    grid: {color: '#e1e0d9'},
                    ticks: {color: '#898781'}
                }
            }
        }
    });
}

function formatNumber(value) {
    return new Intl.NumberFormat('ko-KR').format(value);
}
