(function () {
    const card = document.getElementById('householdProfileSummaryCard');
    const textEl = document.getElementById('householdProfileSummaryText');
    if (!card || !textEl) {
        return;
    }

    fetch('/api/future-goal/household-profile-summary')
        .then((res) => {
            if (res.status === 204) {
                return null;
            }
            if (!res.ok) {
                throw new Error('household-profile-summary 조회 실패: ' + res.status);
            }
            return res.json();
        })
        .then((data) => {
            if (!data || !data.summaryText) {
                return;
            }
            textEl.textContent = data.summaryText;
            card.classList.remove('hidden');
        })
        .catch(() => {
            // 요약 카드는 보조 정보라 실패해도 화면 자체는 그대로 둔다 (카드만 숨김 유지)
        });
})();

(function () {
    const cardsEl = document.getElementById('goalPresetCards');
    const amountFieldEl = document.getElementById('goalAmountField');
    const quickPicksEl = document.getElementById('goalAmountQuickPicks');
    const resetButtonEl = document.getElementById('goalAmountResetButton');
    const magnitudeEl = document.getElementById('goalAmountMagnitude');
    const hiddenInputEl = document.getElementById('goalAmountInput');
    const submitButtonEl = document.getElementById('goalAmountSubmitButton');
    const paceMagnitudeEl = document.getElementById('goalPaceMagnitude');
    const paceUnitEl = document.getElementById('goalPaceUnit');
    const timelineEndLabelEl = document.getElementById('goalPaceTimelineEndLabel');
    const ageSpectrumCardEl = document.getElementById('goalAgeSpectrumCard');
    const ageSpectrumToggleEl = document.getElementById('goalAgeSpectrumToggle');
    const ageSpectrumYAxisEl = document.getElementById('goalAgeSpectrumYAxis');
    const ageSpectrumAreaEl = document.getElementById('goalAgeSpectrumArea');
    const ageSpectrumCurveEl = document.getElementById('goalAgeSpectrumCurve');
    const ageSpectrumDotsEl = document.getElementById('goalAgeSpectrumCohortDots');
    const ageSpectrumGoalTickEl = document.getElementById('goalAgeSpectrumGoalTick');
    const ageSpectrumGoalMarkerEl = document.getElementById('goalAgeSpectrumGoalMarker');
    const ageSpectrumLabelsEl = document.getElementById('goalAgeSpectrumLabels');
    const ageSpectrumPercentEl = document.getElementById('goalAgeSpectrumPercent');
    const ageSpectrumPercentTextEl = document.getElementById('goalAgeSpectrumPercentText');
    const ageSpectrumCaptionEl = document.getElementById('goalAgeSpectrumCaption');
    const ageBenchmarkAmountEl = document.getElementById('goalAgeBenchmarkAmount');
    const ageGoalAmountEl = document.getElementById('goalAgeGoalAmount');
    const ageComparisonGoalEl = document.getElementById('goalAgeComparisonGoal');
    const householdSpectrumCardEl = document.getElementById('goalHouseholdSpectrumCard');
    const householdSpectrumToggleEl = document.getElementById('goalHouseholdSpectrumToggle');
    const householdCalloutEl = document.getElementById('goalHouseholdCallout');
    const householdBarEl = document.getElementById('goalHouseholdBar');
    const householdBarLabelsEl = document.getElementById('goalHouseholdBarLabels');
    const householdGoalLineEl = document.getElementById('goalHouseholdGoalLine');
    const householdSpectrumPercentEl = document.getElementById('goalHouseholdSpectrumPercent');
    const householdSpectrumPercentTextEl = document.getElementById('goalHouseholdSpectrumPercentText');
    const householdSpectrumCaptionEl = document.getElementById('goalHouseholdSpectrumCaption');
    const detailProgressBarEl = document.getElementById('goalDetailProgressBar');
    const progressPercentEl = document.getElementById('goalPaceProgressPercent');
    const detailNetWorthEl = document.getElementById('goalDetailNetWorth');
    const detailRemainingEl = document.getElementById('goalDetailRemaining');
    const detailDateEl = document.getElementById('goalDetailDate');
    const detailIncomeEl = document.getElementById('goalDetailIncome');
    const detailExpenseEl = document.getElementById('goalDetailExpense');
    const detailLoanPaymentEl = document.getElementById('goalDetailLoanPayment');
    const detailSavingsEl = document.getElementById('goalDetailSavings');
    const detailNoteEl = document.getElementById('goalDetailNote');
    if (!cardsEl || !amountFieldEl || !hiddenInputEl || !submitButtonEl) {
        return;
    }

    const SVG_NS = 'http://www.w3.org/2000/svg';

    const PRESET_META = {
        JEONSE: {icon: 'home'},
        SEED_MONEY: {icon: 'eco'},
        FINANCIAL_FREEDOM: {icon: 'explore'},
        EMERGENCY_FUND: {icon: 'shield'}
    };
    const EMERGENCY_FUND_CAPTION = '당신의 월평균 생활비 기준';
    const DECADE_TICKS = [
        {amount: 1000000, label: '+100만'},
        {amount: 5000000, label: '+500만'},
        {amount: 10000000, label: '+1,000만'},
        {amount: 100000000, label: '+1억'}
    ];

    const PREVIEW_DEBOUNCE_MS = 300;

    // 연령대 스펙트럼 차트 — 실제 연령 구간(29세 이하~60세 이상)의 대표 나이를 x축에 그대로 반영해서
    // "40~49세"와 "50~59세" 사이 같은 균등 간격이 실제 나이 차이를 정직하게 보여주게 한다.
    const AGE_SPECTRUM_WIDTH = 300;
    const AGE_SPECTRUM_PAD_LEFT = 40;
    const AGE_SPECTRUM_PAD_RIGHT = 14;
    const AGE_SPECTRUM_BASE_Y = 170;
    const AGE_SPECTRUM_TOP_Y = 14;
    const AGE_MIDPOINTS = [25, 35, 45, 55, 65];
    const AGE_SPECTRUM_YAXIS_FRACTIONS = [0, 0.25, 0.5, 0.75, 1];

    let previewDebounceTimer = null;
    let latestRequestedAmount = null;
    let presetCardEls = [];
    let currentAmount = 0;
    let activePresetRow = null;
    let ageCohortData = null;
    let ageSpectrumSelectedIndex = 0;
    let ageSpectrumMaxValue = 1;
    let householdCohortData = null;
    let householdSpectrumSelectedIndex = 0;
    let householdSpectrumMaxValue = 1;

    // ---------- 포맷 ----------
    // "3억 5,000만" + "원" 처럼 자릿수(억/만)를 나눠서 타이포 위계를 줄 수 있게 문자열만 만든다.
    function formatMagnitude(amount) {
        const eok = Math.floor(amount / 100000000);
        const man = Math.floor((amount % 100000000) / 10000);
        const parts = [];
        if (eok > 0) {
            parts.push(`${eok}억`);
        }
        if (man > 0) {
            parts.push(`${new Intl.NumberFormat('ko-KR').format(man)}만`);
        }
        return parts.length ? parts.join(' ') : '0';
    }

    function formatWon(amount) {
        return `${new Intl.NumberFormat('ko-KR').format(Math.round(Number(amount) || 0))}원`;
    }

    function formatMonthsSplit(monthsToGoal) {
        if (monthsToGoal === null || monthsToGoal === undefined) {
            return {magnitude: '예측 어려움', unit: ''};
        }
        const years = Math.floor(monthsToGoal / 12);
        const months = monthsToGoal % 12;
        if (years > 0) {
            return {magnitude: `${years}년`, unit: months > 0 ? `${months}개월` : ''};
        }
        return {magnitude: `${months}개월`, unit: ''};
    }

    // ---------- 연령대별 순자산 스펙트럼 ----------
    function ageSpectrumXForIndex(i) {
        const minAge = AGE_MIDPOINTS[0];
        const maxAge = AGE_MIDPOINTS[AGE_MIDPOINTS.length - 1];
        const ratio = (AGE_MIDPOINTS[i] - minAge) / (maxAge - minAge);
        return AGE_SPECTRUM_PAD_LEFT + ratio * (AGE_SPECTRUM_WIDTH - AGE_SPECTRUM_PAD_LEFT - AGE_SPECTRUM_PAD_RIGHT);
    }

    function ageSpectrumYForValue(value) {
        const ratio = Math.max(0, Math.min(1, value / ageSpectrumMaxValue));
        return AGE_SPECTRUM_BASE_Y - ratio * (AGE_SPECTRUM_BASE_Y - AGE_SPECTRUM_TOP_Y);
    }

    // Y축 눈금선 + 금액 라벨 — 곡선의 값 차이를 감이 아니라 실제 금액으로 읽을 수 있게 한다.
    // 코호트 곡선처럼 데이터가 오면 한 번만 그린다(목표 금액 갱신과 무관).
    function renderAgeSpectrumYAxis() {
        if (!ageSpectrumYAxisEl) {
            return;
        }
        ageSpectrumYAxisEl.innerHTML = '';
        AGE_SPECTRUM_YAXIS_FRACTIONS.forEach((fraction) => {
            const value = ageSpectrumMaxValue * fraction;
            const y = ageSpectrumYForValue(value);

            const line = document.createElementNS(SVG_NS, 'line');
            line.setAttribute('class', 'fp-age-spectrum-yaxis-line');
            line.setAttribute('x1', String(AGE_SPECTRUM_PAD_LEFT));
            line.setAttribute('y1', String(y));
            line.setAttribute('x2', String(AGE_SPECTRUM_WIDTH - AGE_SPECTRUM_PAD_RIGHT));
            line.setAttribute('y2', String(y));
            ageSpectrumYAxisEl.appendChild(line);

            const label = document.createElementNS(SVG_NS, 'text');
            label.setAttribute('class', 'fp-age-spectrum-yaxis-label');
            label.setAttribute('x', String(AGE_SPECTRUM_PAD_LEFT - 6));
            label.setAttribute('y', String(y));
            label.setAttribute('text-anchor', 'end');
            label.setAttribute('dominant-baseline', 'middle');
            label.textContent = fraction === 0 ? '0원' : `${formatMagnitude(value)}원`;
            ageSpectrumYAxisEl.appendChild(label);
        });
    }

    // 5개 점을 지나는 부드러운 곡선(Catmull-Rom → 3차 베지어 변환) — 막대그래프 대신
    // "나이 들수록 자산이 쌓이는 흐름"을 연속된 지형선으로 표현한다.
    function catmullRomPath(points) {
        if (points.length < 2) {
            return '';
        }
        let d = `M${points[0].x},${points[0].y}`;
        for (let i = 0; i < points.length - 1; i++) {
            const p0 = points[i - 1] || points[i];
            const p1 = points[i];
            const p2 = points[i + 1];
            const p3 = points[i + 2] || p2;
            const cp1x = p1.x + (p2.x - p0.x) / 6;
            const cp1y = p1.y + (p2.y - p0.y) / 6;
            const cp2x = p2.x - (p3.x - p1.x) / 6;
            const cp2y = p2.y - (p3.y - p1.y) / 6;
            d += ` C${cp1x},${cp1y} ${cp2x},${cp2y} ${p2.x},${p2.y}`;
        }
        return d;
    }

    // 코호트 곡선(면적/점/라벨)은 데이터가 오면 한 번만 그린다 — y축 스케일은 코호트 중앙값
    // 기준으로 고정해서, 목표 금액을 바꿔도 곡선 자체는 흔들리지 않고 목표 마커만 움직인다.
    function renderAgeSpectrumStatic(data) {
        if (!ageSpectrumCardEl || !data.cohorts || data.cohorts.length === 0 || data.userAgeGroupIndex < 0) {
            return;
        }
        ageCohortData = data;
        ageSpectrumSelectedIndex = data.userAgeGroupIndex;

        ageSpectrumCardEl.classList.remove('hidden');
        updateAgeSpectrumToggleButtons();
        updateAgeSpectrumGoalMarker(currentAmount);
    }

    function updateAgeSpectrumToggleButtons() {
        if (!ageSpectrumToggleEl || !ageCohortData) {
            return;
        }
        ageSpectrumToggleEl.querySelectorAll('.fp-age-toggle-btn').forEach((btn) => {
            const offset = Number(btn.dataset.offset);
            const idx = ageCohortData.userAgeGroupIndex + offset;
            btn.classList.toggle('active', idx === ageSpectrumSelectedIndex);
            if (offset !== 0) {
                btn.disabled = idx < 0 || idx >= ageCohortData.cohorts.length;
            }
        });
    }

    // 목표 금액이 바뀌거나(직접 입력) 비교 대상 연령대가 바뀔 때(토글) 마커·퍼센트·캡션만 갱신한다.
    // 곡선 자체는 다시 그리지 않아 조작 중에도 시각적으로 안정적이다.
    function updateAgeSpectrumGoalMarker(amount) {
        if (!ageCohortData) {
            return;
        }
        const selectedIndex = ageSpectrumSelectedIndex;
        const cohort = ageCohortData.cohorts[selectedIndex];
        const median = Number(cohort.medianNetWorth);
        const netWorth = Number(ageCohortData.currentNetWorth) || 0;
        const deltaPercent = ((netWorth - median) / median) * 100;
        const sign = deltaPercent >= 0 ? '+' : '';
        if (ageBenchmarkAmountEl) ageBenchmarkAmountEl.textContent = formatWon(median);
        if (ageGoalAmountEl) ageGoalAmountEl.textContent = formatWon(netWorth);
        if (ageComparisonGoalEl) ageComparisonGoalEl.style.width = `${Math.min(100, Math.max(0, netWorth / median * 100))}%`;
        ageSpectrumPercentEl.textContent = `${sign}${deltaPercent.toFixed(1)}%`;
        // 불안을 조장하지 않는 담담한 어휘 — "뒤처졌다"류 표현 대신 위치만 서술
        ageSpectrumPercentTextEl.textContent = deltaPercent >= 0
            ? `${cohort.label} 중앙값보다 높아요`
            : `${cohort.label} 중앙값보다 낮아요`;
        const gap = Math.max(0, median - netWorth);
        ageSpectrumCaptionEl.textContent = gap > 0
            ? `${cohort.label} 중앙값보다 ${formatWon(gap)} 낮아요. 목표 금액은 이 차이와 별도로 설정할 수 있어요.`
            : `${cohort.label} 중앙값보다 높은 현재 순자산이에요.`;
    }

    if (ageSpectrumToggleEl) {
        ageSpectrumToggleEl.querySelectorAll('.fp-age-toggle-btn').forEach((btn) => {
            btn.addEventListener('click', () => {
                if (btn.disabled || !ageCohortData) {
                    return;
                }
                const offset = Number(btn.dataset.offset);
                const newIndex = ageCohortData.userAgeGroupIndex + offset;
                if (newIndex < 0 || newIndex >= ageCohortData.cohorts.length) {
                    return;
                }
                ageSpectrumSelectedIndex = newIndex;
                updateAgeSpectrumToggleButtons();
                updateAgeSpectrumGoalMarker(currentAmount);
            });
        });
    }

    // ---------- 금액 표시 갱신 ----------
    function updateAmountDisplay(amount) {
        magnitudeEl.textContent = formatMagnitude(amount);
        updateAgeSpectrumGoalMarker(amount);
        updateHouseholdGoalCallout(amount);
    }

    function highlightQuickPick(amount) {
        if (!quickPicksEl) {
            return;
        }
        quickPicksEl.querySelectorAll('.fp-amount-quick-pick').forEach((btn) => {
            btn.classList.toggle('active', Number(btn.dataset.amount) === amount);
        });
    }

    function applyFunctionalState(amount) {
        currentAmount = amount;
        hiddenInputEl.value = amount;
        if (activePresetRow) {
            activePresetRow.dataset.amount = String(amount);
            activePresetRow.querySelector('.fp-amount-input input').value = new Intl.NumberFormat('ko-KR').format(amount);
        }
        highlightMatchingPresetCard(amount);
        highlightQuickPick(amount);
        schedulePreview(amount);
        submitButtonEl.disabled = false;
        updateQuickPickAvailability();
    }

    function setGoalAmount(amount) {
        applyFunctionalState(amount);
        amountFieldEl.value = new Intl.NumberFormat('ko-KR').format(amount);
        updateAmountDisplay(amount);
    }

    function resetGoalAmount() {
        window.clearTimeout(previewDebounceTimer);
        latestRequestedAmount = null;
        currentAmount = 0;
        activePresetRow = null;
        hiddenInputEl.value = '0';
        amountFieldEl.value = '0';
        highlightMatchingPresetCard(0);
        highlightQuickPick(0);
        updateAmountDisplay(0);
        submitButtonEl.disabled = true;
        updateQuickPickAvailability();
    }

    function highlightMatchingPresetCard(amount) {
        presetCardEls.forEach((el) => {
            const matches = el === activePresetRow;
            el.classList.toggle('selected', matches);
            el.setAttribute('aria-selected', String(matches));
            el.querySelector('.fp-goal-choice').checked = matches;
        });
    }

    // ---------- 빠른 금액 선택 ----------
    function renderQuickPicks() {
        if (!quickPicksEl) {
            return;
        }
        quickPicksEl.innerHTML = '';
        DECADE_TICKS.forEach(({amount, label}) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'fp-amount-quick-pick';
            btn.dataset.amount = String(amount);
            btn.textContent = label;
            btn.addEventListener('click', () => {
                const enteredAmount = Number(amountFieldEl.value.replace(/[^0-9]/g, '')) || 0;
                setGoalAmount(enteredAmount + amount);
            });
            quickPicksEl.appendChild(btn);
        });
        updateQuickPickAvailability();
    }

    function updateQuickPickAvailability() {
        if (!quickPicksEl) {
            return;
        }
        quickPicksEl.querySelectorAll('.fp-amount-quick-pick').forEach((btn) => {
            btn.disabled = !activePresetRow;
        });
    }

    if (resetButtonEl) {
        resetButtonEl.addEventListener('click', resetGoalAmount);
    }

    // ---------- 금액 직접 입력 ----------
    amountFieldEl.addEventListener('input', () => {
        const digits = amountFieldEl.value.replace(/[^0-9]/g, '');
        amountFieldEl.value = digits ? new Intl.NumberFormat('ko-KR').format(Number(digits)) : '';
        if (!digits || Number(digits) <= 0) {
            return;
        }
        applyFunctionalState(Number(digits));
        updateAmountDisplay(Number(digits));
    });

    amountFieldEl.addEventListener('blur', () => {
        amountFieldEl.value = new Intl.NumberFormat('ko-KR').format(currentAmount);
    });

    // ---------- 페이스 카드: 목표 도달 라벨 ----------
    // 진행률 막대 우측 끝에 붙는 라벨 — "지금 ~ 목표 도달까지 약 N년 M개월" 문구만 담당한다.
    function renderPaceEndLabel(monthsToGoal) {
        if (monthsToGoal === null || monthsToGoal === undefined) {
            timelineEndLabelEl.textContent = '예측하기 어려워요';
            return;
        }
        if (monthsToGoal === 0) {
            timelineEndLabelEl.textContent = '이미 도달했어요';
            return;
        }
        const {magnitude, unit} = formatMonthsSplit(monthsToGoal);
        timelineEndLabelEl.textContent = `목표 도달 · 약 ${magnitude} ${unit} 후`.replace(/\s+/g, ' ').trim();
    }

    // ---------- 가구원수별 구성비 막대 (Household Composition Bar, 마리메코형) ----------
    // 연령대 스펙트럼(생애주기 곡선)과 일부러 다른 문법 — 막대 폭은 전체 가구 중 비중
    // (distributionPct), 막대 높이는 순자산 중앙값(공유 축)을 인코딩한다. 그리고 그 축 위에
    // "내 목표" 점선을 가로로 그어서 각 가구 규모와 내 목표를 한눈에 비교할 수 있게 한다 —
    // 연령대 스펙트럼의 "곡선 위 마커"에 대응하는, 가구원별 버전의 위치 표시.
    const HOUSEHOLD_MIN_SEGMENT_HEIGHT_PCT = 12;

    function householdValueToHeightPercent(value) {
        const ratio = Math.max(0, Math.min(1, value / householdSpectrumMaxValue));
        return Math.max(HOUSEHOLD_MIN_SEGMENT_HEIGHT_PCT, ratio * 100);
    }

    // 막대(폭·높이)는 데이터가 오면 한 번만 그린다 — 금액 입력으로 다시 그리지 않고,
    // "내 목표" 선(goalHouseholdGoalLine)만 같은 축 위에서 움직인다.
    function renderHouseholdSpectrumStatic(data) {
        if (!householdSpectrumCardEl || !data.cohorts || data.cohorts.length === 0 || data.userHouseholdSizeIndex < 0) {
            return;
        }
        householdCohortData = data;
        householdSpectrumSelectedIndex = data.userHouseholdSizeIndex;

        const medians = data.cohorts.map((c) => Number(c.medianNetWorth));
        householdSpectrumMaxValue = Math.max(...medians) * 1.25;

        householdBarEl.innerHTML = '';
        householdBarEl.appendChild(householdGoalLineEl);
        householdBarLabelsEl.innerHTML = '';

        data.cohorts.forEach((cohort, i) => {
            const widthPct = Number(cohort.distributionPct);
            const median = Number(cohort.medianNetWorth);

            const segmentEl = document.createElement('button');
            segmentEl.type = 'button';
            segmentEl.className = 'fp-household-segment';
            segmentEl.style.width = widthPct + '%';
            segmentEl.style.height = householdValueToHeightPercent(median) + '%';
            segmentEl.dataset.index = String(i);
            segmentEl.setAttribute(
                'aria-label',
                `${cohort.label} · 전체 가구의 ${widthPct.toFixed(1)}% · 순자산 중앙값 ${formatMagnitude(median)}원`
            );
            segmentEl.innerHTML = '<span class="material-symbols-outlined fp-household-segment-check" aria-hidden="true">check</span>';
            segmentEl.addEventListener('click', () => {
                householdSpectrumSelectedIndex = i;
                updateHouseholdSpectrumToggleButtons();
                updateHouseholdGoalCallout(currentAmount);
            });
            householdBarEl.appendChild(segmentEl);

            const labelEl = document.createElement('span');
            labelEl.className = 'fp-household-bar-label';
            labelEl.style.width = widthPct + '%';
            labelEl.textContent = cohort.label;
            labelEl.dataset.index = String(i);
            householdBarLabelsEl.appendChild(labelEl);
        });

        householdSpectrumCardEl.classList.remove('hidden');
        updateHouseholdSpectrumToggleButtons();
        updateHouseholdGoalCallout(currentAmount);
    }

    function updateHouseholdSpectrumToggleButtons() {
        if (!householdSpectrumToggleEl || !householdCohortData) {
            return;
        }
        householdSpectrumToggleEl.querySelectorAll('.fp-age-toggle-btn').forEach((btn) => {
            const offset = Number(btn.dataset.offset);
            const idx = householdCohortData.userHouseholdSizeIndex + offset;
            btn.classList.toggle('active', idx === householdSpectrumSelectedIndex);
            if (offset !== 0) {
                btn.disabled = idx < 0 || idx >= householdCohortData.cohorts.length;
            }
        });
    }

    // 목표 금액이 바뀌거나(직접 입력) 비교 대상 가구원수가 바뀔 때(토글·세그먼트 클릭)
    // "내 목표" 선 위치·선택 표시·퍼센트·캡션을 갱신한다. 막대 자체는 다시 그리지 않는다.
    function updateHouseholdGoalCallout(amount) {
        if (!householdCohortData) {
            return;
        }
        const selectedIndex = householdSpectrumSelectedIndex;
        Array.from(householdBarEl.children).forEach((segEl) => {
            if (segEl.classList.contains('fp-household-segment')) {
                segEl.classList.toggle('selected', Number(segEl.dataset.index) === selectedIndex);
            }
        });
        Array.from(householdBarLabelsEl.children).forEach((labelEl) => {
            labelEl.classList.toggle('active', Number(labelEl.dataset.index) === selectedIndex);
        });

        householdGoalLineEl.style.bottom = householdValueToHeightPercent(amount) + '%';

        const cohort = householdCohortData.cohorts[selectedIndex];
        householdCalloutEl.innerHTML =
            `${cohort.label} <span class="fp-household-callout-pct">전체 가구의 ${Number(cohort.distributionPct).toFixed(1)}%</span>`;

        const median = Number(cohort.medianNetWorth);
        const deltaPercent = ((amount - median) / median) * 100;
        const sign = deltaPercent >= 0 ? '+' : '';
        householdSpectrumPercentEl.textContent = `${sign}${deltaPercent.toFixed(1)}%`;
        householdSpectrumPercentTextEl.textContent = deltaPercent >= 0
            ? `${cohort.label} 가구 중앙값보다 높아요`
            : `${cohort.label} 가구 중앙값보다 낮아요`;
        householdSpectrumCaptionEl.textContent =
            `KOSIS 가계금융복지조사 · ${householdCohortData.surveyYear}년 기준 · ${cohort.label} 가구 중앙값 대비 (실제 계산값)`;
    }

    if (householdSpectrumToggleEl) {
        householdSpectrumToggleEl.querySelectorAll('.fp-age-toggle-btn').forEach((btn) => {
            btn.addEventListener('click', () => {
                if (btn.disabled || !householdCohortData) {
                    return;
                }
                const offset = Number(btn.dataset.offset);
                const newIndex = householdCohortData.userHouseholdSizeIndex + offset;
                if (newIndex < 0 || newIndex >= householdCohortData.cohorts.length) {
                    return;
                }
                householdSpectrumSelectedIndex = newIndex;
                updateHouseholdSpectrumToggleButtons();
                updateHouseholdGoalCallout(currentAmount);
            });
        });
    }

    // ---------- 프리뷰 API ----------
    function schedulePreview(amount) {
        window.clearTimeout(previewDebounceTimer);
        previewDebounceTimer = window.setTimeout(() => fetchPreview(amount), PREVIEW_DEBOUNCE_MS);
    }

    function fetchPreview(amount) {
        latestRequestedAmount = amount;
        fetch(`/api/future-goal/preview?goalAmount=${encodeURIComponent(amount)}`)
            .then((res) => {
                if (!res.ok) {
                    throw new Error('preview 조회 실패: ' + res.status);
                }
                return res.json();
            })
            .then((data) => {
                if (amount !== latestRequestedAmount) {
                    return; // 그 사이에 더 최신 요청이 나갔으면 느리게 도착한 응답은 무시한다
                }
                const {magnitude, unit} = formatMonthsSplit(data.monthsToGoal);
                paceMagnitudeEl.textContent = magnitude;
                paceUnitEl.textContent = unit;
                renderPaceEndLabel(data.monthsToGoal);
                renderGoalDetail(data);
                updateSelectedRowPreview(data);
            })
            .catch(() => {
                if (amount !== latestRequestedAmount) {
                    return;
                }
                paceMagnitudeEl.textContent = '불러오지 못했어요';
                paceUnitEl.textContent = '';
            });
    }

    // 계산 명세(월 소득/생활비/원리금상환액 → 월 저축 가능액)는 항상 표에 그대로 보여주고,
    // detailNoteEl은 "왜 산정이 어려운지" 같은 예외 상황을 짚어주는 경고 줄로만 쓴다.
    function setDetailNote(text) {
        if (!text) {
            detailNoteEl.textContent = '';
            detailNoteEl.classList.add('hidden');
            return;
        }
        detailNoteEl.textContent = text;
        detailNoteEl.classList.remove('hidden');
    }

    function renderGoalDetail(data) {
        if (!detailProgressBarEl) {
            return;
        }
        const goal = Number(currentAmount);
        const netWorth = Number(data.currentNetWorth) || 0;
        const progress = goal > 0 ? Math.max(0, Math.min(100, netWorth / goal * 100)) : 0;
        detailProgressBarEl.style.width = progress + '%';
        if (progressPercentEl) {
            progressPercentEl.textContent = `${progress.toFixed(1)}%`;
        }
        detailNetWorthEl.textContent = formatWon(netWorth);
        detailRemainingEl.textContent = formatWon(data.remainingAmount);
        detailIncomeEl.textContent = formatWon(data.monthlyIncome);
        detailExpenseEl.textContent = formatWon(data.monthlyLivingExpense);
        detailLoanPaymentEl.textContent = formatWon(data.monthlyLoanPayment);
        detailSavingsEl.textContent = formatWon(data.monthlySavingsCapacity);

        if (data.savingsCapacityInsufficient) {
            detailDateEl.textContent = '산정 불가';
            setDetailNote('월 생활비와 원리금상환액이 월 소득보다 많아 저축 여력이 부족합니다.');
            return;
        }
        if (data.monthsToGoal === 0) {
            detailDateEl.textContent = '이미 도달';
            setDetailNote('현재 순자산이 설정한 목표 금액에 도달했습니다.');
            return;
        }
        if (data.monthsToGoal === null || data.monthsToGoal === undefined) {
            detailDateEl.textContent = '장기 소요';
            setDetailNote('현재 현금흐름 기준으로 100년 이내 도달 시점을 산정하기 어렵습니다.');
            return;
        }
        const date = new Date();
        date.setMonth(date.getMonth() + Number(data.monthsToGoal));
        detailDateEl.textContent = `${date.getFullYear()}년 ${date.getMonth() + 1}월`;
        setDetailNote('');
    }

    function updateSelectedRowPreview(data) {
        const selected = presetCardEls.find((row) => row.classList.contains('selected'));
        if (!selected) {
            return;
        }
        const duration = selected.querySelector('.fp-goal-duration');
        const status = selected.querySelector('.fp-goal-status');
        if (!duration || !status) {
            return;
        }
        if (data.savingsCapacityInsufficient) {
            duration.textContent = '산정 불가';
            status.textContent = '저축 여력 부족';
            status.dataset.status = 'insufficient';
        } else if (data.monthsToGoal === null) {
            duration.textContent = '산정 불가';
            status.textContent = '도달 어려움';
            status.dataset.status = 'very-long';
        } else {
            const split = formatMonthsSplit(data.monthsToGoal);
            duration.textContent = `${split.magnitude} ${split.unit}`.trim();
            status.textContent = data.monthsToGoal > 120 ? '10년 초과' : '10년 이내';
            status.dataset.status = data.monthsToGoal > 120 ? 'long' : 'normal';
        }
    }

    function populateRowPreview(rowEl, amount) {
        fetch(`/api/future-goal/preview?goalAmount=${encodeURIComponent(amount)}`)
            .then((res) => res.ok ? res.json() : Promise.reject())
            .then((data) => {
                const duration = rowEl.querySelector('.fp-goal-duration');
                const status = rowEl.querySelector('.fp-goal-status');
                if (data.savingsCapacityInsufficient) {
                    duration.textContent = '산정 불가';
                    status.textContent = '저축 여력 부족';
                    status.dataset.status = 'insufficient';
                } else if (data.monthsToGoal === null) {
                    duration.textContent = '산정 불가';
                    status.textContent = '도달 어려움';
                    status.dataset.status = 'very-long';
                } else {
                    const split = formatMonthsSplit(data.monthsToGoal);
                    duration.textContent = `${split.magnitude} ${split.unit}`.trim();
                    status.textContent = data.monthsToGoal > 120 ? '10년 초과' : '10년 이내';
                    status.dataset.status = data.monthsToGoal > 120 ? 'long' : 'normal';
                }
            })
            .catch(() => {});
    }

    // ---------- 프리셋 비교표 ----------
    function renderPresetCards(presets) {
        cardsEl.innerHTML = '';
        presetCardEls = presets.map((preset) => {
            const meta = PRESET_META[preset.key] || {icon: 'savings'};
            const rowEl = document.createElement('tr');
            rowEl.className = 'fp-goal-row';
            rowEl.setAttribute('aria-selected', 'false');
            rowEl.dataset.amount = preset.amount;
            rowEl.dataset.presetKey = preset.key;
            rowEl.innerHTML = `
                <td><div class="fp-goal-name-cell"><span class="material-symbols-outlined fp-goal-icon" aria-hidden="true">${meta.icon}</span><span><strong>${preset.label}</strong>${preset.key === 'EMERGENCY_FUND' ? `<small>${EMERGENCY_FUND_CAPTION}</small>` : ''}</span></div></td>
                <td><label class="fp-amount-input"><input type="text" inputmode="numeric" aria-label="${preset.label} 목표 금액" value="${new Intl.NumberFormat('ko-KR').format(preset.amount)}"><span>원</span></label></td>
                <td class="fp-goal-duration">선택 시 계산</td>
                <td><span class="fp-goal-status" data-status="normal">계산 중</span></td>
                <td><input class="fp-goal-choice" type="radio" name="goalPresetChoice" aria-label="${preset.label} 선택"></td>
            `;
            const amountInput = rowEl.querySelector('.fp-amount-input input');
            const choose = () => {
                activePresetRow = rowEl;
                setGoalAmount(Number(rowEl.dataset.amount));
            };
            rowEl.querySelector('.fp-goal-choice').addEventListener('change', choose);
            rowEl.addEventListener('click', (event) => {
                if (event.target !== amountInput) {
                    rowEl.querySelector('.fp-goal-choice').checked = true;
                    choose();
                }
            });
            amountInput.addEventListener('input', () => {
                const digits = amountInput.value.replace(/[^0-9]/g, '');
                amountInput.value = digits ? new Intl.NumberFormat('ko-KR').format(Number(digits)) : '';
                if (!digits || Number(digits) <= 0) {
                    return;
                }
                rowEl.dataset.amount = digits;
                activePresetRow = rowEl;
                rowEl.querySelector('.fp-goal-choice').checked = true;
                setGoalAmount(Number(digits));
                populateRowPreview(rowEl, digits);
            });
            amountInput.addEventListener('focus', () => rowEl.querySelector('.fp-goal-choice').checked = true);
            cardsEl.appendChild(rowEl);
            populateRowPreview(rowEl, preset.amount);
            return rowEl;
        });
    }

    renderQuickPicks();

    fetch('/api/future-goal/age-cohort-spectrum')
        .then((res) => {
            if (res.status === 204) {
                return null;
            }
            if (!res.ok) {
                throw new Error('age-cohort-spectrum 조회 실패: ' + res.status);
            }
            return res.json();
        })
        .then((data) => {
            if (data) {
                renderAgeSpectrumStatic(data);
            }
        })
        .catch(() => {
            // 보조 시각화라 실패해도 화면은 그대로 둔다(카드만 숨김 유지)
        });

    fetch('/api/future-goal/household-cohort-spectrum')
        .then((res) => {
            if (res.status === 204) {
                return null;
            }
            if (!res.ok) {
                throw new Error('household-cohort-spectrum 조회 실패: ' + res.status);
            }
            return res.json();
        })
        .then((data) => {
            if (data) {
                renderHouseholdSpectrumStatic(data);
            }
        })
        .catch(() => {
            // 보조 시각화라 실패해도 화면은 그대로 둔다(카드만 숨김 유지)
        });

    fetch('/api/future-goal/presets')
        .then((res) => {
            if (!res.ok) {
                throw new Error('presets 조회 실패: ' + res.status);
            }
            return res.json();
        })
        .then((presets) => {
            renderPresetCards(presets);
            resetGoalAmount();
        })
        .catch(() => {
            cardsEl.innerHTML = '<p class="fcs-panel-sub">추천 목표를 불러오지 못했어요. 아래에 직접 입력해보세요.</p>';
            resetGoalAmount();
        });
})();
