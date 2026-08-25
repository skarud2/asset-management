const form = document.querySelector('#stressTestForm');
const submitButton = document.querySelector('#submitButton');
const resetButton = document.querySelector('#resetButton');
const errorMessage = document.querySelector('#errorMessage');
const resultArea = document.querySelector('#resultArea');
const runwayValue = document.querySelector('#runwayValue');
const runwayReason = document.querySelector('#runwayReason');
const livingExpenseDisclaimer = document.querySelector('#livingExpenseDisclaimer');
const chartSection = document.querySelector('#chartSection');

let timelineChart = null;

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

    const rateDeltaPercent = document.querySelector('#rateDeltaPercent').value;
    const incomeDropPercent = document.querySelector('#incomeDropPercent').value;
    const unexpectedExpenseAmount = document.querySelector('#unexpectedExpenseAmount').value;
    const simulationMonths = document.querySelector('#simulationMonths').value;

    try {
        const data = await fetchStressTest(
            rateDeltaPercent, incomeDropPercent, unexpectedExpenseAmount, simulationMonths
        );
        renderResult(data);
    } catch (error) {
        errorMessage.textContent = error.message;
        errorMessage.classList.remove('hidden');
    } finally {
        submitButton.disabled = false;
    }
});

async function fetchStressTest(rateDeltaPercent, incomeDropPercent, unexpectedExpenseAmount, simulationMonths) {
    const params = new URLSearchParams({
        rateDeltaPercent, incomeDropPercent, unexpectedExpenseAmount, simulationMonths
    });
    const response = await fetch(`/api/stress-test/personal?${params}`);
    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.message || `요청 처리 중 오류가 발생했습니다. (HTTP ${response.status})`);
    }

    return data;
}

function renderResult(data) {
    if (data.runwayCalculable) {
        runwayValue.textContent = data.runwayMonths === null ? '무기한 (안전)' : `${data.runwayMonths}개월`;
        runwayReason.classList.add('hidden');
    } else {
        runwayValue.textContent = '계산 불가';
        runwayReason.textContent = data.runwayUnavailableReason;
        runwayReason.classList.remove('hidden');
    }

    document.querySelector('#resultStressedLoanPayment').textContent = formatNumber(data.totalStressedLoanPayment);
    document.querySelector('#resultCurrentLoanPayment').textContent = formatNumber(data.totalCurrentLoanPayment);
    document.querySelector('#resultLivingExpense').textContent = formatNumber(data.monthlyLivingExpense);

    const liquidAssetsEl = document.querySelector('#resultLiquidAssets');
    if (data.isLiquidAssetDataAvailable) {
        liquidAssetsEl.textContent = `${formatNumber(data.totalLiquidAssets)}원`;
    } else {
        liquidAssetsEl.textContent = '데이터 없음';
    }

    livingExpenseDisclaimer.textContent = data.livingExpenseDisclaimer;

    if (data.timeline && data.timeline.length > 0) {
        chartSection.classList.remove('hidden');
        renderChart(data.timeline);
    } else {
        chartSection.classList.add('hidden');
    }

    resultArea.classList.remove('hidden');
}

function renderChart(timeline) {
    const ctx = document.querySelector('#timelineChart');

    if (timelineChart) {
        timelineChart.destroy();
    }

    timelineChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: timeline.map((point) => `${point.monthOffset}개월`),
            datasets: [{
                label: '유동자산 잔액',
                data: timeline.map((point) => point.balance),
                borderColor: '#d64545',
                backgroundColor: 'rgba(214, 69, 69, 0.12)',
                pointBackgroundColor: '#d64545',
                pointRadius: 3,
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
                    beginAtZero: true,
                    grid: {color: '#e1e0d9'},
                    ticks: {color: '#898781'}
                }
            }
        }
    });
}

function formatNumber(value) {
    if (value === null || value === undefined) {
        return '-';
    }
    return new Intl.NumberFormat('ko-KR').format(value);
}
