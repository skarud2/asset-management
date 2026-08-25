document.addEventListener("DOMContentLoaded", () => {
    const connectButton =
        document.querySelector(".connect-button");

    if (!connectButton) {
        return;
    }

    connectButton.addEventListener("click", () => {
        connectButton.textContent = "연동 중...";
    });
});