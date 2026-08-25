(function () {
    const goalAmountInputEl = document.getElementById('leverGoalAmount');
    const assumedReturnRateEl = document.getElementById('assumedReturnRate');
    const customAssumedReturnRateEl = document.getElementById('customAssumedReturnRate');
    const baselineMagnitudeEl = document.getElementById('leverBaselineMagnitude');
    const baselineUnitEl = document.getElementById('leverBaselineUnit');
    const rankingLoadingEl = document.getElementById('leverRankingLoading');
    const rankingListEl = document.getElementById('leverRankingList');
    const leverBestInsightEl = document.getElementById('leverBestInsight');
    const leverLoanComparisonEl = document.getElementById('leverLoanComparison');
    const leverLoanComparisonBodyEl = document.getElementById('leverLoanComparisonBody');
    const combinationCardEl = document.getElementById('leverCombinationCard');
    const combinationLoadingEl = document.getElementById('leverCombinationLoading');
    const combinationTitleEl = document.getElementById('leverCombinationTitle');
    const combinationItemsEl = document.getElementById('leverCombinationItems');
    const combinationEffectEl = document.getElementById('leverCombinationEffect');
    const combinationReasonEl = document.getElementById('leverCombinationReason');
    const debtPriorityCardEl = document.getElementById('debtPriorityCard');
    const debtPriorityListEl = document.getElementById('debtPriorityList');
    const rateRiskTabsEl = document.getElementById('rateRiskTabs');
    const rateRiskLoadingEl = document.getElementById('rateRiskLoading');
    const rateRiskEmptyEl = document.getElementById('rateRiskEmpty');
    const rateRiskContentEl = document.getElementById('rateRiskContent');
    const rateRiskLoanTypeEl = document.getElementById('rateRiskLoanType');
    const rateRiskCurrentRateEl = document.getElementById('rateRiskCurrentRate');
    const rateRiskCurrentPaymentEl = document.getElementById('rateRiskCurrentPayment');
    const rateRiskFinalPaymentEl = document.getElementById('rateRiskFinalPayment');
    const rateRiskInsightEl = document.getElementById('rateRiskInsight');
    const rateRiskPathBodyEl = document.getElementById('rateRiskPathBody');
    const recommendedComboPreviewEl = document.getElementById('recommendedComboPreview');
    const recommendedComboPreviewTextEl = document.getElementById('recommendedComboPreviewText');
    if (!goalAmountInputEl || !rankingListEl) {
        return;
    }

    const goalAmount = Number(goalAmountInputEl.value || 100000000);
    let assumedReturnRate = Number(localStorage.getItem('futuresimAssumedReturnRate') || 4);
    let currentLeverImpact = null;
    // "이 조합을 추천해요" 섹션이 카드에서 선택한 강도를 그대로 반영하도록 공유하는 state — 신규대출은
    // 이 조합에서 애초에 후보가 아니라서 넣지 않는다.
    const selectedIntensity = {INCOME_CHANGE: null, LOAN_PREPAYMENT: null, LOAN_TERM_EXTENSION: null};
    if (assumedReturnRateEl) {
        const preset = ['2.5', '4', '6'].includes(String(assumedReturnRate)) ? String(assumedReturnRate) : 'custom';
        assumedReturnRateEl.value = preset;
        customAssumedReturnRateEl.value = preset === 'custom' ? assumedReturnRate : '';
        customAssumedReturnRateEl.classList.toggle('hidden', preset !== 'custom');
        const updateRate = () => {
            const value = assumedReturnRateEl.value === 'custom' ? Number(customAssumedReturnRateEl.value) : Number(assumedReturnRateEl.value);
            if (!Number.isFinite(value) || value < 0 || value > 10) return;
            localStorage.setItem('futuresimAssumedReturnRate', String(value));
            window.location.reload();
        };
        assumedReturnRateEl.addEventListener('change', () => {
            customAssumedReturnRateEl.classList.toggle('hidden', assumedReturnRateEl.value !== 'custom');
            if (assumedReturnRateEl.value !== 'custom') updateRate();
        });
        customAssumedReturnRateEl.addEventListener('change', updateRate);
    }
    const returnRateQuery = `&assumedReturnRate=${encodeURIComponent(assumedReturnRate)}`;

    // loan/ratesimulation 쪽 JS(my-loans-widget.js 등)에서 쓰는 것과 같은 라벨 맵 — 대출 종류 코드는
    // 그쪽 도메인(dsr.dto.type.LoanType)이 정의하므로 표기 관례만 맞춘다.
    const LOAN_TYPE_LABELS = {
        MORTGAGE_LOAN: '주택담보대출',
        CREDIT_LOAN: '신용대출',
        JEONSE_LOAN: '전세자금대출',
        STUDENT_LOAN: '학자금대출'
    };

    // 레버 카드 하나하나의 문구를 만드는 곳 — 아이콘/이름은 3단계 미리보기 카드(growth.html)와 같은 아이콘을 재사용.
    // headline은 "지금 이만큼 하면" 문장, explanation은 왜/언제 유리한지 설명(ctx로 대출금리·가정수익률을 받는다).
    const LEVER_COPY = {
        INCOME_CHANGE: {
            icon: 'trending_up',
            name: '월 추가 확보',
            unitLabel: '원',
            formatIntensity: (v) => formatWon(v),
            headline: (v) => `매달 ${formatWon(v)}을 더 확보하면`,
            explanation: () =>
                '이직이나 인상 등으로 소득이 늘어나면, 늘어난 만큼 그대로 저축으로 돌리는 게 목표를 가장 빠르게 앞당기는 방법이에요.',
            detailText: () => null
        },
        LOAN_PREPAYMENT: {
            icon: 'payments',
            name: '대출 조기상환',
            unitLabel: '원',
            formatIntensity: (v) => formatWon(v),
            headline: (v) => `지금 ${formatWon(v)}을 조기상환하면`,
            explanation: (ctx) => {
                if (ctx.loanRatePercent === null || ctx.assumedReturnRatePercent === null) {
                    return '';
                }
                const loanRate = Number(ctx.loanRatePercent).toFixed(2);
                const returnRate = Number(ctx.assumedReturnRatePercent).toFixed(2);
                return Number(ctx.loanRatePercent) > Number(ctx.assumedReturnRatePercent)
                    ? `현재 대출금리(${loanRate}%)가 여유자금 예상 수익률(${returnRate}%)보다 높아요. 여유자금이 있다면 투자보다 조기상환이 유리해요. 목돈이 생기는 대로 실행하는 걸 추천해요.`
                    : `현재 대출금리(${loanRate}%)가 여유자금 예상 수익률(${returnRate}%)보다 낮아요. 조기상환을 서두르기보다는 여유자금을 그대로 굴리는 것도 방법이에요. 다만 매달 상환 부담을 줄이고 싶다면 고려해볼 수 있어요.`;
            },
            detailText: (preset) => {
                const d = preset.detail;
                if (!d || d.beforeMonthlyPayment === null || d.beforeMonthlyPayment === undefined) {
                    return null;
                }
                return `월 상환액 ${formatWon(d.beforeMonthlyPayment)} → ${formatWon(d.afterMonthlyPayment)}`;
            }
        },
        LOAN_TERM_EXTENSION: {
            icon: 'event_repeat',
            name: '만기 연장',
            unitLabel: '개월',
            formatIntensity: (v) => formatDuration(v),
            headline: (v) => `만기를 ${formatDuration(v)} 늘리면`,
            explanation: () =>
                '매달 갚아야 하는 돈이 줄어들어서 저축 여력이 늘어나요. 다만 갚는 기간이 길어지는 만큼 총 이자는 더 많아질 수 있다는 점은 참고하세요.',
            detailText: (preset) => {
                const d = preset.detail;
                if (!d || d.beforeMonthlyPayment === null || d.beforeMonthlyPayment === undefined) {
                    return null;
                }
                return `월 상환액 ${formatWon(d.beforeMonthlyPayment)} → ${formatWon(d.afterMonthlyPayment)} · 총이자 약 ${formatWon(d.extraTotalInterest)} 증가`;
            }
        },
        NEW_LOAN: {
            icon: 'add_card',
            name: '신규 대출 실행',
            unitLabel: '원',
            formatIntensity: (v) => formatWon(v),
            headline: (v) => `${formatWon(v)}을 신규 대출로 실행하면`,
            explanation: () =>
                '가정 금리(연 4.5%)가 여유자금 예상 수익률보다 높아서, 대출을 새로 받을수록 목표가 오히려 늦어져요. 꼭 필요한 경우가 아니라면 추천하지 않아요.',
            detailText: (preset) => {
                const d = preset.detail;
                if (!d || d.afterMonthlyPayment === null || d.afterMonthlyPayment === undefined) {
                    return null;
                }
                return `매달 ${formatWon(d.afterMonthlyPayment)}의 상환 부담이 새로 생겨요`;
            }
        }
    };

    // 12개월 이상이면 "N년 M개월"로 — "143개월"보다 "11년 11개월"이 훨씬 감이 온다.
    function formatDuration(months) {
        const abs = Math.round(Math.abs(months));
        const years = Math.floor(abs / 12);
        const rest = abs % 12;
        if (years > 0) {
            return rest > 0 ? `${years}년 ${rest}개월` : `${years}년`;
        }
        return `${rest}개월`;
    }

    function effectPhrase(diffMonths) {
        if (diffMonths === null || diffMonths === undefined) {
            return '목표 도달을 예측하기 어려워요';
        }
        if (diffMonths === 0) {
            return '변화가 없어요';
        }
        return diffMonths > 0 ? `${formatDuration(diffMonths)} 빨라져요` : `${formatDuration(diffMonths)} 늦어져요`;
    }

    function formatWon(amount) {
        return `${new Intl.NumberFormat('ko-KR').format(Math.round(Number(amount) || 0))}원`;
    }

    // 3단계 growth.js의 formatMonthsSplit()과 같은 규칙(N년 M개월).
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

    // ---------- 히어로 + 섹션1: 현재 페이스와 레버 카드는 같이 그려야 해서(설명 문구에 가정수익률이
    // 필요) 두 API를 함께 기다린다. ----------
    Promise.all([
        fetch(`/api/future-simulation/projection?goalAmount=${encodeURIComponent(goalAmount)}${returnRateQuery}`)
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status))),
        fetch(`/api/future-simulation/lever-impact?goalAmount=${encodeURIComponent(goalAmount)}${returnRateQuery}`)
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
    ])
        .then(([projection, leverImpact]) => {
            const {magnitude, unit} = formatMonthsSplit(projection.monthsToGoal);
            baselineMagnitudeEl.textContent = magnitude;
            baselineUnitEl.textContent = unit;
            currentLeverImpact = leverImpact;
            leverImpact.levers.forEach((item) => {
                if (item.leverType in selectedIntensity) selectedIntensity[item.leverType] = item.defaultIntensity;
            });
            renderLeverCards(leverImpact, projection.assumedReturnRatePercent);
            loadRecommendedCombo();
        })
        .catch(() => {
            baselineMagnitudeEl.textContent = '불러오지 못했어요';
            rankingLoadingEl.textContent = '불러오지 못했어요';
        });

    // ---------- 조합 추천: 위 카드에서 선택한 강도(월 추가확보/조기상환/만기연장)를 그대로 반영해서
    // 다시 계산한다 — 카드 값을 바꿀 때마다 loadRecommendedCombo()를 다시 호출한다(신규대출은 이 조합에서
    // 애초에 제외 대상이라 selectedIntensity에 넣지 않는다). comboToken은 레버 카드의 latestToken과 같은
    // 이유(응답 뒤섞임 방지)로 둔다.
    let comboToken = 0;

    function loadRecommendedCombo() {
        const token = ++comboToken;
        const params = new URLSearchParams({goalAmount: String(goalAmount)});
        if (selectedIntensity.INCOME_CHANGE != null) params.set('monthlyExtraCapacity', String(selectedIntensity.INCOME_CHANGE));
        if (selectedIntensity.LOAN_PREPAYMENT != null) params.set('prepaymentAmount', String(selectedIntensity.LOAN_PREPAYMENT));
        if (selectedIntensity.LOAN_TERM_EXTENSION != null) params.set('termExtensionMonths', String(selectedIntensity.LOAN_TERM_EXTENSION));

        return fetch(`/api/future-simulation/recommended-combo?${params.toString()}`)
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
            .then((data) => {
                if (token !== comboToken) return;
                renderCombination({
                    chosenLevers: data.levers.map((item) => ({leverType: item.type, intensity: item.intensity})),
                    baselineMonths: data.baselineMonthsToGoal,
                    combinedMonths: data.comboMonthsToGoal,
                    diffMonths: data.diffMonths,
                    liquidAssetBudget: null,
                    combinationsEvaluated: null
                });
                if (data.diffMonths > 0) {
                    recommendedComboPreviewTextEl.textContent = `이 조건들을 조합하면 ${formatDuration(data.diffMonths)} 단축돼요.`;
                } else {
                    recommendedComboPreviewTextEl.textContent = '지금 페이스가 이미 최선이에요.';
                }
                recommendedComboPreviewEl.classList.remove('hidden');
            })
            .catch(() => {
                if (token !== comboToken) return;
                combinationLoadingEl.innerHTML = '<span class="material-symbols-outlined" aria-hidden="true">info</span><div><strong>추천 조합을 만들지 못했어요</strong><p>아래 방법별 효과를 참고해 직접 계획을 만들어보세요.</p></div>';
                combinationLoadingEl.classList.remove('hidden');
                combinationCardEl.classList.add('hidden');
            });
    }

    // renderCombination()은 페이지 진입 시 한 번뿐 아니라 카드 값이 바뀔 때마다 다시 호출되므로,
    // 매번 스피너/카드 표시 상태를 처음부터 다시 결정해야 한다(이전 호출의 상태가 남아있지 않게).
    function renderCombination(data) {
        if (!data.chosenLevers || data.chosenLevers.length === 0) {
            combinationLoadingEl.innerHTML = '<span class="material-symbols-outlined" aria-hidden="true">check_circle</span><div><strong>지금 페이스가 이미 최선이에요</strong><p>아래 방법별 효과를 참고해 직접 계획을 만들어보세요.</p></div>';
            combinationLoadingEl.classList.remove('hidden');
            combinationCardEl.classList.add('hidden');
            return;
        }

        const isSingle = data.chosenLevers.length === 1;
        combinationTitleEl.textContent = isSingle ? '이 방법을 추천해요' : '이 조합을 추천해요';

        const priorityOrder = {INCOME_CHANGE: 1, LOAN_PREPAYMENT: 2, LOAN_TERM_EXTENSION: 3, NEW_LOAN: 9};
        const chosen = [...data.chosenLevers].sort((a, b) => priorityOrder[a.leverType] - priorityOrder[b.leverType]);
        combinationItemsEl.innerHTML = chosen
            .map((item, index) => {
                const copy = LEVER_COPY[item.leverType];
                const value = copy.formatIntensity(item.intensity);
                return `<span class="fp-lever-combination-item"><b>${index + 1}.</b> ${copy.name} ${value}</span>`;
            })
            .join('');

        if (data.baselineMonths === null || data.baselineMonths === undefined) {
            const {magnitude, unit} = formatMonthsSplit(data.combinedMonths);
            combinationEffectEl.textContent =
                data.combinedMonths === null
                    ? '지금 페이스로는 목표 도달이 어려워요. 위 방법이 그래도 도움이 돼요.'
                    : `지금 페이스로는 목표 도달이 어렵지만, 이 방법을 실행하면 ${magnitude} ${unit} 후 도달할 수 있어요.`;
        } else if (isSingle) {
            combinationEffectEl.textContent = `이 방법만으로도 ${effectPhrase(data.diffMonths)}`;
        } else {
            combinationEffectEl.textContent =
                `따로따로 하는 것보다, 이 조합을 함께 실행하면 ${effectPhrase(data.diffMonths)}`;
        }

        const reasonParts = ['우선순위는 월 추가 확보 → 조기상환 → 만기 조정 순으로 제안해요. 만기 조정은 현금흐름이 부족할 때 마지막으로 검토하세요.'];
        if (data.combinationsEvaluated) {
            reasonParts.push(`가능한 강도 조합 ${data.combinationsEvaluated}가지를 전부 비교해서 찾았어요.`);
        }
        if (data.liquidAssetBudget !== null && data.liquidAssetBudget !== undefined && Number(data.liquidAssetBudget) > 0) {
            reasonParts.push(`조기상환은 지금 보유한 유동자산(${formatWon(data.liquidAssetBudget)}) 안에서만 후보로 넣었어요.`);
        }
        const includesNewLoan = data.chosenLevers.some((item) => item.leverType === 'NEW_LOAN');
        if (!includesNewLoan) {
            reasonParts.push('신규 대출도 후보에 넣어봤지만, 가정금리가 여유자금 수익률보다 높아서 어떤 강도로도 선택되지 않았어요.');
        }
        combinationReasonEl.textContent = reasonParts.join(' ');

        combinationCardEl.classList.remove('hidden');
        combinationLoadingEl.classList.add('hidden');
    }

    // ---------- 대출 우선순위: 대출이 2개 이상일 때만(loananalysis 재사용) ----------
    fetch('/api/future-simulation/debt-priorities')
        .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
        .then(renderDebtPriorities)
        .catch(() => {
            // 실패해도 부가 정보라 조용히 숨긴 채로 둔다.
        });

    // loananalysis/DebtPriorityCalculator의 가중치(연체40%+이자30%+수수료15%+소액10%+학자금5%) 그대로 —
    // 어떤 항목이 점수를 끌어올렸는지 0점 초과인 것만 보여준다.
    function scoreBreakdown(loan) {
        return [
            {label: '연체', value: loan.overdueScore, weight: 40},
            {label: '이자', value: loan.interestScore, weight: 30},
            {label: '수수료', value: loan.feeScore, weight: 15},
            {label: '소액대출', value: loan.smallLoanScore, weight: 10},
            {label: '학자금', value: loan.studentLoanScore, weight: 5}
        ]
            .filter((part) => Number(part.value) > 0)
            .map((part) => `${part.label} ${Number(part.value).toFixed(1)}점(가중치 ${part.weight}%)`)
            .join(' · ');
    }

    function renderDebtPriorities(priorities) {
        if (!priorities || priorities.length === 0) {
            return;
        }

        debtPriorityListEl.innerHTML = priorities
            .map((loan) => `
                <li class="fp-debt-priority-item">
                    <span class="fp-debt-priority-rank">${loan.priorityRank}</span>
                    <div class="fp-debt-priority-body">
                        <p class="fp-debt-priority-name">${LOAN_TYPE_LABELS[loan.loanType] || loan.loanType}
                            <span class="fp-debt-priority-balance">${formatWon(loan.currentBalance)} · 연 ${Number(loan.interestRate).toFixed(2)}%</span>
                        </p>
                        <p class="fp-debt-priority-reason">${loan.reason}</p>
                        <p class="fp-debt-priority-score">총점 ${Number(loan.priorityScore).toFixed(2)}점 — ${scoreBreakdown(loan)}</p>
                    </div>
                </li>
            `)
            .join('');

        debtPriorityCardEl.classList.remove('hidden');
    }

    function renderLeverCards(data, assumedReturnRatePercent) {
        const available = data.levers.filter((item) => item.available);
        rankingLoadingEl.classList.add('hidden');

        if (available.length === 0) {
            rankingLoadingEl.textContent = '비교할 수 있는 방법이 없어요';
            rankingLoadingEl.classList.remove('hidden');
            return;
        }

        const ctx = {
            loanRatePercent: data.representativeLoanRatePercent ?? null,
            assumedReturnRatePercent: assumedReturnRatePercent ?? null
        };

        available
            .slice()
            .sort((a, b) => Math.abs(b.diffMonths || 0) - Math.abs(a.diffMonths || 0))
            .forEach((item) => rankingListEl.appendChild(buildLeverCard(item, ctx)));

        rankingListEl.classList.remove('hidden');
        renderLeverLoanComparison(data, available);
    }

    function formatDiff(value, suffix) {
        const number = Number(value || 0);
        if (number === 0) return '';
        const direction = number > 0 ? '+' : '−';
        const tone = number > 0 ? 'increase' : 'decrease';
        return `<small class="fp-loan-diff ${tone}">(${direction}${suffix(Math.abs(number))})</small>`;
    }

    function loanMetric(value, diff, formatter) {
        return `${formatter(value)} ${formatDiff(diff, formatter)}`;
    }

    function renderLeverLoanComparison(data, available) {
        if (!leverLoanComparisonEl || !leverLoanComparisonBodyEl || !data.baseline) return;
        const rows = [{leverType: 'BASELINE', loanSummary: data.baseline, diffMonths: 0}, ...available];
        leverLoanComparisonBodyEl.innerHTML = rows.map((item) => {
            const isBaseline = item.leverType === 'BASELINE';
            const label = isBaseline ? '현재 유지' : LEVER_COPY[item.leverType].name;
            const summary = item.loanSummary;
            const incomeAdjusted = item.leverType === 'INCOME_CHANGE';
            const periodUnchanged = !isBaseline && Number(summary.repaymentPeriodDiff) === 0;
            const incomeCapacity = Math.abs(Number(summary.monthlyBurdenDiff || 0));
            const monthlyLabel = incomeAdjusted ? '월 추가 상환 여력' : '월 부담';
            const monthlyValue = incomeAdjusted
                ? `<strong class="fp-loan-benefit">매달 ${formatWon(incomeCapacity)} 더 확보</strong>`
                : loanMetric(summary.monthlyBurden, summary.monthlyBurdenDiff, formatWon);
            const interestLabel = item.leverType === 'LOAN_PREPAYMENT' ? '총 이자 절감' : '총 이자';
            const interestValue = item.leverType === 'LOAN_PREPAYMENT' && Number(summary.totalInterestDiff) < 0
                ? `<strong class="fp-loan-benefit">${formatWon(Math.abs(Number(summary.totalInterestDiff)))} 절감</strong>`
                : loanMetric(summary.totalInterest, summary.totalInterestDiff, formatWon);
            const periodValue = periodUnchanged
                ? '<span class="fp-loan-unchanged">상환 기간 유지</span>'
                : loanMetric(summary.repaymentPeriodMonths, summary.repaymentPeriodDiff, (v) => `${Math.round(v)}개월`);
            return `<article class="fp-lever-loan-summary ${isBaseline ? 'fp-loan-baseline-row' : ''}">` +
                `<h4>${label}<span>${isBaseline ? '기준' : effectPhrase(item.diffMonths)}</span></h4>` +
                `<dl><div><dt>${monthlyLabel}</dt><dd>${monthlyValue}</dd></div>` +
                `<div><dt>${interestLabel}</dt><dd>${interestValue}</dd></div>` +
                `<div><dt>상환 기간</dt><dd>${periodValue}</dd></div></dl>` +
                `</article>`;
        }).join('');
        leverLoanComparisonEl.classList.remove('hidden');

        const best = available.filter((item) => Number(item.diffMonths) > 0)
            .sort((a, b) => Number(b.diffMonths) - Number(a.diffMonths))[0];
        if (!best || !leverBestInsightEl) return;
        const interestDiff = Number(best.loanSummary.totalInterestDiff || 0);
        const name = LEVER_COPY[best.leverType].name;
        const interestCopy = interestDiff <= 0
            ? `총이자도 ${formatWon(Math.abs(interestDiff))} 절감돼요`
            : `단, 총이자는 ${formatWon(interestDiff)} 늘어나요`;
        leverBestInsightEl.textContent = `${name}가 가장 효과적이에요 — ${formatDuration(best.diffMonths)} 단축 + ${interestCopy}`;
        leverBestInsightEl.classList.remove('hidden');
    }

    // isCurrent()가 false면(그 사이 다른 강도가 선택됨) 비교표(currentLeverImpact 공유 state)와
    // "선택 결과" 박스 둘 다 이 응답을 반영하지 않고 버린다 — 두 영역이 항상 같은 타이밍에, 같은
    // 응답으로만 같이 갱신되게 하나의 게이트로 묶는다.
    function updateLoanComparisonForIntensity(leverType, intensity, isCurrent, onUpdated) {
        return fetch(`/api/future-simulation/lever-loan-summary?goalAmount=${encodeURIComponent(goalAmount)}` +
            `&leverType=${leverType}&intensity=${encodeURIComponent(intensity)}${returnRateQuery}`)
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
            .then((updated) => {
                if (!isCurrent() || !currentLeverImpact) return;
                const item = currentLeverImpact.levers.find((candidate) => candidate.leverType === updated.leverType);
                if (!item) return;
                item.loanSummary = updated.loanSummary;
                item.diffMonths = updated.diffMonths;
                renderLeverLoanComparison(currentLeverImpact, currentLeverImpact.levers.filter((candidate) => candidate.available));
                if (onUpdated) onUpdated(updated.loanSummary);
            })
            .catch(() => {});
    }

    // 카드 하나 — 헤드라인(지금 이만큼 하면 N개월 빨라져요) + 왜/언제 유리한지 설명 + 강도 칩(2~3개).
    // 칩 클릭 시 서버 재조회 없이 즉시 전환된다 — presets가 이미 강도별 diffMonths를 다 들고 있기 때문.
    function buildLeverCard(item, ctx) {
        const copy = LEVER_COPY[item.leverType];
        const presets = item.presets && item.presets.length > 0
            ? item.presets
            : [{intensity: item.defaultIntensity, diffMonths: item.diffMonths}];
        const defaultPresetIndex = Math.max(0, presets.findIndex(
            (p) => Number(p.intensity) === Number(item.defaultIntensity)
        ));

        const li = document.createElement('li');
        li.className = 'fp-lever-card';
        li.dataset.lever = item.leverType;

        // 헤드라인은 클라이언트에 이미 있는 preset 값으로 즉시 갱신되는 반면, "선택 결과" 박스와
        // 아래 비교표(currentLeverImpact 공유 state)는 /lever-loan-summary를 매번 새로 호출해서
        // 채워진다 — 칩을 빠르게 연속으로 바꾸면 먼저 보낸 요청의 응답이 나중에 도착해 최신 선택과
        // 다른 값으로 덮어쓸 수 있다. 요청마다 토큰을 발급해서, 그 사이 다른 선택으로 토큰이 바뀌었으면
        // 응답을 버리고(비교표·박스 둘 다) 최신 선택의 결과만 반영한다.
        let latestToken = 0;

        const headlineEl = document.createElement('p');
        headlineEl.className = 'fp-lever-card-headline';

        const detailEl = document.createElement('p');
        detailEl.className = 'fp-lever-card-detail';

        const selectedSummaryEl = document.createElement('p');
        selectedSummaryEl.className = 'fp-lever-card-selected-summary';

        const explanationEl = document.createElement('p');
        explanationEl.className = 'fp-lever-card-explanation';

        const chipsEl = document.createElement('div');
        chipsEl.className = 'fp-lever-card-chips';

        function deactivateAllChips() {
            chipsEl.querySelectorAll('.fp-lever-chip').forEach((c) => c.classList.remove('active'));
            customWrapEl.classList.remove('active');
        }

        presets.forEach((preset, index) => {
            const chip = document.createElement('button');
            chip.type = 'button';
            chip.className = 'fp-lever-chip' + (index === defaultPresetIndex ? ' active' : '');
            chip.textContent = copy.formatIntensity(preset.intensity);
            chip.addEventListener('click', () => {
                deactivateAllChips();
                chip.classList.add('active');
                customInputEl.value = '';
                renderCardBody(preset);
                const token = ++latestToken;
                updateLoanComparisonForIntensity(item.leverType, preset.intensity, () => token === latestToken, renderSelectedSummary);
                if (item.leverType in selectedIntensity) {
                    selectedIntensity[item.leverType] = preset.intensity;
                    loadRecommendedCombo();
                }
            });
            chipsEl.appendChild(chip);
        });

        // 직접 입력도 레버의 실제 단위를 쓴다. 과거 슬라이더의 0~100 제한을 재사용하지 않는다.
        const inputConstraints = {
            INCOME_CHANGE: {min: 100000, max: 10000000, step: 100000},
            LOAN_PREPAYMENT: {min: 1000000, max: Number(presets[presets.length - 1].intensity), step: 1000000},
            LOAN_TERM_EXTENSION: {min: 12, max: 240, step: 12},
            NEW_LOAN: {min: 10000000, max: 500000000, step: 10000000}
        }[item.leverType];
        const minIntensity = inputConstraints.min;
        const maxIntensity = Math.max(minIntensity, inputConstraints.max);
        const customWrapEl = document.createElement('div');
        customWrapEl.className = 'fp-lever-chip-custom';
        customWrapEl.innerHTML = `
            <input type="number" inputmode="numeric" class="fp-lever-chip-custom-input" min="${minIntensity}" max="${maxIntensity}" step="${inputConstraints.step}" placeholder="직접 입력">
            <span class="fp-lever-chip-custom-unit">${copy.unitLabel}</span>
        `;
        const customInputEl = customWrapEl.querySelector('.fp-lever-chip-custom-input');

        function submitCustomIntensity() {
            const rawDigits = customInputEl.value.replace(/[^0-9]/g, '');
            if (rawDigits === '') {
                return;
            }
            const requested = Math.min(maxIntensity, Math.max(minIntensity, Number(rawDigits)));

            const token = ++latestToken;
            customWrapEl.classList.add('loading');
            fetch(
                `/api/future-simulation/lever-intensity?goalAmount=${encodeURIComponent(goalAmount)}` +
                `&leverType=${item.leverType}&intensity=${encodeURIComponent(requested)}${returnRateQuery}`
            )
                .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
                .then((result) => {
                    if (token !== latestToken) return;
                    customInputEl.value = String(Math.round(Number(result.intensity)));
                    deactivateAllChips();
                    customWrapEl.classList.add('active');
                    renderCardBody(result);
                    updateLoanComparisonForIntensity(item.leverType, result.intensity, () => token === latestToken, renderSelectedSummary);
                    if (item.leverType in selectedIntensity) {
                        selectedIntensity[item.leverType] = result.intensity;
                        loadRecommendedCombo();
                    }
                })
                .catch(() => {
                    // 실패하면 조용히 무시 — 기존 표시값을 그대로 둔다.
                })
                .finally(() => {
                    customWrapEl.classList.remove('loading');
                });
        }

        customInputEl.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                submitCustomIntensity();
            }
        });
        customInputEl.addEventListener('blur', submitCustomIntensity);
        chipsEl.appendChild(customWrapEl);

        function renderCardBody(preset) {
            const diffMonths = preset.diffMonths;
            const isNegative = diffMonths !== null && diffMonths !== undefined && diffMonths < 0;
            headlineEl.innerHTML =
                `${copy.headline(preset.intensity)}<br>` +
                `<span class="fp-lever-card-effect${isNegative ? ' fp-lever-card-effect-negative' : ''}">${effectPhrase(diffMonths)}</span>`;
            const detailText = copy.detailText(preset);
            detailEl.textContent = detailText || '';
            detailEl.classList.toggle('hidden', !detailText);
            explanationEl.textContent = copy.explanation(ctx);
            explanationEl.classList.toggle('hidden', explanationEl.textContent === '');
        }

        function renderSelectedSummary(summary) {
            const monthlyDiff = Number(summary.monthlyBurdenDiff || 0);
            const interestDiff = Number(summary.totalInterestDiff || 0);
            const periodDiff = Number(summary.repaymentPeriodDiff || 0);
            if (item.leverType === 'INCOME_CHANGE') {
                selectedSummaryEl.textContent = `선택 결과 · 매달 ${formatWon(Math.abs(monthlyDiff))}의 추가 상환 여력이 생겨요`;
            } else if (item.leverType === 'LOAN_PREPAYMENT') {
                selectedSummaryEl.textContent = `선택 결과 · 월 부담 ${formatWon(Math.abs(monthlyDiff))} 감소 · 총이자 ${formatWon(Math.abs(interestDiff))} 절감`;
            } else if (item.leverType === 'LOAN_TERM_EXTENSION') {
                selectedSummaryEl.textContent = `선택 결과 · 월 부담 ${formatWon(Math.abs(monthlyDiff))} 감소 · 상환기간 ${formatDuration(periodDiff)} 연장`;
            } else {
                selectedSummaryEl.textContent = `선택 결과 · 월 부담 ${formatWon(Math.abs(monthlyDiff))} 증가 · 총이자 ${formatWon(Math.abs(interestDiff))} 증가`;
            }
            selectedSummaryEl.classList.remove('hidden');
        }

        renderCardBody(presets[defaultPresetIndex]);
        renderSelectedSummary(item.loanSummary);

        li.innerHTML = `
            <div class="fp-lever-card-head">
                <span class="material-symbols-outlined fp-lever-card-icon" aria-hidden="true">${copy.icon}</span>
                <span class="fp-lever-card-name">${copy.name}</span>
            </div>
        `;
        li.appendChild(headlineEl);
        li.appendChild(detailEl);
        li.appendChild(selectedSummaryEl);
        li.appendChild(explanationEl);
        li.appendChild(chipsEl);
        if (item.leverType === 'NEW_LOAN') {
            const assumption = document.createElement('p');
            assumption.className = 'fp-lever-card-assumption';
            assumption.textContent = '가정: 금리 연 4.5%, 만기 60개월, 원리금균등상환 — 실제 대출 조건에 따라 달라질 수 있어요';
            li.appendChild(assumption);
        }

        return li;
    }

    // ---------- 섹션2: 금리 리스크 참고 카드 ----------
    function loadRateRisk(mode) {
        rateRiskLoadingEl.classList.remove('hidden');
        rateRiskEmptyEl.classList.add('hidden');
        rateRiskContentEl.classList.add('hidden');

        fetch(`/api/future-simulation/rate-risk-reference?mode=${mode}`)
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
            .then(renderRateRisk)
            .catch(() => {
                rateRiskLoadingEl.textContent = '불러오지 못했어요';
            });
    }

    function renderRateRisk(data) {
        rateRiskLoadingEl.classList.add('hidden');

        if (!data.available) {
            rateRiskEmptyEl.classList.remove('hidden');
            return;
        }

        rateRiskContentEl.classList.remove('hidden');
        rateRiskLoanTypeEl.textContent = LOAN_TYPE_LABELS[data.loanType] || data.loanType || '-';
        rateRiskCurrentRateEl.textContent = `연 ${Number(data.currentRate).toFixed(2)}%`;
        rateRiskCurrentPaymentEl.textContent = formatWon(data.currentMonthlyPayment);

        const finalStep = data.path && data.path.length > 0 ? data.path[data.path.length - 1] : null;
        rateRiskFinalPaymentEl.textContent = finalStep ? formatWon(finalStep.monthlyPayment) : '-';
        if (rateRiskPathBodyEl) {
            rateRiskPathBodyEl.innerHTML = (data.path || []).map((step) => {
                const diff = Number(step.monthlyPayment) - Number(data.currentMonthlyPayment);
                const sign = diff > 0 ? '+' : '';
                return `<tr><td>${Number(step.monthOffset) === 0 ? '현재' : `${step.monthOffset}개월 후`}</td><td>연 ${Number(step.appliedRate).toFixed(2)}%</td><td>${formatWon(step.monthlyPayment)}</td><td>${sign}${formatWon(diff)}</td></tr>`;
            }).join('');
        }

        const threshold = formatWon(data.thresholdMonthlyPayment);
        let insightHtml;
        let danger = false;
        if (data.alreadyExceeded) {
            danger = true;
            insightHtml = `이미 지금 금리에서도 월상환액이 기준(<strong>${threshold}</strong>)을 넘었어요.`;
        } else if (data.breakevenReached) {
            const margin = (Number(data.breakevenRate) - Number(data.currentRate)).toFixed(2);
            insightHtml =
                `금리가 연 <strong>${Number(data.breakevenRate).toFixed(2)}%</strong>까지 오르면 월상환액이 ` +
                `기준(${threshold})을 넘어요. (현재 대비 +${margin}%p)`;
        } else {
            insightHtml = `이 시나리오 범위 안에서는 월상환액이 기준(<strong>${threshold}</strong>)을 넘지 않아요.`;
        }
        rateRiskInsightEl.innerHTML = insightHtml;
        rateRiskInsightEl.classList.toggle('fp-rate-risk-insight-danger', danger);
    }

    rateRiskTabsEl.querySelectorAll('.fp-rate-risk-tab').forEach((tab) => {
        tab.addEventListener('click', () => {
            rateRiskTabsEl.querySelectorAll('.fp-rate-risk-tab').forEach((t) => {
                t.classList.remove('active');
                t.setAttribute('aria-selected', 'false');
            });
            tab.classList.add('active');
            tab.setAttribute('aria-selected', 'true');
            loadRateRisk(tab.dataset.mode);
        });
    });

    loadRateRisk('SIMPLE');
})();
