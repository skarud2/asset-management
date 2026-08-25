
document.addEventListener("DOMContentLoaded", () => {
    initializeHeroCarousel();
    initializeServiceTabs();
    initializeRevealEffects();
    initializeProfilePopover();
    initializeTokenRefresh();
});

function initializeHeroCarousel() {
    const carousel = document.querySelector("[data-carousel]");
    if (!carousel) return;

    const slides = Array.from(carousel.querySelectorAll(".hero-slide"));
    const pages = Array.from(carousel.querySelectorAll("[data-carousel-page]"));
    const previousButton = carousel.querySelector("[data-carousel-prev]");
    const nextButton = carousel.querySelector("[data-carousel-next]");
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const intervalMilliseconds = 3000;

    let currentIndex = 0;
    let timer = null;

    function showSlide(index) {
        currentIndex = (index + slides.length) % slides.length;

        slides.forEach((slide, slideIndex) => {
            const active = slideIndex === currentIndex;
            slide.classList.toggle("active", active);
            slide.setAttribute("aria-hidden", String(!active));
        });

        pages.forEach((page, pageIndex) => {
            const active = pageIndex === currentIndex;
            page.classList.toggle("active", active);
            if (active) {
                page.setAttribute("aria-current", "true");
            } else {
                page.removeAttribute("aria-current");
            }
        });
    }

    function stopAutoPlay() {
        if (timer !== null) {
            window.clearInterval(timer);
            timer = null;
        }
    }

    function startAutoPlay() {
        stopAutoPlay();
        if (!reduceMotion && !document.hidden) {
            timer = window.setInterval(() => showSlide(currentIndex + 1), intervalMilliseconds);
        }
    }

    previousButton?.addEventListener("click", () => {
        showSlide(currentIndex - 1);
        startAutoPlay();
    });

    nextButton?.addEventListener("click", () => {
        showSlide(currentIndex + 1);
        startAutoPlay();
    });

    pages.forEach(page => {
        page.addEventListener("click", () => {
            showSlide(Number(page.dataset.carouselPage));
            startAutoPlay();
        });
    });

    carousel.addEventListener("mouseenter", stopAutoPlay);
    carousel.addEventListener("mouseleave", startAutoPlay);
    carousel.addEventListener("focusin", stopAutoPlay);
    carousel.addEventListener("focusout", event => {
        if (!carousel.contains(event.relatedTarget)) startAutoPlay();
    });

    document.addEventListener("visibilitychange", () => {
        if (document.hidden) stopAutoPlay();
        else startAutoPlay();
    });

    showSlide(0);
    startAutoPlay();
}

function initializeServiceTabs() {
    document.querySelectorAll("[data-service-tabs]").forEach(container => {
        const tabs = Array.from(container.querySelectorAll("[data-service-tab]"));
        const panels = Array.from(container.querySelectorAll("[data-service-panel]"));

        tabs.forEach(tab => {
            tab.addEventListener("click", () => {
                const selected = tab.dataset.serviceTab;
                tabs.forEach(item => item.classList.toggle("active", item === tab));
                panels.forEach(panel => {
                    panel.hidden = panel.dataset.servicePanel !== selected;
                });
            });
        });
    });
}

function initializeRevealEffects() {
    const targets = document.querySelectorAll(".product-area, .sidebar");
    if (!("IntersectionObserver" in window)) {
        targets.forEach(target => target.classList.add("active"));
        return;
    }

    const observer = new IntersectionObserver(entries => {
        entries.forEach(entry => entry.target.classList.toggle("active", entry.isIntersecting));
    }, { threshold: 0.08 });

    targets.forEach(target => {
        target.classList.add("reveal-element");
        observer.observe(target);
    });
}

function initializeProfilePopover() {
    const button = document.getElementById("userAvatarBtn");
    const popover = document.getElementById("profilePopover");
    if (!button || !popover) return;

    button.addEventListener("click", event => {
        event.stopPropagation();
        const active = popover.classList.toggle("active");
        button.setAttribute("aria-expanded", String(active));
    });

    document.addEventListener("click", event => {
        if (!popover.contains(event.target) && !button.contains(event.target)) {
            popover.classList.remove("active");
            button.setAttribute("aria-expanded", "false");
        }
    });
}

