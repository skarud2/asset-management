(function () {
    const svg = document.getElementById('growthChartSvg');
    const curvePathEl = document.getElementById('growthCurvePath');
    const contributionAreaEl = document.getElementById('growthContributionArea');
    const returnAreaEl = document.getElementById('growthReturnArea');
    const goalLineEl = document.getElementById('growthGoalLine');
    const benchmarkLineEl = document.getElementById('growthBenchmarkLine');
    const goalMarkerEl = document.getElementById('growthGoalMarker');
    const benchmarkMarkerEl = document.getElementById('growthBenchmarkMarker');
    const scrubberLineEl = document.getElementById('growthScrubberLine');
    const scrubberDotEl = document.getElementById('growthScrubberDot');
    const hoverDotEl = document.getElementById('growthHoverDot');
    const hoverLineEl = document.getElementById('growthHoverLine');
    const tooltipEl = document.getElementById('growthTooltip');
    const paceMagnitudeEl = document.getElementById('growthPaceMagnitude');
    const paceUnitEl = document.getElementById('growthPaceUnit');
    const warningNoteEl = document.getElementById('growthWarningNote');
    const milestonesEl = document.getElementById('growthMilestones');
    const benchmarkLegendItemEl = document.getElementById('growthBenchmarkLegendItem');
    const goalAmountInputEl = document.getElementById('growthGoalAmount');
    const benchmarkCalloutEl = document.getElementById('growthBenchmarkCallout');
    const benchmarkPercentEl = document.getElementById('growthBenchmarkPercent');
    const benchmarkPercentTextEl = document.getElementById('growthBenchmarkPercentText');
    const benchmarkCaptionEl = document.getElementById('growthBenchmarkCaption');
    const benchmarkNeutralNoteEl = document.getElementById('growthBenchmarkNeutralNote');
    const detailIncomeEl = document.getElementById('growthDetailIncome');
    const detailExpenseEl = document.getElementById('growthDetailExpense');
    const detailLoanPaymentEl = document.getElementById('growthDetailLoanPayment');
    const detailSavingsEl = document.getElementById('growthDetailSavings');
    const detailReturnRateEl = document.getElementById('growthDetailReturnRate');
    const summaryTextEl = document.getElementById('growthSummaryText');
    const journeyGoalEl = document.getElementById('growthJourneyGoal');
    const journeyProgressEl = document.getElementById('growthJourneyProgress');
    const journeyNowEl = document.getElementById('growthJourneyNow');
    const journeyYear1El = document.getElementById('growthJourneyYear1');
    const journeyYear3El = document.getElementById('growthJourneyYear3');
    const journeyArrivalEl = document.getElementById('growthJourneyArrival');
    const scrubberInputEl = document.getElementById('growthScrubberInput');
    const scrubberLabelEl = document.getElementById('growthScrubberLabel');
    const scrubberNetWorthEl = document.getElementById('growthScrubberNetWorth');
    const scrubberRemainingEl = document.getElementById('growthScrubberRemaining');
    const scrubberContributionEl = document.getElementById('growthScrubberContribution');
    const scrubberReturnEl = document.getElementById('growthScrubberReturn');
    const scrubberLoanRowEl = document.getElementById('growthScrubberLoanRow');
    const scrubberLoanBalanceEl = document.getElementById('growthScrubberLoanBalance');
    if (!svg || !curvePathEl || !goalAmountInputEl) {
        return;
    }

    const CHART_WIDTH = 600;
    const CHART_HEIGHT = 220;
    const PAD_X = 8;
    const PAD_TOP = 16;
    const PAD_BOTTOM = 24;
    const DRAW_DURATION_MS = 550;

    function formatWon(amount) {
        return `${new Intl.NumberFormat('ko-KR').format(Math.round(Number(amount) || 0))}원`;
    }

    // 2단계 goal-preset.js의 formatMonthsSplit()과 같은 규칙(N년 M개월) — 화면 전체에서 개월수 표기를 통일한다.
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

    function monthLabel(monthOffset) {
        if (monthOffset === 0) {
            return '지금';
        }
        const {magnitude, unit} = formatMonthsSplit(monthOffset);
        return `${magnitude} ${unit}`.trim() + ' 후';
    }

    const goalAmount = Number(goalAmountInputEl.value || 100000000);

    fetch(`/api/future-simulation/projection?goalAmount=${encodeURIComponent(goalAmount)}`)
        .then((res) => {
            if (!res.ok) {
                throw new Error('projection 조회 실패: ' + res.status);
            }
            return res.json();
        })
        .then(render)
        .catch(() => {
            paceMagnitudeEl.textContent = '불러오지 못했어요';
            paceUnitEl.textContent = '';
        });

    function render(data) {
        const {magnitude, unit} = formatMonthsSplit(data.monthsToGoal);
        paceMagnitudeEl.textContent = magnitude;
        paceUnitEl.textContent = unit;
        renderGoalJourney(data);

        if (!data.crossoverReached) {
            warningNoteEl.textContent =
                '현재 페이스로는 20년(240개월) 안에 목표에 도달하기 어려워요. 저축 여력이나 목표 금액을 다시 확인해보세요.';
            warningNoteEl.classList.remove('hidden');
        }

        const points = renderChart(data);
        renderMilestonesAndBenchmark(data);
        renderSummaryCard(data);
        renderSavingsBreakdown(data);
        setupScrubber(data, points);
        bindHover(points);
    }

    function renderGoalJourney(data) {
        const timeline = data.timeline || [];
        const at = (month) => timeline.find((point) => point.monthOffset >= month) || timeline[timeline.length - 1];
        const now = at(0);
        const year1 = at(12);
        const year3 = at(36);
        const goal = Number(data.goalAmount);
        const current = Number(now.netWorth);
        const remaining = Math.max(0, goal - current);
        const progress = goal > 0 ? Math.max(0, Math.min(100, current / goal * 100)) : 0;
        journeyGoalEl.textContent = formatWon(goal);
        journeyProgressEl.style.width = `${progress}%`;
        journeyNowEl.textContent = `지금 순자산 ${formatWon(current)} · 목표까지 ${formatWon(remaining)} 남았어요`;
        journeyYear1El.textContent = year1 ? formatWon(year1.netWorth) : '-';
        journeyYear3El.textContent = year3 ? formatWon(year3.netWorth) : '-';
        journeyArrivalEl.textContent = data.monthsToGoal === null || data.monthsToGoal === undefined
            ? '현재 방식으로는 예측 어려움'
            : `${formatMonthsSplit(data.monthsToGoal).magnitude} ${formatMonthsSplit(data.monthsToGoal).unit} 후`;
    }

    // ---------- 누적구성 영역 차트 ----------
    function renderChart(data) {
        const timeline = data.timeline;
        const maxMonth = timeline[timeline.length - 1].monthOffset;
        const values = timeline.map((p) => Number(p.netWorth));

        let maxValue = Math.max(...values, Number(data.goalAmount));
        if (data.benchmarkNetWorth !== null && data.benchmarkNetWorth !== undefined) {
            maxValue = Math.max(maxValue, Number(data.benchmarkNetWorth));
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

        const points = timeline.map((p) => ({
            x: xFor(p.monthOffset),
            y: yFor(Number(p.netWorth)),
            month: p.monthOffset,
            value: Number(p.netWorth),
            contribution: Number(p.contributionAmount),
            returnValue: Number(p.returnAmount)
        }));

        curvePathEl.setAttribute(
            'd',
            points.map((p, i) => (i === 0 ? `M${p.x},${p.y}` : `L${p.x},${p.y}`)).join(' ')
        );

        // 스택 영역 — 바닥(0 또는 그 아래로 내려가면 최솟값)부터 원금까지, 원금부터 순자산(=원금+투자수익)까지.
        const baselineY = yFor(Math.min(0, minValue));
        const contributionTop = points.map((p) => ({x: p.x, y: yFor(p.contribution)}));
        const netWorthTop = points.map((p) => ({x: p.x, y: p.y}));
        contributionAreaEl.setAttribute('d', bandPath(contributionTop, baselineY));
        returnAreaEl.setAttribute('d', bandPath(netWorthTop, contributionTop));

        const goalY = yFor(Number(data.goalAmount));
        goalLineEl.setAttribute('x1', String(PAD_X));
        goalLineEl.setAttribute('x2', String(CHART_WIDTH - PAD_X));
        goalLineEl.setAttribute('y1', String(goalY));
        goalLineEl.setAttribute('y2', String(goalY));

        if (data.monthsToGoal !== null && data.monthsToGoal !== undefined) {
            const marker = points.find((p) => p.month === data.monthsToGoal) || points[points.length - 1];
            goalMarkerEl.setAttribute('cx', String(marker.x));
            goalMarkerEl.setAttribute('cy', String(marker.y));
            goalMarkerEl.classList.remove('hidden');
        } else {
            goalMarkerEl.classList.add('hidden');
        }

        if (data.benchmarkNetWorth !== null && data.benchmarkNetWorth !== undefined) {
            const benchmarkY = yFor(Number(data.benchmarkNetWorth));
            benchmarkLineEl.setAttribute('x1', String(PAD_X));
            benchmarkLineEl.setAttribute('x2', String(CHART_WIDTH - PAD_X));
            benchmarkLineEl.setAttribute('y1', String(benchmarkY));
            benchmarkLineEl.setAttribute('y2', String(benchmarkY));
            benchmarkLineEl.classList.remove('hidden');
            benchmarkLegendItemEl.classList.remove('hidden');

            if (data.benchmarkCrossMonth !== null && data.benchmarkCrossMonth !== undefined) {
                const marker = points.find((p) => p.month === data.benchmarkCrossMonth) || points[0];
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

        playDrawAnimation();
        return points;
    }

    // 위쪽 경계선(topPoints)과 아래쪽 경계(고정 y 또는 또 다른 점 배열)로 둘러싸인 띠(band) 모양 path.
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

    // stroke-dasharray/dashoffset을 곡선 실제 길이로 맞춘 뒤 0으로 트랜지션 — 400~600ms 드로잉 효과.
    // 스택 영역은 같은 타이밍에 opacity 페이드인으로 맞춘다(면적 path 자체를 드로잉 애니메이션하면 과함).
    function playDrawAnimation() {
        const length = curvePathEl.getTotalLength();
        curvePathEl.style.transition = 'none';
        curvePathEl.style.strokeDasharray = `${length}`;
        curvePathEl.style.strokeDashoffset = `${length}`;
        contributionAreaEl.style.transition = 'none';
        contributionAreaEl.style.opacity = '0';
        returnAreaEl.style.transition = 'none';
        returnAreaEl.style.opacity = '0';
        // eslint-disable-next-line no-unused-expressions
        curvePathEl.getBoundingClientRect(); // 강제 리플로우 — 트랜지션이 처음부터 걸리게 함
        curvePathEl.style.transition = `stroke-dashoffset ${DRAW_DURATION_MS}ms ease`;
        contributionAreaEl.style.transition = `opacity ${DRAW_DURATION_MS}ms ease`;
        returnAreaEl.style.transition = `opacity ${DRAW_DURATION_MS}ms ease`;
        requestAnimationFrame(() => {
            curvePathEl.style.strokeDashoffset = '0';
            contributionAreaEl.style.opacity = '1';
            returnAreaEl.style.opacity = '1';
        });
    }

    // ---------- 호버 툴팁 ----------
    function bindHover(points) {
        svg.addEventListener('pointermove', (e) => onHover(e, points));
        svg.addEventListener('pointerleave', hideHover);
    }

    function onHover(e, points) {
        const rect = svg.getBoundingClientRect();
        const localX = ((e.clientX - rect.left) / rect.width) * CHART_WIDTH;

        let nearest = points[0];
        let nearestDist = Infinity;
        points.forEach((p) => {
            const dist = Math.abs(p.x - localX);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        });

        hoverDotEl.setAttribute('cx', String(nearest.x));
        hoverDotEl.setAttribute('cy', String(nearest.y));
        hoverDotEl.classList.remove('hidden');

        hoverLineEl.setAttribute('x1', String(nearest.x));
        hoverLineEl.setAttribute('x2', String(nearest.x));
        hoverLineEl.setAttribute('y1', String(PAD_TOP));
        hoverLineEl.setAttribute('y2', String(CHART_HEIGHT - PAD_BOTTOM));
        hoverLineEl.classList.remove('hidden');

        tooltipEl.style.left = `${(nearest.x / CHART_WIDTH) * 100}%`;
        tooltipEl.style.top = `${(nearest.y / CHART_HEIGHT) * 100}%`;
        tooltipEl.textContent = `${monthLabel(nearest.month)} · ${formatWon(nearest.value)}`;
        tooltipEl.classList.add('visible');
    }

    function hideHover() {
        hoverDotEl.classList.add('hidden');
        hoverLineEl.classList.add('hidden');
        tooltipEl.classList.remove('visible');
    }

    // ---------- 3. 타임라인 탐색기 ----------
    // timeline은 backend에서 month 0..N을 빠짐없이 순서대로 만들기 때문에, index === monthOffset이다.
    // 그 전제로 슬라이더 값을 그대로 points 배열 인덱스로 써서, 추가 API 호출 없이 즉시 조회한다.
    function setupScrubber(data, points) {
        scrubberInputEl.max = String(points.length - 1);
        const defaultIndex = Math.floor((points.length - 1) / 2);
        scrubberInputEl.value = String(defaultIndex);
        updateScrubber(defaultIndex, data, points);
        scrubberInputEl.addEventListener('input', () => {
            updateScrubber(Number(scrubberInputEl.value), data, points);
        });
    }

    function updateScrubber(index, data, points) {
        const point = points[Math.max(0, Math.min(points.length - 1, index))];

        scrubberLabelEl.textContent = monthLabel(point.month);
        scrubberLineEl.setAttribute('x1', String(point.x));
        scrubberLineEl.setAttribute('x2', String(point.x));
        scrubberLineEl.setAttribute('y1', String(PAD_TOP));
        scrubberLineEl.setAttribute('y2', String(CHART_HEIGHT - PAD_BOTTOM));
        scrubberDotEl.setAttribute('cx', String(point.x));
        scrubberDotEl.setAttribute('cy', String(point.y));

        scrubberNetWorthEl.textContent = formatWon(point.value);
        scrubberRemainingEl.textContent = formatWon(Math.max(0, Number(data.goalAmount) - point.value));
        scrubberContributionEl.textContent = formatWon(point.contribution);
        scrubberReturnEl.textContent = formatWon(point.returnValue);

        const totalLoanBalance = Number(data.totalLoanBalance) || 0;
        if (totalLoanBalance > 0) {
            scrubberLoanBalanceEl.textContent = formatWon(totalLoanBalance);
            scrubberLoanRowEl.classList.remove('hidden');
        } else {
            scrubberLoanRowEl.classList.add('hidden');
        }
    }

    // ---------- 4. 목표 도달 시점 기준 원금/투자수익 요약 ----------
    function renderSummaryCard(data) {
        const timeline = data.timeline;
        const hasGoalMonth = data.monthsToGoal !== null && data.monthsToGoal !== undefined;
        const targetMonth = hasGoalMonth ? data.monthsToGoal : timeline[timeline.length - 1].monthOffset;
        const point = timeline.find((p) => p.monthOffset === targetMonth) || timeline[timeline.length - 1];

        const when = hasGoalMonth ? '목표 도달 시점 기준' : '시뮬레이션 종료 시점(20년) 기준';
        summaryTextEl.innerHTML =
            `${when} 전체 <strong>${formatWon(point.netWorth)}</strong> 중 ` +
            `<strong>${formatWon(point.contributionAmount)}</strong>은 직접 모은 돈, ` +
            `<strong>${formatWon(point.returnAmount)}</strong>은 투자수익이에요.`;
    }

    // ---------- 이 곡선을 만드는 계산 값 ----------
    function renderSavingsBreakdown(data) {
        detailIncomeEl.textContent = formatWon(data.monthlyIncome);
        detailExpenseEl.textContent = formatWon(data.monthlyLivingExpense);
        detailLoanPaymentEl.textContent = formatWon(data.monthlyLoanPayment);
        detailSavingsEl.textContent = formatWon(data.monthlySavingsCapacity);
        detailReturnRateEl.textContent = `연 ${Number(data.assumedReturnRatePercent).toFixed(2)}%`;
    }

    // ---------- 5. 마일스톤 + 또래 대비(조건부) ----------
    function renderMilestonesAndBenchmark(data) {
        milestonesEl.innerHTML = '';

        const items = [
            {label: '지금', text: `순자산 ${formatWon(data.timeline[0].netWorth)}`, reached: true}
        ];

        const hasBenchmark = data.benchmarkNetWorth !== null && data.benchmarkNetWorth !== undefined;
        const crossed = data.benchmarkCrossMonth !== null && data.benchmarkCrossMonth !== undefined;

        if (hasBenchmark && crossed) {
            const label = data.benchmarkLabel ? `${data.benchmarkLabel} ` : '';
            items.push({
                label: monthLabel(data.benchmarkCrossMonth),
                text: `${label}중앙값을 넘어서요`,
                reached: data.benchmarkCrossMonth === 0
            });
        }

        if (data.monthsToGoal !== null && data.monthsToGoal !== undefined) {
            items.push({
                label: monthLabel(data.monthsToGoal),
                text: '목표에 도달해요',
                reached: data.monthsToGoal === 0
            });
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

        benchmarkCalloutEl.classList.add('hidden');
        benchmarkNeutralNoteEl.classList.add('hidden');
        if (!hasBenchmark) {
            return;
        }

        const label = data.benchmarkLabel ? `${data.benchmarkLabel} ` : '';
        if (crossed) {
            // 기간 내에 넘어서는 경우 — 2단계와 같은 % 콜아웃(지금 시점 기준 위치)을 그대로 보여준다.
            const currentNetWorth = Number(data.timeline[0].netWorth);
            const median = Number(data.benchmarkNetWorth);
            const deltaPercent = ((currentNetWorth - median) / median) * 100;
            const sign = deltaPercent >= 0 ? '+' : '';

            benchmarkPercentEl.textContent = `${sign}${deltaPercent.toFixed(1)}%`;
            benchmarkPercentTextEl.textContent = deltaPercent >= 0
                ? `${label}중앙값보다 높아요`
                : `${label}중앙값보다 낮아요`;
            benchmarkCaptionEl.textContent = `${label}중앙값 ${formatWon(median)} 대비 · 지금 내 순자산 기준`;
            benchmarkCalloutEl.classList.remove('hidden');
        } else {
            // 기간(20년) 내에 못 넘어서는 경우 — 자극적인 % 대신, 이번 목표 금액과 엮은 담담한 문장으로.
            benchmarkNeutralNoteEl.textContent =
                `이 목표(${formatWon(data.goalAmount)})만으로는 ${label}중앙값(${formatWon(data.benchmarkNetWorth)})에는 아직 못 미쳐요.`;
            benchmarkNeutralNoteEl.classList.remove('hidden');
        }
    }
})();
