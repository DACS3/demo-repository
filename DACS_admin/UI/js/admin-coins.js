import { 
    addCoinsToUser, 
    subtractCoinsFromUser 
} from "../firestore-queries.js";

// Khởi tạo đối tượng lưu trữ dùng chung
window.adminState = window.adminState || {
    allUsers: [],
    allQuizzes: [],
    allCoinTxs: [],
    currentComments: []
};

let currentPageCoins = 1;
const itemsPerPage = 7;

export function renderCoins(userList) {
    const tbody = document.querySelector('#coins-table tbody');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    if (userList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" class="text-center text-muted py-4">Không tìm thấy người dùng nào.</td></tr>';
        return;
    }
    userList.forEach(user => {
        const row = document.createElement('tr');
        row.setAttribute('data-uid', user.uid);
        row.innerHTML = `
            <td>${user.email}</td>
            <td class="fw-bold text-warning coin-display"><i class="fa-solid fa-coins me-1"></i> ${user.coins || 0} Xu</td>
            <td>
                <div class="input-group input-group-sm" style="max-width: 250px;">
                    <input type="number" class="form-control coin-input" placeholder="Số lượng" data-uid="${user.uid}">
                    <button class="btn btn-success" type="button" onclick="addCoinsAction('${user.uid}', this)">+ Cộng</button>
                    <button class="btn btn-danger" type="button" onclick="subtractCoinsAction('${user.uid}', this)">- Trừ</button>
                </div>
            </td>
        `;
        tbody.appendChild(row);
    });
}

export function filterCoins() {
    const searchInput = document.getElementById('coin-search');
    const keyword = searchInput ? searchInput.value.toLowerCase() : '';
    
    let filtered = window.adminState.allUsers;
    if (keyword) {
        filtered = window.adminState.allUsers.filter(u => (u.email || '').toLowerCase().includes(keyword));
    }
    
    const totalItems = filtered.length;
    const totalPages = Math.ceil(totalItems / itemsPerPage);
    if (currentPageCoins > totalPages) {
        currentPageCoins = Math.max(1, totalPages);
    }
    
    const startIndex = (currentPageCoins - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, totalItems);
    const paginatedUsers = filtered.slice(startIndex, endIndex);
    
    renderCoins(paginatedUsers);
    renderPaginationCoins(totalItems);
}

export function renderPaginationCoins(totalItems) {
    const container = document.getElementById('pagination-coins-container');
    const info = document.getElementById('pagination-coins-info');
    const controls = document.getElementById('pagination-coins-controls');
    
    if (!container || !info || !controls) return;
    
    if (totalItems <= itemsPerPage) {
        container.style.display = 'none';
        return;
    }
    container.style.display = 'flex';
    
    const totalPages = Math.ceil(totalItems / itemsPerPage);
    const startIndex = (currentPageCoins - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, totalItems);
    
    info.textContent = `Hiển thị từ ${startIndex + 1} đến ${endIndex} trong số ${totalItems} người dùng`;
    
    controls.innerHTML = '';
    
    const prevLi = document.createElement('li');
    prevLi.className = `page-item ${currentPageCoins === 1 ? 'disabled' : ''}`;
    prevLi.innerHTML = `<a class="page-link" href="#" onclick="changePageCoins(${currentPageCoins - 1}); return false;"><i class="fa-solid fa-chevron-left"></i></a>`;
    controls.appendChild(prevLi);
    
    for (let i = 1; i <= totalPages; i++) {
        const pageLi = document.createElement('li');
        pageLi.className = `page-item ${currentPageCoins === i ? 'active' : ''}`;
        pageLi.innerHTML = `<a class="page-link" href="#" onclick="changePageCoins(${i}); return false;">${i}</a>`;
        controls.appendChild(pageLi);
    }
    
    const nextLi = document.createElement('li');
    nextLi.className = `page-item ${currentPageCoins === totalPages ? 'disabled' : ''}`;
    nextLi.innerHTML = `<a class="page-link" href="#" onclick="changePageCoins(${currentPageCoins + 1}); return false;"><i class="fa-solid fa-chevron-right"></i></a>`;
    controls.appendChild(nextLi);
}

export function changePageCoins(page) {
    currentPageCoins = page;
    filterCoins();
}

export async function addCoinsAction(uid, btn) {
    const input = btn.parentElement.querySelector('.coin-input');
    const amount = parseInt(input.value);
    if (!amount || amount <= 0) { alert("Vui lòng nhập số lượng xu hợp lệ!"); return; }
    const newCoins = await addCoinsToUser(uid, amount);
    if (newCoins !== null) {
        input.value = '';
    } else {
        alert("Lỗi khi cộng xu!");
    }
}

export async function subtractCoinsAction(uid, btn) {
    const input = btn.parentElement.querySelector('.coin-input');
    const amount = parseInt(input.value);
    if (!amount || amount <= 0) { alert("Vui lòng nhập số lượng xu hợp lệ!"); return; }
    const newCoins = await subtractCoinsFromUser(uid, amount);
    if (newCoins !== null) {
        input.value = '';
    } else {
        alert("Lỗi khi trừ xu!");
    }
}

// Đăng ký các hàm ra phạm vi toàn cục window để gọi trực tiếp từ HTML
window.filterCoins = filterCoins;
window.changePageCoins = changePageCoins;
window.addCoinsAction = addCoinsAction;
window.subtractCoinsAction = subtractCoinsAction;
