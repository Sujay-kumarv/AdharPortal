const API_BASE = 'http://127.0.0.1:8080/api';

console.log("[UserDashboard] Script Loaded");

// 1. Auth Check & Fetch User
// 1. Auth Check & Fetch User
async function initDashboard() {
    console.log("[Dashboard] initDashboard started");

    // Declare msgEl once at the top scope of the function
    const msgEl = document.getElementById('app-msg');

    try {
        if (msgEl) msgEl.textContent = "Initializing...";

        const token = localStorage.getItem('accessToken');
        if (!token) {
            console.warn("[Dashboard] No token found, redirecting...");
            window.location.href = 'login.html';
            return;
        }

        if (msgEl) msgEl.textContent = "Connecting to server...";

        console.log("[Dashboard] Fetching /users/me...");
        const startTime = Date.now();

        // Create a timeout promise (10 seconds)
        const timeoutPromise = new Promise((_, reject) =>
            setTimeout(() => reject(new Error('Request timed out')), 10000)
        );

        const fetchPromise = fetch(`${API_BASE}/users/me`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        // Race fetch against timeout
        const response = await Promise.race([fetchPromise, timeoutPromise]);

        console.log(`[Dashboard] Response received in ${Date.now() - startTime}ms. Status:`, response.status);

        if (response.ok) {
            const user = await response.json();
            console.log("[Dashboard] User data loaded:", user);
            renderDashboard(user);
        } else {
            console.error('[Dashboard] Failed to fetch user:', response.status);
            if (msgEl) msgEl.textContent = `Error: Server returned status ${response.status}. Try logging out and back in.`;

            if (response.status === 401 || response.status === 403) {
                localStorage.removeItem('accessToken');
                window.location.href = 'login.html';
            }
        }
    } catch (error) {
        console.error('[Dashboard] Error:', error);
        if (msgEl) {
            if (error.message === 'Request timed out') {
                msgEl.textContent = "Server is taking too long to respond. Please check your connection.";
            } else {
                msgEl.textContent = `Network Error: ${error.message}. Is the backend running?`;
            }
        }
    }
}

// 2. Render Logic
function renderDashboard(user) {
    // Status
    const statusEl = document.getElementById('app-status');
    const msgEl = document.getElementById('app-msg');

    statusEl.textContent = user.status;
    statusEl.style.color = getStatusColor(user.status);

    if (user.status === 'APPROVED') {
        msgEl.textContent = "Congratulations! Your New Aadhaar is ready.";
        renderAadhaarCard(user, false);
    } else if (user.status === 'REJECTED') {
        msgEl.textContent = "Your application has been rejected. Please contact support.";
        document.getElementById('aadhaar-section').style.display = 'none';
    } else {
        msgEl.textContent = "Your application is currently under verification.";
        renderAadhaarCard(user, true); // Blurred
    }
}

function getStatusColor(status) {
    if (status === 'APPROVED') return 'var(--success-neon)';
    if (status === 'REJECTED') return 'var(--danger-neon)';
    return 'var(--primary-neon)';
}

// 3. Render Card
function renderAadhaarCard(user, isBlurred) {
    const container = document.getElementById('card-container');
    const photoUrl = user.photoPath ? `${API_BASE}/users/files/${user.photoPath}` : 'https://i.pravatar.cc/150?u=' + user.id;

    const blurredClass = isBlurred ? 'blurred' : '';

    const cardHtml = `
        <div class="aadhaar-card ${blurredClass}" id="aadhaarCard">
            <div class="card-header">
                <h2>New Aadhaar Portal</h2>
                <p>Digital Identity Card</p>
            </div>
            <div class="card-body">
                <div class="card-row">
                    <img src="${photoUrl}" class="user-photo" alt="User Photo" onerror="this.src='https://via.placeholder.com/150'">
                    <div class="card-details">
                        <div class="detail-group">
                            <div class="detail-label">Full Name</div>
                            <div class="detail-value">${user.fullName}</div>
                        </div>
                        <div class="detail-group">
                            <div class="detail-label">Date of Birth</div>
                            <div class="detail-value">${user.dob}</div>
                        </div>
                    </div>
                </div>
                
                <div class="card-row">
                    <div class="detail-group">
                        <div class="detail-label">Gender</div>
                        <div class="detail-value">${user.gender}</div>
                    </div>
                     <div class="detail-group">
                        <div class="detail-label">Aadhaar Number</div>
                        <div class="aadhaar-number">${user.aadhaarNumber || 'XXXX XXXX XXXX'}</div>
                    </div>
                </div>

                <div class="card-actions">
                    ${isBlurred ? '' : `
                        <button class="neon-btn btn-approve" onclick="downloadPdf(${user.id})" style="flex:1; display:flex; align-items:center; justify-content:center; gap:0.5rem;">
                            <span>⬇</span> Download PDF
                        </button>
                    `}
                </div>
            </div>
            <div class="card-footer">
                Generated by New Aadhaar Portal — For Authorized Use Only.
            </div>
        </div>
    `;

    container.innerHTML = cardHtml;

    // Animation
    if (!isBlurred) {
        gsap.fromTo("#aadhaarCard",
            { opacity: 0, y: 50, scale: 0.9 },
            { opacity: 1, y: 0, scale: 1, duration: 0.8, ease: "power3.out" }
        );
    } else {
        gsap.set("#aadhaarCard", { opacity: 0.7 });
    }
}

// 4. Download Action
async function downloadPdf(userId) {
    const token = localStorage.getItem('accessToken');

    // Direct link doesn't allow headers easily, so we use fetch to get blob
    try {
        const response = await fetch(`${API_BASE}/newaadhaar/download/${userId}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `My_New_Aadhaar_${userId}.pdf`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        } else {
            alert("Failed to download. Please try again.");
        }
    } catch (e) {
        console.error("Download error", e);
        alert("Download failed.");
    }
}

// 5. Logout (Reuse)
async function logout() {
    try {
        await fetch(`${AUTH_API}/logout`, { method: 'POST' });
        localStorage.removeItem('accessToken');
        window.location.href = 'login.html';
    } catch (e) {
        window.location.href = 'login.html';
    }
}

// Init
document.addEventListener('DOMContentLoaded', initDashboard);
