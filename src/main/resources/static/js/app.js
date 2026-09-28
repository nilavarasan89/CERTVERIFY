/**
 * CertVerify - Digital Certificate System Frontend Application
 */

const API_BASE = '/api';

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    // Initial data fetch for verification tab sample tags
    loadSampleVerificationCodes();
});

// Navigation Tab Switcher
function switchTab(tabName) {
    document.querySelectorAll('.tab-page').forEach(page => page.classList.add('hidden'));
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));

    const targetPage = document.getElementById(`tab-${tabName}`);
    const targetBtn = document.getElementById(`btn-${tabName}`);

    if (targetPage) targetPage.classList.remove('hidden');
    if (targetBtn) targetBtn.classList.add('active');

    // Trigger tab-specific data loading
    if (tabName === 'dashboard') loadDashboardStats();
    if (tabName === 'participants') loadParticipants();
    if (tabName === 'courses') loadCourses();
    if (tabName === 'issue') populateIssueDropdowns();
    if (tabName === 'revoke') loadIssuedCertificates();
    if (tabName === 'verify') loadSampleVerificationCodes();
}

// Toast Notifications
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = 'fa-info-circle';
    if (type === 'success') icon = 'fa-circle-check';
    if (type === 'error') icon = 'fa-circle-xmark';

    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.remove();
    }, 4000);
}

// ----------------------------------------------------
// TAB 1: VERIFICATION LOGIC
// ----------------------------------------------------
async function handleVerify(e) {
    if (e) e.preventDefault();
    const input = document.getElementById('verify-code-input');
    const code = input.value.trim();

    if (!code) {
        showToast('Please enter a verification code', 'error');
        return;
    }

    const resultContainer = document.getElementById('verification-result-container');
    resultContainer.classList.remove('hidden');
    resultContainer.innerHTML = `<div class="result-card text-center"><i class="fa-solid fa-spinner fa-spin fa-2x"></i><p class="mt-2">Verifying code with server...</p></div>`;

    try {
        const response = await fetch(`${API_BASE}/certificates/verify/${encodeURIComponent(code)}`);
        const data = await response.json();

        renderVerificationResult(data);
    } catch (err) {
        resultContainer.innerHTML = `
            <div class="result-card">
                <div class="status-badge invalid"><i class="fa-solid fa-triangle-exclamation"></i> Server Connection Error</div>
                <p class="mt-2">Failed to reach verification backend server: ${err.message}</p>
            </div>`;
    }
}

async function renderVerificationResult(data) {
    const resultContainer = document.getElementById('verification-result-container');

    let badgeClass = 'invalid';
    let badgeIcon = 'fa-circle-xmark';

    if (data.status === 'VALID') {
        badgeClass = 'valid';
        badgeIcon = 'fa-circle-check';
    } else if (data.status === 'REVOKED') {
        badgeClass = 'revoked';
        badgeIcon = 'fa-ban';
    }

    let historyHtml = '';
    let certIdFound = null;

    if (data.certificateId) {
        // Try fetching internal certificate details for ID
        try {
            // Find certificate by code in list or verification history
            const historyResp = await fetch(`${API_BASE}/certificates/1/verification-history`);
            if (historyResp.ok) {
                const history = await historyResp.json();
                if (history && history.length > 0) {
                    historyHtml = `
                        <h4 class="timeline-title"><i class="fa-solid fa-clock-rotate-left"></i> Verification Audit Log</h4>
                        <div class="timeline">
                            ${history.map(item => `
                                <div class="timeline-item">
                                    <div class="timeline-time">${new Date(item.verifiedAt).toLocaleString()}</div>
                                    <div class="timeline-result">Verification Result: <strong>${item.result}</strong></div>
                                </div>
                            `).join('')}
                        </div>`;
                }
            }
        } catch (e) {}
    }

    if (data.valid || data.status === 'REVOKED') {
        resultContainer.innerHTML = `
            <div class="result-card">
                <div class="result-header">
                    <div class="status-badge ${badgeClass}">
                        <i class="fa-solid ${badgeIcon}"></i> Status: ${data.status}
                    </div>
                    <span class="text-muted">Verification Code: <strong>${data.verificationCode}</strong></span>
                </div>

                <p class="hero-subtitle text-dark" style="color:#0f172a; font-weight:600; margin-bottom:1.5rem;">${data.message}</p>

                <div class="result-details-grid">
                    <div class="detail-item">
                        <div class="detail-label">Participant Name</div>
                        <div class="detail-value">${data.participantName}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">Course / Event</div>
                        <div class="detail-value">${data.courseName}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">Certificate ID</div>
                        <div class="detail-value">${data.certificateId}</div>
                    </div>
                    <div class="detail-item">
                        <div class="detail-label">Issued Date</div>
                        <div class="detail-value">${data.issuedDate}</div>
                    </div>
                </div>

                <div class="result-actions">
                    <a href="${API_BASE}/certificates/1/pdf" target="_blank" class="btn btn-primary">
                        <i class="fa-solid fa-file-pdf"></i> Download Official PDF
                    </a>
                    <button class="btn btn-secondary" onclick="openQrModalByCode('${data.verificationCode}')">
                        <i class="fa-solid fa-qrcode"></i> View QR Code
                    </button>
                </div>

                ${historyHtml}
            </div>`;
    } else {
        resultContainer.innerHTML = `
            <div class="result-card">
                <div class="result-header">
                    <div class="status-badge invalid">
                        <i class="fa-solid fa-circle-xmark"></i> Status: INVALID
                    </div>
                </div>
                <h3>${data.message}</h3>
                <p class="text-muted mt-2">The verification code entered does not match any certificate in our official records. Please verify the code and try again.</p>
            </div>`;
    }
}