function initializeTokenRefresh() {
    const refreshBtn = document.getElementById("headerTokenRefreshBtn");
    const remainingTime = document.getElementById("headerSessionRemainingTime");
    if (!refreshBtn || !remainingTime) return;

    const deadlineKey = "viaTokenExtensionDeadline";
    let countdownId = null;

    function formatRemainingTime(totalSeconds) {
        const minutes = Math.floor(totalSeconds / 60);
        const seconds = totalSeconds % 60;
        return `${minutes}분 ${String(seconds).padStart(2, "0")}초`;
    }

    function renderCountdown() {
        const deadline = Number(sessionStorage.getItem(deadlineKey) || 0);
        if (!deadline) {
            remainingTime.textContent = "00분 00초";
            refreshBtn.setAttribute("aria-label", "접속시간 1시간 연장");
            return false;
        }

        const remainingSeconds = Math.max(
            0,
            Math.ceil((deadline - Date.now()) / 1000)
        );
        if (remainingSeconds <= 0) {
            sessionStorage.removeItem(deadlineKey);
            remainingTime.textContent = "00분 00초";
            refreshBtn.setAttribute("aria-label", "접속시간 1시간 연장");
            if (countdownId) clearInterval(countdownId);
            countdownId = null;
            return false;
        }

        remainingTime.textContent = formatRemainingTime(remainingSeconds);
        refreshBtn.setAttribute("aria-label", `접속시간 1시간 연장, 현재 남은 시간 ${remainingTime.textContent}`);
        return true;
    }

    function startCountdown() {
        sessionStorage.setItem(deadlineKey, String(Date.now() + 3_599_000));
        if (countdownId) clearInterval(countdownId);
        renderCountdown();
        countdownId = setInterval(renderCountdown, 1000);
    }

    async function initializeCountdownFromLogin() {
        try {
            const response = await fetch("/api/auth/token/extension-status", {
                headers: {"Accept": "application/json"}
            });
            if (response.ok) {
                const status = await response.json();
                const remainingSeconds = Number(
                    status.remainingSeconds ?? status.remaining_seconds ?? 0
                );
                if (remainingSeconds > 0) {
                    sessionStorage.setItem(
                        deadlineKey,
                        String(Date.now() + remainingSeconds * 1000)
                    );
                } else {
                    sessionStorage.removeItem(deadlineKey);
                }
            }
        } catch (error) {
            console.error("Session extension status failed:", error);
        }

        if (renderCountdown()) {
            countdownId = setInterval(renderCountdown, 1000);
        }
    }

    initializeCountdownFromLogin();

    refreshBtn.addEventListener("click", async () => {
        if (refreshBtn.disabled) return;

        refreshBtn.disabled = true;
        refreshBtn.textContent = "연장 중...";
        try {
            const response = await fetch("/api/auth/token/extend", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                }
            });

            if (response.ok) {
                const message = await response.text();
                startCountdown();
                showToast(message || "토큰 및 세션 시간이 성공적으로 연장되었습니다.", "success");
            } else {
                const errorText = await response.text();
                showToast(errorText || "토큰 재발급 중 오류가 발생했습니다.", "error");
            }
        } catch (error) {
            console.error("Token refresh failed:", error);
            showToast("네트워크 오류가 발생했습니다.", "error");
        } finally {
            refreshBtn.disabled = false;
            refreshBtn.textContent = "연장";
            renderCountdown();
        }
    });
}

function showToast(message, type = "success") {
    let toast = document.getElementById("viaToast");
    if (!toast) {
        toast = document.createElement("div");
        toast.id = "viaToast";
        toast.className = "via-toast";
        document.body.appendChild(toast);
    }

    toast.className = `via-toast ${type} show`;
    toast.innerHTML = `<span class="material-symbols-outlined">${type === 'success' ? 'check_circle' : 'error'}</span><span>${message}</span>`;

    setTimeout(() => {
        toast.classList.remove("show");
    }, 3000);
}
