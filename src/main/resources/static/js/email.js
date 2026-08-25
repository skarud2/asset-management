const loginEmail = document.querySelector('#loginEmail');
const sendCodeButton = document.querySelector('#sendCodeButton');
const verificationArea = document.querySelector('#verificationArea');
const verificationCode = document.querySelector('#verificationCode');
const verifyCodeButton = document.querySelector('#verifyCodeButton');
const emailVerificationMessage = document.querySelector('#emailVerificationMessage');
const nextButton = document.querySelector('#nextButton');
const csrfToken = document
    .querySelector('meta[name="_csrf"]')
    .getAttribute('content');

const csrfHeader = document
    .querySelector('meta[name="_csrf_header"]')
    .getAttribute('content');

sendCodeButton.addEventListener('click', async () => {
    if (!loginEmail.checkValidity()) {
        loginEmail.reportValidity();
        return;
    }

    sendCodeButton.disabled = true;
    emailVerificationMessage.textContent =
        '인증번호를 전송하고 있습니다.';
    emailVerificationMessage.className = 'success-message';

    try {
        const data = await requestEmailVerification(
            '/api/email-verify/send',
            {
                email: loginEmail.value
            }
        );

        verificationArea.hidden = false;
        verificationArea.classList.remove('hidden');
        verificationCode.focus();

        emailVerificationMessage.textContent = data.message;
        emailVerificationMessage.className = 'success-message';
    } catch (error) {
        emailVerificationMessage.textContent = error.message;
        emailVerificationMessage.className = 'field-error';
    } finally {
        sendCodeButton.disabled = false;
    }
});

verifyCodeButton.addEventListener('click', async () => {
    const code = verificationCode.value.trim();

    if (!code) {
        emailVerificationMessage.textContent =
            '인증번호를 입력해주세요.';
        emailVerificationMessage.className = 'field-error';
        verificationCode.focus();
        return;
    }

    verifyCodeButton.disabled = true;

    try {
        const data = await requestEmailVerification(
            '/api/email-verify/verify',
            {
                email: loginEmail.value,
                code
            }
        );

        emailVerificationMessage.textContent = data.message;
        emailVerificationMessage.className = 'success-message';

        loginEmail.readOnly = true;
        verificationCode.readOnly = true;
        sendCodeButton.disabled = true;
        verifyCodeButton.disabled = true;

        nextButton.disabled = false;
    } catch (error) {
        emailVerificationMessage.textContent = error.message;
        emailVerificationMessage.className = 'field-error';

        verificationCode.focus();
        verifyCodeButton.disabled = false;
    }
});

async function requestEmailVerification(url, body) {
    const response = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify(body)
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(
            data.message || '요청 처리 중 오류가 발생했습니다.'
        );
    }

    return data;
}

nextButton.addEventListener('click', () => {
    window.location.href = '/signup';
});