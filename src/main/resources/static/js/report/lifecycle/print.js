(function () {
    function render(card) {
        const rows = card.detailRows || [];
        const expenseSectionIndex = rows.findIndex((row) => row.label === '시나리오 순서별 지출 금액');
        const monthlyBreakdownSectionIndex = rows.findIndex((row) => row.label === '월 지출 상세 구성');
        const costSectionIndex = rows.findIndex((row) => row.label === '시나리오 순서별 소요 비용');
        const oneTimeBreakdownSectionIndex = rows.findIndex((row) => row.label === '일회성 비용 상세 구성');
        const analysisSectionIndex = rows.findIndex((row) => row.label === '상세 분석 보고서');

        const expenses = expenseSectionIndex < 0 ? [] : rows.slice(expenseSectionIndex + 1, monthlyBreakdownSectionIndex >= 0 ? monthlyBreakdownSectionIndex : costSectionIndex);
        const monthlyBreakdownRows = monthlyBreakdownSectionIndex < 0 ? [] : rows.slice(monthlyBreakdownSectionIndex + 1, costSectionIndex);
        const costs = costSectionIndex < 0 ? [] : rows.slice(costSectionIndex + 1, oneTimeBreakdownSectionIndex >= 0 ? oneTimeBreakdownSectionIndex : (analysisSectionIndex < 0 ? rows.length : analysisSectionIndex));
        const oneTimeBreakdownRows = oneTimeBreakdownSectionIndex < 0 ? [] : rows.slice(oneTimeBreakdownSectionIndex + 1, analysisSectionIndex);
        const analysis = analysisSectionIndex < 0 ? [] : rows.slice(analysisSectionIndex + 1);

        const titleParts = String(card.title || '').split(/\s*·\s*/);
        const reportEyebrow = titleParts[0] || '금융 라이프 플랜';
        const reportTitle = titleParts.slice(1).join(' · ') || '나의 미래 라이프 플랜';

        const eventGroups = new Map();
        analysis.forEach((row) => {
            const match = String(row.label || '').match(/^STEP\s+(\d+)/i);
            if (!match) return;
            const step = Number(match[1]);
            if (!eventGroups.has(step)) eventGroups.set(step, []);
            eventGroups.get(step).push(row);
        });

        const expensesHtml = renderExpenses(expenses, monthlyBreakdownRows);
        const costsHtml = renderCosts(costs, oneTimeBreakdownRows);

        let childbirthRecommendationsShown = false;
        const eventsHtml = Array.from(eventGroups.entries()).map(([step, group], order) => {
            const cost = costs[order];
            const parsed = parseLifecycleStep(cost?.label, step);
            let visibleGroup = group;
            const isChildbirth = parsed.event.includes('출산') || group.some((row) => /출산/.test(String(row.label || '')));
            if (isChildbirth) {
                const recPattern = /추천\s*금융\s*상품|맞춤\s*복지\s*혜택|·\s*(상품|복지)\s+/;
                const hasRecs = group.some((row) => recPattern.test(String(row.label || '')));
                if (hasRecs && childbirthRecommendationsShown) {
                    visibleGroup = group.filter((row) => !recPattern.test(String(row.label || '')));
                } else if (hasRecs) {
                    childbirthRecommendationsShown = true;
                }
            }

            let amortizationRows = visibleGroup.filter((row) => /·\s*원금균등상환 추이\s*·/.test(String(row.label || '')));
            if (!amortizationRows.length) {
                amortizationRows = buildEqualPrincipalChartRows(visibleGroup, step, parsed.event);
            }

            return renderEventModal(visibleGroup, step, parsed.event, parsed.date, cost?.value, amortizationRows);
        }).join('');

        return `
            <article class="report-lifecycle-document">
                <header class="report-lifecycle-hero">
                    <span>${escapeHtml(reportEyebrow)}</span>
                    <h1>${escapeHtml(reportTitle)}</h1>
                    <p>시나리오 비용 분석 보고서 (최종 순자산: ${escapeHtml(card.headlineValue || '-')})</p>
                </header>

                ${expensesHtml}
                <section class="report-lifecycle-section report-lifecycle-detail-section">
                    <div class="report-lifecycle-section-title">
                        <span>${expenses.length ? '02' : '01'}</span>
                        <div>
                            <h3>상세 분석 보고서</h3>
                            <p>이벤트별 세부 산출 내역 및 자금 조달 분석 정보입니다.</p>
                        </div>
                    </div>
                    <div class="report-lifecycle-events">
                        ${eventsHtml}
                    </div>
                </section>
            </article>
        `;
    }

    function renderExpenses(expenses, monthlyBreakdown) {
        const visible = expenses.filter((r) => parseReportMoney(r.value) > 0);
        if (!visible.length) return '';
        const chart = renderBarChart(visible, '월 지출');
        return `
            <section class="report-lifecycle-section report-lifecycle-monthly-summary">
                <div class="report-lifecycle-section-title">
                    <span>01</span>
                    <div>
                        <h3>시나리오 순서별 월 지출 금액</h3>
                        <p>이벤트별 월 총액(생활비·대출상환액) 변화 추이입니다.</p>
                    </div>
                </div>
                ${chart}
            </section>
        `;
    }

    function renderCosts(costs, oneTimeBreakdown) {
        if (!costs || !costs.length) return '';
        const chart = renderBarChart(costs, '소요 비용');
        const table = renderBreakdownTable(costs, oneTimeBreakdown);
        return `
            <section class="report-lifecycle-section report-lifecycle-one-time-summary">
                <div class="report-lifecycle-section-title">
                    <span>02</span>
                    <div>
                        <h3>시나리오 순서별 소요 비용</h3>
                        <p>차트는 이벤트별 총액, 표는 총액의 세부 구성을 보여줍니다.</p>
                    </div>
                </div>
                ${chart}
                ${table}
            </section>
        `;
    }

    function renderBarChart(rows, valueLabel) {
        if (!rows || !rows.length) return '';
        const values = rows.map((row) => parseReportMoney(row.value));
        const max = Math.max(...values, 1);
        const bars = rows.map((row, index) => {
            const parsed = parseLifecycleStep(row.label, index + 1);
            const height = values[index] <= 0 ? 3 : Math.max(10, Math.round(values[index] / max * 100));
            return `
                <div class="report-lifecycle-chart-item" title="${escapeHtml(`${parsed.event} ${valueLabel} ${row.value}`)}">
                    <div class="report-lifecycle-chart-value">${escapeHtml(row.value)}</div>
                    <div class="report-lifecycle-chart-track"><span style="height:${height}%"></span></div>
                    <b>STEP ${parsed.step}</b><small>${escapeHtml(parsed.event)}</small>
                </div>
            `;
        }).join('');
        return `<div class="report-lifecycle-chart" role="img" aria-label="${escapeHtml(valueLabel)} 단계별 막대 차트">${bars}</div>`;
    }

    function renderBreakdownTable(totals, breakdownRows) {
        const groups = totals.map((total) => {
            const parsed = parseLifecycleStep(total.label, 1);
            const components = (breakdownRows || []).filter((row) => {
                const rParsed = parseLifecycleStep(row.label, 1);
                return rParsed.step === parsed.step;
            });
            const componentRows = components.map((row) => {
                const parts = String(row.label || '').split(/\s*[·ㆍ]\s*/);
                const itemName = parts[2] || parts[parts.length - 1];
                return `<tr><td></td><td>${escapeHtml(itemName)}</td><td>${escapeHtml(row.value)}</td></tr>`;
            }).join('');
            return `<tbody><tr class="is-total"><th>STEP ${parsed.step} · ${escapeHtml(parsed.event)}</th><th>총액</th><td>${escapeHtml(total.value)}</td></tr>${componentRows}</tbody>`;
        }).join('');
        return groups ? `<table class="report-lifecycle-breakdown-table"><thead><tr><th>이벤트</th><th>산출 항목</th><th>금액</th></tr></thead>${groups}</table>` : '';
    }

    function renderEventModal(rows, step, event, date, totalCostValue, amortizationRows) {
        const icon = getEventIcon(event);
        const findVal = (pattern) => rows.find((r) => pattern.test(String(r.label || '')) && r.value)?.value || '';

        const totalCost = totalCostValue || findVal(/총 필요자금/) || findVal(/직접 입력 결혼비용/) || findVal(/이벤트 비용/) || '-';
        const isHomePurchase = event.includes('주택') || event.includes('매입');
        const eventCostLabel = isHomePurchase ? '주택 매입 총 필요자금(매입가+취득세)' : '일회성 총비용';
        const requiredAmountLabel = isHomePurchase ? '대출 제외 필요 현금' : '최종 본인 필요 자금';
        const loanAmountLabel = isHomePurchase ? '주택담보대출 실행액(전체 원금)' : '필요 대출금액';

        // 1. 세부 산출 내역
        const calcRows = rows.filter((r) => {
            const label = trimStepPrefix(r.label, step, event);
            return /예식장|식대|혼수|신혼여행|산후조리|카시트|유모차|아기침대|기타 준비물|자산가격|매입·취득|취득세|세금|등기비|중개보수/.test(label)
                && r.value && !/총 필요자금/.test(label);
        });
        const userContribution = findVal(/입력 자기자금/) || findVal(/본인 분담/);
        const familySupport = findVal(/가족 지원금/);
        const userRequired = findVal(/본인 필요자금/) || findVal(/최종 본인 필요 자금/);
        const newLoan = findVal(/신규 대출/);
        const monthlyExpense = findVal(/^월 지출$/);
        const monthlyLoanPayment = findVal(/월 대출상환/);

        let calcDetailsHtml = '';
        if (calcRows.length > 0) {
            const itemsHtml = calcRows.map((r) => {
                const label = trimStepPrefix(r.label, step, event).replace(/^·\s*/, '');
                return `<div class="calc-item-row"><span class="calc-name">${escapeHtml(label)}</span><strong class="calc-price">${escapeHtml(r.value)}</strong></div>`;
            }).join('');

            calcDetailsHtml = `
                <div class="result-calc-breakdown">
                    <div class="result-calc-list">
                        ${itemsHtml}
                        <div class="calc-divider-line"></div>
                        <div class="calc-item-row is-total-row">
                            <span class="calc-total-label">총 예상 소요 비용</span>
                            <strong class="calc-total-val">${escapeHtml(totalCost)}</strong>
                        </div>
                    </div>
                    <div class="result-funding-summary">
                        ${userContribution ? `<div class="funding-pill"><span>본인 분담(자기자금)</span><b>${escapeHtml(userContribution)}</b></div>` : ''}
                        ${familySupport ? `<div class="funding-pill"><span>가족 지원금</span><b class="is-minus">-${escapeHtml(familySupport)}</b></div>` : ''}
                        ${userRequired ? `<div class="funding-pill is-final-target"><span>${escapeHtml(requiredAmountLabel)}</span><b class="final-val">${escapeHtml(userRequired)}</b></div>` : ''}
                    </div>
                </div>
            `;
        }

        // 2. PLAN CHECK 타당성 진단
        const planTitle = findVal(/진행 가능 여부/) || '';
        const planMsg = findVal(/계획 진단/) || '';
        const shortage = findVal(/부족 현금/);
        const delay = findVal(/권장 연기/);
        let feasibilityHtml = '';
        if (planTitle || planMsg) {
            const isDanger = /어려움|부족|경고/.test(planTitle + planMsg);
            const statusClass = isDanger ? 'danger' : 'success';
            feasibilityHtml = `
                <section class="lifecycle-feasibility ${statusClass}">
                    <span>PLAN CHECK</span>
                    <h4>${escapeHtml(planTitle || '계획 분석')}</h4>
                    <p>${escapeHtml(planMsg)}</p>
                    ${delay ? `<strong>권장 준비기간: ${escapeHtml(delay)}</strong>` : ''}
                    ${parseReportMoney(shortage) > 0 ? `<strong>부족 자금: ${escapeHtml(shortage)}</strong>` : ''}
                </section>
            `;
        }

        // 3. 비용 및 자금 조달 요약 그리드
        const gridItems = [];
        if (totalCost && totalCost !== '-') gridItems.push({label: eventCostLabel, value: totalCost});
        if (userContribution) gridItems.push({label: '본인 분담(자기자금)', value: userContribution});
        if (familySupport) gridItems.push({label: '가족 지원금', value: familySupport});
        if (userRequired) gridItems.push({label: requiredAmountLabel, value: userRequired});
        if (newLoan && parseReportMoney(newLoan) > 0) gridItems.push({label: loanAmountLabel, value: newLoan});
        if (monthlyExpense && parseReportMoney(monthlyExpense) > 0) gridItems.push({label: '월 고정비/생활비', value: monthlyExpense});
        if (monthlyLoanPayment && parseReportMoney(monthlyLoanPayment) > 0) gridItems.push({label: '월 대출 상환액(원금+이자)', value: monthlyLoanPayment});

        const netAssetChange = findVal(/순자산 변화/);
        if (netAssetChange) gridItems.push({label: '순자산 변화', value: netAssetChange});
        const dsrChange = findVal(/DSR 변화/) || findVal(/이벤트 후 DSR/);
        if (dsrChange) gridItems.push({label: '예상 DSR', value: dsrChange});

        const summaryGridHtml = gridItems.map((item) =>
            `<article><span>${escapeHtml(item.label)}</span><strong>${escapeHtml(item.value)}</strong></article>`
        ).join('');



        let amortizationPageHtml = '';
        if (amortizationRows && amortizationRows.length > 0) {
            const chartHtml = renderAmortizationChart(amortizationRows);
            if (chartHtml) {
                amortizationPageHtml = `
                    <article class="report-lifecycle-event report-lifecycle-modal-page report-lifecycle-amortization-page">
                        <div class="report-lifecycle-detail-page-heading">
                            <span>MY FINANCIAL LIFE PLAN · 대출 상환 계획</span>
                            <b>STEP ${step}</b>
                        </div>
                        <div class="lifecycle-snapshot-modal-panel report-pdf-modal-panel">
                            <div class="lifecycle-snapshot-modal-body report-pdf-modal-body">
                                <div class="lifecycle-modal-result-wrapper report-modal-result-wrapper">
                                    <div class="modal-event-hero">
                                        <span class="modal-event-icon">📉</span>
                                        <div class="modal-event-hero-info">
                                            <div class="modal-event-hero-badges">
                                                <span class="snapshot-step-badge">STEP ${step}</span>
                                                <span class="modal-date-badge">원금균등상환</span>
                                            </div>
                                            <h3>${escapeHtml(event)} 대출 상환 상세 추이</h3>
                                        </div>
                                    </div>
                                    <section class="lifecycle-modal-block">
                                        ${chartHtml}
                                    </section>
                                </div>
                            </div>
                        </div>
                    </article>
                `;
            }
        }

        const mainModalHtml = `
            <article class="report-lifecycle-event report-lifecycle-modal-page">
                <div class="report-lifecycle-detail-page-heading">
                    <span>MY FINANCIAL LIFE PLAN</span>
                    <b>STEP ${step}</b>
                </div>
                <div class="lifecycle-snapshot-modal-panel report-pdf-modal-panel">
                    <div class="lifecycle-snapshot-modal-body report-pdf-modal-body">
                        <div class="lifecycle-modal-result-wrapper report-modal-result-wrapper">
                            <div class="modal-event-hero">
                                <span class="modal-event-icon">${icon}</span>
                                <div class="modal-event-hero-info">
                                    <div class="modal-event-hero-badges">
                                        <span class="snapshot-step-badge">STEP ${step}</span>
                                        ${date ? `<span class="modal-date-badge">${escapeHtml(date)}</span>` : ''}
                                    </div>
                                    <h3>${escapeHtml(event)} 상세 분석 보고서</h3>
                                </div>
                                <div class="modal-hero-cost">
                                    <span>${escapeHtml(eventCostLabel)}</span>
                                    <strong>${escapeHtml(totalCost)}</strong>
                                </div>
                            </div>

                            ${calcDetailsHtml ? `
                                <section class="lifecycle-modal-block">
                                    <h4>📋 세부 산출 내역</h4>
                                    ${calcDetailsHtml}
                                </section>
                            ` : ''}

                            ${feasibilityHtml}

                            ${summaryGridHtml ? `
                                <section class="lifecycle-modal-block">
                                    <h4>💰 비용 및 자금 조달 요약</h4>
                                    <div class="lifecycle-modal-grid">
                                        ${summaryGridHtml}
                                    </div>
                                </section>
                            ` : ''}
                        </div>
                    </div>
                </div>
            </article>
        `;

        return `${mainModalHtml}${amortizationPageHtml}`;
    }

    function renderAmortizationChart(rows) {
        if (!rows || !rows.length) return '';
        const values = rows.map((row) => parseReportMoney(row.value));
        const max = Math.max(...values, 1);
        const bars = rows.map((row, index) => {
            const year = String(row.label || '').match(/(\d+)년\s*차/)?.[1] || index + 1;
            const height = Math.max(10, Math.round(values[index] / max * 100));
            return `<div class="report-amortization-bar"><span>${escapeHtml(row.value)}</span><div><i style="height:${height}%"></i></div><b>${escapeHtml(year)}년 차</b></div>`;
        }).join('');
        return `<section class="report-amortization"><div class="report-amortization-title"><b>원금균등상환 월 납입액 변화</b><span>원금이 줄어들수록 월 납입액이 감소합니다.</span></div><div class="report-amortization-chart">${bars}</div></section>`;
    }

    function buildEqualPrincipalChartRows(rows, step, event) {
        const findRow = (pattern) => rows.find((row) => pattern.test(String(row.label || '')));
        const repayment = findRow(/상환방식|상환\s*방식|대출상환/);
        const repaymentText = String(repayment?.value || '') + ' ' + String(findRow(/원금균등/)?.label || '');
        if (!/원금균등/.test(repaymentText)) return [];

        const principal = parseReportMoney(findRow(/신규\s*대출/)?.value);
        const monthlyPrincipal = parseReportMoney(findRow(/원금\s*상환/)?.value);
        const firstInterest = parseReportMoney(findRow(/이자\s*납부/)?.value);
        const periodText = String(findRow(/대출기간|기간/)?.value || '');
        const months = Number(periodText.match(/(\d+)개월/)?.[1] || 0) || (Number(periodText.match(/(\d+)년/)?.[1] || 0) * 12) || 360;
        if (!principal) return [];

        const principalPerMonth = monthlyPrincipal || (principal / months);
        const monthlyRate = firstInterest > 0 ? (firstInterest / principal) : (0.04 / 12);
        const totalYears = Math.ceil(months / 12);
        return [1, 5, 10, 15, 20, 25, 30, 35, 40]
            .filter((year) => year <= totalYears)
            .map((year) => {
                const month = Math.min((year - 1) * 12 + 1, months);
                const remaining = Math.max(0, principal - principalPerMonth * (month - 1));
                const payment = principalPerMonth + remaining * monthlyRate;
                return {
                    label: `STEP ${step} · ${event} · 원금균등상환 추이 · ${year}년 차`,
                    value: `${Math.round(payment).toLocaleString('ko-KR')}원/월`
                };
            });
    }

    function getEventIcon(eventName) {
        const text = String(eventName || '').toLowerCase();
        if (text.includes('결혼') || text.includes('marriage')) return '💍';
        if (text.includes('출산') || text.includes('childbirth')) return '👶';
        if (text.includes('차량') || text.includes('자동차') || text.includes('vehicle')) return '🚗';
        if (text.includes('월세') || text.includes('rent')) return '🏢';
        if (text.includes('전세') || text.includes('jeonse')) return '🏡';
        if (text.includes('주택') || text.includes('집') || text.includes('home')) return '🏠';
        if (text.includes('상환') || text.includes('대출') || text.includes('repayment')) return '💳';
        return '📅';
    }

    function parseLifecycleStep(label, fallbackStep) {
        const text = String(label || '');
        const parts = text.split(/\s*[·ㆍ]\s*/).filter(Boolean);
        const stepMatch = (parts[0] || text).match(/STEP\s+(\d+)/i);
        return {
            step: stepMatch ? Number(stepMatch[1]) : fallbackStep,
            event: parts[1] || `이벤트 ${fallbackStep}`,
            date: parts.slice(2).join(' · ')
        };
    }

    function trimStepPrefix(label, step, event) {
        return String(label || '')
            .replace(new RegExp(`^STEP\\s+${step}\\s*[·ㆍ]?\\s*`), '')
            .replace(new RegExp(`^${escapeRegExp(event)}\\s*[·ㆍ]?\\s*`), '')
            .replace(/^분석$/, '분석');
    }

    function parseReportMoney(value) {
        if (!value) return 0;
        const text = String(value || '').replace(/,/g, '');
        const eok = Number(text.match(/(-?[\d.]+)억/)?.[1] || 0) * 100000000;
        const man = Number(text.match(/(-?[\d.]+)만/)?.[1] || 0) * 10000;
        if (eok || man) return Math.abs(eok + man);
        return Math.abs(Number(text.replace(/[^\d.-]/g, '')) || 0);
    }

    function escapeRegExp(value) {
        return String(value || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    }

    function escapeHtml(value) {
        if (value == null) return '';
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    window.ReportLifecyclePdfCard = {
        render: render
    };
})();
