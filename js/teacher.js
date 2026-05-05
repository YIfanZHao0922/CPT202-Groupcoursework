// Display username
function setWelcomeMessage() {
    let username = localStorage.getItem('loggedInUsername');
    const sessionUser = sessionStorage.getItem('project_system_user');
    if (!username && sessionUser) {
        try {
            const user = JSON.parse(sessionUser);
            if (user.name) username = user.name;
        } catch (e) {}
    }
    if (!username) {
        username = "Teacher";
    }
    document.getElementById('welcomeUserSpan').innerText = "Welcome, " + username;
}

async function getTeacherProjects() {
    try {
        return await ProjectAPI.getMine();
    } catch (err) {
        console.warn('Backend unavailable, using local mock data for projects');
        loadData();
        return projects || [];
    }
}

async function getApplicationsForTeacher() {
    const teacherProjects = await getTeacherProjects();
    let allApps = [];
    for (const p of teacherProjects) {
        try {
            const apps = await ApplicationAPI.getByProject(p.id);
            allApps = allApps.concat(apps);
        } catch (err) {
            console.warn('Backend unavailable for applications');
            loadData();
            const localApps = (applications || []).filter(a => a.projectId === p.id);
            allApps = allApps.concat(localApps);
        }
    }
    return allApps;
}

async function renderProjectsTable() {
    const teacherProjects = await getTeacherProjects();
    const filterTitle = $("#project-search").val();
    const filterStatus = $("#project-status-filter").val();
    let filtered = teacherProjects.filter(p => {
        if(filterTitle && !p.title.toLowerCase().includes(filterTitle.toLowerCase())) return false;
        if(filterStatus && p.status !== filterStatus) return false;
        return true;
    });
    let tbody = "";
    filtered.forEach(p => {
        tbody += `<tr>
            <td>${p.id}</td>
            <td>${escapeHtml(p.title)}</td>
            <td>${escapeHtml(p.description)}</td>
            <td>${escapeHtml(p.requiredSkills)}</td>
            <td>${p.maxStudents}</td>
            <td><span class="badge ${p.status === 'available' ? 'badge-available' : 'badge-closed'}">${p.status === 'available' ? 'Available' : 'Closed'}</span></td>
            <td class="action-buttons">
                <button class="edit-project btn btn-outline" data-id="${p.id}">Edit</button>
                <button class="delete-project btn btn-danger" data-id="${p.id}">Delete</button>
                ${p.status === 'available' ? `<button class="close-project btn btn-warning" data-id="${p.id}">Close</button>` : ''}
            </td>
        </tr>`;
    });
    $("#projects-tbody").html(tbody);
}

async function renderApprovalsTable() {
    const allApps = await getApplicationsForTeacher();
    const statusFilter = $("#approval-status-filter").val();
    let filtered = allApps.filter(app => !statusFilter || app.status === statusFilter);
    let tbody = "";
    filtered.forEach(app => {
        let statusClass = "", statusText = "";
        if(app.status === 'pending') { statusClass = "badge-pending"; statusText = "Pending"; }
        else if(app.status === 'approved') { statusClass = "badge-approved"; statusText = "Approved"; }
        else { statusClass = "badge-rejected"; statusText = "Rejected"; }
        tbody += `<tr>
            <td>${escapeHtml(app.studentName)}</td>
            <td>${escapeHtml(app.projectTitle || 'Unknown')}</td>
            <td>${escapeHtml(app.reason)}</td>
            <td><span class="badge ${statusClass}">${statusText}</span></td>
            <td>${escapeHtml(app.teacherFeedback || '-')}</td>
            <td class="action-buttons">
                ${app.status === 'pending' ? `<button class="approve-app btn btn-success" data-id="${app.id}">Approve</button> <button class="reject-app btn btn-danger" data-id="${app.id}">Reject</button>` : ''}
            </td>
        </tr>`;
    });
    $("#approvals-tbody").html(tbody);
}

