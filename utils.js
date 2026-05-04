// ========== Storage Keys ==========
const STORAGE_USERS = 'projectSystem_users';
const STORAGE_PROJECTS = 'projectSystem_projects';
const STORAGE_APPLICATIONS = 'projectSystem_applications';
const STORAGE_CURRENT_USER = 'projectSystem_currentUser';

// ========== Initialize Default Data ==========
function loadUsers() {
    let users = localStorage.getItem(STORAGE_USERS);
    if (users) return JSON.parse(users);
    return [
        { id: 1, username: "student1", password: "123", name: "John Doe", email: "s1@test.com", role: "STUDENT" },
        { id: 2, username: "teacher1", password: "123", name: "Prof. Smith", email: "t1@test.com", role: "TEACHER" },
        { id: 3, username: "admin1", password: "123", name: "System Admin", email: "admin@test.com", role: "ADMIN" }
    ];
}
function saveUsers(users) { localStorage.setItem(STORAGE_USERS, JSON.stringify(users)); }

function loadProjects() {
    let projects = localStorage.getItem(STORAGE_PROJECTS);
    if (projects) return JSON.parse(projects);
    return [
        { id: 1, title: "Smart Campus Management System", description: "Develop a comprehensive smart campus platform including course selection, attendance, and grade management.", category: "Web Development", requiredSkills: ["Java", "Spring", "React"], teacherName: "Li Teacher", teacherId: 2, maxStudents: 3, currentApplicants: 1, status: "AVAILABLE" },
        { id: 2, title: "Deep Learning Based Image Recognition", description: "Use CNN to build a waste classification system with over 90% accuracy.", category: "Artificial Intelligence", requiredSkills: ["Python", "TensorFlow"], teacherName: "Professor Wang", teacherId: 3, maxStudents: 2, currentApplicants: 0, status: "AVAILABLE" },
        { id: 3, title: "Cross-platform Campus Assistant App", description: "Flutter-based campus assistant integrating schedule, events, and lost & found.", category: "Mobile App", requiredSkills: ["Flutter", "Dart"], teacherName: "Teacher Chen", teacherId: 4, maxStudents: 4, currentApplicants: 2, status: "REQUESTED" },
        { id: 4, title: "Big Data Analytics Platform", description: "Data analysis and visualization platform based on Hadoop and Spark.", category: "Data Science", requiredSkills: ["Hadoop", "Spark", "Scala"], teacherName: "Professor Zhao", teacherId: 5, maxStudents: 2, currentApplicants: 2, status: "AGREED" }
    ];
}
function saveProjects(projects) { localStorage.setItem(STORAGE_PROJECTS, JSON.stringify(projects)); }

function loadApplications() {
    let apps = localStorage.getItem(STORAGE_APPLICATIONS);
    if (apps) return JSON.parse(apps);
    return [
        { id: 1, projectId: 1, studentId: 1, status: "PENDING", note: "Very interested" },
        { id: 2, projectId: 3, studentId: 1, status: "PENDING", note: "" }
    ];
}
function saveApplications(apps) { localStorage.setItem(STORAGE_APPLICATIONS, JSON.stringify(apps)); }

// Global data
let mockUsers = loadUsers();
let mockProjects = loadProjects();
let mockApplications = loadApplications();
let currentUser = null;

function persistAll() {
    saveUsers(mockUsers);
    saveProjects(mockProjects);
    saveApplications(mockApplications);
}

