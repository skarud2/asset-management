(function () {
    const goalAmountInputEl = document.getElementById('comboGoalAmount');
    const baselineMagnitudeEl = document.getElementById('comboBaselineMagnitude');
    const baselineUnitEl = document.getElementById('comboBaselineUnit');
    const comboHeroGoalEl = document.getElementById('comboHeroGoal');
    const comboHeroMessageEl = document.getElementById('comboHeroMessage');
    const leverLoadingEl = document.getElementById('comboLeverLoading');
    const leverListEl = document.getElementById('comboLeverList');
    const goalLineEl = document.getElementById('comboGoalLine');
    const baselineCurveEl = document.getElementById('comboBaselineCurve');
    const resultCurveEl = document.getElementById('comboResultCurve');
    const resultLegendItemEl = document.getElementById('comboResultLegendItem');
    const summaryTextEl = document.getElementById('comboSummaryText');
    const contributionAreaEl = document.getElementById('comboContributionArea');
    const returnAreaEl = document.getElementById('comboReturnArea');
    const benchmarkLineEl = document.getElementById('comboBenchmarkLine');
    const benchmarkMarkerEl = document.getElementById('comboBenchmarkMarker');
    const benchmarkLegendItemEl = document.getElementById('comboBenchmarkLegendItem');
    const goalMarkerEl = document.getElementById('comboGoalMarker');
    const scrubberLineEl = document.getElementById('comboScrubberLine');
    const scrubberDotEl = document.getElementById('comboScrubberDot');
    const scrubberInputEl = document.getElementById('comboScrubberInput');
    const scrubberLabelEl = document.getElementById('comboScrubberLabel');
    const scrubberBaselineNetWorthEl = document.getElementById('comboScrubberBaselineNetWorth');
    const scrubberResultNetWorthEl = document.getElementById('comboScrubberResultNetWorth');
    const scrubberBaselineContributionEl = document.getElementById('comboScrubberBaselineContribution');
    const scrubberResultContributionEl = document.getElementById('comboScrubberResultContribution');
    const scrubberBaselineReturnEl = document.getElementById('comboScrubberBaselineReturn');
    const scrubberResultReturnEl = document.getElementById('comboScrubberResultReturn');
    const milestonesEl = document.getElementById('comboMilestones');
    const saveButtonEl = document.getElementById('comboSaveButton');
    const saveNoteEl = document.getElementById('comboSaveNote');
    const planNameInputEl = document.getElementById('comboPlanName');
    const loadedPlanLeversEl = document.getElementById('loadedPlanLevers');
    const planSummaryGoalEl = document.getElementById('planSummaryGoal');
    const planSummaryCurrentNetWorthEl = document.getElementById('planSummaryCurrentNetWorth');
    const planSummaryMonthsEl = document.getElementById('planSummaryMonths');
    const planSummaryDiffEl = document.getElementById('planSummaryDiff');
    const planSummaryLeversEl = document.getElementById('planSummaryLevers');
    const planBaselineMonthsEl = document.getElementById('planBaselineMonths');
    const planProjectedMonthsEl = document.getElementById('planProjectedMonths');
    const planCompareBadgeEl = document.getElementById('planCompareBadge');
    const planRetirementNoteEl = document.getElementById('planRetirementNote');
    const planActionGuideEl = document.getElementById('planActionGuide');
    const planLoanImpactEl = document.getElementById('planLoanImpact');
    const planLoanImpactTextEl = document.getElementById('planLoanImpactText');
    const editToggleEl = document.getElementById('comboEditToggle');
    const editSectionEl = document.getElementById('comboEditSection');
    const csrfTokenEl = document.getElementById('csrfTokenValue');
    const csrfHeaderEl = document.getElementById('csrfHeaderName');
    if (!goalAmountInputEl || !leverListEl) {
        return;
    }

    const goalAmount = Number(goalAmountInputEl.value || 100000000);
    // "가능한 범위" 개념은 화면에 안 보여주기로 했지만(4단계와 동일), 추천 조합 계산 자체는
    // 이 값들을 그대로 써서 /recommended-combo에 넘긴다 — loadRecommendedCombo() 참고.
    const monthlyExtraCapacity = localStorage.getItem('futuresimmonthlyExtraCapacity') || '';
    const targetMonthlyLoanPayment = localStorage.getItem('futuresimtargetMonthlyLoanPayment') || '';
    const csrfHeaderName = csrfHeaderEl.value;
    const csrfTokenValue = csrfTokenEl.value;

    const CHART_WIDTH = 600;
    const CHART_HEIGHT = 220;
    const PAD_X = 8;
    const PAD_TOP = 16;
    const PAD_BOTTOM = 24;

    // 4단계와 같은 아이콘/이름 — 강도 단위 포맷만 이 화면에 맞게 재구성.
    const LEVER_META = {
        INCOME_CHANGE: {icon: 'trending_up', name: '월 추가 확보', unit: '원', formatIntensity: (v) => formatWon(v)},
        LOAN_PREPAYMENT: {icon: 'payments', name: '대출 조기상환', unit: '원', formatIntensity: (v) => formatWon(v)},
        LOAN_TERM_EXTENSION: {icon: 'event_repeat', name: '만기 연장', unit: '개월', formatIntensity: (v) => formatDuration(v)},
        NEW_LOAN: {icon: 'add_card', name: '신규 대출 실행', unit: '원', formatIntensity: (v) => formatWon(v)}
    };

    // 4단계 futuresim-levers.js의 LOAN_TYPE_LABELS와 같은 맵 — 대출 종류 코드 표기 관례를 맞춘다.
    const LOAN_TYPE_LABELS = {
        MORTGAGE_LOAN: '주택담보대출',
        CREDIT_LOAN: '신용대출',
        JEONSE_LOAN: '전세자금대출',
        STUDENT_LOAN: '학자금대출'
    };

    function formatWon(amount) {
        return `${new Intl.NumberFormat('ko-KR').format(Math.round(Number(amount) || 0))}원`;
    }

    // 4단계 futuresim-levers.js의 formatDuration()과 같은 규칙(N년 M개월).
    function formatDuration(months) {
        const abs = Math.round(Math.abs(Number(months)));
        const years = Math.floor(abs / 12);
        const rest = abs % 12;
        if (years > 0) {
            return rest > 0 ? `${years}년 ${rest}개월` : `${years}년`;
        }
        return `${rest}개월`;
    }

    function formatMonthsSplit(months) {
        if (months === null || months === undefined) {
            return {magnitude: '예측 어려움', unit: ''};
        }
        const years = Math.floor(months / 12);
        const rest = months % 12;
        if (years > 0) {
            return {magnitude: `${years}년`, unit: rest > 0 ? `${rest}개월` : ''};
        }
        return {magnitude: `${rest}개월`, unit: ''};
    }

    // 3단계 growth.js의 monthLabel()과 같은 규칙 — 마일스톤/스크러버 라벨에 쓴다.
    function monthLabel(monthOffset) {
        if (monthOffset === 0) {
            return '지금';
        }
        const {magnitude, unit} = formatMonthsSplit(monthOffset);
        return `${magnitude} ${unit}`.trim() + ' 후';
    }

    function diffPhrase(diffMonths) {
        if (diffMonths === null || diffMonths === undefined) {
            return '';
        }
        if (diffMonths === 0) {
            return ' (기존과 같아요)';
        }
        return diffMonths > 0
            ? ` (기존보다 ${formatDuration(diffMonths)} 단축)`
            : ` (기존보다 ${formatDuration(diffMonths)} 지연)`;
    }

    const selections = new Map(); // leverType -> intensity(number)
    let baselineTimeline = null;
    let leverImpactData = null;
    // /projection이 이미 벤치마크(연령별/가구원별 중앙값)를 계산해서 내려주므로, 3단계처럼 별도
    // API 호출 없이 이 값을 그대로 재사용한다 — 1단계에서 비교 기준을 안 골랐으면 둘 다 null.
    let benchmarkNetWorth = null;
    let benchmarkLabel = null;
    // 타임라인 탐색기(슬라이더) — 칩을 바꿀 때마다 다시 그려지지만, 사용자가 보던 시점은 유지한다.
    let scrubberInitialized = false;
    let currentBaselinePoints = null;
    let currentResultPoints = null;

    // ---------- 초기 로드: 기준선(3단계 projection 재사용) + 레버 목록(4단계 lever-impact 재사용) ----------
    Promise.all([
        fetch(`/api/future-simulation/projection?goalAmount=${encodeURIComponent(goalAmount)}`)
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status))),
        fetch(`/api/future-simulation/lever-impact?goalAmount=${encodeURIComponent(goalAmount)}`)
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
    ])
        .then(([projection, leverImpact]) => {
            const {magnitude, unit} = formatMonthsSplit(projection.monthsToGoal);
            baselineMagnitudeEl.textContent = magnitude;
            baselineUnitEl.textContent = unit;
            baselineTimeline = projection.timeline;
            benchmarkNetWorth = projection.benchmarkNetWorth ?? null;
            benchmarkLabel = projection.benchmarkLabel ?? null;
            comboHeroGoalEl.textContent = formatWon(goalAmount);
            leverImpactData = leverImpact;
            planSummaryGoalEl.textContent = formatWon(goalAmount);
            planSummaryCurrentNetWorthEl.textContent = formatWon(projection.timeline[0].netWorth);
            updatePlanSummary(projection.monthsToGoal, 0, projection.monthsToGoal);
            renderLeverList(leverImpact);
            drawChart(baselineTimeline, null, projection.monthsToGoal, null);

            // 목록에서 "불러오기"로 들어온 경우, FutureSimViewController가 loadedPlanLevers 히든
            // 인풋에 저장된 계획의 레버 선택을 실어준다 — 이땐 추천 조합 대신 그 값으로 시작한다.
            const loadedLeversRaw = loadedPlanLeversEl ? loadedPlanLeversEl.value : '';
            if (loadedLeversRaw) {
                try {
                    JSON.parse(loadedLeversRaw).forEach((item) => selections.set(item.leverType, Number(item.intensity)));
                } catch (e) {
                    // 파싱 실패하면 조용히 무시하고 추천 조합으로 폴백
                }
            }
            if (selections.size > 0) {
                syncSelectionControls();
                runSimulation();
            } else {
                loadRecommendedCombo();
            }
        })
        .catch(() => {
            baselineMagnitudeEl.textContent = '불러오지 못했어요';
            leverLoadingEl.textContent = '불러오지 못했어요';
        });

    editToggleEl.addEventListener('click', () => {
        const expanded = editToggleEl.getAttribute('aria-expanded') === 'true';
        editToggleEl.setAttribute('aria-expanded', String(!expanded));
        editToggleEl.textContent = expanded ? '계획 수정하기' : '수정 완료';
        editSectionEl.classList.toggle('hidden', expanded);
    });

    function loadRecommendedCombo() {
        fetch(`/api/future-simulation/recommended-combo?goalAmount=${encodeURIComponent(goalAmount)}&monthlyExtraCapacity=${encodeURIComponent(monthlyExtraCapacity)}&targetMonthlyLoanPayment=${encodeURIComponent(targetMonthlyLoanPayment)}`)
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
            .then((data) => {
                (data.levers || []).forEach((lever) => selections.set(lever.type, Number(lever.intensity)));
                syncSelectionControls();
                runSimulation();
            })
            .catch(() => updatePlanSummary(null, 0));
    }

    function syncSelectionControls() {
        leverListEl.querySelectorAll('.fp-combo-lever-item').forEach((item) => {
            const type = item.dataset.lever;
            const selected = selections.get(type);
            const checkbox = item.querySelector('.fp-combo-lever-checkbox');
            const panel = item.querySelector('.fp-combo-lever-panel');
            if (selected === undefined) return;
            checkbox.checked = true;
            panel.classList.add('expanded');
            item.querySelectorAll('.fp-lever-chip').forEach((chip) =>
                chip.classList.toggle('active', Number(chip.dataset.intensity) === selected));
        });
    }

    function renderLeverList(data) {
        leverLoadingEl.classList.add('hidden');
        const available = data.levers.filter((item) => item.available);
        if (available.length === 0) {
            leverLoadingEl.textContent = '적용할 수 있는 방법이 없어요';
            leverLoadingEl.classList.remove('hidden');
            return;
        }
        available.forEach((item) => leverListEl.appendChild(buildLeverItem(item)));
        leverListEl.classList.remove('hidden');
    }

    // 체크박스 하나 — 체크하면 바로 아래에 4단계와 같은 칩+직접입력 패널이 펼쳐진다(슬라이더 없음).
    function buildLeverItem(item) {
        const meta = LEVER_META[item.leverType];
        const presets = item.presets && item.presets.length > 0
            ? item.presets
            : [{intensity: item.defaultIntensity}];
        const minIntensity = Number(presets[0].intensity);
        const maxIntensity = Number(presets[presets.length - 1].intensity);

        const li = document.createElement('li');
        li.className = 'fp-combo-lever-item';
        li.dataset.lever = item.leverType;

        const panel = document.createElement('div');
        panel.className = 'fp-combo-lever-panel';

        const panelBody = document.createElement('div');
        panelBody.className = 'fp-combo-lever-panel-body';

        const chipsEl = document.createElement('div');
        chipsEl.className = 'fp-lever-card-chips';

        function setActiveChip(intensity) {
            chipsEl.querySelectorAll('.fp-lever-chip').forEach((chip) => {
                chip.classList.toggle('active', Number(chip.dataset.intensity) === Number(intensity));
            });
            const anyChipActive = !!chipsEl.querySelector('.fp-lever-chip.active');
            customWrapEl.classList.toggle('active', !anyChipActive);
        }

        presets.forEach((preset) => {
            const chip = document.createElement('button');
            chip.type = 'button';
            chip.className = 'fp-lever-chip';
            chip.dataset.intensity = preset.intensity;
            chip.textContent = meta.formatIntensity(preset.intensity);
            chip.addEventListener('click', () => {
                customInputEl.value = '';
                setActiveChip(preset.intensity);
                selections.set(item.leverType, Number(preset.intensity));
                runSimulation();
            });
            chipsEl.appendChild(chip);
        });

        const customWrapEl = document.createElement('div');
        customWrapEl.className = 'fp-lever-chip-custom';
        customWrapEl.innerHTML = `
            <input type="text" inputmode="numeric" class="fp-lever-chip-custom-input" placeholder="직접 입력">
            <span class="fp-lever-chip-custom-unit">${meta.unit}</span>
        `;
        const customInputEl = customWrapEl.querySelector('.fp-lever-chip-custom-input');

        function submitCustom() {
            const rawDigits = customInputEl.value.replace(/[^0-9]/g, '');
            if (rawDigits === '') {
                return;
            }
            const requested = Math.min(maxIntensity, Math.max(minIntensity, Number(rawDigits)));
            customInputEl.value = String(requested);
            setActiveChip(NaN);
            selections.set(item.leverType, requested);
            runSimulation();
        }

        customInputEl.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                submitCustom();
            }
        });
        customInputEl.addEventListener('blur', submitCustom);

        chipsEl.appendChild(customWrapEl);
        panelBody.appendChild(chipsEl);
        panel.appendChild(panelBody);

        const row = document.createElement('label');
        row.className = 'fp-combo-lever-checkbox-row';
        row.innerHTML = `
            <input type="checkbox" class="fp-combo-lever-checkbox">
            <span class="material-symbols-outlined fp-combo-lever-icon" aria-hidden="true">${meta.icon}</span>
            <span class="fp-combo-lever-name">${meta.name}</span>
        `;
        const checkbox = row.querySelector('.fp-combo-lever-checkbox');
        checkbox.addEventListener('change', () => {
            if (checkbox.checked) {
                const defaultIntensity = Number(item.defaultIntensity);
                selections.set(item.leverType, defaultIntensity);
                setActiveChip(defaultIntensity);
                panel.classList.add('expanded');
            } else {
                selections.delete(item.leverType);
                panel.classList.remove('expanded');
            }
            runSimulation();
        });

        li.appendChild(row);
        li.appendChild(panel);
        return li;
    }

    // ---------- 조합 시뮬레이션 ----------
    function runSimulation() {
        const levers = Array.from(selections.entries()).map(([type, intensity]) => ({type, intensity}));
        fetch('/api/future-simulation/combo-simulation', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeaderName]: csrfTokenValue
            },
            body: JSON.stringify({goalAmount, levers})
        })
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
            .then(renderComboResult)
            .catch(() => {
                summaryTextEl.textContent = '계산에 실패했어요. 잠시 후 다시 시도해주세요.';
            });
    }

    function renderComboResult(data) {
        if (selections.size === 0) {
            resultCurveEl.classList.add('hidden');
            resultLegendItemEl.classList.add('hidden');
            summaryTextEl.classList.remove('hidden');
            summaryTextEl.textContent = '아직 고른 방법이 없어요 — 위에서 방법을 체크해보세요';
            drawChart(baselineTimeline, null, data.baselineMonthsToGoal, null);
            updatePlanSummary(data.comboMonthsToGoal, 0, data.baselineMonthsToGoal);
            return;
        }

        summaryTextEl.classList.add('hidden');
        drawChart(baselineTimeline, data.timeline, data.comboMonthsToGoal, data.diffMonths);
        resultCurveEl.classList.remove('hidden');
        resultLegendItemEl.classList.remove('hidden');

        comboHeroMessageEl.textContent = data.diffMonths > 0
            ? `실행 계획으로 ${formatDuration(data.diffMonths)} 앞당길 수 있어요.`
            : '현재 페이스를 기준으로 계산했어요.';
        updatePlanSummary(data.comboMonthsToGoal, data.diffMonths, data.baselineMonthsToGoal);
    }

    // 레버별 개별 효과(diffMonths) — /lever-impact 응답에 이미 있는 값을 그대로 쓴다(별도 호출 없음).
    // 선택한 강도가 기본 강도와 같으면 lever.diffMonths, 프리셋 칩과 같으면 그 프리셋의 diffMonths,
    // 직접 입력한 값처럼 둘 다 아니면 알 수 없으니 표시하지 않는다.
    function individualDiffFor(type, intensity) {
        if (!leverImpactData) return null;
        const lever = leverImpactData.levers.find((item) => item.leverType === type);
        if (!lever) return null;
        if (Number(lever.defaultIntensity) === Number(intensity)) return lever.diffMonths;
        const preset = (lever.presets || []).find((item) => Number(item.intensity) === Number(intensity));
        return preset ? preset.diffMonths : null;
    }

    function individualEffectText(diffMonths) {
        if (diffMonths === null || diffMonths === undefined) return '';
        if (diffMonths === 0) return '변화 없음';
        return diffMonths > 0 ? `${formatDuration(diffMonths)} 단축` : `${formatDuration(diffMonths)} 지연`;
    }

    // "선택한 방법"을 4단계 레버 카드처럼 아이콘+항목별 행으로 분리해서 보여준다(가운뎃점으로
    // 이어붙인 한 줄 텍스트 대신).
    function renderPlanSummaryLevers() {
        const entries = Array.from(selections.entries());
        if (entries.length === 0) {
            planSummaryLeversEl.innerHTML = '<li class="fp-plan-summary-lever-empty">기존 페이스 유지</li>';
            return;
        }
        planSummaryLeversEl.innerHTML = entries.map(([type, intensity]) => {
            const meta = LEVER_META[type];
            const effectText = individualEffectText(individualDiffFor(type, intensity));
            return '<li class="fp-plan-summary-lever-item">' +
                `<span class="material-symbols-outlined fp-plan-summary-lever-icon" aria-hidden="true">${meta.icon}</span>` +
                `<span class="fp-plan-summary-lever-name">${meta.name}</span>` +
                `<span class="fp-plan-summary-lever-value">${meta.formatIntensity(intensity)}</span>` +
                (effectText ? `<span class="fp-plan-summary-lever-effect">${effectText}</span>` : '') +
                '</li>';
        }).join('');
    }

    // 카드에서 제일 먼저 눈에 띄어야 하는 건 개별 전/후 숫자가 아니라 이 배지 — 단축이면 밝은
    // 강조색(흰 배경 + 네이비 텍스트), 지연이면 경고색(--fp-danger 계열)으로 분기한다.
    function updateCompareBadge(diffMonths) {
        let text;
        if (diffMonths === null || diffMonths === undefined) {
            text = '비교 불가';
        } else if (diffMonths === 0) {
            text = '변화 없음';
        } else if (diffMonths > 0) {
            text = `-${formatDuration(diffMonths)} 단축`;
        } else {
            text = `+${formatDuration(Math.abs(diffMonths))} 지연`;
        }
        planCompareBadgeEl.textContent = text;
        planCompareBadgeEl.classList.toggle('fp-plan-compare-badge-warning', Number(diffMonths) < 0);
    }

    function updatePlanSummary(monthsToGoal, diffMonths, baselineMonths) {
        const {magnitude, unit} = formatMonthsSplit(monthsToGoal);
        planSummaryMonthsEl.textContent = `${magnitude} ${unit}`.trim();
        planSummaryDiffEl.textContent = diffMonths > 0 ? `${formatDuration(diffMonths)} 단축` : '선택한 방법 없음';
        renderPlanSummaryLevers();
        const baseline = formatMonthsSplit(baselineMonths);
        planBaselineMonthsEl.textContent = `${baseline.magnitude} ${baseline.unit}`.trim();
        planProjectedMonthsEl.textContent = `${magnitude} ${unit}`.trim();
        updateCompareBadge(diffMonths);
        planRetirementNoteEl.textContent = monthsToGoal === null
            ? '만 60세 정년 전에는 목표 도달이 어려워요. 목표·저축액·실행 방법을 다시 조정해보세요.'
            : '만 60세 정년 전 도달 기준으로 계산한 예상 시점이에요.';
        planActionGuideEl.textContent = diffMonths > 0
            ? `현재 페이스보다 ${formatDuration(diffMonths)} 앞당기는 계획이에요. 선택한 방법의 강도는 ‘계획 수정하기’에서 바꿀 수 있어요.`
            : '현재 조건에서는 선택한 방법이 목표 시점을 앞당기지 못해요. 계획 수정하기에서 강도를 조정해보세요.';
        renderLoanImpact();
    }

    function renderLoanImpact() {
        const levers = Array.from(selections.entries()).map(([type, intensity]) => ({type, intensity}));
        fetch('/api/future-simulation/loan-impact', {method: 'POST', headers: {'Content-Type': 'application/json', [csrfHeaderName]: csrfTokenValue}, body: JSON.stringify({goalAmount, levers})})
            .then((res) => res.ok ? res.json() : [])
            .then(renderCombinedLoanImpact)
            .catch(() => renderCombinedLoanImpact([]));
    }

    // 대출 종류 라벨/상환기간 변화/중도상환수수료/만기연장 총이자 증가 설명까지 — /loan-impact 응답이
    // 이제 loanType·beforeRemainingMonths·afterRemainingMonths·prepaymentFeeRate·prepaymentFeeEndDate를
    // 같이 내려준다(LeverLoanComparisonService.combinedImpact 참고). 대출이 여러 개 영향을 받으면
    // impacts 배열이 그만큼 늘어나서 카드가 세로로 나열된다(.fp-plan-loan-impact-list가 grid+gap).
    function renderCombinedLoanImpact(impacts) {
        if (!impacts || impacts.length === 0) {
            planLoanImpactEl.classList.add('hidden');
            return;
        }
        const includesTermExtension = selections.has('LOAN_TERM_EXTENSION');
        const cards = impacts.map((impact) => {
            const diff = Number(impact.beforeMonthlyPayment) - Number(impact.afterMonthlyPayment);
            const interest = Number(impact.totalInterestDiff);
            const loanLabel = LOAN_TYPE_LABELS[impact.loanType] || impact.loanType || '대출 상환 계획';
            const periodChanged = Number(impact.beforeRemainingMonths) !== Number(impact.afterRemainingMonths);
            const feeRate = impact.prepaymentFeeRate;
            const hasFee = feeRate !== null && feeRate !== undefined && Number(feeRate) > 0;

            const badges = [
                interest > 0 ? `<span class="fp-plan-impact-badge warning">총이자 +${formatWon(interest)}</span>` : '',
                hasFee ? `<span class="fp-plan-impact-badge fee">중도상환수수료 ${Number(feeRate).toFixed(2)}%${impact.prepaymentFeeEndDate ? ` · ${impact.prepaymentFeeEndDate}까지` : ''}</span>` : ''
            ].filter(Boolean).join('');

            return '<article class="fp-plan-loan-impact-card">' +
                `<div class="fp-plan-loan-impact-head"><span class="material-symbols-outlined">payments</span><strong>${loanLabel}</strong><span class="fp-plan-impact-badge">월 ${formatWon(diff)} 절감</span></div>` +
                '<div class="fp-plan-loan-impact-values">' +
                    `<div><span>기존</span><b>${formatWon(impact.beforeMonthlyPayment)}</b></div>` +
                    '<span class="material-symbols-outlined">arrow_forward</span>' +
                    `<div><span>계획 적용 후</span><b>${formatWon(impact.afterMonthlyPayment)}</b></div>` +
                '</div>' +
                (periodChanged ? `<p class="fp-plan-loan-impact-period">상환 기간 ${impact.beforeRemainingMonths}개월 → ${impact.afterRemainingMonths}개월</p>` : '') +
                (badges ? `<div class="fp-plan-loan-impact-badges">${badges}</div>` : '') +
                (interest > 0 && includesTermExtension
                    ? '<div class="fp-plan-loan-fee-note"><span class="material-symbols-outlined">info</span><span>상환기간이 늘어난 만큼 총이자도 늘어나요</span></div>'
                    : '') +
                '</article>';
        });
        planLoanImpactTextEl.innerHTML = cards.join('');
        planLoanImpactEl.classList.remove('hidden');
    }

    // ---------- 차트: 기존 페이스(옅은 회색 참조선) + 레버 적용(원금/투자수익 스택 + 남색 곡선) 오버레이.
    // 3단계 growth.js의 renderChart()와 같은 좌표계/밴드 계산을 재사용하되, 레버 적용 곡선만 스택으로
    // 쌓는다(둘 다 쌓으면 비교가 안 읽힘). goalMonths/diffMonths는 마일스톤 문구에 쓴다. ----------
    function drawChart(baseline, result, goalMonths, diffMonths) {
        const values = baseline.map((p) => Number(p.netWorth));
        let maxMonth = baseline[baseline.length - 1].monthOffset;
        if (result) {
            values.push(...result.map((p) => Number(p.netWorth)));
            maxMonth = Math.max(maxMonth, result[result.length - 1].monthOffset);
        }
        let maxValue = Math.max(...values, Number(goalAmount));
        if (benchmarkNetWorth !== null) {
            maxValue = Math.max(maxValue, Number(benchmarkNetWorth));
        }
        maxValue *= 1.08;
        const minValue = Math.min(...values, 0);

        const innerWidth = CHART_WIDTH - PAD_X * 2;
        const innerHeight = CHART_HEIGHT - PAD_TOP - PAD_BOTTOM;

        function xFor(month) {
            return maxMonth === 0 ? PAD_X : PAD_X + (month / maxMonth) * innerWidth;
        }

        function yFor(value) {
            const span = maxValue - minValue || 1;
            const ratio = (value - minValue) / span;
            return PAD_TOP + innerHeight - ratio * innerHeight;
        }

        baselineCurveEl.setAttribute('d', pathFor(baseline, xFor, yFor));
        const baselinePoints = baseline.map((p) => ({
            x: xFor(p.monthOffset), y: yFor(Number(p.netWorth)), month: p.monthOffset, value: Number(p.netWorth),
            contribution: Number(p.contributionAmount), returnValue: Number(p.returnAmount)
        }));

        let resultPoints = null;
        if (result) {
            resultCurveEl.setAttribute('d', pathFor(result, xFor, yFor));
            resultPoints = result.map((p) => ({
                x: xFor(p.monthOffset), y: yFor(Number(p.netWorth)), month: p.monthOffset, value: Number(p.netWorth),
                contribution: Number(p.contributionAmount), returnValue: Number(p.returnAmount)
            }));

            const baselineY = yFor(Math.min(0, minValue));
            const contributionTop = resultPoints.map((p) => ({x: p.x, y: yFor(p.contribution)}));
            const netWorthTop = resultPoints.map((p) => ({x: p.x, y: p.y}));
            contributionAreaEl.setAttribute('d', bandPath(contributionTop, baselineY));
            returnAreaEl.setAttribute('d', bandPath(netWorthTop, contributionTop));
            contributionAreaEl.classList.remove('hidden');
            returnAreaEl.classList.remove('hidden');
        } else {
            contributionAreaEl.classList.add('hidden');
            returnAreaEl.classList.add('hidden');
        }

        const goalY = yFor(Number(goalAmount));
        goalLineEl.setAttribute('x1', String(PAD_X));
        goalLineEl.setAttribute('x2', String(CHART_WIDTH - PAD_X));
        goalLineEl.setAttribute('y1', String(goalY));
        goalLineEl.setAttribute('y2', String(goalY));

        const activePoints = resultPoints || baselinePoints;

        if (goalMonths !== null && goalMonths !== undefined) {
            const marker = activePoints.find((p) => p.month === goalMonths) || activePoints[activePoints.length - 1];
            goalMarkerEl.setAttribute('cx', String(marker.x));
            goalMarkerEl.setAttribute('cy', String(marker.y));
            goalMarkerEl.classList.remove('hidden');
        } else {
            goalMarkerEl.classList.add('hidden');
        }

        if (benchmarkNetWorth !== null) {
            const benchmarkY = yFor(Number(benchmarkNetWorth));
            benchmarkLineEl.setAttribute('x1', String(PAD_X));
            benchmarkLineEl.setAttribute('x2', String(CHART_WIDTH - PAD_X));
            benchmarkLineEl.setAttribute('y1', String(benchmarkY));
            benchmarkLineEl.setAttribute('y2', String(benchmarkY));
            benchmarkLineEl.classList.remove('hidden');
            benchmarkLegendItemEl.classList.remove('hidden');

            const crossMonth = findFirstCrossMonth(activePoints, Number(benchmarkNetWorth));
            if (crossMonth !== null) {
                const marker = activePoints.find((p) => p.month === crossMonth) || activePoints[0];
                benchmarkMarkerEl.setAttribute('cx', String(marker.x));
                benchmarkMarkerEl.setAttribute('cy', String(marker.y));
                benchmarkMarkerEl.classList.remove('hidden');
            } else {
                benchmarkMarkerEl.classList.add('hidden');
            }
        } else {
            benchmarkLineEl.classList.add('hidden');
            benchmarkMarkerEl.classList.add('hidden');
            benchmarkLegendItemEl.classList.add('hidden');
        }

        setupScrubber(baselinePoints, resultPoints);
        renderMilestones(activePoints, !!result, goalMonths, diffMonths);
    }

    function pathFor(points, xFor, yFor) {
        return points
            .map((p, i) => `${i === 0 ? 'M' : 'L'}${xFor(p.monthOffset)},${yFor(Number(p.netWorth))}`)
            .join(' ');
    }

    // 3단계 growth.js의 bandPath()와 동일 — 원금/투자수익 스택 영역(band) 모양 path를 만든다.
    function bandPath(topPoints, bottomYOrPoints) {
        let d = `M${topPoints[0].x},${topPoints[0].y}`;
        for (let i = 1; i < topPoints.length; i++) {
            d += ` L${topPoints[i].x},${topPoints[i].y}`;
        }
        if (typeof bottomYOrPoints === 'number') {
            const last = topPoints[topPoints.length - 1];
            const first = topPoints[0];
            d += ` L${last.x},${bottomYOrPoints} L${first.x},${bottomYOrPoints} Z`;
        } else {
            for (let i = bottomYOrPoints.length - 1; i >= 0; i--) {
                d += ` L${bottomYOrPoints[i].x},${bottomYOrPoints[i].y}`;
            }
            d += ' Z';
        }
        return d;
    }

    // 3단계 growth.js의 findFirstCrossMonth 서버 로직과 같은 규칙 — 이미 픽셀 좌표까지 계산해둔
    // points 배열(baselinePoints/resultPoints)로 클라이언트에서 바로 찾는다(추가 API 호출 없음).
    function findFirstCrossMonth(points, target) {
        const hit = points.find((p) => p.value >= target);
        return hit ? hit.month : null;
    }

    // ---------- 타임라인 탐색기(3단계와 같은 슬라이더) — 기존 페이스 vs 레버 적용을 같은 시점에서 비교 ----------
    // 칩을 바꿀 때마다 drawChart()가 다시 호출되므로, 슬라이더 자체(min/max/이벤트)는 최초 1회만
    // 초기화하고 이후에는 현재 위치를 유지한 채 값만 새 데이터로 갱신한다.
    function setupScrubber(baselinePoints, resultPoints) {
        currentBaselinePoints = baselinePoints;
        currentResultPoints = resultPoints;

        if (!scrubberInitialized) {
            scrubberInputEl.max = String(baselinePoints.length - 1);
            scrubberInputEl.value = String(Math.floor((baselinePoints.length - 1) / 2));
            scrubberInputEl.addEventListener('input', () => {
                updateScrubber(Number(scrubberInputEl.value), currentBaselinePoints, currentResultPoints);
            });
            scrubberInitialized = true;
        }
        updateScrubber(Number(scrubberInputEl.value), baselinePoints, resultPoints);
    }

    function updateScrubber(index, baselinePoints, resultPoints) {
        const activePoints = resultPoints || baselinePoints;
        const clampedIndex = Math.max(0, Math.min(activePoints.length - 1, index));
        const activePoint = activePoints[clampedIndex];
        const baselinePoint = baselinePoints[clampedIndex] || baselinePoints[baselinePoints.length - 1];
        const resultPoint = resultPoints ? (resultPoints[clampedIndex] || resultPoints[resultPoints.length - 1]) : null;

        scrubberLabelEl.textContent = monthLabel(activePoint.month);
        scrubberLineEl.setAttribute('x1', String(activePoint.x));
        scrubberLineEl.setAttribute('x2', String(activePoint.x));
        scrubberLineEl.setAttribute('y1', String(PAD_TOP));
        scrubberLineEl.setAttribute('y2', String(CHART_HEIGHT - PAD_BOTTOM));
        scrubberDotEl.setAttribute('cx', String(activePoint.x));
        scrubberDotEl.setAttribute('cy', String(activePoint.y));

        scrubberBaselineNetWorthEl.textContent = formatWon(baselinePoint.value);
        scrubberBaselineContributionEl.textContent = formatWon(baselinePoint.contribution);
        scrubberBaselineReturnEl.textContent = formatWon(baselinePoint.returnValue);

        scrubberResultNetWorthEl.textContent = resultPoint ? formatWon(resultPoint.value) : '-';
        scrubberResultContributionEl.textContent = resultPoint ? formatWon(resultPoint.contribution) : '-';
        scrubberResultReturnEl.textContent = resultPoint ? formatWon(resultPoint.returnValue) : '-';
    }

    // ---------- 마일스톤 리스트(3단계와 같은 점+라벨+텍스트 스타일) — "이 조합이면 목표까지 X (기존보다
    // Y 단축)" 문구를 여기로 옮겨서, 라벨(시점)과 텍스트(설명)로 나눠 보여준다 ----------
    function renderMilestones(activePoints, hasResult, goalMonths, diffMonths) {
        milestonesEl.innerHTML = '';

        const items = [
            {label: '지금', text: `순자산 ${formatWon(activePoints[0].value)}`, reached: true}
        ];

        if (benchmarkNetWorth !== null) {
            const crossMonth = findFirstCrossMonth(activePoints, Number(benchmarkNetWorth));
            if (crossMonth !== null) {
                const label = benchmarkLabel ? `${benchmarkLabel} ` : '';
                items.push({label: monthLabel(crossMonth), text: `${label}중앙값을 넘어서요`, reached: crossMonth === 0});
            }
        }

        if (goalMonths !== null && goalMonths !== undefined) {
            const text = hasResult ? `목표에 도달해요${diffPhrase(diffMonths)}` : '목표에 도달해요';
            items.push({label: monthLabel(goalMonths), text, reached: goalMonths === 0});
        } else {
            items.push({label: '도달 어려움', text: '이 페이스로는 목표 도달이 어려워요', reached: false});
        }

        items.forEach((item) => {
            const li = document.createElement('li');
            li.className = 'fp-milestone-item' + (item.reached ? ' reached' : '');
            li.innerHTML =
                `<span class="fp-milestone-dot"></span>` +
                `<span class="fp-milestone-label">${item.label}</span>` +
                `<span class="fp-milestone-text">${item.text}</span>`;
            milestonesEl.appendChild(li);
        });
    }

    // ---------- "다음" — 현재 계획 저장(upsert) ----------
    saveButtonEl.addEventListener('click', () => {
        const levers = Array.from(selections.entries()).map(([type, intensity]) => ({type, intensity}));
        saveButtonEl.disabled = true;
        fetch('/api/future-simulation/save-plan', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [csrfHeaderName]: csrfTokenValue
            },
            // assumedReturnRate는 일부러 안 보낸다 — 화면의 "실행 전후 비교"(/projection,
            // /combo-simulation)가 수익률 파라미터 없이 서버 기본값으로 계산되는 것과 항상 같은 결과가
            // 저장되게 하기 위해서다. 여기서 localStorage의 4단계 수익률 설정을 같이 보내면, 화면에
            // 보이는 숫자와 실제로 저장/목록에 뜨는 숫자가 서로 다른 수익률로 계산돼 어긋난다.
            body: JSON.stringify({planName: planNameInputEl.value, goalAmount, goalPresetKey: null, levers})
        })
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
            .then((data) => {
                // 이름을 비워뒀으면 서버가 날짜 기반 기본 이름을 붙여서 돌려준다 — 입력칸에 그대로
                // 반영해서, 이어서 또 저장하면 같은 이름으로 덮어써지게(새 행이 계속 안 생기게) 한다.
                planNameInputEl.value = data.planName || planNameInputEl.value;
                saveNoteEl.innerHTML = `<span class="material-symbols-outlined" aria-hidden="true">check_circle</span>"${data.planName}"(으)로 저장했어요.`;
                saveNoteEl.classList.remove('error');
                saveNoteEl.classList.remove('hidden');
                saveNoteEl.scrollIntoView({behavior: 'smooth', block: 'center'});
            })
            .catch(() => {
                saveNoteEl.innerHTML = '<span class="material-symbols-outlined" aria-hidden="true">error</span>저장에 실패했어요. 잠시 후 다시 시도해주세요.';
                saveNoteEl.classList.add('error');
                saveNoteEl.classList.remove('hidden');
                saveNoteEl.scrollIntoView({behavior: 'smooth', block: 'center'});
            })
            .finally(() => {
                saveButtonEl.disabled = false;
            });
    });
})();
