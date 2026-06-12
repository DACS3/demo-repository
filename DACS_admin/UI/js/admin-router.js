import { auth } from "../firebase-init.js";
import { signOut } from "https://www.gstatic.com/firebasejs/10.11.0/firebase-auth.js";
import { updateCharts } from "./admin-charts.js";

export function switchTabInternal(tabId, menuItem) {
    document.querySelectorAll('.sidebar-menu .menu-item').forEach(item => {
        item.classList.remove('active');
    });
    menuItem.classList.add('active');
    
    const titles = {
        'thongke': 'Thống kê hệ thống',
        'users': 'Quản lý Người dùng',
        'comments': 'Quản lý Bình luận',
        'coins': 'Quản lý Xu (Coin)'
    };
    if (document.querySelector('.top-navbar h4')) {
        document.querySelector('.top-navbar h4').innerText = titles[tabId] || 'Thống kê hệ thống';
    }
    
    document.querySelectorAll('.content-section').forEach(sec => {
        sec.classList.remove('active');
    });
    
    const targetSec = document.getElementById(`section-${tabId}`);
    if (targetSec) targetSec.classList.add('active');
    
    if (tabId === 'thongke') {
        updateCharts();
    }
}

export function switchTab(tabId) {
    window.location.hash = tabId;
}

export async function logout() {
    if (confirm("Bạn muốn đăng xuất khỏi hệ thống quản trị?")) {
        try { 
            await signOut(auth); 
            window.location.href = "login.html"; 
        } catch (error) { 
            alert("Lỗi đăng xuất: " + error.message); 
        }
    }
}

// Đăng ký vào phạm vi toàn cục (global scope) để thẻ HTML gọi được qua thuộc tính onclick
window.switchTab = switchTab;
window.logout = logout;

// Lắng nghe sự kiện hashchange
window.addEventListener('hashchange', () => {
    const tabId = window.location.hash.substring(1) || 'thongke';
    if (['thongke', 'users', 'comments', 'coins'].includes(tabId)) {
        const menuItem = document.querySelector(`.sidebar-menu .menu-item[onclick*="${tabId}"]`);
        if (menuItem) {
            switchTabInternal(tabId, menuItem);
        }
    }
});

// Khởi tạo tab ban đầu từ hash khi load trang
setTimeout(() => {
    const initialTab = window.location.hash.substring(1) || 'thongke';
    const initialMenuItem = document.querySelector(`.sidebar-menu .menu-item[onclick*="${initialTab}"]`);
    if (initialMenuItem) {
        switchTabInternal(initialTab, initialMenuItem);
    } else {
        window.location.hash = 'thongke';
    }
}, 100);
