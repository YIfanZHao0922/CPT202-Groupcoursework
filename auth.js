// Check if user is logged in (run on all protected pages)
function checkAuthStatus() {
    const isLoggedIn = localStorage.getItem('studentIsLoggedIn');
    const currentPath = window.location.pathname;

    // If not logged in and not on login/register page, redirect to login
    /*if (!isLoggedIn &&
        !currentPath.includes('login.html') &&
        !currentPath.includes('register.html') &&
        !currentPath.includes('index.html')) {
        window.location.href = 'login.html';
    }*/
}

// Login Form Submission
document.addEventListener('DOMContentLoaded', function() {
    // Run auth check on page load
    checkAuthStatus();

    // Login Form Handler
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', function(e) {
            e.preventDefault();

            const email = document.getElementById('studentEmail').value;
            const password = document.getElementById('password').value;

            // TODO: Replace with your team's backend API call
            console.log('Login attempt:', { email, password });

            // Mock login success (replace with real API response)
            localStorage.setItem('studentIsLoggedIn', 'true');
            localStorage.setItem('studentEmail', email);
            window.location.href = 'student-dashboard.html';
        });
    }

    // Logout Button Handler
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function() {
            localStorage.removeItem('authToken');
            localStorage.removeItem('studentIsLoggedIn');
            localStorage.removeItem('studentEmail');
            sessionStorage.removeItem('project_system_user');
            // Use URL replacement to avoid relative path resolution issues under file protocol
            var loginUrl = window.location.href.replace(/\/student\/html\/[^/]+$/, '/common/html/login.html');
            window.location.href = loginUrl;
        });
    }
});