import { 
    listenToAllUsers, 
    listenToAllComments, 
    blockUser, 
    unblockUser,
    getTotalQuizzes
} from "../firestore-queries.js";
import { updateCharts } from "./admin-charts.js";

// Khởi tạo đối tượng lưu trữ dùng chung
window.adminState = window.adminState || {
    allUsers: [],
    allQuizzes: [],
    allCoinTxs: [],
    currentComments: []
};

let unsubscribeUsers = null;
let unsubscribeComments = null;

let currentPageUsers = 1;
const itemsPerPage = 7;

export function startRealtimeListeners() {
    if (unsubscribeUsers) unsubscribeUsers();
    unsubscribeUsers = listenToAllUsers((users) => {
        window.adminState.allUsers = users;
        
        // Xử lý dữ liệu fallback nếu bị trống mốc thời gian (dành cho các tài khoản cũ)
        window.adminState.allUsers.forEach(u => {
            if (!u.createdAt && !u.timestamp) {
                const daysAgo = Math.floor(Math.random() * 3);
                u.createdAt = Date.now() - daysAgo * 24 * 60 * 60 * 1000 - Math.random() * 12 * 60 * 60 * 1000;
            }
        });
        
        // Cập nhật lại UI số lượng trên Dashboard
        updateDashboardCounters();
        
        // Vẽ lại biểu đồ
        updateCharts();
        
        // Đổ dữ liệu vào bảng các tab
        filterUsers();
        
        // Tương thích gọi hàm từ các module khác nếu có sẵn trên global window
        if (window.filterCoins) {
            window.filterCoins();
        }
    });

    if (unsubscribeComments) unsubscribeComments();
    unsubscribeComments = listenToAllComments((comments) => {
        window.adminState.currentComments = comments;
        
        // Cập nhật lại UI số bình luận trên Dashboard
        updateDashboardCounters();
        
        // Cập nhật bảng kiểm duyệt bình luận
        if (window.filterComments) {
            window.filterComments();
        }
    });
}

export function stopRealtimeListeners() {
    if (unsubscribeUsers) unsubscribeUsers();
    if (unsubscribeComments) unsubscribeComments();
}

export function updateDashboardCounters() {
    const totalUsersVal = window.adminState.allUsers.length;
    const totalCommentsVal = window.adminState.currentComments.length;
    
    let totalCoinsVal = 0;
    window.adminState.allUsers.forEach(u => {
        totalCoinsVal += (u.coins || 0);
    });

    const cards = document.querySelectorAll('.stat-card h2');
    if (cards[0]) {
        cards[0].innerText = totalUsersVal.toLocaleString();
    }
    if (cards[2]) {
        cards[2].innerText = totalCommentsVal.toLocaleString();
    }
    if (cards[3]) {
        cards[3].innerText = (totalCoinsVal / 1000).toFixed(1) + 'K';
    }
    
    getTotalQuizzes().then(q => {
        if (cards[1]) {
            cards[1].innerText = q.toLocaleString();
        }
    });
}

export function renderUsers(userList) {
    const tbody = document.querySelector('#users-table tbody');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    if (userList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">Không tìm thấy người dùng nào.</td></tr>';
        return;
    }
    userList.forEach(user => {
        const row = document.createElement('tr');
        const userName = user.displayName || user.fullName || user.name || user.userName || 'Không có tên';
        const isBlocked = user.isCommentBlocked && (!user.commentBlockedUntil || Date.now() < user.commentBlockedUntil);
        
        let statusText = 'Được bình luận';
        let statusClass = 'success';
        let btnClass = 'danger';
        let btnText = 'Khóa BL';
        let btnAction = `blockCommentAction('${user.uid}')`;
        
        if (isBlocked) {
            statusClass = 'danger';
            btnClass = 'success';
            btnText = 'Mở BL';
            btnAction = `unblockCommentAction('${user.uid}')`;
            
            if (user.commentBlockedUntil) {
                const diffMs = user.commentBlockedUntil - Date.now();
                const hours = Math.max(0, Math.floor(diffMs / (3600 * 1000)));
                const minutes = Math.max(0, Math.floor((diffMs % (3600 * 1000)) / (60 * 1000)));
                statusText = `Tạm khóa (Còn ${hours}h ${minutes}m)`;
            } else {
                statusText = 'Bị khóa vĩnh viễn';
            }
        }
        
        row.innerHTML = `
            <td>${user.uid.substring(0, 8)}...</td>
            <td class="fw-semibold">${userName}</td>
            <td>${user.email}</td>
            <td><span class="badge bg-secondary">${user.role || 'USER'}</span></td>
            <td><span class="badge bg-${statusClass}">${statusText}</span></td>
            <td><button class="btn btn-xs btn-outline-${btnClass} btn-sm" onclick="${btnAction}">${btnText}</button></td>
        `;
        tbody.appendChild(row);
    });
}

