const form = document.getElementById('preferenceForm');
const submitButton = document.getElementById('submitButton');
const errorBox = document.getElementById('errorBox');
const resultSection = document.getElementById('resultSection');
const operationAmountInput = document.getElementById('operationAmount');
const stepTabs = Array.from(document.querySelectorAll('[data-step-target]'));
const stepPanels = Array.from(document.querySelectorAll('[data-step-panel]'));
const questionCards = Array.from(document.querySelectorAll('[data-question-index]'));
const productFilterButtons = Array.from(document.querySelectorAll('[data-product-filter]'));
const reviewGuideRecordButton = document.getElementById('reviewGuideRecordButton');
const backToProductsButton = document.getElementById('backToProductsButton');
const saveGuideVersionButton = document.getElementById('saveGuideVersionButton');
const guideNameInput = document.getElementById('guideName');
const guideSaveMessage = document.getElementById('guideSaveMessage');
const guideRecordContent = document.getElementById('guideRecordContent');
const guideRecordEmptyState = document.getElementById('guideRecordEmptyState');
const guideNameCard = document.getElementById('guideNameCard');
const guideCurrentActions = document.getElementById('guideCurrentActions');
const guideHistoryActions = document.getElementById('guideHistoryActions');
const backToCurrentGuideButton = document.getElementById('backToCurrentGuideButton');

const styleLabels = {
    STABLE: '안정형',
    BALANCED: '균형형',
    AGGRESSIVE: '공격형'
};

const assetLabels = {
    CASH: '현금',
    ETF: 'ETF',
    FUND: '펀드'
};

const assetColors = {
    CASH: '#1b2430',
    ETF: '#5271c4',
    FUND: '#a8b6df'
};

const assetOrder = ['CASH', 'ETF', 'FUND'];

const questionRadioNames = [
    'investmentPurpose',
    'investmentPeriodMonths',
    'lossToleranceLevel',
    'liquidityNeed',
    'experienceLevel'
];

const productEmptyMessages = {
    DEFAULT: 'ETF·펀드 상품 데이터를 준비하고 있습니다. 데이터 연동 후 자산배분 결과에 맞는 관련 상품을 확인할 수 있습니다.',
    CASH: '현금 배정분은 유동성 확보를 위한 보유 금액이며, 현재 상품 탐색 대상에서 제외됩니다.'
};

const wonFormatter = new Intl.NumberFormat('ko-KR', {
    maximumFractionDigits: 0
});

const stepCopy = {
    1: {
        title: '여유자금 설정',
        description: '마이데이터를 기반으로 실제 운용 가능한 여유자금을 계산합니다.'
    },
    2: {
        title: '운용성향 설문',
        description: '투자 목적과 손실 감내 수준을 확인합니다.'
    },
    3: {
        title: '자산배분 결과',
        description: '설문 결과를 바탕으로 자산군별 가이드 비율을 확인합니다.'
    },
    4: {
        title: '관련 상품 탐색',
        description: '자산배분 결과에 맞는 관련 상품을 살펴봅니다.'
    },
    5: {
        title: '운용 기록',
        description: '이번 결과를 저장하거나 이전에 저장한 완주 기록을 확인합니다.'
    }
};

let currentStep = 1;
let currentQuestionIndex = 0;
let hasAnalysisResult = false;
let latestAllocations = [];
let currentProductFilter = 'ETF';
let isSubmitting = false;
let latestCalculationId = null;
let latestPlanId = null;
let latestReasons = [];
let guideSaveIdempotencyKey = null;
let savedGuideVersionId = null;
let viewingSavedGuideVersionId = null;
let guideDetailRequestController = null;

window.applySurplusFundAmount = (amount) => {
    const normalizedAmount = Number(amount);

    if (!Number.isFinite(normalizedAmount) || normalizedAmount <= 0) {
        return false;
    }

    operationAmountInput.value = String(normalizedAmount);
    operationAmountInput.dispatchEvent(
        new Event('input', { bubbles: true })
    );
    operationAmountInput.dispatchEvent(
        new Event('change', { bubbles: true })
    );

    return true;
};

[
    'withdrawableAmount',
    'livingExpense',
    'scheduledExpense',
    'emergencyFund',
    'nextIncomeDate',
    'operationAmount'
].forEach((elementId) => {
    const element = document.getElementById(elementId);

    element?.addEventListener('input', () => {
        if (hasAnalysisResult || savedGuideVersionId) {
            latestCalculationId = null;
            resetAnalysisAfterCalculation();
        }
    });
});

document.getElementById('startSurveyButton')
    .addEventListener('click', async () => {
        if (!validateOperationAmount()) {
            return;
        }

        hideError();

        try {
            if (
                typeof window.saveSurplusFundCalculation
                === 'function'
            ) {
                const calculationResult =
                    await window.saveSurplusFundCalculation();

                const calculationId =
                    Number(calculationResult?.calculationId);

                if (!Number.isInteger(calculationId) || calculationId <= 0) {
                    throw new Error('저장된 여유자금 계산 결과 ID를 확인할 수 없습니다.');
                }

                latestCalculationId = calculationId;
                resetAnalysisAfterCalculation();
            }

            showQuestion(0, false);
            showStep(2);
        } catch (error) {
            showError(
                error.message
                || '여유자금 계산 결과 저장에 실패했습니다.'
            );
        }
    });

