(function () {
    function render(card) {
        const plan = card.futuresimPrintData;
        const warning = String(plan.diffLabel || '').startsWith('+') ? ' warning' : '';
        return `<article class="report-futuresim-document"><header><p>미래 금융 시뮬레이터 · 저장한 실행 계획</p><h2>${escapeHtml(plan.planName || card.title)}</h2><span>저장 ${escapeHtml(plan.savedAt)}</span></header><section><h3>저장할 계획 요약</h3><dl class="report-futuresim-summary"><div><dt>내 목표</dt><dd>${escapeHtml(plan.goalAmount)}</dd></div><div><dt>현재 순자산</dt><dd>${escapeHtml(plan.currentNetWorth)}</dd></div><div><dt>예상 도달 시점</dt><dd>${escapeHtml(plan.projectedDuration)}</dd></div><div><dt>앞당긴 기간</dt><dd>${escapeHtml(String(plan.diffLabel || '').replace('-', ''))}</dd></div></dl><p class="report-futuresim-method-title">선택한 방법</p>${renderLevers(plan.levers)}<p class="report-futuresim-note">개별 효과 합이 아니라 조합 재계산 결과예요.</p></section><section><h3>실행 전후 비교</h3><div class="report-futuresim-compare"><div><span>지금 페이스</span><strong>${escapeHtml(plan.baselineDuration)}</strong></div><i>→</i><div><span>실행 계획 적용</span><strong>${escapeHtml(plan.projectedDuration)}</strong></div></div><b class="report-futuresim-badge${warning}">${escapeHtml(plan.diffLabel)}</b><p class="report-futuresim-note">만 60세 정년 전 도달 기준으로 계산한 예상 시점이에요.</p></section>${renderLoans(plan.loanImpacts)}${renderGrowth(plan)}${renderTimeline(plan)}${renderMilestones(plan)}`;
    }

    function renderLevers(levers) {
        if (!levers || levers.length === 0) return '<p class="report-futuresim-note">기존 페이스를 유지하는 계획이에요.</p>';
        return `<ul class="report-futuresim-levers">${levers.map((lever) => `<li><span>${leverIcon(lever.icon)}</span><strong>${escapeHtml(lever.label)}</strong><b>${escapeHtml(lever.intensity)}</b>${lever.effect ? `<em>${escapeHtml(lever.effect)}</em>` : ''}${lever.financialEffect ? `<small>${escapeHtml(lever.financialEffect)}</small>` : ''}</li>`).join('')}</ul>`;
    }

    function renderLoans(impacts) {
        if (!impacts || impacts.length === 0) return '';
        return `<section><h3>대출 영향</h3><div class="report-futuresim-loans">${impacts.map((impact) => `<article><header><strong>${escapeHtml(impact.loanType)}</strong>${impact.monthlySaving ? `<b>${escapeHtml(impact.monthlySaving)}</b>` : ''}</header><div><span>기존 <strong>${escapeHtml(impact.beforeMonthlyPayment)}</strong></span><i>→</i><span>계획 적용 후 <strong>${escapeHtml(impact.afterMonthlyPayment)}</strong></span></div><p>${[impact.repaymentPeriod && `상환 기간 ${impact.repaymentPeriod}`, impact.totalInterestDiff, impact.prepaymentFee].filter(Boolean).map(escapeHtml).join(' · ')}</p></article>`).join('')}</div></section>`;
    }

    function renderGrowth(plan) {
        const baseline = plan.baselineTimeline || [];
        const combo = plan.comboTimeline || [];
        if (!baseline.length || !combo.length) return '';
        return `<section><h3>성장 곡선 비교</h3><canvas class="report-futuresim-growth-canvas" aria-label="직접 모은 돈과 투자수익으로 구성된 성장 곡선 비교"></canvas><p class="report-futuresim-legend"><span class="report-futuresim-legend-baseline">기존 페이스</span><span class="report-futuresim-legend-contribution">내가 모은 돈</span><span class="report-futuresim-legend-return">투자수익</span><span class="report-futuresim-legend-goal">목표</span>${plan.benchmarkLabel ? `<span class="report-futuresim-legend-benchmark">${escapeHtml(plan.benchmarkLabel)} 중앙값</span>` : ''}</p></section>`;
    }

    function renderChart(container, card) {
        const canvas = container.querySelector('.report-futuresim-growth-canvas');
        const plan = card.futuresimPrintData;
        const baseline = plan.baselineTimeline || [];
        const combo = plan.comboTimeline || [];
        if (!canvas || !baseline.length || !combo.length) return;

        const width = Math.max(600, Math.round(canvas.getBoundingClientRect().width));
        const height = 220;
        const pixelRatio = window.devicePixelRatio || 1;
        canvas.width = width * pixelRatio;
        canvas.height = height * pixelRatio;
        canvas.style.height = `${height}px`;
        const context = canvas.getContext('2d');
        context.scale(pixelRatio, pixelRatio);

        const goal = numeric(plan.goalAmount);
        const goalPoint = combo.find((point) => Number(point.netWorth) >= goal) || combo[combo.length - 1];
        const maxMonth = Math.max(Number(baseline[baseline.length - 1].monthOffset), Number(goalPoint.monthOffset));
        const result = combo.filter((point) => Number(point.monthOffset) <= Number(goalPoint.monthOffset));
        const values = baseline.concat(result).map((point) => Number(point.netWorth));
        const benchmark = numeric(plan.benchmarkMedianNetWorth);
        const max = Math.max(goal, benchmark, ...values) * 1.08;
        const min = Math.min(0, ...values);
        const padding = {left: 12, right: 12, top: 16, bottom: 28};
        const x = (month) => padding.left + Number(month) / (maxMonth || 1) * (width - padding.left - padding.right);
        const y = (value) => padding.top + (height - padding.top - padding.bottom) - (Number(value) - min) / ((max - min) || 1) * (height - padding.top - padding.bottom);
        const baseY = y(Math.min(0, min));

        context.clearRect(0, 0, width, height);
        context.lineWidth = 1;
        context.strokeStyle = '#e6ebf1';
        context.fillStyle = '#7b8798';
        context.font = '10px sans-serif';
        context.textAlign = 'center';
        for (const month of timelineCheckpoints(maxMonth)) {
            const pointX = x(month);
            context.beginPath();
            context.moveTo(pointX, padding.top);
            context.lineTo(pointX, height - padding.bottom);
            context.stroke();
            context.fillText(duration(month), pointX, height - 7);
        }

        drawLine(context, padding.left, y(goal), width - padding.right, y(goal), '#8e7445', [5, 4], 1.4);
        if (benchmark > 0) drawLine(context, padding.left, y(benchmark), width - padding.right, y(benchmark), '#738fa6', [5, 4], 1.2);

        fillArea(context, result.map((point) => ({x: x(point.monthOffset), y: y(point.contributionAmount)})), baseY, '#315b8f');
        fillBand(context,
            result.map((point) => ({x: x(point.monthOffset), y: y(point.netWorth)})),
            result.map((point) => ({x: x(point.monthOffset), y: y(point.contributionAmount)})),
            '#9bb9de');
        drawCurve(context, baseline, x, y, '#9aa4b1', 2);
        drawCurve(context, result, x, y, '#274b7a', 2.8);

        const goalMarker = result.find((point) => Number(point.netWorth) >= goal) || result[result.length - 1];
        drawMarker(context, x(goalMarker.monthOffset), y(goalMarker.netWorth), '#274b7a');
        if (benchmark > 0 && plan.benchmarkCrossMonth !== null && plan.benchmarkCrossMonth !== undefined) {
            const benchmarkPoint = pointAt(result, plan.benchmarkCrossMonth);
            drawMarker(context, x(benchmarkPoint.monthOffset), y(benchmarkPoint.netWorth), '#738fa6');
        }
    }

    function fillArea(context, points, bottomY, color) {
        if (!points.length) return;
        context.beginPath();
        context.moveTo(points[0].x, bottomY);
        points.forEach((point) => context.lineTo(point.x, point.y));
        context.lineTo(points[points.length - 1].x, bottomY);
        context.closePath();
        context.fillStyle = color;
        context.fill();
    }

    function fillBand(context, topPoints, bottomPoints, color) {
        if (!topPoints.length) return;
        context.beginPath();
        topPoints.forEach((point, index) => index === 0 ? context.moveTo(point.x, point.y) : context.lineTo(point.x, point.y));
        bottomPoints.slice().reverse().forEach((point) => context.lineTo(point.x, point.y));
        context.closePath();
        context.fillStyle = color;
        context.fill();
    }

    function drawCurve(context, points, x, y, color, lineWidth) {
        if (!points.length) return;
        context.beginPath();
        points.forEach((point, index) => index === 0
            ? context.moveTo(x(point.monthOffset), y(point.netWorth))
            : context.lineTo(x(point.monthOffset), y(point.netWorth)));
        context.strokeStyle = color;
        context.lineWidth = lineWidth;
        context.lineJoin = 'round';
        context.lineCap = 'round';
        context.stroke();
    }

    function drawLine(context, startX, startY, endX, endY, color, dash, lineWidth) {
        context.save();
        context.beginPath();
        context.setLineDash(dash);
        context.moveTo(startX, startY);
        context.lineTo(endX, endY);
        context.strokeStyle = color;
        context.lineWidth = lineWidth;
        context.stroke();
        context.restore();
    }

    function drawMarker(context, markerX, markerY, color) {
        context.beginPath();
        context.arc(markerX, markerY, 5, 0, Math.PI * 2);
        context.fillStyle = color;
        context.fill();
        context.lineWidth = 2;
        context.strokeStyle = '#ffffff';
        context.stroke();
    }

    function renderTimeline(plan) {
        const baseline = plan.baselineTimeline || [];
        const combo = plan.comboTimeline || [];
        if (!baseline.length || !combo.length) return '';
        const goal = numeric(plan.goalAmount);
        const goalPoint = combo.find((point) => Number(point.netWorth) >= goal) || combo[combo.length - 1];
        return `<section><h3>타임라인 탐색기</h3><table class="report-futuresim-timeline"><thead><tr><th>시점</th><th>기존 페이스</th><th>레버 적용</th></tr></thead><tbody>${timelineCheckpoints(Number(goalPoint.monthOffset)).map((month) => `<tr><th>${month === 0 ? '지금' : `${duration(month)} 후`}</th><td>${timelineValue(pointAt(baseline, month))}</td><td>${timelineValue(pointAt(combo, month))}</td></tr>`).join('')}</tbody></table></section>`;
    }

    function renderMilestones(plan) {
        const points = plan.comboTimeline || [];
        if (!points.length) return '';
        const benchmark = plan.benchmarkCrossMonth === null || plan.benchmarkCrossMonth === undefined ? '' : `<li><strong>${duration(plan.benchmarkCrossMonth)} 후</strong><span>${escapeHtml(plan.benchmarkLabel || '또래')} 중앙값을 넘어서요</span></li>`;
        return `<ul class="report-futuresim-milestones"><li><strong>지금</strong><span>순자산 ${won(points[0].netWorth)}</span></li>${benchmark}<li><strong>${escapeHtml(plan.projectedDuration)} 후</strong><span>목표에 도달해요 (기존보다 ${escapeHtml(String(plan.diffLabel || '').replace('-', ''))})</span></li></ul>`;
    }

    function timelineValue(point) {
        return `<strong>${won(point.netWorth)}</strong><span>직접 모은 돈 ${won(point.contributionAmount)}</span><span>투자수익 ${won(point.returnAmount)}</span>`;
    }

    function timelineCheckpoints(goalMonth) {
        return Array.from(new Set([0, 1, 2, 3, 4].map((index) => Math.round(goalMonth * index / 4))));
    }

    function pointAt(points, month) {
        return points.find((point) => Number(point.monthOffset) === Number(month)) || points[points.length - 1];
    }

    function duration(months) {
        const value = Math.abs(Number(months));
        const years = Math.floor(value / 12);
        const rest = value % 12;
        return years ? `${years}년${rest ? ` ${rest}개월` : ''}` : `${rest}개월`;
    }

    function numeric(value) {
        return Number(String(value || '').replace(/[^0-9-]/g, '')) || 0;
    }

    function won(value) {
        return `${new Intl.NumberFormat('ko-KR').format(Math.round(Number(value) || 0))}원`;
    }

    function leverIcon(icon) {
        return ({trending_up: '↗', payments: '₩', event_repeat: '↔', add_card: '+'}[icon] || '•');
    }

    function escapeHtml(value) {
        return String(value ?? '').replace(/[&<>'"]/g, (character) => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'}[character]));
    }

    window.ReportFuturesimPdfCard = {render, renderChart};
})();
