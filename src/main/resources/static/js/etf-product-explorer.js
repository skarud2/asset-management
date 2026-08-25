(function () {
    "use strict";

    const API_URL = "/api/surplus-funds/products/etfs";
    const MAX_COMPARE_COUNT = 4;
    const INITIAL_VISIBLE_LIMIT = 8;
    const LOAD_MORE_STEP = 8;
    const COMPARE_PANEL_ID = "etfComparePanel";

    const productGrid = document.getElementById("etfProductArea");
    const productEmptyState = document.getElementById("productEmptyState");
    const exploreProductsButton = document.getElementById("exploreProductsButton");
    const filterButtons = Array.from(document.querySelectorAll("[data-product-filter]"));

    if (!productGrid || filterButtons.length === 0) {
        return;
    }

    let allocationAmount = readAllocationAmountFromFilter();
    let activeFilter = "ALL";
    let currentKeyword = "";
    let currentSort = "TRADING_VALUE_DESC";
    let currentDataBaseDate = null;
    let currentNotice = null;
    let currentResultCount = 0;
    let visibleLimit = INITIAL_VISIBLE_LIMIT;
    let comparePanelOpen = false;
    let requestController = null;
    let products = [];
    const selectedProducts = new Map();

    window.getSelectedEtfProductIds = function () {
        return Array.from(selectedProducts.keys());
    };

    window.getSelectedEtfProducts = function () {
        return Array.from(selectedProducts.values()).map(function (product) {
            return {...product};
        });
    };

    window.clearSelectedEtfProducts = function () {
        selectedProducts.clear();
        comparePanelOpen = false;
        notifySelectionChanged();

        if (products.length > 0 && (activeFilter === "ALL" || activeFilter === "ETF")) {
            renderExplorer(products, currentDataBaseDate, currentNotice, currentResultCount);
        }
    };

    window.updateEtfAllocationAmount = function (amount) {
        const normalized = Number(amount);
        allocationAmount = Number.isFinite(normalized) && normalized > 0
            ? normalized
            : 0;
        if (products.length > 0 && (activeFilter === "ALL" || activeFilter === "ETF")) {
            renderExplorer(products, currentDataBaseDate, currentNotice, currentResultCount);
        }
    };

    window.addEventListener("surplus:allocation-updated", function (event) {
        const allocations = event.detail && Array.isArray(event.detail.allocations)
            ? event.detail.allocations
            : [];
        const etfAllocation = allocations.find(function (allocation) {
            return allocation.assetType === "ETF";
        });
        window.updateEtfAllocationAmount(etfAllocation ? etfAllocation.amount : 0);
    });

    filterButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            activeFilter = button.dataset.productFilter || "ALL";
            if (activeFilter === "ALL" || activeFilter === "ETF") {
                window.setTimeout(loadProducts, 0);
            } else if (requestController) {
                requestController.abort();
            }
        });
    });

    if (exploreProductsButton) {
        exploreProductsButton.addEventListener("click", function () {
            window.setTimeout(function () {
                activeFilter = getSelectedFilter();
                if (activeFilter === "ALL" || activeFilter === "ETF") {
                    loadProducts();
                }
            }, 0);
        });
    }

    async function loadProducts() {
        if (requestController) {
            requestController.abort();
        }
        requestController = new AbortController();
        renderLoading();

        const query = new URLSearchParams({
            sort: currentSort,
            limit: String(visibleLimit)
        });
        if (currentKeyword) {
            query.set("keyword", currentKeyword);
        }

        try {
            const response = await fetch(API_URL + "?" + query.toString(), {
                method: "GET",
                headers: {"Accept": "application/json"},
                signal: requestController.signal
            });

            const body = await response.json().catch(function () {
                return null;
            });
            if (!response.ok) {
                throw new Error(body && body.message
                    ? body.message
                    : "ETF 상품 정보를 불러오지 못했습니다.");
            }

            if (activeFilter !== "ALL" && activeFilter !== "ETF") {
                return;
            }
            products = Array.isArray(body.products) ? body.products : [];
            currentDataBaseDate = body.dataBaseDate || null;
            currentNotice = body.notice || null;
            currentResultCount = Number.isFinite(Number(body.count))
                ? Number(body.count)
                : products.length;
            renderExplorer(products, currentDataBaseDate, currentNotice, currentResultCount);
        } catch (error) {
            if (error.name !== "AbortError") {
                renderError(error.message);
            }
        }
    }

    function renderExplorer(productItems, dataBaseDate, notice, resultCount) {
        productGrid.hidden = false;
        if (productEmptyState) {
            productEmptyState.hidden = true;
        }

        const wrapper = element("section", "etf-explorer");
        if (selectedProducts.size > 0) {
            wrapper.classList.add("has-compare-selection");
        }
        wrapper.append(createHeader(dataBaseDate, resultCount));

        const sortNotice = element("p", "etf-explorer__sort-notice");
        sortNotice.textContent = "거래대금·시가총액 등의 정렬은 공식 시세정보를 확인하기 위한 기준이며, "
            + "추천 또는 적합성 순위가 아닙니다. 레버리지·인버스 상품도 포함될 수 있습니다.";
        wrapper.append(sortNotice, createToolbar());

        if (productItems.length === 0) {
            const empty = element("p", "etf-explorer__empty");
            empty.textContent = "조건에 맞는 ETF 시세정보가 없습니다. 동기화 상태와 검색어를 확인해주세요.";
            wrapper.append(empty);
        } else {
            const cardGrid = element("div", "etf-explorer__grid");
            productItems.forEach(function (product) {
                cardGrid.append(createProductCard(product));
            });
            wrapper.append(cardGrid);
            if (resultCount >= visibleLimit) {
                wrapper.append(createLoadMoreButton());
            }
        }

        if (comparePanelOpen && selectedProducts.size > 0) {
            wrapper.append(createComparePanel());
        }

        const noticeElement = element("p", "etf-explorer__notice");
        noticeElement.textContent = notice ||
            "표시된 정보는 공식 시세 기준의 탐색·비교 정보이며, "
            + "특정 상품의 투자권유·자문이나 수익 보장을 의미하지 않습니다.";
        wrapper.append(noticeElement);

        if (selectedProducts.size > 0) {
            wrapper.append(createCompareBar());
        }

        productGrid.replaceChildren(wrapper);
    }

    function createHeader(dataBaseDate, resultCount) {
        const header = element("div", "etf-explorer__header");
        const titleGroup = element("div", "etf-explorer__title-group");
        const title = element("h3", "etf-explorer__title");
        title.textContent = "ETF 상품 탐색";
        const description = element("p", "etf-explorer__description");
        description.textContent = allocationAmount > 0
            ? "ETF 배정금액 " + formatWon(allocationAmount)
            + "을 기준으로 공식 시세와 단순 환산 결과를 비교합니다."
            : "공식 ETF 시세정보를 검색하고 직접 비교할 수 있습니다.";
        titleGroup.append(title, description);

        const meta = element("div", "etf-explorer__meta");
        const date = element("span", "etf-explorer__date");
        date.textContent = dataBaseDate ? "데이터 기준일 " + dataBaseDate : "기준일 확인 필요";
        const count = element("span", "etf-explorer__count");
        count.textContent = "현재 조회 상품 " + formatNumber(resultCount, 0) + "개";
        meta.append(date, count);
        header.append(titleGroup, meta);
        return header;
    }

    function createToolbar() {
        const form = element("form", "etf-explorer__toolbar");
        form.setAttribute("role", "search");

        const searchGroup = element("div", "etf-explorer__search-group");
        const label = element("label", "etf-explorer__search-label");
        label.setAttribute("for", "etfProductKeyword");
        label.textContent = "ETF 검색";
        const input = element("input", "etf-explorer__search");
        input.id = "etfProductKeyword";
        input.name = "keyword";
        input.type = "search";
        input.maxLength = 100;
        input.placeholder = "상품명·종목코드·기초지수";
        input.value = currentKeyword;
        searchGroup.append(label, input);

        const sortGroup = element("div", "etf-explorer__sort-group");
        const sortLabel = element("label", "etf-explorer__sort-label");
        sortLabel.setAttribute("for", "etfProductSort");
        sortLabel.textContent = "정렬 기준";
        const sort = element("select", "etf-explorer__sort");
        sort.id = "etfProductSort";
        [
            ["TRADING_VALUE_DESC", "거래대금 높은 순"],
            ["MARKET_CAP_DESC", "시가총액 높은 순"],
            ["FLUCTUATION_RATE_DESC", "등락률 높은 순"],
            ["NAME_ASC", "상품명 순"]
        ].forEach(function (optionData) {
            const option = document.createElement("option");
            option.value = optionData[0];
            option.textContent = optionData[1];
            option.selected = currentSort === optionData[0];
            sort.append(option);
        });
        sortGroup.append(sortLabel, sort);

        const submit = element("button", "etf-explorer__search-button");
        submit.type = "submit";
        submit.textContent = "검색";
        form.append(searchGroup, sortGroup, submit);

        form.addEventListener("submit", function (event) {
            event.preventDefault();
            currentKeyword = input.value.trim();
            currentSort = sort.value;
            visibleLimit = INITIAL_VISIBLE_LIMIT;
            loadProducts();
        });
        sort.addEventListener("change", function () {
            currentSort = sort.value;
            visibleLimit = INITIAL_VISIBLE_LIMIT;
            loadProducts();
        });
        return form;
    }

    function createLoadMoreButton() {
        const button = element("button", "etf-explorer__load-more");
        button.type = "button";
        button.textContent = LOAD_MORE_STEP + "개 더 보기";
        button.addEventListener("click", function () {
            visibleLimit += LOAD_MORE_STEP;
            loadProducts();
        });
        return button;
    }

    function createProductCard(product) {
        const productId = product.investmentProductId;
        const isSelected = selectedProducts.has(productId);
        const article = element("article", "etf-product-card");
        article.dataset.productId = String(productId);
        if (isSelected) {
            article.classList.add("is-selected");
        }

        const actionRow = element("div", "etf-product-card__action-row");
        const badge = element("span", "etf-product-card__badge");
        badge.textContent = "ETF";
        const compareButton = element("button", "etf-product-card__compare");
        compareButton.type = "button";
        compareButton.textContent = isSelected ? "선택됨" : "비교 담기";
        compareButton.setAttribute("aria-pressed", String(isSelected));
        compareButton.addEventListener("click", function () {
            toggleCompare(product);
        });
        actionRow.append(badge, compareButton);

        const title = element("h4", "etf-product-card__name");
        title.textContent = product.productName || "상품명 정보 미제공";
        const meta = element("p", "etf-product-card__meta");
        meta.textContent = "종목코드 " + (product.productCode || "-")
            + " · 데이터 기준일 " + (product.priceBaseDate || "-")
            + " · 기초지수 " + (product.baseIndexName || "정보 미제공");

        const core = element("div", "etf-product-card__core");
        const priceGroup = element("div", "etf-product-card__price-group");
        const priceLabel = element("span", "etf-product-card__core-label");
        priceLabel.textContent = "종가";
        const price = element("strong", "etf-product-card__price");
        price.textContent = formatWon(product.closingPrice);
        const rate = element("span", "etf-product-card__rate");
        rate.textContent = "등락률 " + formatPercent(product.fluctuationRate);
        priceGroup.append(priceLabel, price, rate);
        core.append(priceGroup, createAllocationEstimate(product));

        const metrics = element("dl", "etf-product-card__metrics");
        appendMetric(metrics, "NAV", formatNumber(product.nav, 2));
        appendCompactWonMetric(metrics, "거래대금", product.tradingValue);
        appendCompactWonMetric(metrics, "시가총액", product.marketCap);
        appendCompactWonMetric(metrics, "순자산총액", product.netAssetTotalAmount);
        appendMetric(
            metrics,
            "기초지수",
            product.baseIndexName || "정보 미제공",
            {fullWidth: true}
        );

        const details = createProductDetails(product);
        article.append(actionRow, title, meta, core, metrics, details);
        return article;
    }

    function createAllocationEstimate(product) {
        const box = element("div", "etf-product-card__estimate");
        const label = element("strong", "etf-product-card__estimate-title");
        label.textContent = "ETF 배정금액 기준 단순 환산";
        const text = element("p", "etf-product-card__estimate-value");

        const price = Number(product.closingPrice);
        if (allocationAmount > 0 && Number.isFinite(price) && price > 0) {
            const quantity = Math.floor(allocationAmount / price);
            const remaining = allocationAmount - quantity * price;
            text.textContent = "약 " + formatNumber(quantity, 0)
                + "주 · 잔여 " + formatWon(remaining);
        } else {
            text.textContent = "배정금액 또는 종가 정보가 필요합니다.";
        }

        const caution = element("small", "etf-product-card__estimate-notice");
        caution.textContent = "종가만 사용한 단순 계산이며 실제 체결가격·수수료·매수 가능 여부와 다를 수 있습니다.";
        box.append(label, text, caution);
        return box;
    }

    function createProductDetails(product) {
        const details = document.createElement("details");
        details.className = "etf-product-card__details";
        const summary = document.createElement("summary");
        summary.textContent = "시세 상세 확인";
        const list = element("dl", "etf-product-card__detail-grid");
        appendMetric(list, "시가", formatWon(product.openingPrice));
        appendMetric(list, "고가", formatWon(product.highPrice));
        appendMetric(list, "저가", formatWon(product.lowPrice));
        appendMetric(list, "거래량", formatNumber(product.tradingVolume, 0));
        appendMetric(list, "거래대금(원 단위)", formatWon(product.tradingValue), {fullWidth: true});
        appendMetric(list, "시가총액(원 단위)", formatWon(product.marketCap), {fullWidth: true});
        appendMetric(list, "순자산총액(원 단위)", formatWon(product.netAssetTotalAmount), {fullWidth: true});
        details.append(summary, list);
        return details;
    }

    function toggleCompare(product) {
        const id = product.investmentProductId;
        if (selectedProducts.has(id)) {
            selectedProducts.delete(id);
            if (selectedProducts.size === 0) {
                comparePanelOpen = false;
            }
        } else {
            if (selectedProducts.size >= MAX_COMPARE_COUNT) {
                window.alert(
                    "ETF는 최대 "
                    + MAX_COMPARE_COUNT
                    + "개까지 비교할 수 있습니다."
                );
                return;
            }
            selectedProducts.set(id, product);
        }
        notifySelectionChanged();
        renderExplorer(products, currentDataBaseDate, currentNotice, currentResultCount);
    }

    function createCompareBar() {
        const bar = element("section", "etf-compare-bar");
        bar.setAttribute("aria-label", "ETF 비교 상품 선택");
        const count = element("strong", "etf-compare-bar__count");
        count.textContent = "비교 상품 " + selectedProducts.size + "/" + MAX_COMPARE_COUNT;
        const chips = element("div", "etf-compare-bar__chips");
        selectedProducts.forEach(function (product) {
            const chip = element("span", "etf-compare-bar__chip");
            chip.textContent = product.productName || product.productCode || "ETF";
            chip.title = chip.textContent;
            chips.append(chip);
        });

        const actions = element("div", "etf-compare-bar__actions");
        const clear = element("button", "etf-compare-bar__clear");
        clear.type = "button";
        clear.textContent = "전체 해제";
        clear.addEventListener("click", function () {
            selectedProducts.clear();
            comparePanelOpen = false;
            notifySelectionChanged();
            renderExplorer(products, currentDataBaseDate, currentNotice, currentResultCount);
        });
        const compare = element("button", "etf-compare-bar__open");
        compare.type = "button";
        compare.textContent = "비교하기";
        compare.setAttribute("aria-controls", COMPARE_PANEL_ID);
        compare.setAttribute("aria-expanded", String(comparePanelOpen));
        compare.addEventListener("click", function () {
            comparePanelOpen = true;
            renderExplorer(products, currentDataBaseDate, currentNotice, currentResultCount);
            const panel = document.getElementById(COMPARE_PANEL_ID);
            if (panel) {
                panel.scrollIntoView({behavior: "smooth", block: "start"});
            }
        });
        actions.append(clear, compare);
        bar.append(count, chips, actions);
        return bar;
    }

    function createComparePanel() {
        const section = element("section", "etf-compare");
        section.id = COMPARE_PANEL_ID;
        section.setAttribute("aria-live", "polite");
        section.setAttribute("aria-labelledby", "etfCompareTitle");

        const header = element("div", "etf-compare__header");
        const title = element("h4", "etf-compare__title");
        title.id = "etfCompareTitle";
        title.textContent = "ETF 직접 비교";
        const count = element("span", "etf-compare__count");
        count.textContent = "선택 " + selectedProducts.size + "개";
        const close = element("button", "etf-compare__close");
        close.type = "button";
        close.textContent = "닫기";
        close.setAttribute("aria-label", "ETF 직접 비교 패널 닫기");
        close.addEventListener("click", function () {
            comparePanelOpen = false;
            renderExplorer(products, currentDataBaseDate, currentNotice, currentResultCount);
        });
        header.append(title, count, close);

        const tableWrap = element("div", "etf-compare__table-wrap");
        tableWrap.setAttribute("tabindex", "0");
        const table = element("table", "etf-compare__table");
        const head = document.createElement("thead");
        const headRow = document.createElement("tr");
        ["상품명", "종가", "등락률", "거래대금", "시가총액", "배정금액 기준 단순 환산 수량"]
            .forEach(function (text) {
                const th = document.createElement("th");
                th.scope = "col";
                th.textContent = text;
                headRow.append(th);
            });
        head.append(headRow);

        const body = document.createElement("tbody");
        selectedProducts.forEach(function (product) {
            const row = document.createElement("tr");
            const price = Number(product.closingPrice);
            const quantity = allocationAmount > 0 && Number.isFinite(price) && price > 0
                ? Math.floor(allocationAmount / price)
                : null;
            appendTableCell(row, product.productName || "상품명 정보 미제공");
            appendTableCell(row, formatWon(product.closingPrice));
            appendTableCell(row, formatPercent(product.fluctuationRate));
            appendCompactWonTableCell(row, product.tradingValue);
            appendCompactWonTableCell(row, product.marketCap);
            appendTableCell(row, quantity == null ? "-" : formatNumber(quantity, 0) + "주");
            body.append(row);
        });
        table.append(head, body);
        tableWrap.append(table);

        const notice = element("p", "etf-compare__notice");
        notice.textContent = "표시된 정보는 공식 시세 기준의 탐색·비교 정보이며, "
            + "특정 상품의 투자권유·자문이나 수익 보장을 의미하지 않습니다.";
        section.append(header, tableWrap, notice);
        return section;
    }

    function appendCompactWonMetric(list, labelText, rawValue) {
        const exactValue = formatWon(rawValue);
        appendMetric(list, labelText, formatCompactWon(rawValue), {
            title: exactValue,
            ariaLabel: labelText + " " + exactValue,
            compact: true
        });
    }

    function appendMetric(list, labelText, valueText, options) {
        const settings = options || {};
        const group = element("div", "etf-product-card__metric");
        if (settings.fullWidth) {
            group.classList.add("is-full-width");
        }
        const term = document.createElement("dt");
        term.textContent = labelText;
        const value = document.createElement("dd");
        value.textContent = valueText;
        if (settings.compact) {
            value.classList.add("is-compact-money");
        }
        if (settings.title) {
            value.title = settings.title;
        }
        if (settings.ariaLabel) {
            value.setAttribute("aria-label", settings.ariaLabel);
        }
        group.append(term, value);
        list.append(group);
    }

    function appendTableCell(row, text) {
        const cell = document.createElement("td");
        cell.textContent = text;
        row.append(cell);
    }

    function appendCompactWonTableCell(row, rawValue) {
        const exactValue = formatWon(rawValue);
        const cell = document.createElement("td");
        cell.className = "etf-compare__compact-money";
        cell.textContent = formatCompactWon(rawValue);
        cell.title = exactValue;
        cell.setAttribute("aria-label", exactValue);
        row.append(cell);
    }

    function renderLoading() {
        productGrid.hidden = false;
        if (productEmptyState) {
            productEmptyState.hidden = true;
        }
        const loading = element("div", "etf-explorer__loading");
        loading.setAttribute("role", "status");
        loading.textContent = "ETF 공식 시세정보를 불러오는 중입니다.";
        productGrid.replaceChildren(loading);
    }

    function renderError(message) {
        const error = element("div", "etf-explorer__error");
        error.setAttribute("role", "alert");
        error.textContent = message;
        productGrid.replaceChildren(error);
    }

    function getSelectedFilter() {
        const selected = filterButtons.find(function (button) {
            return button.classList.contains("active")
                || button.classList.contains("is-active")
                || button.getAttribute("aria-pressed") === "true";
        });
        return selected ? selected.dataset.productFilter : "ALL";
    }

    function readAllocationAmountFromFilter() {
        const amountElement = document.getElementById("productFilterEtfAmount");
        if (!amountElement) {
            return 0;
        }
        const value = Number(amountElement.textContent.replace(/[^0-9.-]/g, ""));
        return Number.isFinite(value) && value > 0 ? value : 0;
    }

    function formatCompactWon(value) {
        const number = Number(value);
        if (!Number.isFinite(number)) {
            return "정보 미제공";
        }
        const absolute = Math.abs(number);
        if (absolute >= 1000000000000) {
            return formatNumber(number / 1000000000000, 2) + "조원";
        }
        if (absolute >= 100000000) {
            return formatNumber(number / 100000000, 2) + "억원";
        }
        return formatWon(number);
    }

    function formatWon(value) {
        const number = Number(value);
        return Number.isFinite(number)
            ? Math.round(number).toLocaleString("ko-KR") + "원"
            : "정보 미제공";
    }

    function formatPercent(value) {
        const number = Number(value);
        if (!Number.isFinite(number)) {
            return "정보 미제공";
        }
        return (number > 0 ? "+" : "")
            + number.toLocaleString("ko-KR", {maximumFractionDigits: 2})
            + "%";
    }

    function formatNumber(value, fractionDigits) {
        const number = Number(value);
        return Number.isFinite(number)
            ? number.toLocaleString("ko-KR", {
                minimumFractionDigits: 0,
                maximumFractionDigits: fractionDigits
            })
            : "정보 미제공";
    }

    function element(tagName, className) {
        const node = document.createElement(tagName);
        if (className) {
            node.className = className;
        }
        return node;
    }

    function notifySelectionChanged() {
        window.dispatchEvent(new CustomEvent("surplus:etf-selection-changed", {
            detail: {
                productIds: Array.from(selectedProducts.keys()),
                products: Array.from(selectedProducts.values()).map(function (product) {
                    return {...product};
                })
            }
        }));
    }
})();