stepTabs.forEach((tab) => {
    tab.addEventListener('click', () => {
        const targetStep = Number(tab.dataset.stepTarget);

        if (
            (targetStep === 3 || targetStep === 4)
            && !hasAnalysisResult
        ) {
            return;
        }

        if (
            targetStep === 2
            && !validateOperationAmount()
        ) {
            return;
        }

        if (
            targetStep === 2
            && !Number.isInteger(latestCalculationId)
        ) {
            showError('1단계의 다음 버튼을 눌러 계산 결과를 먼저 저장해주세요.');
            showStep(1);
            return;
        }

        hideError();

        if (targetStep === 5) {
            if (hasAnalysisResult) {
                renderGuideSavePreview();
            } else {
                showGuideHistoryOnlyMode();
            }
            loadSavedGuideVersions();
        }

        showStep(targetStep);
    });
});

questionCards.forEach((card) => {
    card.querySelectorAll('[data-question-action]')
        .forEach((button) => {
            button.addEventListener('click', () => {
                const action = button.dataset.questionAction;

                if (action === 'previous') {
                    hideError();

                    if (currentQuestionIndex === 0) {
                        showStep(1);
                        return;
                    }

                    showQuestion(currentQuestionIndex - 1);
                    return;
                }

                if (!validateQuestion(currentQuestionIndex)) {
                    return;
                }

                hideError();
                showQuestion(currentQuestionIndex + 1);
            });
        });
});

document.getElementById('reviewSurveyButton')
    .addEventListener('click', () => {
        hideError();
        showQuestion(0, false);
        showStep(2);
    });

document.getElementById('exploreProductsButton')
    .addEventListener('click', () => {
        if (hasAnalysisResult) {
            hideError();
            showStep(4);
        }
    });

document.getElementById('backToResultButton')
    .addEventListener('click', () => {
        if (hasAnalysisResult) {
            hideError();
            showStep(3);
        }
    });

reviewGuideRecordButton?.addEventListener('click', () => {
    if (!hasAnalysisResult) {
        return;
    }

    hideError();
    renderGuideSavePreview();
    loadSavedGuideVersions();
    showStep(5);
});

backToProductsButton?.addEventListener('click', () => {
    if (hasAnalysisResult) {
        hideGuideSaveMessage();
        showStep(4);
    }
});

saveGuideVersionButton?.addEventListener('click', saveGuideVersion);

backToCurrentGuideButton?.addEventListener('click', () => {
    if (hasAnalysisResult) {
        renderGuideSavePreview();
    } else {
        showGuideHistoryOnlyMode();
    }
});

window.addEventListener('surplus:etf-selection-changed', () => {
    if (
        currentStep === 5
        && !savedGuideVersionId
        && !viewingSavedGuideVersionId
    ) {
        renderGuideSavePreview();
    }
});

productFilterButtons.forEach((button) => {
    button.addEventListener('click', () => {
        currentProductFilter =
            button.dataset.productFilter;

        updateProductExplorer();
    });
});

form.addEventListener('submit', async (event) => {
    event.preventDefault();

    if (isSubmitting) {
        return;
    }

    hideError();

    if (!validateSurveyForSubmit()) {
        return;
    }

    const formData = new FormData(form);
    const operationAmount =
        Number(formData.get('operationAmount'));

    const requestBody = {
        operationAmount,
        investmentPurpose:
            formData.get('investmentPurpose'),
        investmentPeriodMonths:
            Number(formData.get('investmentPeriodMonths')),
        lossToleranceLevel:
            formData.get('lossToleranceLevel'),
        liquidityNeed:
            formData.get('liquidityNeed'),
        experienceLevel:
            formData.get('experienceLevel'),
        surplusAmountConfirmed:
        document.getElementById('surplusAmountConfirmed').checked,
        guideNoticeConfirmed:
        document.getElementById('guideNoticeConfirmed').checked
    };

    setLoading(true);

    try {
        const response = await fetch(
            '/api/surplus-funds/preferences/analyze',
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'same-origin',
                body: JSON.stringify(requestBody)
            }
        );

        const contentType =
            response.headers.get('content-type') || '';

        if (!response.ok) {
            const message =
                contentType.includes('application/json')
                    ? await readErrorMessage(response)
                    : await response.text();

            throw new Error(
                message
                || `요청 처리에 실패했습니다. (${response.status})`
            );
        }

        if (!contentType.includes('application/json')) {
            throw new Error('로그인 세션을 확인해주세요.');
        }

        const result = await response.json();
        renderResult(result);
    } catch (error) {
        showQuestion(questionCards.length - 1, false);
        showStep(2, false);

        showError(
            error.message
            || '운용성향 분석 중 오류가 발생했습니다.'
        );
    } finally {
        setLoading(false);
    }
});

