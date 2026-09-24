/**
 * common.js — Shared utilities used by both admin.js and employee.js
 *
 * Keeps repetitive code in one place so admin.js and employee.js
 * can stay focused on their own responsibilities.
 */

// The base URL of the Spring Boot backend API.
// Because the frontend is now served BY Spring Boot on the same origin,
// we use a root-relative path instead of an absolute localhost URL.
// This works in local dev (http://localhost:8080/api) and in production
// (https://your-domain.com/api) without any changes.
const API_BASE = '/api';

/**
 * Renders a success alert inside the given container element.
 * @param {HTMLElement} container - The DOM element to render into.
 * @param {string} message - The success message to display.
 */
function showSuccess(container, message) {
    container.innerHTML = `<div class="alert alert-success">${message}</div>`;
}

/**
 * Renders an error alert inside the given container element.
 * @param {HTMLElement} container - The DOM element to render into.
 * @param {string} message - The error message to display.
 */
function showError(container, message) {
    container.innerHTML = `<div class="alert alert-error">${message}</div>`;
}

/**
 * Formats an ISO date string (e.g., "2026-09-21") to a readable format (e.g., "21 Sep 2026").
 * @param {string} dateStr - ISO date string.
 * @returns {string} Human-readable date.
 */
function formatDate(dateStr) {
    if (!dateStr) return '—';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}

/**
 * Returns a coloured badge HTML string for a leave status.
 * @param {string} status - "PENDING", "APPROVED", or "REJECTED".
 * @returns {string} HTML string with badge span.
 */
function statusBadge(status) {
    const map = {
        PENDING:  'badge-pending',
        APPROVED: 'badge-approved',
        REJECTED: 'badge-rejected'
    };
    const cls = map[status] || '';
    return `<span class="badge ${cls}">${status}</span>`;
}

/**
 * Counts the number of working days (Monday–Friday) between
 * startDateStr and endDateStr, inclusive of both dates.
 *
 * This mirrors the exact same algorithm in LeaveService.java.
 * We run it on the frontend so we can show a live preview
 * before the employee submits the form.
 *
 * @param {string} startDateStr - ISO date string e.g. "2026-09-21"
 * @param {string} endDateStr   - ISO date string e.g. "2026-09-25"
 * @returns {number} Count of Mon–Fri days in the range (0 if weekend-only)
 */
function calculateWorkingDays(startDateStr, endDateStr) {
    if (!startDateStr || !endDateStr) return 0;

    const start = new Date(startDateStr);
    const end   = new Date(endDateStr);

    if (end < start) return 0;

    let count = 0;
    const current = new Date(start);

    while (current <= end) {
        const day = current.getDay(); // 0 = Sunday, 6 = Saturday
        if (day !== 0 && day !== 6) {
            count++;
        }
        current.setDate(current.getDate() + 1);
    }

    return count;
}
