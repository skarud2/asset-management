document.addEventListener('DOMContentLoaded', () => {
    const purposeSelect = document.getElementById('loanPurpose');
    if (!purposeSelect) {
        return;
    }

    const housingFields = document.querySelectorAll('.housing-condition');
    const mortgageFields = document.querySelectorAll('.mortgage-condition');

    const setFieldEnabled = (field, enabled) => {
        field.hidden = !enabled;
        field.querySelectorAll('select, input').forEach((input) => {
            input.disabled = !enabled;
        });
    };

    const updateVisibility = () => {
        const selectedOption = purposeSelect.options[purposeSelect.selectedIndex];
        const loanType = selectedOption?.dataset.loanType || 'CREDIT';
        const isHousing = loanType === 'MORTGAGE' || loanType === 'JEONSE';
        const isMortgage = loanType === 'MORTGAGE';

        housingFields.forEach((field) => setFieldEnabled(field, isHousing));
        mortgageFields.forEach((field) => setFieldEnabled(field, isMortgage));
    };

    purposeSelect.addEventListener('change', updateVisibility);
    updateVisibility();
});
