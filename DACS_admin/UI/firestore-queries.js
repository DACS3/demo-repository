// firestore-queries.js
// Các hàm query dữ liệu từ Firestore
import { db } from "./firebase-init.js";
import { 
  collection,
  collectionGroup,
  getDocs, 
  query, 
  where, 
  orderBy, 
  limit,
  updateDoc,
  doc,
  deleteDoc,
  addDoc,
  getDoc,
  arrayRemove,
  onSnapshot
} from "https://www.gstatic.com/firebasejs/10.11.0/firebase-firestore.js";

// ===== THỐNG KÊ HỆ THỐNG =====
export async function getTotalUsers() {
  try {
    const usersCol = collection(db, "users");
    const snapshot = await getDocs(usersCol);
    return snapshot.size;
  } catch (error) {
    console.error("Lỗi lấy tổng người dùng:", error);
    return 0;
  }
}

export async function getTotalQuizzes() {
  try {
    const quizzesCol = collection(db, "quizzes");
    const snapshot = await getDocs(quizzesCol);
    return snapshot.size;
  } catch (error) {
    console.error("Lỗi lấy tổng quiz:", error);
    return 0;
  }
}

export async function getTotalComments() {
  try {
    // Dùng collectionGroup — 1 query duy nhất, rất nhanh
    const snapshot = await getDocs(collectionGroup(db, "comments"));
    return snapshot.size;
  } catch (error) {
    console.error("Lỗi lấy tổng bình luận:", error);
    return 0;
  }
}

export async function getTotalCoins() {
  try {
    const usersCol = collection(db, "users");
    const snapshot = await getDocs(usersCol);
    let totalCoins = 0;
    snapshot.forEach(docSnap => {
      const coins = docSnap.data().coins || 0;
      totalCoins += coins;
    });
    return totalCoins;
  } catch (error) {
    console.error("Lỗi lấy tổng xu:", error);
    return 0;
  }
}

// ===== QUẢN LÝ NGƯỜI DÙNG =====
export async function getAllUsers() {
  try {
    const usersCol = collection(db, "users");
    const snapshot = await getDocs(usersCol);
    const users = [];
    snapshot.forEach(docSnap => {
      const userData = docSnap.data();
      console.log("User data:", userData); // Debug: xem cấu trúc dữ liệu
      users.push({
        uid: docSnap.id,
        ...userData
      });
    });
    console.log("Tổng người dùng lấy được:", users.length);
    console.log("Chi tiết users:", users); // Debug: xem tất cả dữ liệu
    return users;
  } catch (error) {
    console.error("Lỗi lấy danh sách người dùng:", error);
    return [];
  }
}

export function listenToAllUsers(callback) {
  try {
    const usersCol = collection(db, "users");
    return onSnapshot(usersCol, (snapshot) => {
      const users = [];
      snapshot.forEach(docSnap => {
        const userData = docSnap.data();
        users.push({
          uid: docSnap.id,
          ...userData
        });
      });
      callback(users);
    }, (error) => {
      console.error("Lỗi lắng nghe danh sách người dùng:", error);
    });
  } catch (error) {
    console.error("Lỗi đăng ký lắng nghe người dùng:", error);
    return null;
  }
}

export async function blockUser(uid) {
  try {
    const userRef = doc(db, "users", uid);
    await updateDoc(userRef, { isCommentBlocked: true });
    return true;
  } catch (error) {
    console.error("Lỗi khóa bình luận:", error);
    return false;
  }
}

export async function unblockUser(uid) {
  try {
    const userRef = doc(db, "users", uid);
    await updateDoc(userRef, { 
      isCommentBlocked: false,
      commentBlockedUntil: 0 
    });
    return true;
  } catch (error) {
    console.error("Lỗi mở bình luận:", error);
    return false;
  }
}

// ===== QUẢN LÝ BÌNH LUẬN =====
// Dùng collectionGroup — 1 query duy nhất thay vì N+1 query
export async function getAllComments() {
  try {
    // Không dùng orderBy ở đây vì cần tạo Firestore index
    // Thay vào đó sort ở phía client
    const snapshot = await getDocs(collectionGroup(db, "comments"));
    const comments = [];
    snapshot.forEach(docSnap => {
      comments.push({
        id: docSnap.id,
        quizId: docSnap.data().quizId, // lưu trong document, dùng để xóa
        ...docSnap.data()
      });
    });
    // Sort mới nhất lên đầu
    comments.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
    console.log("Tổng bình luận lấy được:", comments.length);
    return comments;
  } catch (error) {
    console.error("Lỗi lấy bình luận:", error);
    return [];
  }
}