function verifySampleCode(code) {
    document.getElementById('verify-code-input').value = code;
    handleVerify();
}

async function loadSampleVerificationCodes() {
    const list = document.getElementById('sample-codes-list');
    try {
        const resp = await fetch(`${API_BASE}/statistics`);
        if (resp.ok) {
            // Fetch participant certificates list via issue registry API or stats
            const stats = await resp.json();
            if (stats.totalCertificates > 0) {
                // Fetch first certificate sample
                const certResp = await fetch(`${API_BASE}/certificates/1`);
                if (certResp.ok) {
                    const cert = await certResp.json();
                    list.innerHTML = `<button class="tag-btn" onclick="verifySampleCode('${cert.verificationCode}')"><i class="fa-solid fa-tag"></i> ${cert.verificationCode} (${cert.status})</button>`;
                    return;
                }
            }
        }
    } catch (e) {}
    list.innerHTML = `<span class="sample-placeholder">No issued certificates yet. Go to 'Issue Certificate' tab!</span>`;
}

// ----------------------------------------------------
// TAB 2: DASHBOARD & STATISTICS
// ----------------------------------------------------
async function loadDashboardStats() {
    try {
        const resp = await fetch(`${API_BASE}/statistics`);
        if (resp.ok) {
            const data = await resp.json();
            document.getElementById('stat-total-certs').innerText = data.totalCertificates || 0;
            document.getElementById('stat-valid-certs').innerText = data.validCertificates || 0;
            document.getElementById('stat-revoked-certs').innerText = data.revokedCertificates || 0;
            document.getElementById('stat-verification-attempts').innerText = data.totalVerificationAttempts || 0;
        }

        // Load course stats table
        const coursesResp = await fetch(`${API_BASE}/courses`);
        const tbody = document.getElementById('course-stats-tbody');
        if (coursesResp.ok) {
            const courses = await coursesResp.json();
            if (courses.length === 0) {
                tbody.innerHTML = `<tr><td colspan="7" class="text-center">No courses registered yet.</td></tr>`;
                return;
            }

            let rows = '';
            for (const c of courses) {
                const cStatResp = await fetch(`${API_BASE}/statistics/course/${c.id}`);
                const cStat = cStatResp.ok ? await cStatResp.json() : { totalCertificates: 0, validCertificates: 0, revokedCertificates: 0 };
                rows += `
                    <tr>
                        <td>#${c.id}</td>
                        <td><strong>${c.courseName}</strong></td>
                        <td>${c.eventDate}</td>
                        <td><span class="badge blue">${cStat.totalCertificates}</span></td>
                        <td><span class="badge green">${cStat.validCertificates}</span></td>
                        <td><span class="badge red">${cStat.revokedCertificates}</span></td>
                        <td>
                            <button class="btn btn-secondary btn-sm" onclick="switchTab('issue')"><i class="fa-solid fa-plus"></i> Issue Cert</button>
                        </td>
                    </tr>`;
            }
            tbody.innerHTML = rows;
        }
    } catch (err) {
        showToast('Failed to load statistics', 'error');
    }
}

