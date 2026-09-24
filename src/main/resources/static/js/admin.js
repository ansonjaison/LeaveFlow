/**
 * admin.js — All logic for admin.html
 *
 * Auth guard runs first. If the user is not logged in or not an ADMIN,
 * they are redirected before anything else happens.
 */

// ============================================================
// Auth Guard — runs immediately on page load
// ============================================================
(async () => {
    await requireAuth();   // Redirect to login.html if no Supabase session
    requireAdmin();        // Redirect to employee.html if role is not ADMIN

    // Display the logged-in admin's name in the topbar
    const emp = getStoredEmployee();
    document.getElementById('adminName').textContent = emp.name;

    // Auto-load data
    loadAllEmployees();
    loadAllLeaves();
})();

// ============================================================
// Create Employee
// ============================================================

let currentNewEmployeeEmail = '';  // Stored to show in the password popup

function toggleCreateForm() {
    const form = document.getElementById('createEmployeeForm');
    const label = document.getElementById('toggleLabel');
    const isHidden = form.style.display === 'none';
    form.style.display = isHidden ? 'block' : 'none';
    label.textContent = isHidden ? '− Close Form' : '+ New Employee';
}

/**
 * Creates an employee in the LeaveFlow DB and simultaneously
 * creates a Supabase Auth user via the backend (SupabaseAuthService).
 *
 * On success: shows a password popup with the generated credentials.
 */
