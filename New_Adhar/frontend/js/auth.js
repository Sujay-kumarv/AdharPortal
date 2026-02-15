const AUTH_API = 'http://127.0.0.1:8080/api/auth';
const IDLE_TIMEOUT = 15 * 60 * 1000; // 15 minutes
let idleTimer;

console.log("[Auth] Script Loaded");

// --- Route Guard ---
async function checkAuth() {
    console.log("[Auth] Checking...");
    // Skip check if on login page
    if (window.location.pathname.includes('login.html')) return;

    try {
        const token = localStorage.getItem('accessToken');
        if (!token) {
            window.location.href = 'login.html';
            return;
        }

        const response = await fetch(`${AUTH_API}/validate`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            credentials: 'include' // Important for sending cookies
        });

        if (!response.ok) {
            // Invalid Session
            window.location.href = 'login.html';
        } else {
            // Valid Session - Start Idle Timer
            startIdleTimer();
        }
    } catch (error) {
        console.error("Auth Check Failed", error);
        window.location.href = 'login.html';
    }
}

// --- Logout Logic ---
async function logout() {
    try {
        await fetch(`${AUTH_API}/logout`, {
            method: 'POST',
            credentials: 'include'
        });
        localStorage.removeItem('accessToken');
        window.location.href = 'login.html';
    } catch (error) {
        console.error("Logout Error", error);
        window.location.href = 'login.html';
    }
}

// --- Idle Timeout ---
function startIdleTimer() {
    resetTimer();
    window.onload = resetTimer;
    window.onmousemove = resetTimer;
    window.onmousedown = resetTimer; // catches touchscreen presses as well
    window.ontouchstart = resetTimer;
    window.onclick = resetTimer;     // catches touchpad clicks as well
    window.onkeypress = resetTimer;
    window.addEventListener('scroll', resetTimer, true);
}

function resetTimer() {
    clearTimeout(idleTimer);
    idleTimer = setTimeout(() => {
        alert("Session Expired due to inactivity.");
        logout();
    }, IDLE_TIMEOUT);
}

// Run Auth Check on Page Load
document.addEventListener('DOMContentLoaded', checkAuth);
