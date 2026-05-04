// Render user table
async function renderUsersTable() {
    const roleFilter = $("#user-role-filter").val();
    let allUsers = [];
    try {
        allUsers = await UserAPI.list();
    } catch (err) {
        console.warn('Backend unavailable, using local mock data for users');
        loadData();
        allUsers = users || [];
    }
    let filtered = allUsers.filter(u => !roleFilter || u.role === roleFilter);
    let tbody = "";
    filtered.forEach(u => {
        tbody += `<tr>
            <td>${u.id}</td><td>${escapeHtml(u.username)}</td><td>${escapeHtml(u.fullName)}</td>
            <td>${escapeHtml(u.email)}</td><td>${u.role}</td>
            <td><span class="badge">${u.status === 'active' ? 'Active' : 'Disabled'}</span></td>
            <td>
                <button class="edit-user btn btn-outline" data-id="${u.id}">Edit</button>
                ${u.status === 'active' ? `<button class="disable-user btn btn-warning" data-id="${u.id}">Disable</button>` : `<button class="enable-user btn btn-success" data-id="${u.id}">Enable</button>`}
            </td>
        </tr>`;
    });
    $("#users-tbody").html(tbody);
}

// Render category table
async function renderCategoriesTable() {
    let allCats = [];
    try {
        allCats = await CategoryAPI.list();
    } catch (err) {
        console.warn('Backend unavailable, using local mock data for categories');
        loadData();
        allCats = categories || [];
    }
    let tbody = "";
    allCats.forEach(c => {
        tbody += `<tr>
            <td>${c.id}</td><td>${escapeHtml(c.name)}</td><td>${escapeHtml(c.description)}</td>
            <td><button class="edit-cat btn btn-outline" data-id="${c.id}">Edit</button> <button class="delete-cat btn btn-danger" data-id="${c.id}">Delete</button></td>
        </tr>`;
    });
    $("#categories-tbody").html(tbody);
}

// User form dialog
let userDialog = $(`<div></div>`).appendTo("body");
function showUserForm(user = null) {
    const isEdit = !!user;
    let formHtml = `<form>
        <div class="form-group"><label>Username:</label><input id="uname" value="${isEdit ? user.username : ''}" ${isEdit ? 'disabled' : ''}></div>
        <div class="form-group"><label>Full Name:</label><input id="ufull" value="${isEdit ? user.fullName : ''}"></div>
        <div class="form-group"><label>Email:</label><input id="umail" value="${isEdit ? user.email : ''}"></div>
        <div class="form-group"><label>Role:</label><select id="urole"><option value="student">Student</option><option value="teacher">Teacher</option><option value="admin">Admin</option></select></div>
    </form>`;
    if(isEdit) $(`#urole`, formHtml).val(user.role);
    userDialog.html(formHtml).dialog({
        title: isEdit ? "Edit User" : "New User", modal: true, width: 500,
        buttons: {
            "Save": async function() {
                if(!$("#ufull").val()) { alert("Full name cannot be empty"); return; }
                try {
                    const data = {
                        fullName: $("#ufull").val(),
                        email: $("#umail").val(),
                        role: $("#urole").val()
                    };
                    if(isEdit) {
                        await UserAPI.update(user.id, data);
                    } else {
                        await AuthAPI.register({
                            username: $("#uname").val(),
                            password: "123456",
                            email: data.email,
                            fullName: data.fullName,
                            role: data.role
                        });
                    }
                    await renderUsersTable();
                    userDialog.dialog("close");
                    alert("Saved successfully");
                } catch (err) {
                    alert(err.message || 'Save failed');
                }
            },
            "Cancel": () => userDialog.dialog("close")
        }
    });
}

// Category form dialog
let catDialog = $(`<div></div>`).appendTo("body");
function showCatForm(cat = null) {
    const isEdit = !!cat;
    let formHtml = `<form>
        <div class="form-group"><label>Category Name:</label><input id="cname" value="${isEdit ? cat.name : ''}"></div>
        <div class="form-group"><label>Description:</label><textarea id="cdesc" rows="2">${isEdit ? cat.description : ''}</textarea></div>
    </form>`;
    catDialog.html(formHtml).dialog({
        title: isEdit ? "Edit Category" : "New Category", modal: true, width: 450,
        buttons: {
            "Save": async function() {
                const name = $("#cname").val().trim();
                if(!name) { alert("Category name cannot be empty"); return; }
                try {
                    const data = { name: name, description: $("#cdesc").val() };
                    if(isEdit) {
                        await CategoryAPI.update(cat.id, data);
                    } else {
                        await CategoryAPI.create(data);
                    }
                    await renderCategoriesTable();
                    catDialog.dialog("close");
                } catch (err) {
                    alert(err.message || 'Save failed');
                }
            },
            "Cancel": () => catDialog.dialog("close")
        }
    });
}

$(document).ready(function() {
    renderUsersTable();
    renderCategoriesTable();
    $("#user-role-filter").on("change", renderUsersTable);
    $("#new-user-btn").on("click", () => showUserForm(null));
    $("#new-cat-btn").on("click", () => showCatForm(null));
    $(document).on("click", ".edit-user", async function() {
        let allUsers = [];
        try { allUsers = await UserAPI.list(); }
        catch (e) { loadData(); allUsers = users || []; }
        const u = allUsers.find(u => u.id === parseInt($(this).data("id")));
        if(u) showUserForm(u);
    });
    $(document).on("click", ".disable-user", async function() {
        const id = parseInt($(this).data("id"));
        try {
            await UserAPI.update(id, { status: "disabled" });
            await renderUsersTable();
            alert("User disabled");
        } catch (err) {
            alert(err.message || 'Operation failed');
        }
    });
    $(document).on("click", ".enable-user", async function() {
        const id = parseInt($(this).data("id"));
        try {
            await UserAPI.update(id, { status: "active" });
            await renderUsersTable();
            alert("User enabled");
        } catch (err) {
            alert(err.message || 'Operation failed');
        }
    });
    $(document).on("click", ".edit-cat", async function() {
        let allCats = [];
        try { allCats = await CategoryAPI.list(); }
        catch (e) { loadData(); allCats = categories || []; }
        const c = allCats.find(c => c.id === parseInt($(this).data("id")));
        if(c) showCatForm(c);
    });
    $(document).on("click", ".delete-cat", async function() {
        if(confirm("Delete this category?")) {
            const id = parseInt($(this).data("id"));
            try {
                await CategoryAPI.remove(id);
                await renderCategoriesTable();
                alert("Deleted");
            } catch (err) {
                alert(err.message || 'Delete failed');
            }
        }
    });
});

// Sidebar menu switching
$("#menu-users").click(function() {
    $("#menu-users").addClass("active");
    $("#menu-categories").removeClass("active");
    $("#users-view").addClass("active");
    $("#categories-view").removeClass("active");
});
$("#menu-categories").click(function() {
    $("#menu-categories").addClass("active");
    $("#menu-users").removeClass("active");
    $("#categories-view").addClass("active");
    $("#users-view").removeClass("active");
    renderCategoriesTable();
});

$("#logoutBtn").click(function() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('projectSystemLoggedIn');
    sessionStorage.removeItem('project_system_user');
    window.location.href = "../../common/html/login.html";
});
