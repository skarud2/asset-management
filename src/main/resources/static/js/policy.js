document.addEventListener("DOMContentLoaded", () => {

    // 사이드바 대주제는 한 번에 하나만 펼칩니다.
    // 현재 페이지가 속한 대주제는 처음부터 펼치고 소메뉴를 강조합니다.
    const currentPath = normalizeSidebarPath(window.location.pathname);
    const sidebarLinks = document.querySelectorAll(
        ".sidebar-menu-group a[href]"
    );

    const currentLink = Array.from(sidebarLinks)
        .map(link => ({
            link,
            path: normalizeSidebarPath(
                new URL(link.href, window.location.origin).pathname
            )
        }))
        .filter(item => item.path !== "/"
            && (currentPath === item.path
                || currentPath.startsWith(`${item.path}/`)))
        .sort((first, second) => second.path.length - first.path.length)[0];

    if (currentLink) {
        currentLink.link.classList.add("active");
        currentLink.link.setAttribute("aria-current", "page");

        const currentGroup = currentLink.link.closest(
            "details.sidebar-menu-group"
        );

        if (currentGroup) {
            currentGroup.open = true;
        }
    }

    const sidebarGroups = document.querySelectorAll(
        "details.sidebar-menu-group"
    );

    sidebarGroups.forEach(group => {
        group.addEventListener("toggle", () => {
            if (!group.open) {
                return;
            }

            sidebarGroups.forEach(otherGroup => {
                if (otherGroup !== group) {
                    otherGroup.open = false;
                }
            });
        });
    });

    function normalizeSidebarPath(path) {
        if (!path || path === "/") {
            return "/";
        }

        return path.replace(/\/+$/, "");
    }

    const modal = document.getElementById("programModal");

    /*
     * 금융정책 페이지 등 모달이 없는 페이지에서는
     * 아래 기능을 실행하지 않는다.
     */
    if (!modal) {
        return;
    }

    const modalDialog =
        modal.querySelector(".policy-modal-dialog");

    const openButtons =
        document.querySelectorAll(".open-program-modal");

    const closeButtons =
        modal.querySelectorAll("[data-modal-close]");

    const loadingElement =
        document.getElementById("modalLoading");

    const errorElement =
        document.getElementById("modalError");

    const contentElement =
        document.getElementById("modalContent");

    const applicationUrlElement =
        document.getElementById("modalApplicationUrl");

    let lastFocusedElement = null;

    /*
     * 상세보기 버튼 이벤트
     */
    openButtons.forEach((button) => {

        button.addEventListener("click", async () => {

            const programId =
                button.dataset.programId;

            if (!programId) {
                return;
            }

            lastFocusedElement = button;

            openModal();

            showLoading();

            try {
                const program =
                    await fetchProgramDetail(programId);

                renderProgramDetail(program);

                showContent();

            } catch (error) {

                console.error(
                    "정책상품 상세조회 실패:",
                    error
                );

                showError();
            }
        });
    });

    /*
     * 닫기 버튼 이벤트
     */
    closeButtons.forEach((button) => {

        button.addEventListener("click", () => {
            closeModal();
        });
    });

    /*
     * ESC 키로 닫기
     */
    document.addEventListener("keydown", (event) => {

        if (event.key === "Escape"
            && modal.classList.contains("open")) {

            closeModal();
        }
    });

    /*
     * 모달 내부 클릭은 닫기 이벤트로 전달되지 않게 처리
     */
    modalDialog.addEventListener("click", (event) => {
        event.stopPropagation();
    });

    /**
     * 상세정보 API 요청
     */
    async function fetchProgramDetail(programId) {

        const response =
            await fetch(
                `/api/policy-support/${programId}`,
                {
                    method: "GET",
                    headers: {
                        "Accept": "application/json"
                    }
                }
            );

        if (!response.ok) {
            throw new Error(
                `상세조회 실패: HTTP ${response.status}`
            );
        }

        return response.json();
    }

    /**
     * 모달 열기
     */
    function openModal() {

        modal.classList.add("open");

        modal.setAttribute(
            "aria-hidden",
            "false"
        );

        document.body.classList.add(
            "modal-open"
        );

        const closeButton =
            modal.querySelector(
                ".policy-modal-close"
            );

        if (closeButton) {
            closeButton.focus();
        }
    }

    /**
     * 모달 닫기
     */
    function closeModal() {

        modal.classList.remove("open");

        modal.setAttribute(
            "aria-hidden",
            "true"
        );

        document.body.classList.remove(
            "modal-open"
        );

        resetModal();

        if (lastFocusedElement) {
            lastFocusedElement.focus();
        }
    }

    /**
     * 로딩 상태
     */
    function showLoading() {

        loadingElement.hidden = false;
        errorElement.hidden = true;
        contentElement.hidden = true;
        applicationUrlElement.hidden = true;
    }

    /**
     * 내용 표시 상태
     */
    function showContent() {

        loadingElement.hidden = true;
        errorElement.hidden = true;
        contentElement.hidden = false;
    }

    /**
     * 오류 상태
     */
    function showError() {

        loadingElement.hidden = true;
        contentElement.hidden = true;
        errorElement.hidden = false;
        applicationUrlElement.hidden = true;
    }

    /**
     * 정책상품 상세정보를 모달에 출력
     */
    function renderProgramDetail(program) {

        setText(
            "modalPolicyBadge",
            program.productBadge,
            "대출상품"
        );

        setText(
            "modalProgramName",
            program.programName,
            "정책상품 상세정보"
        );

        setText(
            "modalTargetDescription",
            program.targetDescription,
            "지원 대상은 취급기관에 문의해 주세요."
        );

        setBadge(
            "modalInterestRateType",
            program.interestRateType
        );

        setBadge(
            "modalUsageDescription",
            program.usageDescription
        );

        setBadge(
            "modalSupportArea",
            program.supportArea
        );

        setText(
            "modalMaxSupportAmount",
            formatCurrency(
                program.maxSupportAmount
            ),
            "별도 확인"
        );

        setText(
            "modalInterestRate",
            formatInterestRate(program),
            "별도 확인"
        );

        setText(
            "modalSupportPeriod",
            program.supportPeriodDescription,
            "별도 확인"
        );

        setText(
            "modalRepaymentMethod",
            program.repaymentMethod,
            "별도 확인"
        );

        setText(
            "modalOfferingInstitution",
            program.offeringInstitutionName,
            "별도 확인"
        );

        setText(
            "modalHandlingInstitution",
            program.handlingInstitution,
            "별도 확인"
        );

        setText(
            "modalOperationPeriod",
            program.operationPeriod,
            "기관 문의"
        );

        setText(
            "modalEligibilityDescription",
            program.eligibilityDescription,
            "등록된 상세 지원조건이 없습니다."
        );

        setText(
            "modalApplicationMethod",
            program.applicationMethod,
            "취급기관 문의"
        );

        setText(
            "modalContactDescription",
            program.contactDescription,
            "취급기관 문의"
        );

        renderEligibility(
            program.eligibility
        );

        renderApplicationUrl(
            program.applicationUrl
        );
    }

    /**
     * 일반 문자열 출력
     */
    function setText(
        elementId,
        value,
        defaultValue
    ) {
        const element =
            document.getElementById(elementId);

        if (!element) {
            return;
        }

        element.textContent =
            hasValue(value)
                ? value
                : defaultValue;
    }

    /**
     * 상단 조건 배지 출력
     */
    function setBadge(
        elementId,
        value
    ) {
        const element =
            document.getElementById(elementId);

        if (!element) {
            return;
        }

        if (hasValue(value)) {
            element.textContent = value;
            element.hidden = false;
        } else {
            element.textContent = "";
            element.hidden = true;
        }
    }

    /**
     * 금액 표시
     */
    function formatCurrency(value) {

        if (value === null
            || value === undefined
            || value === "") {

            return null;
        }

        const amount = Number(value);

        if (Number.isNaN(amount)) {
            return null;
        }

        return new Intl.NumberFormat(
            "ko-KR"
        ).format(amount) + "원";
    }

    /**
     * 금리 표시
     */
    function formatInterestRate(program) {

        const min =
            toNumberOrNull(
                program.minInterestRate
            );

        const max =
            toNumberOrNull(
                program.maxInterestRate
            );

        if (min !== null && max !== null) {

            if (min === max) {
                return `${formatNumber(min)}%`;
            }

            return `${formatNumber(min)}% ~ ${formatNumber(max)}%`;
        }

        if (min === null && max !== null) {
            return `최대 ${formatNumber(max)}%`;
        }

        if (min !== null) {
            return `최저 ${formatNumber(min)}%`;
        }

        if (hasValue(
            program.interestRateDescription
        )) {
            return program.interestRateDescription;
        }

        return null;
    }

    function formatNumber(value) {

        return Number(value)
            .toLocaleString(
                "ko-KR",
                {
                    maximumFractionDigits: 3
                }
            );
    }

    function toNumberOrNull(value) {

        if (value === null
            || value === undefined
            || value === "") {

            return null;
        }

        const number = Number(value);

        return Number.isNaN(number)
            ? null
            : number;
    }

    /**
     * eligibility JSON 출력
     */
    function renderEligibility(eligibility) {

        const section =
            document.getElementById(
                "modalEligibilitySection"
            );

        const list =
            document.getElementById(
                "modalEligibilityList"
            );

        list.innerHTML = "";

        if (!eligibility
            || Object.keys(eligibility).length === 0) {

            section.hidden = true;
            return;
        }

        Object.entries(eligibility)
            .forEach(([key, value]) => {

                if (!hasValue(value)) {
                    return;
                }

                const row =
                    document.createElement("div");

                const term =
                    document.createElement("dt");

                const description =
                    document.createElement("dd");

                term.textContent =
                    translateEligibilityKey(key);

                description.textContent =
                    value;

                row.appendChild(term);
                row.appendChild(description);

                list.appendChild(row);
            });

        section.hidden =
            list.children.length === 0;
    }

    /**
     * eligibility 영문 키를 한글로 변환
     */
    function translateEligibilityKey(key) {

        const labels = {
            age: "나이 조건",
            income: "소득 조건",
            residenceArea: "거주지역 조건",
            creditScore: "신용 조건",
            householdCondition: "가구 조건",
            guaranteeInstitution: "보증기관",
            repaymentFee: "중도상환수수료",
            loanIncidentalCost: "대출 부대비용",
            overdueInterestRate: "연체이자율",
            preferentialInterestCondition: "우대금리 조건",
            etcReference: "기타 참고사항",
            handlingInstitutionDetail: "취급기관 상세",
            productCategory: "상품 분류",
            financialEducationProductYn: "금융교육 여부",
            financialEducationProductEtc: "금융교육 기타사항"
        };

        return labels[key] ?? key;
    }

    /**
     * 공식 사이트 버튼
     */
    function renderApplicationUrl(url) {

        if (hasValue(url)) {

            applicationUrlElement.href = url;
            applicationUrlElement.hidden = false;

        } else {

            applicationUrlElement.href = "#";
            applicationUrlElement.hidden = true;
        }
    }

    function hasValue(value) {

        return value !== null
            && value !== undefined
            && String(value).trim() !== "";
    }

    /**
     * 닫을 때 이전 데이터 초기화
     */
    function resetModal() {

        loadingElement.hidden = false;
        errorElement.hidden = true;
        contentElement.hidden = true;

        applicationUrlElement.href = "#";
        applicationUrlElement.hidden = true;
    }
});

