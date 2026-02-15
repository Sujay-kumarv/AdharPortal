// State
let currentStep = 1;
const API_BASE = 'http://127.0.0.1:8080/api';

// Step Navigation
function nextStep(step) {
    if (!validateStep(step)) return;

    document.getElementById(`step-${step}`).classList.remove('active');
    document.getElementById(`step-${step + 1}`).classList.add('active');

    document.getElementById(`step-indicator-${step + 1}`).classList.add('active');
    currentStep = step + 1;
}

function prevStep(step) {
    document.getElementById(`step-${step}`).classList.remove('active');
    document.getElementById(`step-${step - 1}`).classList.add('active');

    document.getElementById(`step-indicator-${step}`).classList.remove('active');
    currentStep = step - 1;
}

// Validation
function validateStep(step) {
    const stepDiv = document.getElementById(`step-${step}`);
    const inputs = stepDiv.querySelectorAll('input, select');
    let isValid = true;

    inputs.forEach(input => {
        if (input.hasAttribute('required') && !input.value) {
            isValid = false;
            input.style.borderColor = 'var(--danger-neon)';
            // Reset border after 2 seconds
            setTimeout(() => input.style.borderColor = 'var(--glass-border)', 2000);
        }
    });

    if (step === 1) {
        const password = document.getElementById('password').value;
        const confirmPassword = document.getElementById('confirmPassword').value;

        if (password !== confirmPassword) {
            alert('❌ Passwords do not match!');
            isValid = false;
        }


    }

    if (!isValid && step !== 1) alert('Please fill all required fields correctly.');
    return isValid;
}

// Email Verification State


function validateEmail(email) {
    return String(email)
        .toLowerCase()
        .match(
            /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/
        );
}

// Image Preview
function previewImage(input, imgId) {
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function (e) {
            const img = document.getElementById(imgId);
            img.src = e.target.result;
            img.style.display = 'block';
        }
        reader.readAsDataURL(input.files[0]);
    }
}

// Form Submission
document.getElementById('registrationForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const formData = new FormData(e.target);

    try {
        const response = await fetch(`${API_BASE}/users/register`, {
            method: 'POST',
            body: formData
        });

        if (response.ok) {
            // Show Success Animation
            const overlay = document.getElementById('successOverlay');
            overlay.style.display = 'flex';
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 3000);
        } else {
            alert('Registration Failed. Please try again.');
        }
    } catch (error) {
        console.error('Submission Error:', error);
        alert('Server Error');
    }
});
