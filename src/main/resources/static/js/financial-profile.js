document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("financialProfileForm");
    const editButton = document.getElementById("editButton");
    const saveButton = document.getElementById("saveButton");
    const cancelButton = document.getElementById("cancelButton");

    if (!form || !editButton || !saveButton || !cancelButton) {
        return;
    }

    const editableFields = form.querySelectorAll("[data-editable]");
    const initialValues = new Map();

    editableFields.forEach((field) => {
        initialValues.set(field, field.value);
    });

    editButton.addEventListener("click", () => {
        setEditMode(true);
    });

    cancelButton.addEventListener("click", () => {
        restoreInitialValues();
        setEditMode(false);
    });

    form.addEventListener("submit", (event) => {
        const hasChanged = Array.from(editableFields).some((field) => {
            return field.value !== initialValues.get(field);
        });

        if (!hasChanged) {
            event.preventDefault();

            alert("수정된 정보가 없습니다.");

            restoreInitialValues();
            setEditMode(false);
        }
    });

    function restoreInitialValues() {
        editableFields.forEach((field) => {
            field.value = initialValues.get(field);
        });
    }

    function setEditMode(isEditing) {
        editableFields.forEach((field) => {
            field.disabled = !isEditing;
        });

        editButton.hidden = isEditing;
        saveButton.hidden = !isEditing;
        cancelButton.hidden = !isEditing;

        if (isEditing && editableFields.length > 0) {
            editableFields[0].focus();
        }
    }
});