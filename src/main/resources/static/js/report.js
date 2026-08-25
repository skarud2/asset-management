(function () {
    const MAX_CARDS = 4;

    const headerLoadingEl = document.getElementById('reportHeaderLoading');
    const headerBoxEl = document.getElementById('reportHeaderBox');
    const totalScoreEl = document.getElementById('reportTotalScore');
    const gradeEl = document.getElementById('reportGrade');
    const barListEl = document.getElementById('reportBarList');
    const gridEl = document.getElementById('reportCardGrid');
    const modalBackdropEl = document.getElementById('reportModalBackdrop');
    const modalTitleEl = document.getElementById('reportModalTitle');
    const modalListEl = document.getElementById('reportModalList');
    const modalCloseEl = document.getElementById('reportModalClose');
    const overviewEl = document.getElementById('reportOverview');
    if (!gridEl) {
        return;
    }

    const csrfHeaderName = document.getElementById('csrfHeaderName').value;
    const csrfTokenValue = document.getElementById('csrfTokenValue').value;
    const pdfMode = gridEl.closest('[data-pdf-mode]')?.dataset.pdfMode === 'true';

    let cards = [];
    let options = [];

    function authHeaders() {
        return {'Content-Type': 'application/json', [csrfHeaderName]: csrfTokenValue};
    }

    function loadHeader() {
        return fetch('/api/report/header')
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
            .then((header) => {
                headerLoadingEl.classList.add('hidden');
                headerBoxEl.classList.remove('hidden');
                totalScoreEl.textContent = Math.round(Number(header.totalScore));
                gradeEl.textContent = header.grade;
                barListEl.innerHTML = header.dimensions.map((dimension) => {
                    const score = Math.round(Number(dimension.score));
                    return '<li class="report-bar-item">' +
                        `<span class="report-bar-label">${dimension.label} <b>${score}점</b></span>` +
                        '<span class="report-bar-track">' +
                            `<span class="report-bar-fill" style="width:${score}%"></span>` +
                        '</span>' +
                        '</li>';
                }).join('');

                if (window.ReportOverview && overviewEl) {
                    window.ReportOverview.render(
                        overviewEl,
                        header.overview
                    );
                }
            })
            .catch(() => {
                headerLoadingEl.textContent = '불러오지 못했어요';
            });
    }

    function renderGrid() {
        const slots = pdfMode ? cards : [];
        if (!pdfMode) {
            for (let i = 0; i < MAX_CARDS; i += 1) {
                slots.push(cards[i] || null);
            }
        }
        if (pdfMode && slots.length === 0) {
            gridEl.innerHTML = '<p class="report-card-loading">선택한 카드가 없어요.</p>';
            return Promise.resolve();
        }
        gridEl.innerHTML = slots.map((card, index) => card
            ? `<div class="report-card-slot report-card-filled" data-index="${index}"><p class="report-card-loading">불러오는 중…</p></div>`
            : `<button type="button" class="report-card-slot report-card-empty" data-index="${index}">+ 카드 추가</button>`
        ).join('');

        if (!pdfMode) gridEl.querySelectorAll('.report-card-empty').forEach((el) => {
            el.addEventListener('click', () => openAddCardModal());
        });

        return Promise.all(slots.filter(Boolean).map((card, index) => {
            const cardIndex = slots.indexOf(card, index);
            const query = card.refId
                ? `cardKey=${encodeURIComponent(card.cardKey)}&refId=${encodeURIComponent(card.refId)}`
                : `cardKey=${encodeURIComponent(card.cardKey)}`;
            return fetch(`/api/report/card-data?${query}`)
                .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
                .then((data) => renderCard(cardIndex, card.cardKey, data))
                .catch(() => renderCardError(cardIndex));
        }));
    }

    function renderCard(index, cardKey, card) {
        const slot = gridEl.querySelector(`.report-card-slot[data-index="${index}"]`);
        if (!slot) {
            return;
        }
        if (pdfMode) {
            slot.className = 'report-card-slot report-card-filled report-pdf-card-page';
            if (cardKey === 'FUTURESIM' && card.futuresimPrintData) {
                slot.innerHTML = window.ReportFuturesimPdfCard.render(card);
                window.ReportFuturesimPdfCard.renderChart(slot, card);
            } else if (cardKey === 'SURPLUS_FUND' && card.surplusFundPrintData) {
                slot.innerHTML = window.ReportSurplusFundPdfCard.render(card);
            } else if (cardKey === 'FINANCIAL_CYCLE_PLAN') {
                slot.innerHTML = window.ReportLifecyclePdfCard.render(card);
            } else {
                slot.innerHTML = renderGenericPdfCard(card);
            }
            return;
        }
        if (cardKey === 'FINANCIAL_CYCLE_PLAN') {
            slot.innerHTML = renderLifecyclePreviewCard(card, index);
            slot.querySelector('.report-card-remove')?.addEventListener('click', () => removeCard(index));
            slot.querySelector('.report-card-change-plan')?.addEventListener('click', () => openLifecycleResultPickerModal(index));
            return;
        }
        const rows = (card.detailRows || []).map((row) => {
            const section = !row.value;
            return `<li class="report-card-row${section ? ' is-section' : ''}"><span>${escapeHtml(row.label)}</span>${section ? '' : `<span>${escapeHtml(row.value)}</span>`}</li>`;
        }).join('');
        let changeRefButton = '';

        if (!pdfMode && cardKey === 'FUTURESIM') {
            changeRefButton =
                `<button type="button" class="report-card-change-plan" data-index="${index}">계획 변경</button>`;
        } else if (!pdfMode && cardKey === 'SURPLUS_FUND') {
            changeRefButton =
                `<button type="button" class="report-card-change-plan" data-index="${index}">운용기록 변경</button>`;
        } else if (!pdfMode && cardKey === 'FINANCIAL_CYCLE_PLAN') {
            changeRefButton =
                `<button type="button" class="report-card-change-plan" data-index="${index}">시나리오 결과 변경</button>`;
        }
        slot.innerHTML =
            `<button type="button" class="report-card-remove" data-index="${index}" aria-label="카드 삭제">×</button>` +
            `<p class="report-card-title">${card.title}</p>` +
            `<p class="report-card-headline"><span>${card.headlineLabel}</span><br><b>${card.headlineValue}</b></p>` +
            (rows ? `<ul class="report-card-rows">${rows}</ul>` : '') +
            (card.note ? `<p class="report-card-note">${card.note}</p>` : '') +
            changeRefButton;
        const removeButton = slot.querySelector('.report-card-remove');
        if (removeButton) removeButton.addEventListener('click', () => removeCard(index));
        const changeBtn = slot.querySelector('.report-card-change-plan');

        if (changeBtn) {
            changeBtn.addEventListener('click', () => {
                if (cardKey === 'FUTURESIM') {
                    openPlanPickerModal(index);
                } else if (cardKey === 'SURPLUS_FUND') {
                    openSurplusFundPickerModal(index);
                } else if (cardKey === 'FINANCIAL_CYCLE_PLAN') {
                    openLifecycleResultPickerModal(index);
                }
            });
        }
    }

    function renderCardError(index) {
        const slot = gridEl.querySelector(`.report-card-slot[data-index="${index}"]`);
        if (slot) {
            slot.innerHTML = '<p class="report-card-loading">불러오지 못했어요</p>';
        }
    }

    function renderGenericPdfCard(card) {
        const rows = (card.detailRows || []).map((row) => {
            const section = !row.value;
            return `<li class="report-card-row${section ? ' is-section' : ''}"><span>${escapeHtml(row.label)}</span>${section ? '' : `<strong>${escapeHtml(row.value)}</strong>`}</li>`;
        }).join('');
        return `<article class="report-pdf-card-detail"><p class="report-pdf-card-eyebrow">선택한 카드</p><h2>${escapeHtml(card.title)}</h2><p class="report-pdf-card-headline"><span>${escapeHtml(card.headlineLabel)}</span><strong>${escapeHtml(card.headlineValue)}</strong></p>${rows ? `<ul class="report-card-rows">${rows}</ul>` : ''}${card.note ? `<p class="report-card-note">${escapeHtml(card.note)}</p>` : ''}</article>`;
    }

    function escapeHtml(value) {
        return String(value ?? '').replace(/[&<>'"]/g, (character) => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'}[character]));
    }

    function openAddCardModal() {
        modalTitleEl.textContent = '카드 추가';
        const placed = new Set(cards.map((c) => c.cardKey));
        modalListEl.innerHTML = options.map((option) => {
            const disabled = !option.available || placed.has(option.cardKey);
            const badge = !option.available ? '<span class="report-modal-badge">준비중</span>' : '';
            return `<li><button type="button" class="report-modal-option" data-card-key="${option.cardKey}" ${disabled ? 'disabled' : ''}>` +
                `<span>${option.label}</span>${badge}</button></li>`;
        }).join('');
        modalListEl.querySelectorAll('.report-modal-option:not([disabled])').forEach((btn) => {
            btn.addEventListener('click', () => {
                const cardKey = btn.dataset.cardKey;
                if (cardKey === 'FUTURESIM') {
                    openPlanPickerModal(cards.length, true);
                } else if (cardKey === 'SURPLUS_FUND') {
                    openSurplusFundPickerModal(cards.length, true);
                } else if (cardKey === 'FINANCIAL_CYCLE_PLAN') {
                    openLifecycleResultPickerModal(cards.length, true);
                } else {
                    addCard(cardKey);
                }
            });
        });
        modalBackdropEl.classList.remove('hidden');
    }

    function openPlanPickerModal(cardIndex, isNewCard = false) {
        modalTitleEl.textContent = '보여줄 계획 선택';
        modalListEl.innerHTML = '<li class="report-modal-empty">불러오는 중…</li>';
        modalBackdropEl.classList.remove('hidden');

        fetch('/api/future-simulation/plans')
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
            .then((plans) => {
                if (!plans || plans.length === 0) {
                    modalListEl.innerHTML = '<li class="report-modal-empty">저장된 계획이 없어요. 5단계 "나만의 실행 계획"에서 먼저 저장해보세요.</li>';
                    return;
                }
                modalListEl.innerHTML = plans.map((plan) =>
                    `<li><button type="button" class="report-modal-option" data-plan-id="${plan.id}">` +
                        `<span>${plan.planName}</span></button></li>`
                ).join('');
                modalListEl.querySelectorAll('.report-modal-option').forEach((btn) => {
                    btn.addEventListener('click', () => selectCardReference(
                        'FUTURESIM', cardIndex, Number(btn.dataset.planId), isNewCard
                    ));
                });
            })
            .catch(() => {
                modalListEl.innerHTML = '<li class="report-modal-empty">불러오지 못했어요</li>';
            });
    }

    function openSurplusFundPickerModal(cardIndex, isNewCard = false) {
        modalTitleEl.textContent = '보여줄 운용기록 선택';
        modalListEl.innerHTML = '<li class="report-modal-empty">불러오는 중…</li>';

        modalBackdropEl.classList.remove('hidden');

        fetch('/api/surplus-funds/guide-versions')
            .then((res) =>
                res.ok ? res.json() : Promise.reject(res.status)
            )
            .then((versions) => {

                if (!versions || versions.length === 0) {
                    modalListEl.innerHTML = '<li class="report-modal-empty">저장된 운용기록이 없어요.</li>';
                    return;
                }

                modalListEl.innerHTML = versions.map((version) => {
                    const name = version.guideName || `운용기록 ${version.guideVersionNo}`;

                    return `
                    <li>
                        <button
                            type="button"
                            class="report-modal-option"
                            data-guide-version-id="${version.surplusFundGuideVersionId}"
                        >
                            <span>${escapeHtml(name)}</span>
                        </button>
                    </li>
                `;
                }).join('');

                modalListEl.querySelectorAll('.report-modal-option')
                        .forEach((btn) => {

                            btn.addEventListener('click', () => {
                                selectCardReference(
                                    'SURPLUS_FUND', cardIndex,
                                    Number(btn.dataset.guideVersionId), isNewCard
                                );
                            });

                        });
            })
            .catch(() => {
                modalListEl.innerHTML = '<li class="report-modal-empty">불러오지 못했어요.</li>';
            });
    }

    function renderLifecyclePreviewCard(card, index) {
        const rows = card.detailRows || [];
        const expenseIndex = rows.findIndex((row) => row.label === '시나리오 순서별 지출 금액');
        const monthlyBreakdownIndex = rows.findIndex((row) => row.label === '월 지출 상세 구성');
        const costIndex = rows.findIndex((row) => row.label === '시나리오 순서별 소요 비용');
        const oneTimeBreakdownIndex = rows.findIndex((row) => row.label === '일회성 비용 상세 구성');
        const analysisIndex = rows.findIndex((row) => row.label === '상세 분석 보고서');
        const expenseCount = expenseIndex < 0 ? 0 : rows.slice(expenseIndex + 1, monthlyBreakdownIndex >= 0 ? monthlyBreakdownIndex : costIndex).length;
        const costCount = costIndex < 0 ? 0 : rows.slice(costIndex + 1, oneTimeBreakdownIndex >= 0 ? oneTimeBreakdownIndex : analysisIndex).length;
        const eventCount = new Set(rows.slice(analysisIndex + 1).map((row) => String(row.label || '').match(/^STEP\s+(\d+)/)?.[1]).filter(Boolean)).size;
        return `<button type="button" class="report-card-remove" data-index="${index}" aria-label="카드 삭제">×</button>
            <p class="report-card-title">${escapeHtml(card.title)}</p>
            <p class="report-card-headline"><span>${escapeHtml(card.headlineLabel)}</span><br><b>${escapeHtml(card.headlineValue)}</b></p>
            <ul class="report-card-rows">
                <li class="report-card-row"><span>일회성 소요 비용</span><span>${costCount}개 STEP</span></li>
                <li class="report-card-row"><span>월 지출 변화</span><span>${expenseCount}개 STEP</span></li>
                <li class="report-card-row"><span>상세 분석</span><span>${eventCount}개 이벤트</span></li>
            </ul>
            <p class="report-card-note">미리보기에서 전체 금융 라이프 플랜 보고서를 확인할 수 있습니다.</p>
            <button type="button" class="report-card-change-plan" data-index="${index}">시나리오 결과 변경</button>`;
    }
    function openLifecycleResultPickerModal(cardIndex, isNewCard = false) {
        modalTitleEl.textContent = '보여줄 시나리오 결과 선택';
        modalListEl.innerHTML = '<li class="report-modal-empty">불러오는 중…</li>';
        modalBackdropEl.classList.remove('hidden');

        fetch('/api/lifecycle/scenarios/results')
            .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
            .then((results) => {
                if (!results || results.length === 0) {
                    modalListEl.innerHTML = '<li class="report-modal-empty">저장된 시나리오 결과가 없어요.</li>';
                    return;
                }
                modalListEl.innerHTML = results.map((result) => `
                    <li>
                        <button type="button" class="report-modal-option"
                                data-lifecycle-result-id="${result.lifecycleScenarioResultId}">
                            <span>${escapeHtml(result.scenarioName || '금융 라이프 플랜')}</span>
                        </button>
                    </li>
                `).join('');
                modalListEl.querySelectorAll('[data-lifecycle-result-id]').forEach((btn) => {
                    btn.addEventListener('click', () => selectCardReference(
                        'FINANCIAL_CYCLE_PLAN', cardIndex,
                        Number(btn.dataset.lifecycleResultId), isNewCard
                    ));
                });
            })
            .catch(() => {
                modalListEl.innerHTML = '<li class="report-modal-empty">불러오지 못했어요.</li>';
            });
    }

    function closeModal() {
        modalBackdropEl.classList.add('hidden');
    }

    function addCard(cardKey) {
        if (cards.length >= MAX_CARDS || cards.some((c) => c.cardKey === cardKey)) {
            closeModal();
            return;
        }
        cards = [...cards, {cardKey, refId: null}];
        closeModal();
        saveLayout();
    }

    function removeCard(index) {
        cards = cards.filter((_, i) => i !== index);
        saveLayout();
    }

    function setCardRefId(index, refId) {
        cards = cards.map((c, i) => (i === index ? {...c, refId} : c));
        closeModal();
        saveLayout();
    }

    function selectCardReference(cardKey, index, refId, isNewCard) {
        if (isNewCard) {
            cards = [...cards, {cardKey, refId}];
            closeModal();
            saveLayout();
            return;
        }
        setCardRefId(index, refId);
    }

    function saveLayout() {
        renderGrid();
        fetch('/api/report/layout', {
            method: 'POST',
            headers: authHeaders(),
            body: JSON.stringify({cards}),
        }).catch(() => {});
    }

    if (!pdfMode) {
        modalCloseEl.addEventListener('click', closeModal);
        modalBackdropEl.addEventListener('click', (event) => {
            if (event.target === modalBackdropEl) {
                closeModal();
            }
        });
    }

    const headerPromise = loadHeader();

    Promise.all([
        pdfMode ? Promise.resolve([]) : fetch('/api/report/card-options').then((res) => (res.ok ? res.json() : [])),
        fetch('/api/report/layout').then((res) => (res.ok ? res.json() : {cards: []})),
    ]).then(([optionList, layout]) => {
        options = optionList || [];
        cards = (layout && layout.cards) || [];
        return Promise.all([headerPromise, renderGrid()]);
    }).catch(() => headerPromise).finally(() => {
        if (pdfMode) document.body.dataset.chartsReady = 'true';
    });
})();