function showStep(step, shouldScroll = true) {
    currentStep = step;

    stepPanels.forEach((panel) => {
        panel.hidden =
            Number(panel.dataset.stepPanel) !== step;
    });

    stepTabs.forEach((tab) => {
        const tabStep =
            Number(tab.dataset.stepTarget);

        const isActive =
            tabStep === step;

        const isLocked =
            (tabStep === 3 || tabStep === 4)
            && !hasAnalysisResult;

        const isCompleted =
            !isActive
            && (
                (
                    tabStep === 1
                    && Number.isInteger(latestCalculationId)
                    && step > 1
                )
                || (
                    hasAnalysisResult
                    && tabStep < step
                )
            );

        tab.classList.toggle(
            'active',
            isActive
        );

        tab.classList.toggle(
            'completed',
            !isActive && isCompleted
        );

        tab.disabled = isLocked;

        tab.setAttribute(
            'aria-disabled',
            String(isLocked)
        );

        if (isActive) {
            tab.setAttribute(
                'aria-current',
                'step'
            );
        } else {
            tab.removeAttribute(
                'aria-current'
            );
        }
    });

    const copy = stepCopy[step];

    if (copy) {
        const guideTitle =
            document.getElementById('guideTitle');

        const guideDescription =
            document.getElementById('guideDescription');

        if (guideTitle) {
            guideTitle.textContent = copy.title;
        }

        if (guideDescription) {
            guideDescription.textContent =
                copy.description;
        }
    }

    const syncWrap =
        document.getElementById('syncWrap');

    if (syncWrap) {
        syncWrap.hidden =
            step !== 1;
    }

    if (shouldScroll) {
        document.querySelector('.guide-steps')
            .scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
    }
}

function showQuestion(index, shouldScroll = true) {
    const safeIndex = Math.min(
        Math.max(index, 0),
        questionCards.length - 1
    );

    currentQuestionIndex = safeIndex;

    questionCards.forEach((card, cardIndex) => {
        card.hidden =
            cardIndex !== safeIndex;
    });

    if (shouldScroll) {
        questionCards[safeIndex]
            .scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
    }
}

function validateOperationAmount() {
    const operationAmount =
        Number(operationAmountInput.value);

    if (
        !Number.isFinite(operationAmount)
        || operationAmount <= 0
    ) {
        showStep(1, false);
        showError(
            '운용 가능 금액은 0보다 커야 합니다.'
        );
        operationAmountInput.focus();

        return false;
    }

    return true;
}

function validateQuestion(index) {
    const radioName =
        questionRadioNames[index];

    const selectedOption =
        form.querySelector(
            `input[name="${radioName}"]:checked`
        );

    if (!selectedOption) {
        showError(
            '현재 질문의 답변을 선택해주세요.'
        );

        questionCards[index]
            .querySelector(
                `input[name="${radioName}"]`
            )
            .focus();

        return false;
    }

    return true;
}

function validateSurveyForSubmit() {
    if (!validateOperationAmount()) {
        return false;
    }

    for (
        let index = 0;
        index < questionRadioNames.length;
        index += 1
    ) {
        const radioName =
            questionRadioNames[index];

        const selectedOption =
            form.querySelector(
                `input[name="${radioName}"]:checked`
            );

        if (!selectedOption) {
            showStep(2, false);
            showQuestion(index, false);

            showError(
                `${index + 1}번 질문의 답변을 선택해주세요.`
            );

            questionCards[index]
                .querySelector(
                    `input[name="${radioName}"]`
                )
                .focus();

            return false;
        }
    }

    const surplusAmountConfirmed =
        document.getElementById(
            'surplusAmountConfirmed'
        );

    const guideNoticeConfirmed =
        document.getElementById(
            'guideNoticeConfirmed'
        );

    if (
        !surplusAmountConfirmed.checked
        || !guideNoticeConfirmed.checked
    ) {
        showStep(2, false);
        showQuestion(
            questionCards.length - 1,
            false
        );

        showError(
            '필수 확인 항목 두 가지에 모두 동의해주세요.'
        );

        (
            !surplusAmountConfirmed.checked
                ? surplusAmountConfirmed
                : guideNoticeConfirmed
        ).focus();

        return false;
    }

    return true;
}

async function readErrorMessage(response) {
    const errorBody = await response.json();

    return errorBody.message
        || errorBody.detail
        || errorBody.error;
}

function renderResult(result) {
    const planId = Number(result.surplusFundPlanId);

    if (!Number.isInteger(planId) || planId <= 0) {
        throw new Error('저장된 자산배분 결과 ID를 확인할 수 없습니다.');
    }

    latestPlanId = planId;
    latestReasons = Array.isArray(result.reasons)
        ? [...result.reasons]
        : [];
    guideSaveIdempotencyKey = null;
    savedGuideVersionId = null;

    if (saveGuideVersionButton) {
        saveGuideVersionButton.disabled = false;
        saveGuideVersionButton.textContent = '이 운용 기록 저장하기';
    }
    hideGuideSaveMessage();

    if (typeof window.clearSelectedEtfProducts === 'function') {
        window.clearSelectedEtfProducts();
    }

    document.getElementById('styleBadge').textContent =
        `${
            styleLabels[result.investmentStyle]
            || result.investmentStyle
        }`;

    document.getElementById('scoreValue').textContent =
        result.score;

    document.getElementById('ruleVersion').textContent =
        result.ruleVersion;

    document.getElementById('planId').textContent =
        `#${result.surplusFundPlanId}`;

    document.getElementById('guideNotice').textContent =
        result.guideNotice;

    latestAllocations =
        normalizeAllocations(
            result.allocations || []
        );

    renderAllocations(latestAllocations);
    renderReasons(latestReasons);

    hasAnalysisResult = true;

    /*
     * 전체 탭을 제거했으므로
     * 새로운 분석 결과를 받은 경우 ETF 탭을 기본으로 선택한다.
     */
    currentProductFilter = 'ETF';
    updateProductExplorer();

    window.dispatchEvent(
        new CustomEvent(
            'surplus:allocation-updated',
            {
                detail: {
                    allocations: latestAllocations
                }
            }
        )
    );

    showStep(3);
}

