const password = document.getElementById('password');
const passwordConfirm = document.getElementById('passwordConfirm');
const passwordMessage = document.getElementById('passwordMessage');
const resetForm = document.getElementById('resetForm');

function checkPasswordMatch() {
    if (!passwordConfirm.value) {
        passwordMessage.textContent = '';
        return false;
    }

    if (password.value === passwordConfirm.value) {
        passwordMessage.textContent = '비밀번호가 일치합니다.';
        return true;
    }

    passwordMessage.textContent = '비밀번호가 일치하지 않습니다.';
    return false;
}

password.addEventListener('input', checkPasswordMatch);
passwordConfirm.addEventListener('input', checkPasswordMatch);

resetForm.addEventListener('submit', function (event) {
    if (!checkPasswordMatch()) {
        event.preventDefault();
        passwordConfirm.focus();
    }
});