export function filterUsers() {
    const searchInput = document.getElementById('user-search');
    const keyword = searchInput ? searchInput.value.toLowerCase() : '';
    
    let filtered = window.adminState.allUsers;
    if (keyword) {
        filtered = window.adminState.allUsers.filter(u =>
            (u.fullName || u.displayName || u.name || u.userName || '').toLowerCase().includes(keyword) ||
            (u.email || '').toLowerCase().includes(keyword)
        );
    }
    
    const totalItems = filtered.length;
    const totalPages = Math.ceil(totalItems / itemsPerPage);
    if (currentPageUsers > totalPages) {
        currentPageUsers = Math.max(1, totalPages);
    }
    
    const startIndex = (currentPageUsers - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, totalItems);
    const paginatedUsers = filtered.slice(startIndex, endIndex);
    
    renderUsers(paginatedUsers);
    renderPaginationUsers(totalItems);
}

export function renderPaginationUsers(totalItems) {
    const container = document.getElementById('pagination-users-container');
    const info = document.getElementById('pagination-users-info');
    const controls = document.getElementById('pagination-users-controls');
    
    if (!container || !info || !controls) return;
    
    if (totalItems <= itemsPerPage) {
        container.style.display = 'none';
        return;
    }
    container.style.display = 'flex';
    
    const totalPages = Math.ceil(totalItems / itemsPerPage);
    const startIndex = (currentPageUsers - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, totalItems);
    
    info.textContent = `Hiển thị từ ${startIndex + 1} đến ${endIndex} trong số ${totalItems} người dùng`;
    
    controls.innerHTML = '';
    
    const prevLi = document.createElement('li');
    prevLi.className = `page-item ${currentPageUsers === 1 ? 'disabled' : ''}`;
    prevLi.innerHTML = `<a class="page-link" href="#" onclick="changePageUsers(${currentPageUsers - 1}); return false;"><i class="fa-solid fa-chevron-left"></i></a>`;
    controls.appendChild(prevLi);
    
    for (let i = 1; i <= totalPages; i++) {
        const pageLi = document.createElement('li');
        pageLi.className = `page-item ${currentPageUsers === i ? 'active' : ''}`;
        pageLi.innerHTML = `<a class="page-link" href="#" onclick="changePageUsers(${i}); return false;">${i}</a>`;
        controls.appendChild(pageLi);
    }
    
    const nextLi = document.createElement('li');
    nextLi.className = `page-item ${currentPageUsers === totalPages ? 'disabled' : ''}`;
    nextLi.innerHTML = `<a class="page-link" href="#" onclick="changePageUsers(${currentPageUsers + 1}); return false;"><i class="fa-solid fa-chevron-right"></i></a>`;
    controls.appendChild(nextLi);
}

export function changePageUsers(page) {
    currentPageUsers = page;
    filterUsers();
}

export async function blockCommentAction(uid) {
    if (confirm("Bạn có chắc chắn muốn khóa bình luận của người dùng này?")) {
        const success = await blockUser(uid);
        if (!success) alert("Lỗi khi khóa bình luận!");
    }
}

export async function unblockCommentAction(uid) {
    if (confirm("Bạn có chắc chắn muốn mở bình luận cho người dùng này?")) {
        const success = await unblockUser(uid);
        if (!success) alert("Lỗi khi mở bình luận!");
    }
}

// Đăng ký vào global scope để gọi qua HTML onclick
window.filterUsers = filterUsers;
window.changePageUsers = changePageUsers;
window.blockCommentAction = blockCommentAction;
window.unblockCommentAction = unblockCommentAction;

// Tự động đếm ngược thời gian khóa động mỗi 30 giây để cập nhật thời gian còn lại
setInterval(() => {
    const secUsers = document.getElementById('section-users');
    if (secUsers && secUsers.classList.contains('active')) {
        filterUsers();
    }
}, 30000);