function normalizeAllocations(allocations) {
    const allocationsByType = new Map();

    allocations.forEach((allocation) => {
        const ratio =
            Number(allocation?.ratio);

        const amount =
            Number(allocation?.amount);

        const assetType =
            allocation?.assetType;

        if (
            assetOrder.includes(assetType)
            && Number.isFinite(ratio)
            && Number.isFinite(amount)
        ) {
            allocationsByType.set(
                assetType,
                {
                    assetType,
                    ratio,
                    amount
                }
            );
        }
    });

    return assetOrder
        .map(
            (assetType) =>
                allocationsByType.get(assetType)
        )
        .filter(Boolean);
}

function renderAllocations(allocations) {
    const allocationGrid =
        document.getElementById(
            'allocationGrid'
        );

    allocationGrid.replaceChildren();

    const totalAmount =
        allocations.reduce(
            (sum, allocation) =>
                sum + allocation.amount,
            0
        );

    let accumulatedRatio = 0;

    const gradientSegments =
        allocations.map((allocation) => {
            const startRatio =
                accumulatedRatio;

            accumulatedRatio +=
                Math.max(allocation.ratio, 0);

            return `${
                assetColors[allocation.assetType]
            } ${startRatio}% ${accumulatedRatio}%`;
        });

    if (accumulatedRatio < 100) {
        gradientSegments.push(
            `#e6e8eb ${accumulatedRatio}% 100%`
        );
    }

    const donut =
        document.createElement('div');

    donut.className =
        'allocation-donut';

    if (gradientSegments.length > 0) {
        donut.style.setProperty(
            '--allocation-gradient',
            `conic-gradient(${
                gradientSegments.join(', ')
            })`
        );
    }

    donut.setAttribute(
        'role',
        'img'
    );

    donut.setAttribute(
        'aria-label',
        allocations
            .map(
                (allocation) =>
                    `${
                        assetLabels[
                            allocation.assetType
                            ]
                    } ${allocation.ratio}%, ${
                        wonFormatter.format(
                            allocation.amount
                        )
                    }원`
            )
            .join(', ')
    );

    const donutCenter =
        document.createElement('div');

    donutCenter.className =
        'allocation-donut-center';

    const totalLabel =
        document.createElement('span');

    totalLabel.textContent =
        '총 운용금액';

    const totalValue =
        document.createElement('strong');

    totalValue.textContent =
        `${
            wonFormatter.format(totalAmount)
        }원`;

    donutCenter.append(
        totalLabel,
        totalValue
    );

    donut.append(donutCenter);

    const legend =
        document.createElement('div');

    legend.className =
        'allocation-legend';

    allocations.forEach((allocation) => {
        const item =
            document.createElement('article');

        item.className =
            'allocation-legend-item';

        const colorMarker =
            document.createElement('span');

        colorMarker.className =
            'allocation-color-marker';

        colorMarker.style.setProperty(
            '--asset-color',
            assetColors[allocation.assetType]
        );

        const copy =
            document.createElement('div');

        const name =
            document.createElement('strong');

        name.textContent =
            `${
                assetLabels[allocation.assetType]
            } (${allocation.assetType})`;

        const ratio =
            document.createElement('span');

        ratio.textContent =
            `${allocation.ratio}%`;

        copy.append(name, ratio);

        const amount =
            document.createElement('b');

        amount.textContent =
            `${
                wonFormatter.format(
                    allocation.amount
                )
            }원`;

        item.append(
            colorMarker,
            copy,
            amount
        );

        legend.append(item);
    });

    allocationGrid.append(
        donut,
        legend
    );
}

function renderReasons(reasons) {
    const reasonList =
        document.getElementById('reasonList');

    reasonList.replaceChildren();

    reasons.forEach((reason) => {
        const item =
            document.createElement('li');

        item.textContent = reason;

        reasonList.append(item);
    });
}