function saveSession() {
    if (currentUser) localStorage.setItem(STORAGE_CURRENT_USER, JSON.stringify(currentUser));
    else localStorage.removeItem(STORAGE_CURRENT_USER);
}
function loadSession() {
    let saved = localStorage.getItem(STORAGE_CURRENT_USER);
    if (saved) currentUser = JSON.parse(saved);
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>]/g, function(m) {
        if (m === '&') return '&amp;';
        if (m === '<') return '&lt;';
        if (m === '>') return '&gt;';
        return m;
    });
}
function showMessage(selector, msg, type) {
    let $msg = $(selector);
    $msg.removeClass('message-success message-error')
        .addClass(type === 'success' ? 'message-success' : 'message-error')
        .text(msg).show();
    setTimeout(() => $msg.hide(), 3000);
}
function updateHeader() {
    const $userInfo = $('#userInfoArea');
    if (currentUser) {
        let roleText = currentUser.role === 'STUDENT' ? 'Student' : (currentUser.role === 'TEACHER' ? 'Teacher' : 'Admin');
        $userInfo.html(`
            <span>${escapeHtml(currentUser.fullname)} (${roleText})</span>
            <button id="logoutBtn" class="btn btn-outline btn-sm">Logout</button>
        `);
        $('#logoutBtn').off('click').on('click', function() {
            currentUser = null;
            saveSession();
            window.location.href = 'login.html';
        });
    } else {
        $userInfo.html(`<a href="login.html">Login</a> <a href="register.html">Register</a>`);
    }
}
function getProjectsForRole() {
    if (!currentUser) return [];
    if (currentUser.role === 'STUDENT') return mockProjects;
    if (currentUser.role === 'TEACHER') return mockProjects.filter(p => p.teacherId === currentUser.id);
    return mockProjects;
}

// Project list rendering (for projects.html to call)
function loadProjectsList() {
    let keyword = $('#searchKeyword').val().toLowerCase();
    let category = $('#searchCategory').val();
    let status = $('#searchStatus').val();

    let projects = getProjectsForRole();
    let filtered = projects.filter(p => {
        if (keyword && !p.title.toLowerCase().includes(keyword) && !p.description.toLowerCase().includes(keyword)) return false;
        if (category && p.category !== category) return false;
        if (status && p.status !== status) return false;
        return true;
    });

    let container = $('#projectsContainer');
    if (filtered.length === 0) {
        container.html('<p style="text-align:center;">No projects found</p>');
        return;
    }
    let html = '';
    filtered.forEach(p => {
        let statusBadge = '';
        switch(p.status) {
            case 'AVAILABLE': statusBadge = '<span class="badge badge-pending">Available</span>'; break;
            case 'REQUESTED': statusBadge = '<span class="badge badge-pending">Requested</span>'; break;
            case 'AGREED': statusBadge = '<span class="badge badge-accepted">Agreed</span>'; break;
            case 'CLOSED': statusBadge = '<span class="badge badge-withdrawn">Closed</span>'; break;
        }
        let actionButtons = '';
        if (currentUser.role === 'STUDENT') {
            actionButtons = `<button class="btn btn-primary btn-sm view-detail" data-id="${p.id}">View & Apply</button>`;
        } else if (currentUser.role === 'TEACHER') {
            actionButtons = `
                <button class="btn btn-outline btn-sm manage-app" data-id="${p.id}">Applications</button>
                <button class="btn btn-outline btn-sm edit-proj" data-id="${p.id}">Edit</button>
            `;
        } else if (currentUser.role === 'ADMIN') {
            actionButtons = `
                <button class="btn btn-outline btn-sm edit-proj" data-id="${p.id}">Edit</button>
                <button class="btn btn-danger btn-sm delete-proj" data-id="${p.id}">Delete</button>
            `;
        }
        html += `
            <div class="project-card" data-project-id="${p.id}">
                <div class="project-header">
                    <div class="project-title">${escapeHtml(p.title)}</div>
                    ${statusBadge}
                </div>
                <div class="project-supervisor">Supervisor: ${p.teacherName}</div>
                <div class="project-description">${escapeHtml(p.description)}</div>
                <div class="project-meta">
                    <div class="meta-item">Category: ${p.category}</div>
                    <div class="meta-item">Skills: ${p.requiredSkills.join(', ')}</div>
                    <div class="meta-item">Slots: ${p.currentApplicants}/${p.maxStudents || 'Unlimited'}</div>
                </div>
                <div class="project-actions">${actionButtons}</div>
            </div>
        `;
    });
    container.html(html);

    if (currentUser.role === 'STUDENT') {
        $('.view-detail').off('click').on('click', function(e) {
            e.stopPropagation();
            let pid = $(this).data('id');
            window.location.href = `detail.html?projectId=${pid}`;
        });
    } else if (currentUser.role === 'TEACHER') {
        $('.manage-app').off('click').on('click', function(e) {
            e.stopPropagation();
            alert(`View applications for project ${$(this).data('id')} (demo)`);
        });
        $('.edit-proj').off('click').on('click', function(e) {
            e.stopPropagation();
            alert(`Edit project ${$(this).data('id')} (demo)`);
        });
    } else if (currentUser.role === 'ADMIN') {
        $('.edit-proj').off('click').on('click', function(e) {
            e.stopPropagation();
            alert(`Edit project ${$(this).data('id')} (demo)`);
        });
        $('.delete-proj').off('click').on('click', function(e) {
            e.stopPropagation();
            let pid = $(this).data('id');
            if (confirm(`Delete project ${pid}?`)) {
                const index = mockProjects.findIndex(p => p.id == pid);
                if (index !== -1) mockProjects.splice(index, 1);
                persistAll();
                loadProjectsList();
                alert('Project deleted');
            }
        });
    }
}

