/**
 * auth.js — Supabase Authentication helper
 *
 * Loaded on EVERY page (admin.html, employee.html, login.html).
 * Must be loaded BEFORE admin.js or employee.js.
 *
 * Responsibilities:
 *   - Initialize the Supabase JS client
 *   - login(email, password)
 *   - logout()
 *   - requireAuth()   → redirect to login if no session
 *   - requireAdmin()  → redirect if logged-in user is not ADMIN
 *   - requireEmployee() → redirect if logged-in user is not EMPLOYEE
 *   - getStoredEmployee() → returns employee object from localStorage
 */

// ============================================================
// Supabase Client Initialization
// ============================================================
// createClient() is provided by the Supabase JS SDK loaded via CDN.
// It takes your project URL and the anon/publishable key.
// The publishable key is designed to be used in client-side code —
// it only gives access to your Supabase project's public API.
// The service role key (secret) is kept server-side in application.properties.

const SUPABASE_URL         = 'https://gveuidmzbwdyjsmuehas.supabase.co';
const SUPABASE_PUBLISHABLE = 'sb_publishable_cOdNztn95daZOpmPheOwKg_J_4FOY3h';

const { createClient } = supabase;
const supabaseClient = createClient(SUPABASE_URL, SUPABASE_PUBLISHABLE);

// localStorage keys for storing employee info after login
const LS_EMPLOYEE_ID   = 'lf_employee_id';
const LS_EMPLOYEE_NAME = 'lf_employee_name';
const LS_EMPLOYEE_CODE = 'lf_employee_code';
const LS_ROLE          = 'lf_role';
const LS_EMAIL         = 'lf_email';

// ============================================================
// Authenticated Fetch Wrapper
// ============================================================

/**
 * Wraps the native fetch() to automatically attach the Supabase JWT
 * as an Authorization: Bearer header on every API request.
 *
 * Usage: drop-in replacement for fetch() — same arguments, same return.
 *   const response = await authFetch(`${API_BASE}/employees`);
 *   const response = await authFetch(`${API_BASE}/leaves`, {
 *       method: 'POST',
 *       headers: { 'Content-Type': 'application/json' },
 *       body: JSON.stringify(payload)
 *   });
 *
 * How it works:
 *   1. Gets the current Supabase session (contains the access_token JWT).
 *   2. Merges an Authorization header into the existing headers.
 *   3. Calls native fetch() with the enriched options.
 *
 * If there is no session (user not logged in), the request proceeds
 * without the header — the backend's JwtFilter will reject it with 401.
 */
async function authFetch(url, options = {}) {
    const { data: { session } } = await supabaseClient.auth.getSession();

    if (session) {
        // Merge Authorization into existing headers without overwriting them.
        options.headers = {
            ...(options.headers || {}),
            'Authorization': `Bearer ${session.access_token}`
        };
    }

    return fetch(url, options);
}

// ============================================================
// Login
// ============================================================

/**
 * Logs in with email + password via Supabase Auth.
 *
 * On success:
 *   1. Gets the user's email from the Supabase session
 *   2. Calls the LeaveFlow backend to find the matching employee
 *   3. Checks if the employee is active
 *   4. Stores employee info in localStorage
 *   5. Redirects to /admin.html or /employee.html based on role
 *
 * @param {string} email
 * @param {string} password
 * @param {HTMLElement} errorContainer - Where to display error messages
 */
async function login(email, password, errorContainer) {
    errorContainer.innerHTML = '';

    // Step 1: Authenticate with Supabase
    const { data, error } = await supabaseClient.auth.signInWithPassword({ email, password });

    if (error) {
        showError(errorContainer, 'Invalid email or password. Please try again.');
        return;
    }

    // Step 2: Find the employee record in LeaveFlow by email.
    //         Use the access token directly from the sign-in response —
    //         don't rely on getSession() which may not be ready yet.
    const token = data.session.access_token;

    try {
        const response = await fetch(`${API_BASE}/employees/email/${encodeURIComponent(email)}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!response.ok) {
            await supabaseClient.auth.signOut();
            showError(errorContainer, 'No employee account found for this email. Contact your admin.');
            return;
        }

        const employee = await response.json();

        // Step 3: Check if employee is active
        if (!employee.active) {
            await supabaseClient.auth.signOut();
            showError(errorContainer, 'Your account has been deactivated. Please contact your admin.');
            return;
        }

        // Step 4: Store employee info in localStorage
        localStorage.setItem(LS_EMPLOYEE_ID,   employee.id);
        localStorage.setItem(LS_EMPLOYEE_NAME, employee.name);
        localStorage.setItem(LS_EMPLOYEE_CODE, employee.employeeCode);
        localStorage.setItem(LS_ROLE,          employee.role);
        localStorage.setItem(LS_EMAIL,         email);

        // Step 5: Redirect based on role
        if (employee.role === 'ADMIN') {
            window.location.href = '/admin.html';
        } else {
            window.location.href = '/employee.html';
        }

    } catch (err) {
        await supabaseClient.auth.signOut();
        showError(errorContainer, 'Could not reach the server. Is the backend running?');
    }
}

// ============================================================
// Logout
// ============================================================

/**
 * Logs out the user.
 *
 * supabaseClient.auth.signOut() calls the Supabase server to
 * INVALIDATE the session token — it is deleted server-side.
 * After this, the token cannot be used again.
 *
 * Then we clear localStorage and redirect to login.
 */
async function logout() {
    await supabaseClient.auth.signOut();
    clearLocalStorage();
    window.location.href = '/login.html';
}

function clearLocalStorage() {
    localStorage.removeItem(LS_EMPLOYEE_ID);
    localStorage.removeItem(LS_EMPLOYEE_NAME);
    localStorage.removeItem(LS_EMPLOYEE_CODE);
    localStorage.removeItem(LS_ROLE);
    localStorage.removeItem(LS_EMAIL);
}

// ============================================================
// Session Guards
// ============================================================

/**
 * Checks that the user has a valid Supabase session.
 * If not, redirects to /login.html.
 *
 * Call this at the top of every protected page (admin.html, employee.html).
 */
async function requireAuth() {
    const { data: { session } } = await supabaseClient.auth.getSession();
    if (!session) {
        clearLocalStorage();
        window.location.href = '/login.html';
    }
}

/**
 * Checks that the logged-in user has the ADMIN role.
 * If not, redirects to /employee.html.
 *
 * Call this on admin.html after requireAuth().
 */
function requireAdmin() {
    const role = localStorage.getItem(LS_ROLE);
    if (role !== 'ADMIN') {
        window.location.href = '/employee.html';
    }
}

/**
 * Checks that the logged-in user has the EMPLOYEE role.
 * If not, redirects to /admin.html.
 *
 * Call this on employee.html after requireAuth().
 */
function requireEmployee() {
    const role = localStorage.getItem(LS_ROLE);
    if (role !== 'EMPLOYEE') {
        window.location.href = '/admin.html';
    }
}

// ============================================================
// Stored Employee Helpers
// ============================================================

/**
 * Returns the logged-in employee's info from localStorage.
 * Use this instead of repeatedly calling localStorage.getItem().
 */
function getStoredEmployee() {
    return {
        id:           parseInt(localStorage.getItem(LS_EMPLOYEE_ID)),
        name:         localStorage.getItem(LS_EMPLOYEE_NAME),
        employeeCode: localStorage.getItem(LS_EMPLOYEE_CODE),
        role:         localStorage.getItem(LS_ROLE),
        email:        localStorage.getItem(LS_EMAIL)
    };
}