// Project dialog (includes required validation and Student Number)
let projectDialog = $(`<div></div>`).appendTo("body");
function showProjectForm(project = null) {
    const isEdit = !!project;
    let formHtml = `
        <form id="projectForm">
            <div class="form-group" id="title-group">
                <label>Title *</label>
                <input type="text" id="proj-title" value="${isEdit ? escapeHtml(project.title) : ''}">
                <span class="error-hint" id="title-error" style="display:none;">* Required</span>
            </div>
            <div class="form-group" id="desc-group">
                <label>Description *</label>
                <textarea id="proj-desc" rows="3">${isEdit ? escapeHtml(project.description) : ''}</textarea>
                <span class="error-hint" id="desc-error" style="display:none;">* Required</span>
            </div>
            <div class="form-group">
                <label>Required Skills</label>
                <input id="proj-skills" value="${isEdit ? escapeHtml(project.requiredSkills) : ''}">
            </div>
            <div class="form-group" id="keywords-group">
                <label>Keywords *</label>
                <input id="proj-keywords" value="${isEdit ? escapeHtml(project.keywords) : ''}">
                <span class="error-hint" id="keywords-error" style="display:none;">* Required</span>
            </div>
            <div class="form-group">
                <label>Student Number</label>
                <input type="number" id="proj-max" min="1" value="${isEdit ? project.maxStudents : 2}">
            </div>
        </form>
    `;
    projectDialog.html(formHtml).dialog({
        title: isEdit ? "Edit Project" : "New Project",
        modal: true,
        width: 550,
        buttons: {
            "Save": async function() {
                let isValid = true;
                const title = $("#proj-title").val().trim();
                const desc = $("#proj-desc").val().trim();
                const keywords = $("#proj-keywords").val().trim();

                if(!title) {
                    $("#title-error").show();
                    $("#title-group").addClass("has-error");
                    isValid = false;
                } else {
                    $("#title-error").hide();
                    $("#title-group").removeClass("has-error");
                }
                if(!desc) {
                    $("#desc-error").show();
                    $("#desc-group").addClass("has-error");
                    isValid = false;
                } else {
                    $("#desc-error").hide();
                    $("#desc-group").removeClass("has-error");
                }
                if(!keywords) {
                    $("#keywords-error").show();
                    $("#keywords-group").addClass("has-error");
                    isValid = false;
                } else {
                    $("#keywords-error").hide();
                    $("#keywords-group").removeClass("has-error");
                }

                let studentNum = parseInt($("#proj-max").val());
                if(isNaN(studentNum) || studentNum < 1) {
                    alert("Student Number must be at least 1.");
                    isValid = false;
                }

                if(!isValid) return;

                try {
                    const data = {
                        title: title,
                        description: desc,
                        requiredSkills: $("#proj-skills").val(),
                        keywords: keywords,
                        maxStudents: studentNum,
                        status: "available"
                    };
                    if(isEdit) {
                        await ProjectAPI.update(project.id, data);
                    } else {
                        await ProjectAPI.create(data);
                    }
                    await renderProjectsTable();
                    projectDialog.dialog("close");
                    alert("Saved successfully");
                } catch (err) {
                    alert(err.message || 'Save failed');
                }
            },
            "Cancel": () => projectDialog.dialog("close")
        }
    });
    if(isEdit) {
        if(project.title) $("#title-error").hide();
        if(project.description) $("#desc-error").hide();
        if(project.keywords) $("#keywords-error").hide();
    }
}

function showFeedbackDialog(appId, action) {
    ApplicationAPI.get(appId).then(app => {
        if(!app) return;
        let dialogDiv = $(`<div title="${action === 'approve' ? 'Approve Application' : 'Reject Application'}"></div>`).appendTo("body");
        dialogDiv.html(`<div class="form-group"><label>Feedback:</label><textarea id="fb-text" rows="3" style="width:100%">${app.teacherFeedback || ''}</textarea></div>`);
        dialogDiv.dialog({
            modal: true, width: 450,
            buttons: {
                "Confirm": async function() {
                    const feedback = $("#fb-text").val();
                    try {
                        const status = action === 'approve' ? 'ACCEPTED' : 'REJECTED';
                        await ApplicationAPI.decision(appId, status, feedback || (action === 'approve' ? 'Approved' : 'Rejected'));
                        await renderApprovalsTable();
                        dialogDiv.dialog("close");
                        alert(`${action === 'approve' ? 'Approved' : 'Rejected'} successfully`);
                    } catch (err) {
                        alert(err.message || 'Operation failed');
                    }
                },
                "Cancel": () => dialogDiv.dialog("close")
            }
        });
    }).catch(() => {
        alert('Failed to load application details');
    });
}

$(document).ready(function() {
    setWelcomeMessage();
    renderProjectsTable();
    renderApprovalsTable();

    $("#project-search, #project-status-filter").on("input change", renderProjectsTable);
    $("#approval-status-filter").on("change", renderApprovalsTable);
    $("#new-project-btn").on("click", () => showProjectForm(null));
    $(document).on("click", ".edit-project", function() {
        ProjectAPI.get(parseInt($(this).data("id"))).then(p => { if(p) showProjectForm(p); });
    });
    $(document).on("click", ".delete-project", async function() {
        if(confirm("Delete project and all related applications?")) {
            const id = parseInt($(this).data("id"));
            try {
                await ProjectAPI.remove(id);
                await renderProjectsTable();
                await renderApprovalsTable();
                alert("Deleted");
            } catch (err) {
                alert(err.message || 'Delete failed');
            }
        }
    });
    $(document).on("click", ".close-project", async function() {
        const id = parseInt($(this).data("id"));
        try {
            await ProjectAPI.updateStatus(id, 'closed');
            await renderProjectsTable();
            alert("Project closed");
        } catch (err) {
            alert(err.message || 'Close failed');
        }
    });
    $(document).on("click", ".approve-app", function() { showFeedbackDialog(parseInt($(this).data("id")), "approve"); });
    $(document).on("click", ".reject-app", function() { showFeedbackDialog(parseInt($(this).data("id")), "reject"); });
});

// Sidebar switching
$("#menu-projects").click(function() {
    $("#menu-projects").addClass("active");
    $("#menu-approvals").removeClass("active");
    $("#projects-view").addClass("active");
    $("#approvals-view").removeClass("active");
});
$("#menu-approvals").click(function() {
    $("#menu-approvals").addClass("active");
    $("#menu-projects").removeClass("active");
    $("#approvals-view").addClass("active");
    $("#projects-view").removeClass("active");
    renderApprovalsTable();
});

$("#logoutBtn").click(function() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('projectSystemLoggedIn');
    localStorage.removeItem('loggedInUsername');
    sessionStorage.removeItem('project_system_user');
    window.location.href = "../../common/html/login.html";
});
