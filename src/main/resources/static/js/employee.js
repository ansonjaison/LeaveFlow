/**
 * employee.js — All logic for employee.html
 *
 * Auth guard runs first. Employee ID is read from localStorage
 * (set during login by auth.js) — no manual ID input needed.
 */

let currentBalance = 0;  // Tracks live balance for the working-day validator

// ============================================================
// Auth Guard + Auto-load — runs immediately on page load
// ============================================================
(async () => {
    await requireAuth();    // Redirect to login.html if no Supabase session
    requireEmployee();      // Redirect to admin.html if role is not EMPLOYEE

    // Display the logged-in employee's name in the topbar
    const emp = getStoredEmployee();
    document.getElementById('empName').textContent = emp.name;

    // Auto-load their data — no need to enter an ID
    await loadEmployee();
})();

// ============================================================
// Load Employee Profile
// ============================================================

/**
 * Fetches the logged-in employee's data and renders their profile.
 * The employee ID comes from localStorage — set during login.
 */
async function loadEmployee() {
    const emp = getStoredEmployee();

    try {
        const response = await authFetch(`${API_BASE}/employees/${emp.id}`);
        const data = await response.json();

        if (!response.ok) {
            document.getElementById('profileGrid').innerHTML =
                `<div class="alert alert-error">${data.message || 'Could not load profile.'}</div>`;
            return;
        }

        currentBalance = data.leaveBalance;

        // Render profile grid
        document.getElementById('profileGrid').innerHTML = `
            <div class="profile-item"><span class="label">Employee Code</span><span class="value">${data.employeeCode}</span></div>
            <div class="profile-item"><span class="label">Name</span><span class="value">${data.name}</span></div>
            <div class="profile-item"><span class="label">Email</span><span class="value">${data.email}</span></div>
            <div class="profile-item"><span class="label">Department</span><span class="value">${data.department}</span></div>
            <div class="profile-item"><span class="label">Role</span><span class="value">${data.role}</span></div>
            <div class="profile-item">
                <span class="label">Status</span>
                <span class="value">
                    <span class="badge ${data.active ? 'badge-active' : 'badge-inactive'}">
                        ${data.active ? 'Active' : 'Inactive'}
                    </span>
                </span>
            </div>
        `;

        // Update the balance display
        document.getElementById('pBalance').textContent = data.leaveBalance;
        document.getElementById('balanceBox').style.display = 'block';

        // Show apply + history sections only for active employees
        if (data.active) {
            document.getElementById('applySection').style.display = 'block';

            // Lock date pickers to today or future — past dates are not allowed.
            // The 'min' attribute on a date input prevents the user from selecting
            // any date before the specified minimum in the browser's date picker.
            const today = new Date().toISOString().split('T')[0]; // "YYYY-MM-DD"
            document.getElementById('startDate').min = today;
            document.getElementById('endDate').min   = today;
        } else {
            document.getElementById('applySection').style.display = 'none';
        }
        document.getElementById('historySection').style.display = 'block';

        // Load leave history
        loadLeaveHistory();

    } catch (err) {
        document.getElementById('profileGrid').innerHTML =
            `<div class="alert alert-error">Could not reach the server. Is the backend running?</div>`;
    }
}

// ============================================================
// Live Working-Day Preview
// ============================================================

/**
 * Called whenever the start or end date changes.
 * Calculates working days immediately and shows:
 *   - "5 working day(s) selected  ✅ You have enough balance"  (green)
 *   - "15 working day(s) selected ⚠️ Exceeds your balance of 12 days" (red)
 *   - "0 working day(s) — no weekdays in range" (warning)
 *
 * Also enables/disables the Submit button.
 */
function updateWorkingDayPreview() {
    const startVal = document.getElementById('startDate').value;
    const endVal   = document.getElementById('endDate').value;
    const preview  = document.getElementById('workingDayPreview');
    const countEl  = document.getElementById('wdCount');
    const statusEl = document.getElementById('wdStatus');
    const submitBtn = document.getElementById('submitLeaveBtn');

    if (!startVal || !endVal) {
        preview.style.display = 'none';
        return;
    }

    const days = calculateWorkingDays(startVal, endVal);
    countEl.textContent = days;
    preview.style.display = 'block';

    if (days === 0) {
        statusEl.innerHTML = `<span class="wd-warn">⚠️ No weekdays in selected range</span>`;
        preview.className = 'working-day-preview wd-warning';
        submitBtn.disabled = true;
    } else if (days > currentBalance) {
        statusEl.innerHTML = `<span class="wd-error">⚠️ Exceeds your balance of ${currentBalance} day(s)</span>`;
        preview.className = 'working-day-preview wd-error';
        submitBtn.disabled = true;
    } else {
        statusEl.innerHTML = `<span class="wd-ok">✅ Within your balance of ${currentBalance} day(s)</span>`;
        preview.className = 'working-day-preview wd-ok';
        submitBtn.disabled = false;
    }
}

// ============================================================
// Apply for Leave
// ============================================================

async function submitLeave(event) {
    event.preventDefault();
    const messageDiv = document.getElementById('applyMessage');
    const emp = getStoredEmployee();
    messageDiv.innerHTML = '';

    const payload = {
        employeeId: emp.id,
        leaveType:  document.getElementById('leaveType').value,
        startDate:  document.getElementById('startDate').value,
        endDate:    document.getElementById('endDate').value,
        reason:     document.getElementById('reason').value.trim()
    };

    try {
        const response = await authFetch(`${API_BASE}/leaves`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (!response.ok) {
            showError(messageDiv, data.message || 'Failed to submit leave application.');
            return;
        }

        showSuccess(messageDiv,
            `Leave submitted! Status: PENDING. Working days: ${data.days}`);

        // Reset form and hide preview
        document.getElementById('leaveForm').reset();
        document.getElementById('workingDayPreview').style.display = 'none';

        // Refresh balance and history
        await loadEmployee();

    } catch (err) {
        showError(messageDiv, 'Could not reach the server. Is the backend running?');
    }
}

// ============================================================
// Leave History
// ============================================================

async function loadLeaveHistory() {
    const container = document.getElementById('leaveHistoryContainer');
    const emp = getStoredEmployee();
    container.innerHTML = '<p class="hint">Loading...</p>';

    try {
        const response = await authFetch(`${API_BASE}/employees/${emp.id}/leaves`);
        const data = await response.json();

        if (!response.ok) {
            showError(container, data.message || 'Failed to load leave history.');
            return;
        }

        if (data.length === 0) {
            container.innerHTML = '<p class="hint">No leave requests yet.</p>';
            return;
        }

        let rows = '';
        data.forEach(leave => {
            const rejectionCell = leave.rejectionReason
                ? `<span class="rejection-text">${leave.rejectionReason}</span>` : '—';

            rows += `
                <tr>
                    <td>${leave.leaveType}</td>
                    <td>${formatDate(leave.startDate)}</td>
                    <td>${formatDate(leave.endDate)}</td>
                    <td>${leave.days}</td>
                    <td>${leave.reason}</td>
                    <td>${statusBadge(leave.status)}</td>
                    <td>${rejectionCell}</td>
                </tr>
            `;
        });

        container.innerHTML = `
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr>
                            <th>Type</th><th>Start</th><th>End</th><th>Days</th>
                            <th>Reason</th><th>Status</th><th>Rejection Reason</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        `;
    } catch (err) {
        showError(container, 'Could not reach the server.');
    }
}