export function listenToAllComments(callback) {
  try {
    const commentsGroup = collectionGroup(db, "comments");
    return onSnapshot(commentsGroup, (snapshot) => {
      const comments = [];
      snapshot.forEach(docSnap => {
        comments.push({
          id: docSnap.id,
          quizId: docSnap.data().quizId,
          ...docSnap.data()
        });
      });
      comments.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));
      callback(comments);
    }, (error) => {
      console.error("Lỗi lắng nghe bình luận:", error);
    });
  } catch (error) {
    console.error("Lỗi đăng ký lắng nghe bình luận:", error);
    return null;
  }
}

// Cần truyền cả quizId vì comment nằm trong subcollection của quiz
export async function deleteComment(quizId, commentId) {
  try {
    await deleteDoc(doc(db, "quizzes", quizId, "comments", commentId));
    return true;
  } catch (error) {
    console.error("Lỗi xóa bình luận:", error);
    return false;
  }
}

// Xóa một reply cụ thể khỏi mảng replies của comment
export async function deleteReply(quizId, commentId, reply) {
  try {
    const commentRef = doc(db, "quizzes", quizId, "comments", commentId);
    await updateDoc(commentRef, { replies: arrayRemove(reply) });
    return true;
  } catch (error) {
    console.error("Lỗi xóa reply:", error);
    return false;
  }
}


// ===== QUẢN LÝ XU =====
export async function addCoinsToUser(uid, amount) {
  try {
    const userRef = doc(db, "users", uid);
    const userSnap = await getDoc(userRef);
    if (!userSnap.exists()) throw new Error("Người dùng không tồn tại!");
    const currentCoins = userSnap.data().coins || 0;
    const newCoins = currentCoins + amount;
    await updateDoc(userRef, { coins: newCoins });
    
    // Thêm log giao dịch xu
    try {
      const txRef = collection(db, "coin_transactions");
      await addDoc(txRef, {
        userId: uid,
        amount: amount,
        timestamp: Date.now(),
        type: "admin_add"
      });
    } catch (err) {
      console.error("Lỗi ghi log giao dịch xu:", err);
    }
    
    return newCoins; // trả về số xu mới để cập nhật UI ngay
  } catch (error) {
    console.error("Lỗi cộng xu:", error);
    return null;
  }
}

export async function subtractCoinsFromUser(uid, amount) {
  try {
    const userRef = doc(db, "users", uid);
    const userSnap = await getDoc(userRef);
    if (!userSnap.exists()) throw new Error("Người dùng không tồn tại!");
    const currentCoins = userSnap.data().coins || 0;
    const newCoins = Math.max(0, currentCoins - amount);
    await updateDoc(userRef, { coins: newCoins });
    return newCoins; // trả về số xu mới để cập nhật UI ngay
  } catch (error) {
    console.error("Lỗi trừ xu:", error);
    return null;
  }
}

export async function getUserCoins(uid) {
  try {
    const userRef = doc(db, "users", uid);
    const userSnap = await getDoc(userRef);
    if (!userSnap.exists()) {
      return 0;
    }
    return userSnap.data().coins || 0;
  } catch (error) {
    console.error("Lỗi lấy xu người dùng:", error);
    return 0;
  }
}

export async function getAllQuizzes() {
  try {
    const quizzesCol = collection(db, "quizzes");
    const snapshot = await getDocs(quizzesCol);
    const quizzes = [];
    snapshot.forEach(docSnap => {
      quizzes.push({
        id: docSnap.id,
        ...docSnap.data()
      });
    });
    return quizzes;
  } catch (error) {
    console.error("Lỗi lấy danh sách quiz:", error);
    return [];
  }
}

export async function getAllCoinTransactions() {
  try {
    const txsCol = collection(db, "coin_transactions");
    const snapshot = await getDocs(txsCol);
    const txs = [];
    snapshot.forEach(docSnap => {
      txs.push({
        id: docSnap.id,
        ...docSnap.data()
      });
    });
    return txs;
  } catch (error) {
    console.error("Lỗi lấy danh sách giao dịch xu:", error);
    return [];
  }
}
