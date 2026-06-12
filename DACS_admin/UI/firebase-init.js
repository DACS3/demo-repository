// firebase-init.js
// File cấu hình Firebase chung cho toàn bộ dự án
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.11.0/firebase-app.js";
import { getAuth } from "https://www.gstatic.com/firebasejs/10.11.0/firebase-auth.js";
import { getFirestore } from "https://www.gstatic.com/firebasejs/10.11.0/firebase-firestore.js";
import { getStorage } from "https://www.gstatic.com/firebasejs/10.11.0/firebase-storage.js";

// ⚠️ Thay thế các giá trị dưới đây bằng firebaseConfig từ Firebase Console
const firebaseConfig = {
  apiKey: "AIzaSyBq23MfghUeOYIpCycvEGZbO7SYSfjEuIg",
  authDomain: "dacs-f3ee9.firebaseapp.com",
  projectId: "dacs-f3ee9",
  storageBucket: "dacs-f3ee9.firebasestorage.app",
  messagingSenderId: "1000773932220",
  appId: "1:1000773932220:web:00eecbf6793fc4b4e4ecb9",
  measurementId: "G-GK535PSL3X"
};
// Khởi tạo Firebase
const app = initializeApp(firebaseConfig);

// Export các service để sử dụng ở các trang khác
export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);
export const firebaseApp = app;