// Project detail rendering (for detail.html to call)
function showProjectDetail(projectId) {
    let project = mockProjects.find(p => p.id == projectId);
    if (!project) {
        $('#projectDetailContent').html('<p>Project not found.</p>');
        return;
    }
    let isStudent = currentUser && currentUser.role === 'STUDENT';
    let hasApplied = mockApplications.some(app => app.projectId === project.id && app.studentId === currentUser?.id);
    let statusBadge = '';
    switch(project.status) {
        case 'AVAILABLE': statusBadge = '<span class="badge badge-pending">Available</span>'; break;
        case 'REQUESTED': statusBadge = '<span class="badge badge-pending">Requested</span>'; break;
        case 'AGREED': statusBadge = '<span class="badge badge-accepted">Agreed</span>'; break;
        case 'CLOSED': statusBadge = '<span class="badge badge-withdrawn">Closed</span>'; break;
    }
    let skillsHtml = project.requiredSkills.map(s => `<span class="skill-tag">${escapeHtml(s)}</span>`).join('');
    let applyButton = '';
    if (isStudent && project.status === 'AVAILABLE' && !hasApplied) {
        applyButton = `<button id="applyBtn" class="btn btn-success">Apply for this project</button>`;
    } else if (hasApplied) {
        applyButton = `<p style="color:green;">You have already applied. Waiting for approval.</p>`;
    } else if (!isStudent && currentUser) {
        applyButton = `<p style="color:gray;">Only students can apply.</p>`;
    }
    let detailHtml = `
        <div class="card">
            <div class="project-header-top">
                <h1>${escapeHtml(project.title)}</h1>
                ${statusBadge}
            </div>
            <p><strong>Supervisor:</strong> ${project.teacherName}</p>
        </div>
        <div class="card">
            <h2>Description</h2>
            <p>${escapeHtml(project.description)}</p>
            <h3>Required Skills</h3>
            <div class="skills-tags">${skillsHtml}</div>
            <h3>Details</h3>
            <ul>
                <li>Category: ${project.category}</li>
                <li>Max Students: ${project.maxStudents || 'Unlimited'}</li>
                <li>Current Applicants: ${project.currentApplicants}</li>
            </ul>
            ${applyButton}
            <div id="detailMessage" class="message-area"></div>
        </div>
    `;
    $('#projectDetailContent').html(detailHtml);
    if (applyButton && !hasApplied) {
        $('#applyBtn').off('click').on('click', function() {
            if (!currentUser) {
                showMessage('#detailMessage', 'Please login first', 'error');
                return;
            }
            let already = mockApplications.some(a => a.projectId === project.id && a.studentId === currentUser.id);
            if (already) {
                showMessage('#detailMessage', 'Already applied', 'error');
                return;
            }
            if (project.maxStudents && project.currentApplicants >= project.maxStudents) {
                showMessage('#detailMessage', 'Project is full', 'error');
                return;
            }
            let hasAgreed = mockApplications.some(app => app.studentId === currentUser.id &&
                mockProjects.find(p => p.id === app.projectId)?.status === 'AGREED');
            if (hasAgreed) {
                showMessage('#detailMessage', 'You already have an agreed project', 'error');
                return;
            }
            let newApp = {
                id: mockApplications.length+1,
                projectId: project.id,
                studentId: currentUser.id,
                status: 'PENDING',
                note: ''
            };
            mockApplications.push(newApp);
            project.currentApplicants += 1;
            project.status = 'REQUESTED';
            persistAll();
            showMessage('#detailMessage', 'Application submitted!', 'success');
            setTimeout(() => showProjectDetail(project.id), 1000);
        });
    }
}

