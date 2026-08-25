const profileEditBtn =
    document.getElementById('profileEditBtn');

const profileSaveBtn =
    document.getElementById('profileSaveBtn');

const userName =
    document.getElementById('userName');

const phoneNumber =
    document.getElementById('phoneNumber');

const birthDate =
    document.getElementById('birthDate');


profileEditBtn.addEventListener('click', () => {
    userName.readOnly = false;
    phoneNumber.readOnly = false;

    birthDate.type = 'date';
    birthDate.readOnly = false;

    profileEditBtn.hidden = true;
    profileSaveBtn.hidden = false;

    userName.focus();
});


phoneNumber.addEventListener('input', function () {
    this.value = this.value
        .replace(/[^0-9]/g, '')
        .slice(0, 11);
});


const passwordEditBtn =
    document.getElementById('passwordEditBtn');

const passwordSaveBtn =
    document.getElementById('passwordSaveBtn');

const passwordView =
    document.getElementById('passwordView');

const passwordEditArea =
    document.getElementById('passwordEditArea');

const newPassword =
    document.getElementById('newPassword');

const passwordConfirm =
    document.getElementById('passwordConfirm');

const passwordMessage =
    document.getElementById('passwordMessage');


passwordEditBtn.addEventListener('click', () => {

    passwordView.hidden = true;
    passwordEditArea.hidden = false;

    newPassword.disabled = false;
    passwordConfirm.disabled = false;

    passwordEditBtn.hidden = true;
    passwordSaveBtn.hidden = false;

    newPassword.focus();
});


function checkPasswordMatch() {

    if (!passwordConfirm.value) {
        passwordMessage.textContent = '';
        return;
    }

    if (newPassword.value === passwordConfirm.value) {
        passwordMessage.textContent =
            '※ 비밀번호가 일치합니다.';
    } else {
        passwordMessage.textContent =
            '※ 비밀번호가 일치하지 않습니다.';
    }
}

newPassword.addEventListener(
    'input',
    checkPasswordMatch
);

passwordConfirm.addEventListener(
    'input',
    checkPasswordMatch
);


const params =
    new URLSearchParams(window.location.search);

if (params.has('updated')) {
    alert('회원 정보가 수정되었습니다.');
    history.replaceState(null, '', '/profile');
}

if (params.has('passwordUpdated')) {
    alert('비밀번호가 변경되었습니다.');
    history.replaceState(null, '', '/profile');
}

const passwordForm = passwordSaveBtn.closest('form');

passwordForm.addEventListener('submit', event => {

    if (newPassword.value !== passwordConfirm.value) {
        event.preventDefault();

        passwordMessage.textContent = '비밀번호가 일치하지 않습니다.';
        passwordConfirm.focus();
    }
});