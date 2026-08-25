(function () {
    const section = document.getElementById('myLoansSection');
    if (!section) return;

    const listEl = document.getElementById('myLoansList');
    const targetInputId = section.dataset.targetInput;

    const LOAN_TYPE_LABELS = {
        MORTGAGE_LOAN: '주택담보대출',
        CREDIT_LOAN: '신용대출',
        JEONSE_LOAN: '전세자금대출',
        STUDENT_LOAN: '학자금대출'
    };

    const RATE_TYPE_LABELS = {
        FIXED: '고정금리',
        VARIABLE: '변동금리'
    };

    function formatNumber(value) {
        return new Intl.NumberFormat('ko-KR').format(value);
    }

    function renderEmpty(message) {
        listEl.innerHTML = `<p class="my-loans-empty">${message}</p>`;
    }

    function renderLoans(loans) {
        listEl.innerHTML = '';

        loans.forEach((loan) => {
            const item = document.createElement('button');
            item.type = 'button';
            item.className = 'my-loan-item';
            if (String(loan.loanAccountId) === String(section.dataset.activeLoanId)) {
                item.classList.add('active');
            }

            const typeLabel = LOAN_TYPE_LABELS[loan.loanType] || loan.loanType;
            const rateLabel = RATE_TYPE_LABELS[loan.rateType] || loan.rateType;

            item.innerHTML = `
                <span class="my-loan-type">${typeLabel}</span>
                <span class="my-loan-detail">${rateLabel} · ${loan.interestRate}% · 잔액 ${formatNumber(loan.currentBalance)}원</span>
            `;

            item.addEventListener('click', () => {
                if (targetInputId) {
                    const input = document.getElementById(targetInputId);
                    if (input) {
                        input.value = loan.loanAccountId;
                        input.dispatchEvent(new Event('change'));
                    }
                }
                listEl.querySelectorAll('.my-loan-item').forEach((el) => el.classList.remove('active'));
                item.classList.add('active');
            });

            listEl.appendChild(item);
        });
    }

    fetch('/api/mydata/loans')
        .then((res) => (res.ok ? res.json() : []))
        .then((loans) => {
            if (!loans || loans.length === 0) {
                renderEmpty('아직 연동된 보유 대출이 없어요. <a href="/api/mydata/oauth/authorize">마이데이터 연동하기</a>');
                return;
            }
            renderLoans(loans);
        })
        .catch(() => {
            renderEmpty('보유 대출 정보를 불러오지 못했어요.');
        });
})();
