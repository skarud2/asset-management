(() => {
    const calculator = document.getElementById('surplusCalculator');
    if (!calculator) return;

    const accountChecks = Array.from(document.querySelectorAll('.account-check'));
    const selectedAccountBalance = document.getElementById('selectedAccountBalance');
    const withdrawableInput = document.getElementById('withdrawableAmount');
    const livingInput = document.getElementById('livingExpense');
    const scheduledInput = document.getElementById('scheduledExpense');
    const emergencyInput = document.getElementById('emergencyFund');
    const nextIncomeDateInput = document.getElementById('nextIncomeDate');
    const surplusDisplay = document.getElementById('calculatedSurplusAmount');
    const finalSurplusInput = document.getElementById('finalCalculatedSurplus');
    const breakWithdrawable = document.getElementById('breakWithdrawable');
    const breakLiving = document.getElementById('breakLiving');
    const breakScheduled = document.getElementById('breakScheduled');
    const breakEmergency = document.getElementById('breakEmergency');
    const breakTotal = document.getElementById('breakTotal');
    const operationAmountInput = document.getElementById('operationAmount');

    const toNumber = (input) => {
        const value = Number(input?.value);
        if (!Number.isFinite(value) || value < 0) return 0;
        return value;
    };

    const formatWon = (value) => `${Math.round(value).toLocaleString('ko-KR')}원`;

    const calculate = () => {
        const withdrawable = toNumber(withdrawableInput);
        const living = toNumber(livingInput);
        const scheduled = toNumber(scheduledInput);
        const emergency = toNumber(emergencyInput);

        const surplus = Math.max(0, withdrawable - living - scheduled - emergency);

        if (surplusDisplay) surplusDisplay.textContent = formatWon(surplus);
        if (breakWithdrawable) breakWithdrawable.textContent = formatWon(withdrawable);
        if (breakLiving) breakLiving.textContent = `-${formatWon(living)}`;
        if (breakScheduled) breakScheduled.textContent = `-${formatWon(scheduled)}`;
        if (breakEmergency) breakEmergency.textContent = `-${formatWon(emergency)}`;
        if (breakTotal) breakTotal.textContent = formatWon(surplus);
        if (finalSurplusInput) finalSurplusInput.value = String(surplus);

        if (typeof window.applySurplusFundAmount === 'function') {
            window.applySurplusFundAmount(surplus);
        }
    };

    const updateSelectedAccountBalance = () => {
        const selectedBalance = accountChecks
            .filter((check) => check.checked)
            .reduce((sum, check) => sum + (Number(check.dataset.balance) || 0), 0);

        if (withdrawableInput) withdrawableInput.value = String(selectedBalance);
        if (selectedAccountBalance) selectedAccountBalance.textContent = formatWon(selectedBalance);

        calculate();
    };

    accountChecks.forEach((check) => {
        check.addEventListener('change', updateSelectedAccountBalance);
    });

    [livingInput, scheduledInput, emergencyInput].forEach((input) => {
        input?.addEventListener('input', calculate);
    });

    window.saveSurplusFundCalculation = async () => {
        const selectedAccountIds = accountChecks
            .filter((check) => check.checked)
            .map((check) => Number(check.value));

        if (selectedAccountIds.length === 0) {
            throw new Error('여유자금 계산에 사용할 계좌를 선택해주세요.');
        }

        const requestBody = {
            selectedAccountIds,
            adjustedNextIncomeDate: nextIncomeDateInput?.value,
            adjustedLivingExpense: toNumber(livingInput),
            adjustedScheduledExpense: toNumber(scheduledInput),
            adjustedEmergencyFund: toNumber(emergencyInput),
            finalSurplusAmount: toNumber(operationAmountInput)
        };

        const csrfToken = document.getElementById('csrfToken')?.value;
        const csrfHeader = document.getElementById('csrfHeader')?.value;

        const headers = {
            'Content-Type': 'application/json'
        };

        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch('/api/surplus-funds/calculations', {
            method: 'POST',
            headers,
            credentials: 'same-origin',
            body: JSON.stringify(requestBody)
        });

        if (!response.ok) {
            const contentType = response.headers.get('content-type') || '';
            let message = '여유자금 계산 결과 저장에 실패했습니다.';

            if (contentType.includes('application/json')) {
                const body = await response.json();
                message = body.message || body.detail || message;
            }

            throw new Error(message);
        }

        return response.json();
    };

    updateSelectedAccountBalance();
})();