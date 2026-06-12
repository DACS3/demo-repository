import { 
    deleteComment, 
    deleteReply 
} from "../firestore-queries.js";

// Khởi tạo đối tượng lưu trữ dùng chung
window.adminState = window.adminState || {
    allUsers: [],
    allQuizzes: [],
    allCoinTxs: [],
    currentComments: []
};

let currentPageComments = 1;
const itemsPerPage = 7;

export function formatTimeAgo(timestamp) {
    if (!timestamp) return 'N/A';
    const date = timestamp.toDate ? timestamp.toDate() : new Date(timestamp);
    const seconds = Math.floor((new Date() - date) / 1000);
    if (seconds < 60) return 'vừa xong';
    if (seconds < 3600) return Math.floor(seconds / 60) + ' phút trước';
    if (seconds < 86400) return Math.floor(seconds / 3600) + ' giờ trước';
    return Math.floor(seconds / 86400) + ' ngày trước';
}

export function renderComments(commentList) {
    const tbody = document.querySelector('#comments-table tbody');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    if (commentList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">Không có bình luận nào.</td></tr>';
        return;
    }
    commentList.forEach(comment => {
        const commentText = comment.text || '';
        const timeAgo = formatTimeAgo(comment.timestamp);

        const mainRow = document.createElement('tr');
        mainRow.style.borderLeft = '3px solid #6200EE';
        mainRow.innerHTML = `
            <td class="fw-semibold">${comment.userName || 'Ẩn danh'}</td>
            <td>${commentText.substring(0, 80)}${commentText.length > 80 ? '...' : ''}</td>
            <td><span class="badge bg-primary">Bình luận</span></td>
            <td class="text-muted small">${timeAgo}</td>
            <td>
                <button class="btn btn-danger btn-sm" onclick="deleteCommentAction('${comment.quizId}', '${comment.id}')">
                    <i class="fa-solid fa-trash"></i> Xóa
                </button>
            </td>
        `;
        tbody.appendChild(mainRow);

        const replies = comment.replies || [];
        replies.forEach((reply) => {
            const replyText = reply.text || '';
            const replyTime = formatTimeAgo(reply.timestamp);
            const replyRow = document.createElement('tr');
            replyRow.style.backgroundColor = '#f8f4ff';
            replyRow.innerHTML = `
                <td class="ps-4 text-muted small"><i class="fa-solid fa-reply me-1 text-secondary"></i>${reply.userName || 'Ẩn danh'}</td>
                <td class="small">${replyText.substring(0, 80)}${replyText.length > 80 ? '...' : ''}</td>
                <td><span class="badge bg-secondary">Reply</span></td>
                <td class="text-muted small">${replyTime}</td>
                <td>
                    <button class="btn btn-outline-danger btn-sm"
                        data-quiz-id="${comment.quizId}"
                        data-comment-id="${comment.id}"
                        data-reply='${JSON.stringify(reply).replace(/'/g, "&apos;")}'
                        onclick="deleteReplyAction(this)">
                        <i class="fa-solid fa-trash"></i> Xóa
                    </button>
                </td>
            `;
            tbody.appendChild(replyRow);
        });
    });
}

export function filterComments() {
    const searchInput = document.getElementById('comment-search');
    const keyword = searchInput ? searchInput.value.toLowerCase() : '';
    
    let filtered = window.adminState.currentComments;
    if (keyword) {
        filtered = window.adminState.currentComments.filter(c =>
            (c.userName || '').toLowerCase().includes(keyword) ||
            (c.text || '').toLowerCase().includes(keyword) ||
            (c.replies || []).some(r =>
                (r.userName || '').toLowerCase().includes(keyword) ||
                (r.text || '').toLowerCase().includes(keyword)
            )
        );
    }
    
    const totalItems = filtered.length;
    const totalPages = Math.ceil(totalItems / itemsPerPage);
    if (currentPageComments > totalPages) {
        currentPageComments = Math.max(1, totalPages);
    }
    
    const startIndex = (currentPageComments - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, totalItems);
    const paginatedComments = filtered.slice(startIndex, endIndex);
    
    renderComments(paginatedComments);
    renderPaginationComments(totalItems);
}

export function renderPaginationComments(totalItems) {
    const container = document.getElementById('pagination-comments-container');
    const info = document.getElementById('pagination-comments-info');
    const controls = document.getElementById('pagination-comments-controls');
    
    if (!container || !info || !controls) return;
    
    if (totalItems <= itemsPerPage) {
        container.style.display = 'none';
        return;
    }
    container.style.display = 'flex';
    
    const totalPages = Math.ceil(totalItems / itemsPerPage);
    const startIndex = (currentPageComments - 1) * itemsPerPage;
    const endIndex = Math.min(startIndex + itemsPerPage, totalItems);
    
    info.textContent = `Hiển thị từ ${startIndex + 1} đến ${endIndex} trong số ${totalItems} bình luận`;
    
    controls.innerHTML = '';
    
    const prevLi = document.createElement('li');
    prevLi.className = `page-item ${currentPageComments === 1 ? 'disabled' : ''}`;
    prevLi.innerHTML = `<a class="page-link" href="#" onclick="changePageComments(${currentPageComments - 1}); return false;"><i class="fa-solid fa-chevron-left"></i></a>`;
    controls.appendChild(prevLi);
    
    for (let i = 1; i <= totalPages; i++) {
        const pageLi = document.createElement('li');
        pageLi.className = `page-item ${currentPageComments === i ? 'active' : ''}`;
        pageLi.innerHTML = `<a class="page-link" href="#" onclick="changePageComments(${i}); return false;">${i}</a>`;
        controls.appendChild(pageLi);
    }
    
    const nextLi = document.createElement('li');
    nextLi.className = `page-item ${currentPageComments === totalPages ? 'disabled' : ''}`;
    nextLi.innerHTML = `<a class="page-link" href="#" onclick="changePageComments(${currentPageComments + 1}); return false;"><i class="fa-solid fa-chevron-right"></i></a>`;
    controls.appendChild(nextLi);
}

export function changePageComments(page) {
    currentPageComments = page;
    filterComments();
}

export async function deleteCommentAction(quizId, commentId) {
    if (confirm("Xóa bình luận này và tất cả reply bên dưới?")) {
        const success = await deleteComment(quizId, commentId);
        if (!success) alert("Lỗi khi xóa bình luận!");
    }
}

export async function deleteReplyAction(btn) {
    if (confirm("Bạn có chắc muốn xóa reply này?")) {
        const quizId = btn.getAttribute('data-quiz-id');
        const commentId = btn.getAttribute('data-comment-id');
        const reply = JSON.parse(btn.getAttribute('data-reply').replace(/&apos;/g, "'"));
        const success = await deleteReply(quizId, commentId, reply);
        if (!success) alert("Lỗi khi xóa reply!");
    }
}

// Đăng ký các hàm ra phạm vi toàn cục window
window.filterComments = filterComments;
window.changePageComments = changePageComments;
window.deleteCommentAction = deleteCommentAction;
window.deleteReplyAction = deleteReplyAction;
