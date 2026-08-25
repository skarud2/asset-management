const signupForm = document.querySelector('#signupForm');
const password = document.querySelector('#password');
const passwordConfirm = document.querySelector('#passwordConfirm');
const passwordMessage = document.querySelector('#passwordMessage');
const phoneNumber = document.getElementById('phoneNumber');

function checkPasswordMatch() {
    if (!passwordConfirm.value) {
        passwordMessage.textContent = '';
        return false;
    }

    if (password.value === passwordConfirm.value) {
        passwordMessage.textContent =
            '비밀번호가 일치합니다.';
        passwordMessage.className = 'field-error';
        return true;
    }

    passwordMessage.textContent =
        '비밀번호가 일치하지 않습니다.';
    passwordMessage.className = 'field-error';
    return false;
}

password.addEventListener('input', checkPasswordMatch);
passwordConfirm.addEventListener(
    'input',
    checkPasswordMatch
);

signupForm.addEventListener('submit', event => {
    if (!checkPasswordMatch()) {
        event.preventDefault();
        passwordConfirm.focus();
    }
});

phoneNumber.addEventListener('input', function () {
    this.value = this.value
        .replace(/[^0-9]/g, '')
        .slice(0, 11);
});