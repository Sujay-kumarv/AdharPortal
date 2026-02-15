const API_BASE = 'http://127.0.0.1:8080/api';

// DOM Elements
const slabGrid = document.querySelector('.slab-grid');
const modalOverlay = document.querySelector('.modal-overlay');
const closeBtn = document.querySelector('.close-btn');

// Modal Fields
const modalImg = document.getElementById('modal-img');
const modalName = document.getElementById('modal-name');
const modalEmail = document.getElementById('modal-email');
const modalMobile = document.getElementById('modal-mobile');
const modalDob = document.getElementById('modal-dob');
const modalAddress = document.getElementById('modal-address');
const modalStatus = document.getElementById('modal-status');
const idProofPreview = document.querySelector('.id-proof-preview span');

let selectedUserId = null;

// Fetch and Render Users
async function renderSlabs() {
    try {
        const token = localStorage.getItem('accessToken');
        const response = await fetch(`${API_BASE}/users`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        const users = await response.json();

        slabGrid.innerHTML = '';

        if (users.length === 0) {
            slabGrid.innerHTML = '<p style="text-align:center; width:100%;">No users found.</p>';
            return;
        }

        users.forEach(user => {
            const slab = document.createElement('div');
            // Photo URL: from backend or default
            const photoUrl = user.photoPath ? `${API_BASE}/users/files/${user.photoPath}` : 'https://i.pravatar.cc/150';

            slab.className = 'glass-panel user-slab';
            slab.innerHTML = `
                <div class="slab-header">
                    <img src="${photoUrl}" alt="${user.fullName}" class="slab-avatar" onerror="this.src='https://via.placeholder.com/150'">
                    <div>
                        <div class="slab-name">${user.fullName}</div>
                        <div class="slab-id">ID: ${user.id}</div>
                    </div>
                </div>
                <div class="slab-status">Status: <span style="color: ${getStatusColor(user.status)}">${user.status}</span></div>
            `;
            slab.addEventListener('click', () => openModal(user));
            slabGrid.appendChild(slab);
        });
    } catch (error) {
        console.error('Error fetching users:', error);
        slabGrid.innerHTML = '<p style="color: var(--danger-neon)">Failed to load users.</p>';
    }
}

function getStatusColor(status) {
    if (status === 'APPROVED') return 'var(--success-neon)';
    if (status === 'REJECTED') return 'var(--danger-neon)';
    return 'var(--primary-neon)'; // PENDING
}

// Open Modal
function openModal(user) {
    selectedUserId = user.id;

    modalImg.src = user.photoPath ? `${API_BASE}/users/files/${user.photoPath}` : 'https://via.placeholder.com/150';
    modalName.textContent = user.fullName;
    modalEmail.textContent = user.email;
    modalMobile.textContent = user.mobile;
    modalDob.textContent = user.dob;
    modalAddress.textContent = `${user.addressLine1}, ${user.addressLine2 || ''}, ${user.city}, ${user.state} - ${user.pincode}`;
    modalStatus.textContent = user.status;
    modalStatus.style.color = getStatusColor(user.status);

    // Check if ID proof exists
    if (user.idProofPath) {
        idProofPreview.innerHTML = `<a href="${API_BASE}/users/files/${user.idProofPath}" target="_blank" style="color: var(--primary-neon);">View ID Document</a>`;
    } else {
        idProofPreview.innerText = "No Document Uploaded";
    }

    modalOverlay.classList.add('active');
}

// Close Modal
function closeModal() {
    modalOverlay.classList.remove('active');
    selectedUserId = null;
}

closeBtn.addEventListener('click', closeModal);
modalOverlay.addEventListener('click', (e) => {
    if (e.target === modalOverlay) closeModal();
});

// Update Status
async function updateStatus(status) {
    if (!selectedUserId) return;

    try {
        const token = localStorage.getItem('accessToken');
        const response = await fetch(`${API_BASE}/users/${selectedUserId}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ status })
        });

        if (response.ok) {
            alert(`User ${status}!`);
            closeModal();
            renderSlabs(); // Refresh list
        } else {
            alert('Failed to update status');
        }
    } catch (error) {
        console.error('Error updating status:', error);
    }
}

// Global functions for HTML onclick
window.approveUser = () => updateStatus('APPROVED');
window.rejectUser = () => updateStatus('REJECTED');

// Initialize
document.addEventListener('DOMContentLoaded', renderSlabs);
