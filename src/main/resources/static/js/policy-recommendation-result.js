document.addEventListener("DOMContentLoaded", () => {

    const list = document.getElementById("recommendationList");
    const loading = document.getElementById("resultLoading");
    const error = document.getElementById("resultError");
    const unverifiableSection = document.getElementById("unverifiableSection");
    const unverifiableList = document.getElementById("unverifiableList");
    const tabs = document.querySelectorAll(".policy-rec-tab:not([disabled])");
    const summaryFilters = document.querySelectorAll(".policy-rec-summary-card[data-status]");

    // 페이지네이션 요소
    const pagination = document.getElementById("resultPagination");
    const prevPageButton = document.getElementById("prevPageButton");
    const nextPageButton = document.getElementById("nextPageButton");
    const pageNumberContainer = document.getElementById("pageNumberContainer");

    if (!list) return;

    const PAGE_SIZE = 5;
    const PAGE_NUMBER_GROUP_SIZE = 10;

    let results = [];
    let selectedType = "ALL";
    let selectedStatus = "RECOMMENDABLE";
    let currentPage = 1;


    // =====================================================
    // 상품군 탭
    // =====================================================

    tabs.forEach(tab => {

        tab.addEventListener("click", () => {

            selectedType = tab.dataset.type;

            // 필터 변경 시 1페이지로
            currentPage = 1;

            tabs.forEach(item => {
                item.classList.remove("active");
            });

            tab.classList.add("active");

            render();
        });
    });


    // =====================================================
    // 추천 상태 필터
    // =====================================================

    summaryFilters.forEach(filter => {

        filter.addEventListener("click", () => {

            selectedStatus = filter.dataset.status;

            // 상태 변경 시 1페이지로
            currentPage = 1;

            summaryFilters.forEach(item => {

                const active = item === filter;

                item.classList.toggle("active", active);

                item.setAttribute("aria-pressed", String(active));
            });

            render();
        });
    });


    // =====================================================
    // 이전 페이지
    // =====================================================

    if (prevPageButton) {

        prevPageButton.addEventListener("click", () => {

            const visibleItems = getVisibleItems();
            const totalPages = Math.ceil(visibleItems.length / PAGE_SIZE);
            const group = getPageNumberGroup(totalPages);

            if (group.start === 1) {
                return;
            }

            currentPage = group.start - 1;

            render();

            scrollToResultTop();
        });
    }


    // =====================================================
    // 다음 페이지
    // =====================================================

    if (nextPageButton) {

        nextPageButton.addEventListener("click", () => {

            const visibleItems = getVisibleItems();

            const totalPages = Math.ceil(visibleItems.length / PAGE_SIZE);
            const group = getPageNumberGroup(totalPages);

            if (group.end >= totalPages) {
                return;
            }

            currentPage = group.end + 1;

            render();

            scrollToResultTop();
        });
    }


    // =====================================================
    // 결과 조회
    // =====================================================

    async function loadResults() {

        try {

            const response = await fetch("/api/policy/recommendation/results", {
                method: "GET", headers: {
                    "Accept": "application/json"
                }
            });

            if (!response.ok) {

                throw new Error("추천 결과를 불러오지 못했습니다.");
            }

            results = await response.json();

            currentPage = 1;

            loading.hidden = true;

            updateTabCounts();

            render();

        } catch (exception) {

            console.error(exception);

            loading.hidden = true;

            error.hidden = false;

            error.textContent = "추천 결과를 불러오는 중 오류가 발생했습니다.";
        }
    }


    // =====================================================
    // 요약 카드 숫자
    // =====================================================

    function updateSummary(items) {

        const recommendable = items.filter(isRecommendable);

        document
            .getElementById("totalCount").textContent = recommendable.length;

        document
            .getElementById("eligibleCount").textContent = items.filter(item => item.status === "ELIGIBLE").length;

        document
            .getElementById("checkCount").textContent = items.filter(item => item.status === "NEEDS_CONFIRMATION").length;

        document
            .getElementById("ineligibleCount").textContent = items.filter(item => item.status === "INELIGIBLE").length;

        document
            .getElementById("unverifiableCount").textContent = items.filter(item => item.status === "UNVERIFIABLE").length;

        document
            .getElementById("totalCountLabel").textContent = `${productTypeName(selectedType)} 추천 후보`;
    }


    // =====================================================
    // 상품군 탭 숫자
    // =====================================================

    function updateTabCounts() {

        const recommendable = results.filter(isRecommendable);

        document
            .querySelectorAll(".policy-rec-tab[data-type]")
            .forEach(tab => {

                const type = tab.dataset.type;

                const count = type === "ALL"
                    ? recommendable.length
                    : recommendable.filter(item => item.productType === type).length;

                const countElement = tab.querySelector(".policy-rec-tab-count");

                if (countElement) {

                    countElement.textContent = count;
                }
            });
    }


    // =====================================================
    // 현재 필터 조건에 맞는 결과
    // =====================================================

    function getVisibleItems() {

        const filtered = selectedType === "ALL" ? results : results.filter(item => item.productType === selectedType);


        const recommendable = filtered.filter(isRecommendable);


        if (selectedStatus === "UNVERIFIABLE") {

            return filtered.filter(item => item.status === "UNVERIFIABLE");
        }


        if (selectedStatus === "RECOMMENDABLE") {

            return recommendable;
        }

        return filtered.filter(item => item.status === selectedStatus);
    }


    // =====================================================
    // 전체 화면 렌더링
    // =====================================================

    function render() {

        const filtered = selectedType === "ALL" ? results : results.filter(item => item.productType === selectedType);

        updateSummary(filtered);


        // ================================
        // 판정 불가 탭
        // ================================

        if (selectedStatus === "UNVERIFIABLE") {

            list.hidden = true;

            unverifiableSection.hidden = false;

            const unverifiable = filtered.filter(item => item.status === "UNVERIFIABLE");


            const pageItems = getPageItems(unverifiable);


            if (unverifiable.length === 0) {

                unverifiableList.innerHTML = `
                    <div class="policy-rec-message">
                        현재 상품군에는 판정 불가 상품이 없습니다.
                    </div>
                    `;

                hidePagination();

                return;
            }


            unverifiableList.innerHTML = pageItems
                .map(createCard)
                .join("");


            renderPagination(unverifiable.length);

            return;
        }


        // ================================
        // 일반 추천 영역
        // ================================

        list.hidden = false;

        unverifiableSection.hidden = true;


        const recommendable = filtered.filter(isRecommendable);


        const visibleItems = selectedStatus === "RECOMMENDABLE"

            ? recommendable

            : filtered.filter(item => item.status === selectedStatus);


        if (visibleItems.length === 0) {

            list.innerHTML = `
                <div class="policy-rec-message">
                    선택한 조건에 해당하는 추천 상품이 없습니다.
                </div>
                `;

            hidePagination();

            return;
        }


        const pageItems = getPageItems(visibleItems);


        list.innerHTML = pageItems
            .map(createCard)
            .join("");


        renderPagination(visibleItems.length);
    }


    // =====================================================
    // 현재 페이지 상품 5개 추출
    // =====================================================

    function getPageItems(items) {

        const totalPages = Math.ceil(items.length / PAGE_SIZE);


        // 필터 변경 등으로 현재 페이지가
        // 최대 페이지보다 커졌을 때 보정
        if (totalPages > 0 && currentPage > totalPages) {

            currentPage = totalPages;
        }


        if (currentPage < 1) {

            currentPage = 1;
        }


        const startIndex = (currentPage - 1) * PAGE_SIZE;

        const endIndex = startIndex + PAGE_SIZE;


        return items.slice(startIndex, endIndex);
    }


    // =====================================================
    // 페이지네이션
    // =====================================================

    function renderPagination(totalCount) {

        if (!pagination || !pageNumberContainer || !prevPageButton || !nextPageButton) {
            return;
        }


        const totalPages = Math.ceil(totalCount / PAGE_SIZE);


        pageNumberContainer.innerHTML = "";


        // 결과가 5개 이하이면 숨김
        if (totalPages <= 1) {

            hidePagination();

            return;
        }


        pagination.hidden = false;

        const group = getPageNumberGroup(totalPages);

        prevPageButton.disabled = group.start === 1;

        nextPageButton.disabled = group.end === totalPages;


        for (let page = group.start; page <= group.end; page++) {

            const button = document.createElement("button");


            button.type = "button";

            button.className = "policy-rec-page-number";

            button.textContent = page;


            if (page === currentPage) {

                button.classList.add("active");

                button.setAttribute("aria-current", "page");
            }


            button.addEventListener("click", () => {

                currentPage = page;

                render();

                scrollToResultTop();
            });


            pageNumberContainer
                .appendChild(button);
        }
    }


    function getPageNumberGroup(totalPages) {

        const start = Math.floor((currentPage - 1) / PAGE_NUMBER_GROUP_SIZE)
            * PAGE_NUMBER_GROUP_SIZE + 1;

        return {
            start,
            end: Math.min(start + PAGE_NUMBER_GROUP_SIZE - 1, totalPages)
        };
    }


    // =====================================================
    // 페이지네이션 숨기기
    // =====================================================

    function hidePagination() {

        if (pagination) {

            pagination.hidden = true;
        }
    }


    // =====================================================
    // 페이지 이동 시 결과 상단 이동
    // =====================================================

    function scrollToResultTop() {

        let target;


        if (selectedStatus === "UNVERIFIABLE") {

            target = unverifiableSection;

        } else {

            target = list;
        }


        if (!target) {
            return;
        }


        target.scrollIntoView({
            behavior: "smooth", block: "start"
        });
    }


    // =====================================================
    // 카드 생성
    // =====================================================

    function createCard(item) {

        const satisfied = (item.conditions || [])
            .filter(condition => condition.status === "SATISFIED");

        const checks = (item.conditions || [])
            .filter(condition => condition.status === "NEEDS_CONFIRMATION");

        const failed = (item.conditions || [])
            .filter(condition => condition.status === "NOT_SATISFIED");

        const relatedLink = safeHttpUrl(item.relatedUrl);


        return `
            <article class="policy-rec-card ${item.status === "UNVERIFIABLE" ? "unverifiable" : item.status === "INELIGIBLE" ? "ineligible" : ""}">

                <div class="policy-rec-card-badges">

                    <span class="policy-rec-type-badge">
                        ${escapeHtml(productTypeName(item.productType))}
                    </span>

                    <span class="policy-rec-status-badge ${statusClass(item.status)}">
                        ${escapeHtml(statusName(item.status))}
                    </span>

                </div>

                <h2>
                    ${escapeHtml(item.productName || "상품명 미등록")}
                </h2>

                <p class="policy-rec-institution">
                    ${escapeHtml(item.institutionName || "기관 정보 확인 필요")}
                </p>

                <div class="policy-rec-benefit-grid">

                    <div>
                        <span>주요 혜택</span>
                        <strong>
                            ${escapeHtml(item.primaryBenefit || "-")}
                        </strong>
                    </div>

                    <div>
                        <span>추가 정보</span>
                        <strong>
                            ${escapeHtml(item.secondaryBenefit || "-")}
                        </strong>
                    </div>

                    <div>
                        <span>지원 지역</span>
                        <strong>
                            ${escapeHtml(item.supportArea || "전국/기관 확인")}
                        </strong>
                    </div>

                </div>

                ${reasonBlock("확인된 조건", satisfied, "satisfied")}

                ${reasonBlock("추가 확인이 필요한 조건", checks, "check")}

                ${reasonBlock("충족하지 못한 조건", failed, "failed")}

                <details class="policy-rec-detail">

                    <summary>
                        상품 상세조건 보기
                    </summary>

                    <p>
                        <strong>지원대상</strong>
                        <br>
                        ${escapeHtml(item.targetDescription || "-")}
                    </p>

                    <p>
                        <strong>상세 지원조건</strong>
                        <br>
                        ${escapeHtml(item.eligibilityDescription || "-")}
                    </p>

                    <p>
                        <strong>신청방법</strong>
                        <br>
                        ${escapeHtml(item.applicationMethod || "-")}
                    </p>

                </details>

                ${relatedLink

            ? `
                        <a class="policy-rec-primary-button"
                           href="${escapeHtml(relatedLink)}"
                           target="_blank"
                           rel="noopener noreferrer">
                            관련 사이트 확인
                        </a>
                        `

            : ""}

            </article>
        `;
    }


    // =====================================================
    // 조건 블록
    // =====================================================

    function reasonBlock(title, conditions, type) {

        if (!conditions || conditions.length === 0) {
            return "";
        }


        return `
            <div class="policy-rec-condition ${type}">

                <h3>
                    ${escapeHtml(title)}
                </h3>

                <ul>
                    ${conditions
            .map(condition => `
                                    <li>
                                        ${escapeHtml(condition.description || "")}
                                    </li>
                                    `)
            .join("")}
                </ul>

            </div>
        `;
    }


    // =====================================================
    // 상품군 이름
    // =====================================================

    function productTypeName(type) {

        return {
            ALL: "전체", POLICY_LOAN: "서민금융", ASSET: "자산형성", SOCIAL: "사회연대", WELFARE: "복지지원"
        }[type] || "정책금융";
    }


    // =====================================================
    // 상태 이름
    // =====================================================

    function statusName(status) {

        if (status === "ELIGIBLE") {
            return "추천 가능성 높음";
        }


        if (status === "UNVERIFIABLE") {
            return "판정 불가";
        }

        if (status === "INELIGIBLE") {
            return "신청 불가";
        }


        return "추가 확인 필요";
    }


    // =====================================================
    // 상태 CSS
    // =====================================================

    function statusClass(status) {

        if (status === "ELIGIBLE") {
            return "eligible";
        }


        if (status === "UNVERIFIABLE") {
            return "unverifiable";
        }

        if (status === "INELIGIBLE") {
            return "ineligible";
        }


        return "check";
    }


    function isRecommendable(item) {

        return item.status === "ELIGIBLE"
            || item.status === "NEEDS_CONFIRMATION";
    }


    // =====================================================
    // XSS 방지
    // =====================================================

    function escapeHtml(value) {

        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }


    // =====================================================
    // 외부 링크 검증
    // =====================================================

    function safeHttpUrl(value) {

        const text = String(value ?? "").trim();


        return /^https?:\/\//i
            .test(text) ? text : "";
    }


    // 최초 조회
    loadResults();
});