function updateProductExplorer() {
    const allocationsByType =
        new Map(
            latestAllocations.map(
                (allocation) => [
                    allocation.assetType,
                    allocation
                ]
            )
        );

    updateProductFilterAmount(
        'productFilterEtfAmount',
        allocationsByType.get('ETF')
    );

    updateProductFilterAmount(
        'productFilterFundAmount',
        allocationsByType.get('FUND')
    );

    updateProductFilterAmount(
        'productFilterCashAmount',
        allocationsByType.get('CASH')
    );

    productFilterButtons.forEach((button) => {
        const isActive =
            button.dataset.productFilter
            === currentProductFilter;

        button.classList.toggle(
            'active',
            isActive
        );

        button.setAttribute(
            'aria-pressed',
            String(isActive)
        );
    });

    const productGrid =
        document.getElementById(
            'productGrid'
        );

    const etfProductArea =
        document.getElementById(
            'etfProductArea'
        );

    const fundProductArea =
        document.getElementById(
            'fundProductArea'
        );

    const productEmptyState =
        document.getElementById(
            'productEmptyState'
        );

    /*
     * 전체 탭은 존재하지 않는다.
     * ETF와 펀드는 선택한 하나만 표시한다.
     */
    const showEtf =
        currentProductFilter === 'ETF';

    const showFund =
        currentProductFilter === 'FUND';

    const showProductGrid =
        showEtf || showFund;

    if (etfProductArea) {
        etfProductArea.hidden =
            !showEtf;
    }

    if (fundProductArea) {
        fundProductArea.hidden =
            !showFund;
    }

    if (productGrid) {
        productGrid.hidden =
            !showProductGrid;
    }

    if (productEmptyState) {
        if (currentProductFilter === 'CASH') {
            productEmptyState.hidden = false;
            productEmptyState.textContent =
                productEmptyMessages.CASH;
        } else {
            productEmptyState.hidden = true;
            productEmptyState.textContent = '';
        }
    }
}

function updateProductFilterAmount(
    elementId,
    allocation
) {
    const element =
        document.getElementById(elementId);

    if (!element) {
        return;
    }

    element.textContent =
        allocation
            ? `· ${
                wonFormatter.format(
                    allocation.amount
                )
            }원`
            : '· 배정금액 없음';
}

function resetAnalysisAfterCalculation() {
    hasAnalysisResult = false;
    latestPlanId = null;
    latestAllocations = [];
    latestReasons = [];
    guideSaveIdempotencyKey = null;
    savedGuideVersionId = null;
    viewingSavedGuideVersionId = null;

    if (typeof window.clearSelectedEtfProducts === 'function') {
        window.clearSelectedEtfProducts();
    }

    if (saveGuideVersionButton) {
        saveGuideVersionButton.disabled = false;
        saveGuideVersionButton.textContent = '이 운용 기록 저장하기';
    }

    hideGuideSaveMessage();
}

function renderGuideSavePreview() {
    renderGuideRecord({
        selectedAccountBalance: readInputNumber('withdrawableAmount'),
        livingExpense: readInputNumber('livingExpense'),
        scheduledExpense: readInputNumber('scheduledExpense'),
        emergencyFund: readInputNumber('emergencyFund'),
        finalSurplusAmount: readInputNumber('operationAmount'),
        allocations: latestAllocations,
        reasons: latestReasons,
        etfs: getSelectedEtfProducts()
    });

    showCurrentGuideMode();
}

function renderGuideRecord(record) {
    const allocations = Array.isArray(record.allocations)
        ? record.allocations
        : [];
    const reasons = Array.isArray(record.reasons)
        ? record.reasons
        : [];
    const etfs = Array.isArray(record.etfs)
        ? record.etfs
        : [];

    setText('saveSelectedBalance', formatWonValue(record.selectedAccountBalance));
    setText('saveLivingExpense', formatWonValue(record.livingExpense));
    setText('saveScheduledExpense', formatWonValue(record.scheduledExpense));
    setText('saveEmergencyFund', formatWonValue(record.emergencyFund));
    setText('saveFinalSurplusAmount', formatWonValue(record.finalSurplusAmount));

    renderGuideRecordAllocations(allocations);
    renderGuideRecordReasons(reasons);
    renderGuideRecordEtfs(etfs);
}

function renderGuideRecordAllocations(allocations) {
    const allocationList = document.getElementById('saveAllocationList');
    if (!allocationList) {
        return;
    }

    allocationList.replaceChildren();

    if (allocations.length === 0) {
        const empty = document.createElement('p');
        empty.className = 'saved-guide-empty';
        empty.textContent = '저장된 자산배분 결과가 없습니다.';
        allocationList.append(empty);
        return;
    }

    allocations.forEach((allocation) => {
        const item = document.createElement('article');
        item.className = 'guide-save-allocation-item';

        const name = document.createElement('strong');
        name.textContent = assetLabels[allocation.assetType]
            || allocation.assetType;

        const ratio = document.createElement('span');
        ratio.textContent = `${formatPlainNumber(allocation.ratio)}%`;

        const amount = document.createElement('b');
        amount.textContent = formatWonValue(allocation.amount);

        item.append(name, ratio, amount);
        allocationList.append(item);
    });
}

function renderGuideRecordReasons(reasons) {
    const reasonList = document.getElementById('saveReasonList');
    if (!reasonList) {
        return;
    }

    reasonList.replaceChildren();

    if (reasons.length === 0) {
        const item = document.createElement('li');
        item.textContent = '저장된 판정 이유가 없습니다.';
        reasonList.append(item);
        return;
    }

    reasons.forEach((reason) => {
        const item = document.createElement('li');
        item.textContent = reason;
        reasonList.append(item);
    });
}