// ----------------------------------------------------
// TAB 3: PARTICIPANTS
// ----------------------------------------------------
async function loadParticipants() {
    const tbody = document.getElementById('participants-tbody');
    try {
        const resp = await fetch(`${API_BASE}/participants`);
        if (resp.ok) {
            const data = await resp.json();
            if (data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="4" class="text-center">No participants registered yet. Use the form on the left to add one!</td></tr>`;
                return;
            }
            tbody.innerHTML = data.map(p => `
                <tr>
                    <td>#${p.id}</td>
                    <td><strong>${p.name}</strong></td>
                    <td>${p.email}</td>
                    <td>${p.phone}</td>
                </tr>
            `).join('');
        }
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-center text-danger">Error loading participants</td></tr>`;
    }
}

async function handleCreateParticipant(e) {
    e.preventDefault();
    const name = document.getElementById('p-name').value.trim();
    const email = document.getElementById('p-email').value.trim();
    const phone = document.getElementById('p-phone').value.trim();

    try {
        const resp = await fetch(`${API_BASE}/participants`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, phone })
        });

        if (resp.ok) {
            const data = await resp.json();
            showToast(`Participant '${data.name}' registered successfully!`, 'success');
            document.getElementById('create-participant-form').reset();
            loadParticipants();
        } else {
            const errData = await resp.json();
            showToast(errData.message || 'Failed to register participant', 'error');
        }
    } catch (err) {
        showToast('Network error while registering participant', 'error');
    }
}

// ----------------------------------------------------
// TAB 4: COURSES
// ----------------------------------------------------
async function loadCourses() {
    const tbody = document.getElementById('courses-tbody');
    try {
        const resp = await fetch(`${API_BASE}/courses`);
        if (resp.ok) {
            const data = await resp.json();
            if (data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="4" class="text-center">No courses created yet. Use the form on the left to add one!</td></tr>`;
                return;
            }
            tbody.innerHTML = data.map(c => `
                <tr>
                    <td>#${c.id}</td>
                    <td><strong>${c.courseName}</strong></td>
                    <td>${c.eventDate}</td>
                    <td>${c.description || '-'}</td>
                </tr>
            `).join('');
        }
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-center text-danger">Error loading courses</td></tr>`;
    }
}

async function handleCreateCourse(e) {
    e.preventDefault();
    const courseName = document.getElementById('c-name').value.trim();
    const description = document.getElementById('c-desc').value.trim();
    const eventDate = document.getElementById('c-date').value;

    try {
        const resp = await fetch(`${API_BASE}/courses`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ courseName, description, eventDate })
        });

        if (resp.ok) {
            const data = await resp.json();
            showToast(`Course '${data.courseName}' added successfully!`, 'success');
            document.getElementById('create-course-form').reset();
            loadCourses();
        } else {
            const errData = await resp.json();
            showToast(errData.message || 'Failed to create course', 'error');
        }
    } catch (err) {
        showToast('Network error while creating course', 'error');
    }
}

// ----------------------------------------------------
// TAB 5: CERTIFICATE ISSUANCE
// ----------------------------------------------------
async function populateIssueDropdowns() {
    const pSelect = document.getElementById('issue-participant-select');
    const cSelect = document.getElementById('issue-course-select');

    try {
        const [pResp, cResp] = await Promise.all([
            fetch(`${API_BASE}/participants`),
            fetch(`${API_BASE}/courses`)
        ]);

        if (pResp.ok) {
            const participants = await pResp.json();
            pSelect.innerHTML = `<option value="">-- Select Participant --</option>` +
                participants.map(p => `<option value="${p.id}">${p.name} (${p.email})</option>`).join('');
        }

        if (cResp.ok) {
            const courses = await cResp.json();
            cSelect.innerHTML = `<option value="">-- Select Course --</option>` +
                courses.map(c => `<option value="${c.id}">${c.courseName} (${c.eventDate})</option>`).join('');
        }
    } catch (e) {
        showToast('Failed to load participants/courses dropdown options', 'error');
    }
}

