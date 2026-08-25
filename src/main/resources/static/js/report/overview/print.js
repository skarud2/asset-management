(function () {

    function render(target, overview) {
        if (!target) {
            return;
        }

        if (!overview) {
            target.innerHTML = `
                <p class="report-overview-empty">
                    금융 정보를 불러오지 못했어요.
                </p>
            `;
            return;
        }

        target.innerHTML = `
            <div class="report-overview-list-wrap">
                ${renderFinancialProfile(overview.financialProfile)}
                ${renderMyData(overview.myData)}
            </div>
        `;
    }

    function renderFinancialProfile(profile) {
        if (!profile || !profile.available) {
            return `
                <article class="report-overview-card">
                    <div class="report-overview-card-header">
                        <div>
                            <span>PROFILE</span>
                            <h3>나의 금융 프로필</h3>
                        </div>
                    </div>

                    <div class="report-overview-unavailable">
                        <strong>등록된 금융 프로필이 없어요.</strong>
                        <p>금융 프로필을 등록하면 리포트에서 함께 확인할 수 있어요.</p>
                    </div>
                </article>
            `;
        }

        return `
            <article class="report-overview-card">
                <div class="report-overview-card-header">
                    <div>
                        <span>PROFILE</span>
                        <h3>나의 금융 프로필</h3>
                    </div>
                </div>

                <dl class="report-overview-list">
                    ${renderRow(
            '연 소득',
            profile.annualIncome
        )}

                    ${renderRow(
            '소득 유형',
            profile.incomeType
        )}

                    ${renderRow(
            '고용 상태',
            profile.employmentStatus
        )}

                    ${renderRow(
            '신용점수',
            profile.creditScore
        )}

                    ${renderRow(
            '현금성 자산',
            profile.liquidAssetAmount
        )}
                </dl>
            </article>
        `;
    }

    function renderMyData(myData) {
        if (!myData || !myData.connected) {
            return `
                <article class="report-overview-card">
                    <div class="report-overview-card-header">
                        <div>
                            <span>MYDATA</span>
                            <h3>마이데이터 금융 현황</h3>
                        </div>
                    </div>

                    <div class="report-overview-unavailable">
                        <strong>마이데이터가 아직 연동되지 않았어요.</strong>
                        <p>마이데이터를 연동하면 보유 자산과 대출 현황을 확인할 수 있어요.</p>
                    </div>
                </article>
            `;
        }

        return `
            <article class="report-overview-card">
                <div class="report-overview-card-header">
                    <div>
                        <span>MYDATA</span>
                        <h3>마이데이터 금융 현황</h3>
                    </div>
                </div>

                <div class="report-overview-mydata-summary">
                    ${renderSummary(
            '계좌',
            formatCount(myData.accountCount, '개'),
            myData.totalAccountBalance,
            '총 잔액'
        )}

                    ${renderSummary(
            '카드',
            formatCount(myData.cardCount, '개'),
            null,
            null
        )}

                    ${renderSummary(
            '대출',
            formatCount(myData.loanCount, '건'),
            myData.totalLoanBalance,
            '대출 잔액'
        )}
                </div>
            </article>
        `;
    }

    function renderRow(label, value) {
        return `
            <div>
                <dt>${escapeHtml(label)}</dt>
                <dd>${escapeHtml(value || '-')}</dd>
            </div>
        `;
    }

    function renderSummary(
        label,
        count,
        amount,
        amountLabel
    ) {
        return `
            <div class="report-overview-summary-item">
                <div class="report-overview-summary-main">
                    <span>${escapeHtml(label)}</span>
                    <strong>${escapeHtml(count)}</strong>
                </div>

                ${
            amount
                ? `
                            <div class="report-overview-summary-sub">
                                <span>${escapeHtml(amountLabel)}</span>
                                <b>${escapeHtml(amount)}</b>
                            </div>
                        `
                : ''
        }
            </div>
        `;
    }

    function formatCount(value, unit) {
        const number = Number(value);

        if (Number.isNaN(number)) {
            return '-';
        }

        return `${number.toLocaleString('ko-KR')}${unit}`;
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

    window.ReportOverview = {
        render
    };

})();