document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("financialProductModal");
    if (!modal) return;

    const searchForm = document.getElementById("financialProductSearchForm");
    document.querySelectorAll(".financial-product-page-button").forEach(button => {
        button.addEventListener("click", () => {
            if (!searchForm || button.disabled) return;
            searchForm.querySelector('input[name="page"]').value = button.dataset.page;
            searchForm.submit();
        });
    });

    const loading = document.getElementById("financialModalLoading");
    const error = document.getElementById("financialModalError");
    const content = document.getElementById("financialModalContent");
    const site = document.getElementById("financialModalSite");
    let lastFocused = null;

    const endpoints = {
        ASSET: "/api/asset-products/",
        SOCIAL: "/api/social-finance/",
        WELFARE: "/api/welfare-support/"
    };

    document.querySelectorAll(".open-financial-product-modal").forEach(button => {
        button.addEventListener("click", async () => {
            const endpoint = endpoints[button.dataset.productType];
            if (!endpoint || !button.dataset.productId) return;
            lastFocused = button;
            open();
            show("loading");
            try {
                const response = await fetch(endpoint + encodeURIComponent(button.dataset.productId), {
                    headers: {"Accept": "application/json"}
                });
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                render(await response.json());
                show("content");
            } catch (e) {
                console.error("금융상품 상세조회 실패", e);
                show("error");
            }
        });
    });

    modal.querySelectorAll("[data-financial-modal-close]").forEach(button =>
        button.addEventListener("click", close));
    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && modal.classList.contains("open")) close();
    });

    function open() {
        modal.classList.add("open");
        modal.setAttribute("aria-hidden", "false");
        document.body.classList.add("modal-open");
    }

    function close() {
        modal.classList.remove("open");
        modal.setAttribute("aria-hidden", "true");
        document.body.classList.remove("modal-open");
        if (lastFocused) lastFocused.focus();
    }

    function show(state) {
        loading.hidden = state !== "loading";
        error.hidden = state !== "error";
        content.hidden = state !== "content";
    }

    function render(product) {
        document.getElementById("financialModalTitle").textContent = product.title || "상품 상세정보";
        document.getElementById("financialModalBadge").textContent = product.badge || "상품";
        renderFields("financialModalSummary", product.summary);
        renderFields("financialModalConditions", product.conditions);
        renderFields("financialModalApplication", product.application);
        if (product.relatedSite) {
            site.href = product.relatedSite;
            site.hidden = false;
        } else {
            site.href = "#";
            site.hidden = true;
        }
    }

    function renderFields(id, fields) {
        const list = document.getElementById(id);
        list.innerHTML = "";
        Object.entries(fields || {}).forEach(([label, value]) => {
            const row = document.createElement("div");
            const term = document.createElement("dt");
            const description = document.createElement("dd");
            term.textContent = label;
            description.textContent = value || "별도 확인";
            row.append(term, description);
            list.appendChild(row);
        });
    }
});

document.addEventListener("DOMContentLoaded", () => {

    const toggleButton =
        document.getElementById("detailFilterToggle");

    const filterPanel =
        document.getElementById("detailFilterPanel");

    const toggleText =
        document.getElementById("filterToggleText");

    if (!toggleButton
        || !filterPanel
        || !toggleText) {

        return;
    }

    toggleButton.addEventListener("click", () => {

        const isHidden =
            filterPanel.classList.toggle("hidden");

        toggleButton.classList.toggle(
            "collapsed",
            isHidden
        );

        toggleButton.setAttribute(
            "aria-expanded",
            String(!isHidden)
        );

        toggleText.textContent =
            isHidden
                ? "펼치기"
                : "접기";
    });
});