async function createEmployee(event) {
    event.preventDefault();
    const messageDiv = document.getElementById('createMessage');
    messageDiv.innerHTML = '';

    const payload = {
        employeeCode: document.getElementById('ceCode').value.trim(),
        name:         document.getElementById('ceName').value.trim(),
        email:        document.getElementById('ceEmail').value.trim(),
        department:   document.getElementById('ceDept').value.trim(),
        role:         document.getElementById('ceRole').value
    };

    try {
        const response = await authFetch(`${API_BASE}/employees`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await response.json();

        if (!response.ok) {
            showError(messageDiv, data.message || 'Failed to create employee.');
            return;
        }

        // Reset the form
        event.target.reset();
        toggleCreateForm();

        // Show the password popup — the generated password comes from the backend
        openPasswordModal(data.email, data.generatedPassword);

        // Refresh the employees list
        loadAllEmployees();

    } catch (err) {
        showError(messageDiv, 'Could not reach the server. Is the backend running?');
    }
}

// ============================================================
// Password Popup Modal
// ============================================================

function openPasswordModal(email, password) {
    document.getElementById('popupEmail').textContent    = email;
    document.getElementById('popupPassword').textContent = password;
    document.getElementById('passwordModal').style.display = 'flex';
}

function closePasswordModal() {
    document.getElementById('passwordModal').style.display = 'none';
}

// ============================================================
// All Employees (with Deactivate + Reactivate buttons)
// ============================================================

async function loadAllEmployees() {
    const container = document.getElementById('employeesContainer');
    container.innerHTML = '<p class="hint">Loading...</p>';

    try {
        const response = await authFetch(`${API_BASE}/employees`);
        const data = await response.json();

        if (!response.ok) {
            showError(container, data.message || 'Failed to load employees.');
            return;
        }

        if (data.length === 0) {
            container.innerHTML = '<p class="hint">No employees found. Create one above.</p>';
            return;
        }

        let rows = '';
        data.forEach(emp => {
            const activeCell = emp.active
                ? `<span class="badge badge-active">Active</span>`
                : `<span class="badge badge-inactive">Inactive</span>`;

            // Active employees → Deactivate + (if EMPLOYEE) Edit Balance
            // Inactive employees → Reactivate only
            const actionCell = emp.active
                ? `<div class="action-btns">
                       ${emp.role === 'EMPLOYEE'
                           ? `<button class="btn btn-success btn-sm"
                                  onclick="editBalance(${emp.id}, '${emp.name}', ${emp.leaveBalance})">
                                  Edit Balance
                              </button>`
                           : ''}
                       <button class="btn btn-danger btn-sm"
                               onclick="deactivateEmployee(${emp.id}, '${emp.name}')">
                           Deactivate
                       </button>
                   </div>`
                : `<div class="action-btns">
                       <button class="btn btn-success btn-sm"
                               onclick="reactivateEmployee(${emp.id}, '${emp.name}')">
                           Reactivate
                       </button>
                   </div>`;

            rows += `
                <tr id="emp-row-${emp.id}">
                    <td>${emp.id}</td>
                    <td>${emp.employeeCode}</td>
                    <td>${emp.name}</td>
                    <td>${emp.email}</td>
                    <td>${emp.department}</td>
                    <td>${emp.role}</td>
                    <td>${emp.role === 'ADMIN'
                            ? '<span style="color:var(--color-text-muted);font-style:italic;">N/A</span>'
                            : emp.leaveBalance}</td>
                    <td>${activeCell}</td>
                    <td>${actionCell}</td>
                </tr>
            `;
        });

        container.innerHTML = `
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Code</th>
                            <th>Name</th>
                            <th>Email</th>
                            <th>Department</th>
                            <th>Role</th>
                            <th>Balance</th>
                            <th>Status</th>
                            <th>Action</th>
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

async function deactivateEmployee(employeeId, employeeName) {
    if (!confirm(`Deactivate "${employeeName}"?\nThey will not be able to apply for leave.`)) return;

    try {
        const response = await authFetch(`${API_BASE}/employees/${employeeId}/deactivate`, {
            method: 'PATCH'
        });
        const data = await response.json();
        if (!response.ok) { alert(data.message || 'Could not deactivate.'); return; }
        loadAllEmployees();
    } catch (err) {
        alert('Could not reach the server.');
    }
}

async function reactivateEmployee(employeeId, employeeName) {
    if (!confirm(`Reactivate "${employeeName}"?\nThey will be able to log in and apply for leave again.`)) return;

    try {
        const response = await authFetch(`${API_BASE}/employees/${employeeId}/reactivate`, {
            method: 'PATCH'
        });
        const data = await response.json();
        if (!response.ok) { alert(data.message || 'Could not reactivate.'); return; }
        loadAllEmployees();
    } catch (err) {
        alert('Could not reach the server.');
    }
}

/**
 * Prompts the admin to enter a new leave balance for an employee.
 * Sends PATCH /api/employees/{id}/balance with the new value.
 *
 * @param {number} employeeId      - DB id of the employee
 * @param {string} employeeName    - Name shown in the prompt
 * @param {number} currentBalance  - Pre-filled as the default value in the prompt
 */
async function editBalance(employeeId, employeeName, currentBalance) {
    const input = window.prompt(
        `Edit leave balance for "${employeeName}"\n\nCurrent balance: ${currentBalance} day(s)\n\nEnter new balance:`,
        currentBalance
    );

    // User cancelled the prompt
    if (input === null) return;

    const newBalance = parseInt(input, 10);

    if (isNaN(newBalance) || newBalance < 0) {
        alert('Please enter a valid non-negative number.');
        return;
    }

    try {
        const response = await authFetch(`${API_BASE}/employees/${employeeId}/balance`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ leaveBalance: newBalance })
        });
        const data = await response.json();
        if (!response.ok) { alert(data.message || 'Could not update balance.'); return; }

        // Refresh the table to show the new balance
        loadAllEmployees();
    } catch (err) {
        alert('Could not reach the server.');
    }
}

// ============================================================
// Employee Search
// ============================================================

async function searchEmployee() {
    const code = document.getElementById('searchCode').value.trim();
    const resultDiv = document.getElementById('searchResult');

    if (!code) { showError(resultDiv, 'Please enter an employee code.'); return; }
    resultDiv.innerHTML = '<p class="hint">Searching...</p>';

    try {
        const response = await authFetch(`${API_BASE}/employees/code/${code}`);
        const data = await response.json();

        if (!response.ok) {
            showError(resultDiv, data.message || 'Employee not found.');
            return;
        }

        resultDiv.innerHTML = `
            <div class="profile-grid" style="margin-top:16px;">
                <div class="profile-item"><span class="label">Employee Code</span><span class="value">${data.employeeCode}</span></div>
                <div class="profile-item"><span class="label">Name</span><span class="value">${data.name}</span></div>
                <div class="profile-item"><span class="label">Email</span><span class="value">${data.email}</span></div>
                <div class="profile-item"><span class="label">Department</span><span class="value">${data.department}</span></div>
                <div class="profile-item"><span class="label">Role</span><span class="value">${data.role}</span></div>
                <div class="profile-item"><span class="label">Leave Balance</span><span class="value">${data.leaveBalance} day(s)</span></div>
                <div class="profile-item">
                    <span class="label">Status</span>
                    <span class="value">
                        <span class="badge ${data.active ? 'badge-active' : 'badge-inactive'}">
                            ${data.active ? 'Active' : 'Inactive'}
                        </span>
                    </span>
                </div>
            </div>
        `;
    } catch (err) {
        showError(resultDiv, 'Could not reach the server.');
    }
}

document.getElementById('searchCode').addEventListener('keydown', function (e) {
    if (e.key === 'Enter') searchEmployee();
});

// ============================================================
// All Leave Requests
// ============================================================

let currentRejectLeaveId = null;

async function loadAllLeaves() {
    const container = document.getElementById('leaveRequestsContainer');
    container.innerHTML = '<p class="hint">Loading...</p>';

    try {
        const response = await authFetch(`${API_BASE}/leaves`);
        const data = await response.json();

        if (!response.ok) { showError(container, data.message || 'Failed to load.'); return; }

        if (data.length === 0) {
            container.innerHTML = '<p class="hint">No leave requests found.</p>';
            return;
        }

        let rows = '';
        data.forEach(leave => {
            const actionBtns = leave.status === 'PENDING'
                ? `<div class="action-btns">
                       <button class="btn btn-success btn-sm" onclick="approveLeave(${leave.id})">Approve</button>
                       <button class="btn btn-danger  btn-sm" onclick="openRejectModal(${leave.id})">Reject</button>
                   </div>`
                : '—';

            const rejectionCell = leave.rejectionReason
                ? `<span class="rejection-text">${leave.rejectionReason}</span>` : '—';

            rows += `
                <tr>
                    <td>${leave.employeeName}</td>
                    <td>${leave.leaveType}</td>
                    <td>${formatDate(leave.startDate)}</td>
                    <td>${formatDate(leave.endDate)}</td>
                    <td>${leave.days}</td>
                    <td>${leave.reason}</td>
                    <td>${statusBadge(leave.status)}</td>
                    <td>${rejectionCell}</td>
                    <td>${actionBtns}</td>
                </tr>
            `;
        });

        container.innerHTML = `
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr>
                            <th>Employee</th><th>Type</th><th>Start</th><th>End</th>
                            <th>Days</th><th>Reason</th><th>Status</th>
                            <th>Rejection Reason</th><th>Actions</th>
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

async function approveLeave(leaveId) {
    try {
        const response = await authFetch(`${API_BASE}/leaves/${leaveId}/approve`, { method: 'PATCH' });
        const data = await response.json();
        if (!response.ok) { alert('Could not approve: ' + (data.message || 'Unknown error')); return; }
        loadAllLeaves();
        loadAllEmployees();
    } catch (err) { alert('Could not reach the server.'); }
}

function openRejectModal(leaveId) {
    currentRejectLeaveId = leaveId;
    document.getElementById('rejectionReasonInput').value = '';
    document.getElementById('rejectModal').style.display = 'flex';
}

function closeRejectModal() {
    document.getElementById('rejectModal').style.display = 'none';
    currentRejectLeaveId = null;
}

async function confirmReject() {
    const reason = document.getElementById('rejectionReasonInput').value.trim();
    if (!reason) { alert('Please enter a rejection reason.'); return; }

    try {
        const response = await authFetch(`${API_BASE}/leaves/${currentRejectLeaveId}/reject`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ rejectionReason: reason })
        });
        const data = await response.json();
        if (!response.ok) { alert('Could not reject: ' + (data.message || 'Unknown error')); return; }
        closeRejectModal();
        loadAllLeaves();
    } catch (err) { alert('Could not reach the server.'); }
}