function renderGuideRecordEtfs(etfs) {
    setText('saveEtfCount', `${etfs.length}/4개 선택`);

    const etfList = document.getElementById('saveEtfList');
    if (!etfList) {
        return;
    }

    etfList.replaceChildren();

    if (etfs.length === 0) {
        const empty = document.createElement('p');
        empty.className = 'saved-guide-empty';
        empty.textContent = '관심 ETF를 선택하지 않은 운용 기록입니다.';
        etfList.append(empty);
        return;
    }

    etfs.forEach((etf, index) => {
        const item = document.createElement('article');
        item.className = 'guide-save-etf-item';

        const order = document.createElement('span');
        order.className = 'guide-save-etf-order';
        order.textContent = String(etf.selectionOrder || index + 1);

        const copy = document.createElement('div');
        copy.className = 'guide-save-etf-copy';
        const name = document.createElement('strong');
        name.textContent = etf.productName || '상품명 정보 미제공';
        const meta = document.createElement('span');
        meta.textContent = `종목코드 ${etf.productCode || '-'} · 기준일 ${etf.priceBaseDate || '-'}`;
        copy.append(name, meta);

        const price = document.createElement('div');
        price.className = 'guide-save-etf-price';
        const closingPrice = document.createElement('strong');
        closingPrice.textContent = formatWonValue(etf.closingPrice);
        const rate = document.createElement('span');
        rate.textContent = `등락률 ${formatPercentValue(etf.fluctuationRate)}`;
        price.append(closingPrice, rate);

        item.append(order, copy, price);
        etfList.append(item);
    });
}

function showCurrentGuideMode() {
    viewingSavedGuideVersionId = null;
    setGuideRecordHeader(
        'SAVE GUIDE RECORD',
        savedGuideVersionId
            ? '이번 운용 결과를 저장했습니다.'
            : '이번 운용 결과를 저장해주세요.',
        '아래 내용은 나중에 통합 금융 레포트의 여유자금 운용 영역을 구성하는 확정 기록입니다.',
        savedGuideVersionId ? '저장 완료' : '완주 기록'
    );
    setGuideRecordVisibility(true, false);
    setElementHidden(guideNameCard, false);
    setElementHidden(guideCurrentActions, false);
    setElementHidden(guideHistoryActions, true);
    clearSelectedSavedGuide();
}

function showSavedGuideMode(detail) {
    setGuideRecordHeader(
        'SAVED GUIDE RECORD',
        `v${detail.guideVersionNo} · ${detail.guideName}`,
        `${formatDateTime(detail.completedAt)}에 확정한 운용 기록입니다. 저장 당시 값을 조회 전용으로 표시합니다.`,
        '저장 기록'
    );
    setGuideRecordVisibility(true, false);
    setElementHidden(guideNameCard, true);
    setElementHidden(guideCurrentActions, true);
    setElementHidden(guideHistoryActions, !hasAnalysisResult);
    selectSavedGuide(detail.surplusFundGuideVersionId);
    hideGuideSaveMessage();
}

function showGuideHistoryOnlyMode() {
    viewingSavedGuideVersionId = null;
    setGuideRecordHeader(
        'GUIDE RECORD HISTORY',
        '저장된 운용 기록을 확인하세요.',
        '기록을 선택하면 계산 결과·자산배분·판정 이유·관심 ETF가 같은 화면에 표시됩니다.',
        '기록 조회'
    );
    setGuideRecordVisibility(false, true);
    setElementHidden(guideNameCard, true);
    setElementHidden(guideCurrentActions, true);
    setElementHidden(guideHistoryActions, true);
    clearSelectedSavedGuide();
    hideGuideSaveMessage();
}

function setGuideRecordHeader(eyebrow, title, description, badge) {
    setText('guideRecordEyebrow', eyebrow);
    setText('guideRecordTitle', title);
    setText('guideRecordDescription', description);
    setText('guideRecordBadge', badge);
}

function setGuideRecordVisibility(showContent, showEmptyState) {
    setElementHidden(guideRecordContent, !showContent);
    setElementHidden(guideRecordEmptyState, !showEmptyState);
}

function setElementHidden(element, hidden) {
    if (element) {
        element.hidden = hidden;
    }
}

function selectSavedGuide(guideVersionId) {
    document.querySelectorAll('.saved-guide-item').forEach((item) => {
        const selected = Number(item.dataset.guideVersionId) === Number(guideVersionId);
        item.classList.toggle('is-selected', selected);
        item.setAttribute('aria-pressed', String(selected));
    });
}

function clearSelectedSavedGuide() {
    document.querySelectorAll('.saved-guide-item').forEach((item) => {
        item.classList.remove('is-selected');
        item.setAttribute('aria-pressed', 'false');
    });
}