// ========== Project System Shared Data (teacher & admin pages) ==========
let users = [], projects = [], applications = [], categories = [];

function loadData() {
    const stored = localStorage.getItem('projectSystemData');
    if(stored) {
        const data = JSON.parse(stored);
        users = data.users || [];
        projects = data.projects || [];
        applications = data.applications || [];
        categories = data.categories || [];
    }
    if(users.length === 0) {
        users = [
            { id: 1, username: "teacher1", fullName: "Teacher 1", email: "teacher1@uni.edu", role: "teacher", status: "active", teacherId: 101 },
            { id: 2, username: "teacher2", fullName: "Teacher 2", email: "teacher2@uni.edu", role: "teacher", status: "active", teacherId: 102 },
            { id: 3, username: "student1", fullName: "Student 1", email: "s1@stu.edu", role: "student", status: "active", studentId: 201 },
            { id: 4, username: "student2", fullName: "Student 2", email: "s2@stu.edu", role: "student", status: "active", studentId: 202 },
            { id: 5, username: "admin", fullName: "Administrator", email: "admin@system.com", role: "admin", status: "active" }
        ];
        projects = [
            { id: 1, teacherId: 101, title: "Deep Learning for Image Classification", description: "Using CNN to classify images", requiredSkills: "Python, PyTorch", keywords: "AI", maxStudents: 2, status: "available" },
            { id: 2, teacherId: 101, title: "Smart Campus Mini Program", description: "Develop a WeChat mini program", requiredSkills: "JavaScript", keywords: "mini program", maxStudents: 3, status: "available" },
            { id: 3, teacherId: 102, title: "Blockchain Traceability System", description: "Agricultural product traceability", requiredSkills: "Go", keywords: "blockchain", maxStudents: 2, status: "available" }
        ];
        applications = [
            { id: 1, projectId: 1, studentId: 201, studentName: "Student 1", reason: "Interested in deep learning", status: "pending", teacherFeedback: "", createdAt: "2026-04-01" },
            { id: 2, projectId: 2, studentId: 202, studentName: "Student 2", reason: "Experience with mini programs", status: "pending", teacherFeedback: "", createdAt: "2026-04-02" },
            { id: 3, projectId: 1, studentId: 202, studentName: "Student 2", reason: "Also want to try AI", status: "approved", teacherFeedback: "Welcome", createdAt: "2026-03-28" }
        ];
        categories = [
            { id: 1, name: "Artificial Intelligence", description: "AI related projects" },
            { id: 2, name: "Web Development", description: "Frontend/backend projects" },
            { id: 3, name: "Mobile Applications", description: "App/mini program" }
        ];
        saveData();
    }
}
function saveData() {
    localStorage.setItem('projectSystemData', JSON.stringify({ users, projects, applications, categories }));
}
loadData();
