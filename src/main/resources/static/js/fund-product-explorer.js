(function () {
    "use strict";

    const API_URL = "/api/surplus-funds/products/funds";
    const INITIAL_VISIBLE_LIMIT = 8;
    const LOAD_MORE_STEP = 8;

    const productGrid = document.getElementById("fundProductArea");
    const productEmptyState = document.getElementById("productEmptyState");
    const exploreProductsButton = document.getElementById("exploreProductsButton");
    const filterButtons = Array.from(document.querySelectorAll("[data-product-filter]"));

    if (!productGrid || filterButtons.length === 0) {
        return;
    }

    let products = [];
    let allocationAmount = 0;
    let currentKeyword = "";
    let currentSort = "RETURN_12_DESC";
    let visibleLimit = INITIAL_VISIBLE_LIMIT;
    let loaded = false;
    let activeFilter = "ALL";

    window.addEventListener("surplus:allocation-updated", function (event) {
        const allocations = event.detail && Array.isArray(event.detail.allocations)
            ? event.detail.allocations
            : [];

        const fundAllocation = allocations.find(function (allocation) {
            return allocation.assetType === "FUND";
        });

        allocationAmount = fundAllocation ? Number(fundAllocation.amount) || 0 : 0;
    });

    filterButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            activeFilter = button.dataset.productFilter || "ALL";

            if (
                activeFilter !== "ALL"
                && activeFilter !== "FUND"
            ) {
                return;
            }

            visibleLimit = INITIAL_VISIBLE_LIMIT;

            if (loaded) {
                render();
                return;
            }

            loadProducts();
        });
    });

    if (exploreProductsButton) {
        exploreProductsButton.addEventListener("click", function () {
            window.setTimeout(function () {
                const selected = filterButtons.find(function (button) {
                    return button.getAttribute("aria-pressed") === "true";
                });

                activeFilter = selected ? selected.dataset.productFilter : "ALL";

                if (
                    activeFilter === "ALL"
                    || activeFilter === "FUND"
                ) {
                    loadProducts();
                }
            }, 0);
        });
    }

    async function loadProducts() {
        renderLoading();

        try {
            const response = await fetch(API_URL, {
                method: "GET",
                headers: {
                    "Accept": "application/json"
                },
                credentials: "same-origin"
            });

            const body = await response.json();

            if (!response.ok) {
                throw new Error(body?.message || "펀드 정보를 불러오지 못했습니다.");
            }

            if (!Array.isArray(body)) {
                throw new Error("펀드 데이터 형식이 올바르지 않습니다.");
            }

            products = body;
            loaded = true;

            if (
                activeFilter === "ALL"
                || activeFilter === "FUND"
            ) {
                render();
            }
        } catch (error) {
            renderError(error.message || "펀드 정보를 불러오지 못했습니다.");
        }
    }

    function render() {
        if (
            activeFilter !== "ALL"
            && activeFilter !== "FUND"
        ) {
            return;
        }

        productGrid.hidden = false;

        if (productEmptyState) {
            productEmptyState.hidden = true;
        }

        const filteredProducts = getFilteredProducts();
        const wrapper = element("section", "etf-explorer");

        wrapper.append(
            createHeader(filteredProducts.length),
            createNotice(),
            createToolbar()
        );

        if (filteredProducts.length === 0) {
            const empty = element("p", "etf-explorer__empty");
            empty.textContent = "조건에 맞는 펀드 상품이 없습니다.";
            wrapper.append(empty);
        } else {
            const grid = element("div", "etf-explorer__grid");

            filteredProducts.slice(0, visibleLimit).forEach(function (product) {
                grid.append(createProductCard(product));
            });

            wrapper.append(grid);

            if (filteredProducts.length > visibleLimit) {
                wrapper.append(createLoadMoreButton());
            }
        }

        const guideNotice = element("p", "etf-explorer__notice");
        guideNotice.textContent =
            "본 정보는 중소기업은행이 제공한 2024년 12월 31일 기준 펀드 정보입니다. " +
            "표시된 수익률은 펀드 자체의 과거 기준가 변동률이며, 과거 수익률은 미래 수익을 보장하지 않습니다.";

        wrapper.append(guideNotice);
        productGrid.replaceChildren(wrapper);
    }

    function createHeader(count) {
        const header = element("div", "etf-explorer__header");
        const titleGroup = element("div", "etf-explorer__title-group");
        const title = element("h3", "etf-explorer__title");

        title.textContent = "펀드 상품 탐색";

        const description = element("p", "etf-explorer__description");
        description.textContent = allocationAmount > 0
            ? "펀드 배정금액 " + formatWon(allocationAmount) + "을 참고하여 상품 정보를 탐색할 수 있습니다."
            : "중소기업은행이 제공한 펀드 정보를 탐색할 수 있습니다.";

        titleGroup.append(title, description);

        const meta = element("div", "etf-explorer__meta");
        const date = element("span", "etf-explorer__date");
        date.textContent = "데이터 기준일 2024-12-31";

        const countElement = element("span", "etf-explorer__count");
        countElement.textContent = "조회 상품 " + count.toLocaleString("ko-KR") + "개";

        meta.append(date, countElement);
        header.append(titleGroup, meta);

        return header;
    }

    function createNotice() {
        const notice = element("p", "etf-explorer__sort-notice");
        notice.textContent =
            "수익률과 펀드등급은 상품 탐색을 위한 정보이며 특정 상품의 추천 또는 적합성 순위를 의미하지 않습니다.";
        return notice;
    }

    function createToolbar() {
        const form = element("form", "etf-explorer__toolbar");
        form.setAttribute("role", "search");

        const searchGroup = element("div", "etf-explorer__search-group");
        const label = element("label", "etf-explorer__search-label");
        label.setAttribute("for", "fundProductKeyword");
        label.textContent = "펀드 검색";

        const input = element("input", "etf-explorer__search");
        input.id = "fundProductKeyword";
        input.type = "search";
        input.placeholder = "상품명·운용사명";
        input.value = currentKeyword;

        searchGroup.append(label, input);

        const sortGroup = element("div", "etf-explorer__sort-group");
        const sortLabel = element("label", "etf-explorer__sort-label");
        sortLabel.setAttribute("for", "fundProductSort");
        sortLabel.textContent = "정렬 기준";

        const sort = element("select", "etf-explorer__sort");
        sort.id = "fundProductSort";

        [
            ["RETURN_12_DESC", "12개월 수익률 높은 순"],
            ["RETURN_6_DESC", "6개월 수익률 높은 순"],
            ["EXPENSE_ASC", "총보수 낮은 순"],
            ["NAME_ASC", "상품명 순"]
        ].forEach(function (optionData) {
            const option = document.createElement("option");
            option.value = optionData[0];
            option.textContent = optionData[1];
            option.selected = currentSort === optionData[0];
            sort.append(option);
        });

        sortGroup.append(sortLabel, sort);

        const button = element("button", "etf-explorer__search-button");
        button.type = "submit";
        button.textContent = "검색";

        form.append(searchGroup, sortGroup, button);

        form.addEventListener("submit", function (event) {
            event.preventDefault();
            currentKeyword = input.value.trim();
            currentSort = sort.value;
            visibleLimit = INITIAL_VISIBLE_LIMIT;
            render();
        });

        sort.addEventListener("change", function () {
            currentSort = sort.value;
            visibleLimit = INITIAL_VISIBLE_LIMIT;
            render();
        });

        return form;
    }

    function createProductCard(product) {
        const article = element("article", "etf-product-card");
        const badge = element("span", "etf-product-card__badge");
        badge.textContent = product.fundType || "펀드";

        const title = element("h4", "etf-product-card__name");
        title.textContent = product.productName || "상품명 정보 없음";

        const meta = element("p", "etf-product-card__meta");
        meta.textContent = (product.providerName || "운용사 정보 없음")
            + " · 펀드등급 " + formatGrade(product.fundGrade)
            + " · 기준일 " + (product.disclosureBaseDate || "-");

        const metrics = element("dl", "etf-product-card__metrics");

        appendMetric(metrics, "1개월 수익률", formatPercent(product.return1Month));
        appendMetric(metrics, "3개월 수익률", formatPercent(product.return3Months));
        appendMetric(metrics, "6개월 수익률", formatPercent(product.return6Months));
        appendMetric(metrics, "12개월 수익률", formatPercent(product.return12Months));
        appendMetric(metrics, "선취수수료", formatPercent(product.upfrontFeeRate));
        appendMetric(metrics, "총보수", formatPercent(product.totalExpenseRate));

        article.append(badge, title, meta, metrics);
        return article;
    }

    function getFilteredProducts() {
        const keyword = currentKeyword.toLowerCase();

        const filtered = products.filter(function (product) {
            if (!keyword) {
                return true;
            }

            const productName = (product.productName || "").toLowerCase();
            const providerName = (product.providerName || "").toLowerCase();

            return productName.includes(keyword) || providerName.includes(keyword);
        });

        return filtered.sort(function (a, b) {
            if (currentSort === "RETURN_6_DESC") {
                return compareNumberDesc(a.return6Months, b.return6Months);
            }

            if (currentSort === "EXPENSE_ASC") {
                return compareNumberAsc(a.totalExpenseRate, b.totalExpenseRate);
            }

            if (currentSort === "NAME_ASC") {
                return (a.productName || "").localeCompare(b.productName || "", "ko");
            }

            return compareNumberDesc(a.return12Months, b.return12Months);
        });
    }

    function createLoadMoreButton() {
        const button = element("button", "etf-explorer__load-more");
        button.type = "button";
        button.textContent = LOAD_MORE_STEP + "개 더 보기";

        button.addEventListener("click", function () {
            visibleLimit += LOAD_MORE_STEP;
            render();
        });

        return button;
    }

    function appendMetric(list, labelText, valueText) {
        const group = element("div", "etf-product-card__metric");
        const term = document.createElement("dt");
        term.textContent = labelText;

        const value = document.createElement("dd");
        value.textContent = valueText;

        group.append(term, value);
        list.append(group);
    }

    function renderLoading() {
        productGrid.hidden = false;

        if (productEmptyState) {
            productEmptyState.hidden = true;
        }

        const loading = element("div", "etf-explorer__loading");
        loading.setAttribute("role", "status");
        loading.textContent = "펀드 상품 정보를 불러오는 중입니다.";

        productGrid.replaceChildren(loading);
    }

    function renderError(message) {
        if (productEmptyState) {
            productEmptyState.hidden = true;
        }

        const error = element("div", "etf-explorer__error");
        error.setAttribute("role", "alert");
        error.textContent = message;

        productGrid.replaceChildren(error);
    }

    function compareNumberDesc(a, b) {
        const numberA = Number(a);
        const numberB = Number(b);

        if (!Number.isFinite(numberA)) return 1;
        if (!Number.isFinite(numberB)) return -1;

        return numberB - numberA;
    }

    function compareNumberAsc(a, b) {
        const numberA = Number(a);
        const numberB = Number(b);

        if (!Number.isFinite(numberA)) return 1;
        if (!Number.isFinite(numberB)) return -1;

        return numberA - numberB;
    }

    function formatPercent(value) {
        const number = Number(value);

        if (!Number.isFinite(number)) {
            return "정보 없음";
        }

        return (number > 0 ? "+" : "") + number.toLocaleString("ko-KR", {
            maximumFractionDigits: 2
        }) + "%";
    }

    function formatGrade(value) {
        const number = Number(value);

        return Number.isFinite(number)
            ? number + "등급"
            : "정보 없음";
    }

    function formatWon(value) {
        const number = Number(value);

        return Number.isFinite(number)
            ? Math.round(number).toLocaleString("ko-KR") + "원"
            : "0원";
    }

    function element(tagName, className) {
        const node = document.createElement(tagName);

        if (className) {
            node.className = className;
        }

        return node;
    }
})();