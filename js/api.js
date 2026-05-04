const API_BASE_URL = 'http://localhost:8080';

// ========== Generic Request Wrapper ==========
async function request(url, options = {}) {
    const token = localStorage.getItem('authToken');
    const headers = {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
        ...options.headers
    };

    try {
        const response = await fetch(`${API_BASE_URL}${url}`, {
            ...options,
            headers
        });

        const result = await response.json().catch(() => ({}));

        // Standard response shape: { code, message, data }
        if (result.code !== 200) {
            throw new Error(result.message || `Request failed with status ${response.status}`);
        }

        return result.data;
    } catch (error) {
        console.error('API request failed:', error);
        if (error.message === 'Failed to fetch') {
            throw new Error('Cannot connect to backend server. Please make sure the backend is running on ' + API_BASE_URL);
        }
        throw error;
    }
}

function get(url)  { return request(url, { method: 'GET' }); }
function post(url, data) { return request(url, { method: 'POST', body: JSON.stringify(data) }); }
function put(url, data)  { return request(url, { method: 'PUT',  body: JSON.stringify(data) }); }
function patch(url, data){ return request(url, { method: 'PATCH',body: JSON.stringify(data) }); }
function del(url)  { return request(url, { method: 'DELETE' }); }

// ========== Auth APIs ==========
const AuthAPI = {
    login:    (username, password) => post('/api/auth/login', { username, password }),
    register: (data) => post('/api/auth/register', data),
    logout:   () => post('/api/auth/logout')
};

// ========== User APIs ==========
const UserAPI = {
    getMe:         () => get('/api/users/me'),
    updateMe:      (data) => put('/api/users/me', data),
    changePassword:(data) => post('/api/users/me/change-password', data),
    list:          (keyword) => get(`/api/users?keyword=${encodeURIComponent(keyword || '')}`),
    update:        (id, data) => put(`/api/users/${id}`, data),
    remove:        (id) => del(`/api/users/${id}`)
};

// ========== Category APIs ==========
const CategoryAPI = {
    list:   () => get('/api/categories'),
    create: (data) => post('/api/categories', data),
    update: (id, data) => put(`/api/categories/${id}`, data),
    remove: (id) => del(`/api/categories/${id}`)
};

// ========== Project APIs ==========
const ProjectAPI = {
    list: (filters = {}) => {
        const params = new URLSearchParams();
        if (filters.keyword)    params.append('keyword', filters.keyword);
        if (filters.status)     params.append('status', filters.status);
        if (filters.teacherId)  params.append('teacherId', filters.teacherId);
        if (filters.categoryId) params.append('categoryId', filters.categoryId);
        const query = params.toString();
        return get(`/api/projects${query ? '?' + query : ''}`);
    },
    get:        (id) => get(`/api/projects/${id}`),
    getMine:    () => get('/api/projects/mine'),
    create:     (data) => post('/api/projects', data),
    update:     (id, data) => put(`/api/projects/${id}`, data),
    updateStatus:(id, status) => patch(`/api/projects/${id}/status`, { status }),
    remove:     (id) => del(`/api/projects/${id}`)
};

// ========== Application APIs ==========
const ApplicationAPI = {
    getMine:      () => get('/api/applications/mine'),
    getByProject: (projectId) => get(`/api/applications/project/${projectId}`),
    get:          (id) => get(`/api/applications/${id}`),
    create:       (data) => post('/api/applications', data),
    withdraw:     (id) => post(`/api/applications/${id}/withdraw`),
    decision:     (id, status, feedback) => post(`/api/applications/${id}/decision`, { status, feedback })
};