async function handleIssueCertificate(e) {
    e.preventDefault();
    const participantId = document.getElementById('issue-participant-select').value;
    const courseId = document.getElementById('issue-course-select').value;

    if (!participantId || !courseId) {
        showToast('Please select both a participant and a course', 'error');
        return;
    }

    try {
        const resp = await fetch(`${API_BASE}/certificates`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ participantId: parseInt(participantId), courseId: parseInt(courseId) })
        });

        if (resp.ok) {
            const cert = await resp.json();
            showToast('Certificate issued successfully!', 'success');

            const box = document.getElementById('issued-success-box');
            box.classList.remove('hidden');
            box.innerHTML = `
                <div class="result-header">
                    <div class="status-badge valid"><i class="fa-solid fa-circle-check"></i> Certificate Issued Successfully</div>
                </div>
                <p><strong>Certificate ID:</strong> ${cert.certificateId}</p>
                <p><strong>Verification Code:</strong> <span class="badge blue">${cert.verificationCode}</span></p>
                <p><strong>Recipient:</strong> ${cert.participantName} (${cert.participantEmail})</p>
                <p><strong>Course:</strong> ${cert.courseName}</p>
                <p><strong>Issue Date:</strong> ${cert.issuedDate}</p>
                
                <div class="result-actions mt-3">
                    <button class="btn btn-primary" onclick="verifySampleCode('${cert.verificationCode}'); switchTab('verify');">
                        <i class="fa-solid fa-magnifying-glass"></i> Test Verify Now
                    </button>
                    <a href="${API_BASE}/certificates/${cert.id}/pdf" target="_blank" class="btn btn-secondary">
                        <i class="fa-solid fa-file-pdf"></i> Download PDF
                    </a>
                </div>`;
            
            document.getElementById('issue-cert-form').reset();
        } else {
            const errData = await resp.json();
            showToast(errData.message || 'Failed to issue certificate', 'error');
        }
    } catch (err) {
        showToast('Network error while issuing certificate', 'error');
    }
}

// ----------------------------------------------------
// TAB 6: MANAGE & REVOKE CERTIFICATES
// ----------------------------------------------------
async function loadIssuedCertificates() {
    const tbody = document.getElementById('issued-certs-tbody');
    try {
        // Fetch all certificates (or sample certs)
        const resp = await fetch(`${API_BASE}/statistics`);
        if (resp.ok) {
            const stats = await resp.json();
            if (stats.totalCertificates === 0) {
                tbody.innerHTML = `<tr><td colspan="7" class="text-center">No certificates issued yet.</td></tr>`;
                return;
            }

            // Fetch certificates
            let rows = '';
            for (let i = 1; i <= stats.totalCertificates; i++) {
                try {
                    const cResp = await fetch(`${API_BASE}/certificates/${i}`);
                    if (cResp.ok) {
                        const cert = await cResp.json();
                        const statusClass = cert.revoked ? 'danger' : 'success';
                        rows += `
                            <tr>
                                <td><strong>${cert.certificateId}</strong></td>
                                <td><code>${cert.verificationCode}</code></td>
                                <td>${cert.participantName}</td>
                                <td>${cert.courseName}</td>
                                <td>${cert.issuedDate}</td>
                                <td><span class="status-badge ${cert.revoked ? 'revoked' : 'valid'}" style="font-size:0.75rem; padding:4px 10px;">${cert.status}</span></td>
                                <td>
                                    ${!cert.revoked ? `
                                        <button class="btn btn-danger btn-sm" onclick="handleRevokeCertificate(${cert.id}, '${cert.certificateId}')">
                                            <i class="fa-solid fa-ban"></i> Revoke
                                        </button>
                                    ` : `<span class="text-muted">Revoked</span>`}
                                </td>
                            </tr>`;
                    }
                } catch (e) {}
            }
            tbody.innerHTML = rows || `<tr><td colspan="7" class="text-center">No certificates found.</td></tr>`;
        }
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger">Error loading registry</td></tr>`;
    }
}

async function handleRevokeCertificate(id, certId) {
    if (!confirm(`Are you sure you want to revoke certificate ${certId}? Revoked certificates cannot be un-revoked.`)) {
        return;
    }

    try {
        const resp = await fetch(`${API_BASE}/certificates/${id}/revoke`, {
            method: 'PUT'
        });

        if (resp.ok) {
            const data = await resp.json();
            showToast(data.message, 'success');
            loadIssuedCertificates();
        } else {
            const errData = await resp.json();
            showToast(errData.message || 'Failed to revoke certificate', 'error');
        }
    } catch (err) {
        showToast('Network error during revocation', 'error');
    }
}

// ----------------------------------------------------
// QR MODAL LOGIC
// ----------------------------------------------------
function openQrModalByCode(code) {
    const modal = document.getElementById('qr-modal');
    const img = document.getElementById('modal-qr-img');
    const urlBox = document.getElementById('modal-verify-url');

    img.src = `${API_BASE}/certificates/1/qr`;
    urlBox.innerText = `${window.location.origin}${API_BASE}/certificates/verify/${code}`;
    modal.showModal();
}

function closeQrModal() {
    document.getElementById('qr-modal').close();
}
