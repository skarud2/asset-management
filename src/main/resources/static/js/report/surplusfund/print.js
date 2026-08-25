(function () {

    function render(card) {
        const data = card.surplusFundPrintData;

        if (!data) {
            return `
                <article class="report-surplusfund-document">
                    <p class="report-surplusfund-empty">
                        저장된 여유자금 운용 기록이 없습니다.
                    </p>
                </article>
            `;
        }

        return `
            <article class="report-surplusfund-document">
                ${renderHeader(data)}
                ${renderCalculation(data)}
                ${renderAllocation(data)}
                ${renderReasons(data)}
                ${renderEtfs(data)}
            </article>
        `;
    }

    function renderHeader(data) {
        return `
            <header class="report-surplusfund-header">
                <p class="report-surplusfund-eyebrow">
                    여유자금 운용 · 저장한 운용 기록
                </p>

                <h2>
                    ${escapeHtml(data.guideName || `운용 기록 ${data.guideVersionNo}`)}
                </h2>

                <div class="report-surplusfund-meta">
                    <span>
                        저장 ${escapeHtml(data.savedAt || '-')}
                    </span>

                    <span>
                        투자 성향 ${escapeHtml(data.investmentStyle || '-')}
                    </span>
                </div>
            </header>
        `;
    }

    function renderCalculation(data) {
        return `
            <section class="report-surplusfund-section">
                <h3>여유자금 계산 결과</h3>

                <div class="report-surplusfund-main-amount">
                    <span>최종 운용 가능 금액</span>
                    <strong>
                        ${escapeHtml(data.finalSurplusAmount || '-')}
                    </strong>
                </div>

                <dl class="report-surplusfund-calculation">
                    <div>
                        <dt>선택 계좌 잔액</dt>
                        <dd>${escapeHtml(data.selectedAccountBalance || '-')}</dd>
                    </div>

                    <div>
                        <dt>예상 생활비</dt>
                        <dd>${escapeHtml(data.livingExpense || '-')}</dd>
                    </div>

                    <div>
                        <dt>예정 지출</dt>
                        <dd>${escapeHtml(data.scheduledExpense || '-')}</dd>
                    </div>

                    <div>
                        <dt>비상금</dt>
                        <dd>${escapeHtml(data.emergencyFund || '-')}</dd>
                    </div>
                </dl>
            </section>
        `;
    }

    function renderAllocation(data) {
        const allocations = data.allocations || [];

        if (allocations.length === 0) {
            return '';
        }

        return `
            <section class="report-surplusfund-section">
                <h3>자산배분 결과</h3>

                <div class="report-surplusfund-allocation">
                    ${allocations.map((allocation) => `
                        <article
                            class="report-surplusfund-allocation-item"
                            data-asset-type="${escapeHtml(allocation.assetType || '')}"
                        >
                            <div class="report-surplusfund-allocation-head">
                                <strong>
                                    ${escapeHtml(allocation.label || allocation.assetType || '-')}
                                </strong>

                                <span>
                                    ${escapeHtml(allocation.ratio || '-')}
                                </span>
                            </div>

                            <p>
                                ${escapeHtml(allocation.amount || '-')}
                            </p>

                            <div class="report-surplusfund-allocation-track">
                                <span
                                    style="width:${ratioNumber(allocation.ratio)}%"
                                ></span>
                            </div>
                        </article>
                    `).join('')}
                </div>

                <p class="report-surplusfund-note">
                    내부 규칙에 따른 교육용 자산군 배분 예시입니다.
                    수익을 보장하는 투자 포트폴리오가 아닙니다.
                </p>
            </section>
        `;
    }

    function renderReasons(data) {
        const reasons = data.reasons || [];

        if (reasons.length === 0) {
            return '';
        }

        return `
            <section class="report-surplusfund-section">
                <h3>이렇게 배분했어요</h3>

                <ul class="report-surplusfund-reasons">
                    ${reasons.map((reason) => `
                        <li>
                            ${escapeHtml(reason)}
                        </li>
                    `).join('')}
                </ul>
            </section>
        `;
    }

    function renderEtfs(data) {
        const etfs = data.interestedEtfs || [];

        if (etfs.length === 0) {
            return `
                <section class="report-surplusfund-section">
                    <h3>관심 ETF</h3>

                    <p class="report-surplusfund-empty">
                        저장한 관심 ETF가 없습니다.
                    </p>
                </section>
            `;
        }

        return `
            <section class="report-surplusfund-section">
                <h3>관심 ETF</h3>

                <div class="report-surplusfund-etfs">
                    ${etfs.map((etf) => renderEtf(etf)).join('')}
                </div>

                <p class="report-surplusfund-note">
                    표시된 가격 정보는 운용 기록 저장 당시의 스냅샷입니다.
                </p>
            </section>
        `;
    }

    function renderEtf(etf) {
        const fluctuationClass = rateClass(etf.fluctuationRate);

        return `
            <article class="report-surplusfund-etf">
                <div class="report-surplusfund-etf-title">
                    <span>
                        ${escapeHtml(etf.selectionOrder || '')}
                    </span>

                    <div>
                        <strong>
                            ${escapeHtml(etf.productName || '-')}
                        </strong>

                        <small>
                            ${escapeHtml(etf.productCode || '-')}
                        </small>
                    </div>
                </div>

                <dl>
                    <div>
                        <dt>기준일</dt>
                        <dd>
                            ${escapeHtml(etf.priceBaseDate || '-')}
                        </dd>
                    </div>

                    <div>
                        <dt>종가</dt>
                        <dd>
                            ${escapeHtml(etf.closingPrice || '-')}
                        </dd>
                    </div>

                    <div>
                        <dt>등락률</dt>
                        <dd class="${fluctuationClass}">
                            ${escapeHtml(etf.fluctuationRate || '-')}
                        </dd>
                    </div>
                </dl>
            </article>
        `;
    }

    function ratioNumber(value) {
        const number = Number(
            String(value || '')
                .replace('%', '')
                .trim()
        );

        if (Number.isNaN(number)) {
            return 0;
        }

        return Math.min(100, Math.max(0, number));
    }

    function rateClass(value) {
        const number = Number(
            String(value || '')
                .replace('%', '')
                .replace('+', '')
                .trim()
        );

        if (Number.isNaN(number) || number === 0) {
            return '';
        }

        return number > 0
            ? 'report-surplusfund-rate-up'
            : 'report-surplusfund-rate-down';
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(
                /[&<>'"]/g,
                (character) => ({
                    '&': '&amp;',
                    '<': '&lt;',
                    '>': '&gt;',
                    "'": '&#39;',
                    '"': '&quot;'
                }[character])
            );
    }

    window.ReportSurplusFundPdfCard = {
        render
    };

})();