document.addEventListener("DOMContentLoaded", () => {
    const loanType = document.getElementById("loanType");
    const mortgageFields =
        document.getElementById("mortgageFields");
    const interestRateFields =
        document.getElementById("interestRateFields");
    const jeonseFields =
        document.getElementById("jeonseFields");

    function updateConditionalFields() {
        const type = loanType.value;

        const isMortgage = type === "MORTGAGE_LOAN";
        const isCredit = type === "CREDIT_LOAN";
        const isJeonse = type === "JEONSE_LOAN";

        mortgageFields.hidden = !isMortgage;
        interestRateFields.hidden = !(isMortgage || isCredit);
        jeonseFields.hidden = !isJeonse;

        propertyRegion.disabled = !isMortgage;
        propertyRegion.required = isMortgage;

        interestRateType.disabled = !(isMortgage || isCredit);
        interestRateType.required = isMortgage || isCredit;

        housingOwnershipType.disabled = !isJeonse;
        housingOwnershipType.required = isJeonse;

        rentalPropertyRegion.disabled = !isJeonse;
        rentalPropertyRegion.required = isJeonse;
    }

    loanType.addEventListener("change", updateConditionalFields);

    updateConditionalFields();
});