const sendButton = document.getElementById('sendCodeButton');
const verifyButton = document.getElementById('verifyCodeButton');
const nextButton = document.getElementById('nextButton');

const email = document.getElementById('loginEmail');
const code = document.getElementById('verificationCode');
const verificationArea = document.getElementById('verificationArea');
const message = document.getElementById('emailVerificationMessage');

const csrfToken = document
    .querySelector('meta[name="_csrf"]')
    .getAttribute('content');

const csrfHeader = document
    .querySelector('meta[name="_csrf_header"]')
    .getAttribute('content');

sendButton.addEventListener('click', async () => {
    const response = await fetch('/api/email-verify/password/send', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify({
            email: email.value
        })
    });

    const data = await response.json();

    if (!response.ok) {
        message.textContent = data.message;
        return;
    }

    message.textContent = data.message;
    verificationArea.hidden = false;
});

verifyButton.addEventListener('click', async () => {
    const response = await fetch('/api/email-verify/password/verify', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            [csrfHeader]: csrfToken
        },
        body: JSON.stringify({
            email: email.value,
            code: code.value
        })
    });

    const data = await response.json();

    if (!response.ok) {
        message.textContent = data.message;
        return;
    }

    message.textContent = data.message;
    nextButton.disabled = false;
});

nextButton.addEventListener('click', () => {
    window.location.href = '/find/pw/reset';
});
