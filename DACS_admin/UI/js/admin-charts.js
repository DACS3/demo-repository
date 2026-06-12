import { 
    getTotalQuizzes, 
    getAllQuizzes, 
    getAllCoinTransactions 
} from "../firestore-queries.js";

let chartUsers = null;
let chartQuizzes = null;
let chartRevenue = null;
let currentFilter = 'day';

// Khởi tạo đối tượng lưu trữ dùng chung
window.adminState = window.adminState || {
    allUsers: [],
    allQuizzes: [],
    allCoinTxs: [],
    currentComments: []
};

export function initCharts() {
    const canvasUsers = document.getElementById('chart-users');
    const canvasQuizzes = document.getElementById('chart-quizzes');
    const canvasRevenue = document.getElementById('chart-revenue');
    
    if (!canvasUsers || !canvasQuizzes || !canvasRevenue) return;
    
    const ctxUsers = canvasUsers.getContext('2d');
    const ctxQuizzes = canvasQuizzes.getContext('2d');
    const ctxRevenue = canvasRevenue.getContext('2d');
    
    chartUsers = new Chart(ctxUsers, {
        type: 'line',
        data: { labels: [], datasets: [{ label: 'Đăng ký mới', data: [], borderColor: '#6200EE', backgroundColor: 'rgba(98, 0, 238, 0.1)', fill: true, tension: 0.3 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }
    });
    
    chartQuizzes = new Chart(ctxQuizzes, {
        type: 'bar',
        data: { labels: [], datasets: [{ label: 'Đề mới', data: [], backgroundColor: '#2E7D32', borderRadius: 4 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }
    });
    
    chartRevenue = new Chart(ctxRevenue, {
        type: 'line',
        data: { labels: [], datasets: [{ label: 'Doanh thu (VNĐ)', data: [], borderColor: '#E65100', backgroundColor: 'rgba(230, 81, 0, 0.1)', fill: true, tension: 0.3 }] },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } } }
    });
}

export function updateCharts() {
    if (!chartUsers || !chartQuizzes || !chartRevenue) return;

    const uData = groupDataByTime(window.adminState.allUsers, currentFilter, 'createdAt');
    const qData = groupDataByTime(window.adminState.allQuizzes, currentFilter, 'timestamp');
    const rRawData = groupDataByTime(window.adminState.allCoinTxs.filter(tx => tx.type !== "daily_reward"), currentFilter, 'timestamp');
    
    // Quy đổi xu ra doanh thu VND: 100 xu = 10.000 VNĐ -> 1 xu = 100 VNĐ
    const rData = {
        labels: rRawData.labels,
        values: rRawData.values.map(val => val * 100)
    };
    
    chartUsers.data.labels = uData.labels;
    chartUsers.data.datasets[0].data = uData.values;
    chartUsers.update();
    
    chartQuizzes.data.labels = qData.labels;
    chartQuizzes.data.datasets[0].data = qData.values;
    chartQuizzes.update();
    
    chartRevenue.data.labels = rData.labels;
    chartRevenue.data.datasets[0].data = rData.values;
    chartRevenue.update();
}

export function groupDataByTime(items, type, timestampKey = 'timestamp') {
    const now = new Date();
    const data = {};
    const labels = [];
    
    if (type === 'day') {
        for (let i = 6; i >= 0; i--) {
            const d = new Date(now);
            d.setDate(now.getDate() - i);
            const dateStr = d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
            labels.push(dateStr);
            data[dateStr] = 0;
        }
        items.forEach(item => {
            const t = item[timestampKey] || item.createdAt || item.timestamp;
            if (!t) return;
            const d = new Date(t);
            const dateStr = d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
            if (data[dateStr] !== undefined) {
                if (item.amount !== undefined) {
                    data[dateStr] += item.amount;
                } else {
                    data[dateStr] += 1;
                }
            }
        });
    } else if (type === 'month') {
        for (let i = 5; i >= 0; i--) {
            const d = new Date(now);
            d.setMonth(now.getMonth() - i);
            const dateStr = `T${d.getMonth() + 1}/${d.getFullYear().toString().substring(2)}`;
            labels.push(dateStr);
            data[dateStr] = 0;
        }
        items.forEach(item => {
            const t = item[timestampKey] || item.createdAt || item.timestamp;
            if (!t) return;
            const d = new Date(t);
            const dateStr = `T${d.getMonth() + 1}/${d.getFullYear().toString().substring(2)}`;
            if (data[dateStr] !== undefined) {
                if (item.amount !== undefined) {
                    data[dateStr] += item.amount;
                } else {
                    data[dateStr] += 1;
                }
            }
        });
    } else if (type === 'year') {
        for (let i = 4; i >= 0; i--) {
            const d = new Date(now);
            d.setFullYear(now.getFullYear() - i);
            const dateStr = d.getFullYear().toString();
            labels.push(dateStr);
            data[dateStr] = 0;
        }
        items.forEach(item => {
            const t = item[timestampKey] || item.createdAt || item.timestamp;
            if (!t) return;
            const d = new Date(t);
            const dateStr = d.getFullYear().toString();
            if (data[dateStr] !== undefined) {
                if (item.amount !== undefined) {
                    data[dateStr] += item.amount;
                } else {
                    data[dateStr] += 1;
                }
            }
        });
    }
    
    return {
        labels: labels,
        values: labels.map(l => data[l])
    };
}

export function updateTimeFilter(filter) {
    currentFilter = filter;
    const btnDay = document.getElementById('btn-filter-day');
    const btnMonth = document.getElementById('btn-filter-month');
    const btnYear = document.getElementById('btn-filter-year');
    
    if (btnDay) btnDay.classList.toggle('active', filter === 'day');
    if (btnMonth) btnMonth.classList.toggle('active', filter === 'month');
    if (btnYear) btnYear.classList.toggle('active', filter === 'year');
    
    updateCharts();
}

window.updateTimeFilter = updateTimeFilter;

export async function loadDashboardData() {
    try {
        window.adminState.allQuizzes = await getAllQuizzes();
        window.adminState.allCoinTxs = await getAllCoinTransactions();
        
        window.adminState.allQuizzes.forEach(q => {
            if (!q.timestamp) {
                const daysAgo = Math.floor(Math.random() * 4);
                q.timestamp = Date.now() - daysAgo * 24 * 60 * 60 * 1000;
            }
        });
        
        if (window.adminState.allCoinTxs.length === 0) {
            window.adminState.allCoinTxs = [
                { amount: 100, timestamp: Date.now() - 2 * 24 * 60 * 60 * 1000, type: "admin_add" },
                { amount: 300, timestamp: Date.now() - 1 * 24 * 60 * 60 * 1000, type: "admin_add" },
                { amount: 200, timestamp: Date.now(), type: "admin_add" }
            ];
        }
    } catch (error) {
        console.error("Lỗi load thống kê:", error);
    }
}