async function saveGuideVersion() {
    if (savedGuideVersionId || isSubmitting) {
        return;
    }

    hideError();
    hideGuideSaveMessage();

    if (!Number.isInteger(latestCalculationId) || latestCalculationId <= 0) {
        showError('1단계 여유자금 계산 결과를 다시 저장해주세요.');
        showStep(1);
        return;
    }

    if (!Number.isInteger(latestPlanId) || latestPlanId <= 0) {
        showError('운용성향 설문과 자산배분 결과를 다시 확인해주세요.');
        showStep(2);
        return;
    }

    const guideName = guideNameInput?.value.trim() || '';
    if (guideName.length > 100) {
        showGuideSaveMessage('운용 기록 이름은 100자 이하여야 합니다.', false);
        guideNameInput?.focus();
        return;
    }

    const selectedEtfProductIds = getSelectedEtfProducts()
        .map((product) => Number(product.investmentProductId))
        .filter((productId) => Number.isInteger(productId) && productId > 0);

    if (selectedEtfProductIds.length > 4) {
        showGuideSaveMessage('관심 ETF는 최대 4개까지 저장할 수 있습니다.', false);
        return;
    }

    if (!guideSaveIdempotencyKey) {
        guideSaveIdempotencyKey = createIdempotencyKey();
    }

    saveGuideVersionButton.disabled = true;
    saveGuideVersionButton.textContent = '저장하고 있습니다...';

    try {
        const response = await fetch('/api/surplus-funds/guide-versions', {
            method: 'POST',
            headers: createJsonHeaders(),
            credentials: 'same-origin',
            body: JSON.stringify({
                guideName: guideName || null,
                surplusFundCalculationId: latestCalculationId,
                surplusFundPlanId: latestPlanId,
                selectedEtfProductIds,
                idempotencyKey: guideSaveIdempotencyKey
            })
        });

        const contentType = response.headers.get('content-type') || '';
        const responseBody = contentType.includes('application/json')
            ? await response.json()
            : null;

        if (!response.ok) {
            throw new Error(
                responseBody?.message
                || `운용 기록 저장에 실패했습니다. (${response.status})`
            );
        }

        savedGuideVersionId = Number(responseBody.surplusFundGuideVersionId);
        showCurrentGuideMode();
        showGuideSaveMessage(
            `v${responseBody.guideVersionNo} · ${responseBody.guideName} 저장이 완료되었습니다.`,
            true
        );
        saveGuideVersionButton.textContent = '저장 완료';
        await loadSavedGuideVersions();
    } catch (error) {
        saveGuideVersionButton.disabled = false;
        saveGuideVersionButton.textContent = '이 운용 기록 저장하기';
        showGuideSaveMessage(
            error.message || '운용 기록 저장 중 오류가 발생했습니다.',
            false
        );
    }
}

async function loadSavedGuideVersions() {
    const savedGuideList = document.getElementById('savedGuideList');
    if (!savedGuideList) {
        return;
    }

    try {
        const response = await fetch('/api/surplus-funds/guide-versions', {
            method: 'GET',
            headers: { 'Accept': 'application/json' },
            credentials: 'same-origin'
        });

        if (!response.ok) {
            throw new Error('저장된 운용 기록을 불러오지 못했습니다.');
        }

        const contentType = response.headers.get('content-type') || '';
        if (!contentType.includes('application/json')) {
            throw new Error('로그인 세션을 확인해주세요.');
        }

        const guideVersions = await response.json();
        savedGuideList.replaceChildren();

        if (!Array.isArray(guideVersions) || guideVersions.length === 0) {
            const empty = document.createElement('p');
            empty.className = 'saved-guide-empty';
            empty.textContent = '아직 저장된 운용 기록이 없습니다.';
            savedGuideList.append(empty);
            return;
        }

        guideVersions.forEach((guideVersion) => {
            const item = document.createElement('button');
            item.type = 'button';
            item.className = 'saved-guide-item';
            item.dataset.guideVersionId = String(guideVersion.surplusFundGuideVersionId);
            item.setAttribute('aria-pressed', 'false');

            const copy = document.createElement('div');
            const name = document.createElement('strong');
            name.textContent = guideVersion.guideName;
            const detail = document.createElement('span');
            detail.textContent = `v${guideVersion.guideVersionNo} · ${styleLabels[guideVersion.investmentStyle] || guideVersion.investmentStyle} · ${formatWonValue(guideVersion.operationAmount)}`;
            copy.append(name, detail);

            const meta = document.createElement('div');
            meta.className = 'saved-guide-meta';
            const completedAt = document.createElement('strong');
            completedAt.textContent = formatDateTime(guideVersion.completedAt);
            const etfCount = document.createElement('span');
            etfCount.textContent = `관심 ETF ${guideVersion.selectedEtfCount}개`;
            meta.append(completedAt, etfCount);

            item.append(copy, meta);
            item.addEventListener('click', () => {
                loadSavedGuideVersion(guideVersion.surplusFundGuideVersionId);
            });
            savedGuideList.append(item);
        });

        if (viewingSavedGuideVersionId) {
            selectSavedGuide(viewingSavedGuideVersionId);
        }
    } catch (error) {
        savedGuideList.replaceChildren();
        const message = document.createElement('p');
        message.className = 'saved-guide-empty';
        message.textContent = error.message;
        savedGuideList.append(message);
    }
}

