(function () {
    const loadingEl = document.getElementById('planListLoading');
    const emptyEl = document.getElementById('planListEmpty');
    const listEl = document.getElementById('planList');
    if (!listEl) {
        return;
    }

    function formatWon(amount) {
        return `${new Intl.NumberFormat('ko-KR').format(Math.round(Number(amount) || 0))}원`;
    }

    // 4/5단계 formatMonthsSplit()과 같은 규칙(N년 M개월) — 개월수 표기를 통일한다.
    function formatDuration(months) {
        if (months === null || months === undefined) {
            return '예측 어려움';
        }
        const abs = Math.round(Math.abs(Number(months)));
        const years = Math.floor(abs / 12);
        const rest = abs % 12;
        if (years > 0) {
            return rest > 0 ? `${years}년 ${rest}개월` : `${years}년`;
        }
        return `${rest}개월`;
    }

    function formatUpdatedAt(iso) {
        if (!iso) return '';
        const d = new Date(iso);
        if (Number.isNaN(d.getTime())) return '';
        return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')} 수정`;
    }

    fetch('/api/future-simulation/plans')
        .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
        .then((plans) => {
            loadingEl.classList.add('hidden');
            if (!plans || plans.length === 0) {
                emptyEl.classList.remove('hidden');
                return;
            }
            listEl.innerHTML = plans.map((plan) => {
                const diffText = plan.diffMonths > 0 ? ` · ${formatDuration(plan.diffMonths)} 단축` : '';
                return '<li class="fp-plan-list-item">' +
                    '<div class="fp-plan-list-info">' +
                        `<strong>${plan.planName}</strong>` +
                        `<span>목표 ${formatWon(plan.goalAmount)} · 예상 도달 ${formatDuration(plan.projectedMonthsToGoal)}${diffText}</span>` +
                        `<span class="fp-plan-list-updated">${formatUpdatedAt(plan.updatedAt)}</span>` +
                    '</div>' +
                    `<a class="fp-plan-list-load" href="/futuresim/plans/${plan.id}/load">불러오기</a>` +
                    '</li>';
            }).join('');
            listEl.classList.remove('hidden');
        })
        .catch(() => {
            loadingEl.textContent = '불러오지 못했어요';
        });
})();