async function loadSavedGuideVersion(guideVersionId) {
    const normalizedId = Number(guideVersionId);
    if (!Number.isInteger(normalizedId) || normalizedId <= 0) {
        showError('조회할 운용 기록 ID가 올바르지 않습니다.');
        return;
    }

    hideError();
    selectSavedGuide(normalizedId);

    if (guideDetailRequestController) {
        guideDetailRequestController.abort();
    }
    guideDetailRequestController = new AbortController();
    const requestController = guideDetailRequestController;

    const selectedButton = document.querySelector(
        `.saved-guide-item[data-guide-version-id="${normalizedId}"]`
    );
    selectedButton?.setAttribute('aria-busy', 'true');
    selectedButton?.setAttribute('disabled', 'disabled');

    try {
        const response = await fetch(
            `/api/surplus-funds/guide-versions/${normalizedId}`,
            {
                method: 'GET',
                headers: { 'Accept': 'application/json' },
                credentials: 'same-origin',
                signal: requestController.signal
            }
        );

        const contentType = response.headers.get('content-type') || '';
        const responseBody = contentType.includes('application/json')
            ? await response.json()
            : null;

        if (!response.ok) {
            throw new Error(
                responseBody?.message
                || `저장된 운용 기록을 불러오지 못했습니다. (${response.status})`
            );
        }

        if (!responseBody?.calculation || !responseBody?.planResult) {
            throw new Error('저장된 운용 기록의 상세값이 올바르지 않습니다.');
        }

        viewingSavedGuideVersionId = normalizedId;
        renderGuideRecord(toSavedGuideRecord(responseBody));
        showSavedGuideMode(responseBody);
    } catch (error) {
        if (error.name === 'AbortError') {
            return;
        }
        clearSelectedSavedGuide();
        showError(error.message || '저장된 운용 기록 조회 중 오류가 발생했습니다.');
    } finally {
        if (guideDetailRequestController === requestController) {
            guideDetailRequestController = null;
        }
        selectedButton?.removeAttribute('aria-busy');
        selectedButton?.removeAttribute('disabled');
    }
}

function toSavedGuideRecord(detail) {
    const calculation = detail.calculation;
    const planResult = detail.planResult;

    const allocations = Array.isArray(planResult.allocations)
        ? planResult.allocations.map((allocation) => ({
            assetType: allocation.assetType,
            ratio: Number(allocation.allocationRatio),
            amount: Number(allocation.allocationAmount)
        }))
        : [];

    return {
        selectedAccountBalance: toNullableNumber(calculation.selectedAccountBalance),
        livingExpense: toNullableNumber(calculation.adjustedLivingExpense),
        scheduledExpense: toNullableNumber(calculation.adjustedScheduledExpense),
        emergencyFund: toNullableNumber(calculation.adjustedEmergencyFund),
        finalSurplusAmount: toNullableNumber(calculation.finalSurplusAmount),
        allocations,
        reasons: Array.isArray(planResult.reasons) ? planResult.reasons : [],
        etfs: Array.isArray(detail.interestedEtfs) ? detail.interestedEtfs : []
    };
}

function getSelectedEtfProducts() {
    if (typeof window.getSelectedEtfProducts !== 'function') {
        return [];
    }

    const selected = window.getSelectedEtfProducts();
    return Array.isArray(selected) ? selected : [];
}

function createJsonHeaders() {
    const headers = {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
    };
    const csrfToken = document.getElementById('csrfToken')?.value;
    const csrfHeader = document.getElementById('csrfHeader')?.value;

    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    return headers;
}

function createIdempotencyKey() {
    if (window.crypto && typeof window.crypto.randomUUID === 'function') {
        return window.crypto.randomUUID();
    }

    return `surplus-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function readInputNumber(elementId) {
    const value = Number(document.getElementById(elementId)?.value);
    return Number.isFinite(value) ? value : 0;
}

function setText(elementId, value) {
    const element = document.getElementById(elementId);
    if (element) {
        element.textContent = value;
    }
}

function formatWonValue(value) {
    if (value === null || value === undefined || value === '') {
        return '-';
    }

    const number = Number(value);
    return Number.isFinite(number)
        ? `${wonFormatter.format(number)}원`
        : '-';
}

function formatPlainNumber(value) {
    const number = Number(value);
    return Number.isFinite(number)
        ? number.toLocaleString('ko-KR', { maximumFractionDigits: 2 })
        : '-';
}

function formatPercentValue(value) {
    if (value === null || value === undefined || value === '') {
        return '-';
    }

    const number = Number(value);
    if (!Number.isFinite(number)) {
        return '-';
    }

    return `${number > 0 ? '+' : ''}${number.toLocaleString('ko-KR', {
        maximumFractionDigits: 2
    })}%`;
}

function toNullableNumber(value) {
    if (value === null || value === undefined || value === '') {
        return null;
    }

    const number = Number(value);
    return Number.isFinite(number) ? number : null;
}

function formatDateTime(value) {
    if (!value) {
        return '-';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function showGuideSaveMessage(message, success) {
    if (!guideSaveMessage) {
        return;
    }

    guideSaveMessage.textContent = message;
    guideSaveMessage.classList.toggle('message-success', success);
    guideSaveMessage.classList.toggle('message-error', !success);
    guideSaveMessage.hidden = false;
}

function hideGuideSaveMessage() {
    if (!guideSaveMessage) {
        return;
    }

    guideSaveMessage.hidden = true;
    guideSaveMessage.textContent = '';
    guideSaveMessage.classList.remove('message-success', 'message-error');
}

function setLoading(loading) {
    isSubmitting = loading;
    submitButton.disabled = loading;

    submitButton.textContent =
        loading
            ? '분석하고 있습니다...'
            : '자산배분 결과 확인';
}

function showError(message) {
    errorBox.textContent = message;
    errorBox.hidden = false;

    errorBox.scrollIntoView({
        behavior: 'smooth',
        block: 'center'
    });
}

function hideError() {
    errorBox.hidden = true;
    errorBox.textContent = '';
}

showQuestion(0, false);
showStep(1, false);
