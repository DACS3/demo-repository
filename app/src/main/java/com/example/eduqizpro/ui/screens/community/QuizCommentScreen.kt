package com.example.eduqizpro.ui.screens.community

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduqizpro.data.AuthRepository
import com.example.eduqizpro.data.CommunityRepository
import com.example.eduqizpro.data.QuizRepository
import com.example.eduqizpro.data.model.Comment
import com.example.eduqizpro.data.model.Quiz
import com.example.eduqizpro.data.model.Reply
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "Vừa xong"
        minutes < 60 -> "$minutes phút trước"
        hours < 24 -> "$hours giờ trước"
        days == 1L -> {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = timestamp
            val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val m = cal.get(java.util.Calendar.MINUTE)
            "Hôm qua lúc %02d:%02d".format(h, m)
        }
        days < 7 -> "$days ngày trước"
        else -> {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizCommentScreen(
    quiz: Quiz,
    onBack: () -> Unit,
    onNavigateToQuiz: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val quizRepository = remember { QuizRepository() }
    val communityRepository = remember { CommunityRepository() }
    val authRepository = remember { AuthRepository() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var hasCompleted by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<Comment?>(null) }
    var commentToDelete by remember { mutableStateOf<Comment?>(null) }

    // Realtime Flow — tự cập nhật khi admin xóa comment hoặc đổi trạng thái chặn
    val commentsState by remember { communityRepository.getCommentsFlow(quiz.id) }
        .collectAsState(initial = null)
    val isLoadingComments = commentsState == null
    val comments = commentsState ?: emptyList()

    // Dùng flow riêng đọc trực tiếp snapshot — tránh lỗi Kotlin 'is' prefix với toObject()
    val blockStatus by remember(currentUserId) {
        if (currentUserId != null) authRepository.getUserBlockStatusFlow(currentUserId)
        else kotlinx.coroutines.flow.flowOf(Pair(false, 0L))
    }.collectAsState(initial = Pair(false, 0L))

    val isCommentBlockedByUser = blockStatus.first && (blockStatus.second == 0L || System.currentTimeMillis() < blockStatus.second)
    val commentBlockedUntil = blockStatus.second

    LaunchedEffect(Unit) {
        if (currentUserId != null) {
            hasCompleted = quizRepository.hasCompletedQuiz(currentUserId, quiz.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bình luận", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {

            if (isLoadingComments) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (comments.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Chưa có bình luận nào.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(comments) { comment ->
                        CommentItem(
                            comment = comment,
                            isOwner = currentUserId == comment.userId,
                            isCommentBlocked = isCommentBlockedByUser,
                            currentUserId = currentUserId,
                            communityRepository = communityRepository,
                            onReply = { replyingToComment = comment },
                            onDelete = { commentToDelete = comment }
                        )
                    }
                }
            }

            commentToDelete?.let { toDelete ->
                AlertDialog(
                    onDismissRequest = { commentToDelete = null },
                    title = { Text("Xóa bình luận") },
                    text = { Text("Bạn có chắc chắn muốn xóa bình luận này?") },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                val success = communityRepository.deleteComment(quiz.id, toDelete.id)
                                if (success) {
                                    // Không cần cập nhật thủ công — snapshot listener tự xóa
                                    Toast.makeText(context, "Đã xóa", Toast.LENGTH_SHORT).show()
                                }
                                commentToDelete = null
                            }
                        }) { Text("XÓA", color = Color.Red) }
                    },
                    dismissButton = { TextButton(onClick = { commentToDelete = null }) { Text("HỦY") } }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Phân luồng điều kiện hiển thị ô nhập liệu hoặc cảnh báo chặn
            if (isCommentBlockedByUser) {
                // Nhánh 1: Tài khoản người dùng bị khóa quyền bình luận
                val blockedUntilText = if (commentBlockedUntil > 0L) {
                    val sdf = java.text.SimpleDateFormat("HH:mm dd/MM/yyyy", java.util.Locale.getDefault())
                    " đến " + sdf.format(java.util.Date(commentBlockedUntil))
                } else ""
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tài khoản của bạn đã bị khóa tính năng bình luận do vi phạm chính sách$blockedUntilText.",
                        fontSize = 13.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (!hasCompleted && currentUserId != quiz.creatorId) {
                // Nhánh 2: Chưa làm đề trắc nghiệm (Giữ nguyên tính năng cũ của ứng dụng)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hãy làm bộ đề này để có thể bình luận.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            } else {
                // Nhánh 3: Đầy đủ điều kiện, cho phép soạn thảo bình luận bình thường
                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp)) {
                    if (replyingToComment != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                            Text(
                                "Đang trả lời ${replyingToComment!!.userName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { replyingToComment = null }, modifier = Modifier.size(16.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = { Text(if (replyingToComment != null) "Viết phản hồi..." else "Viết bình luận...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            enabled = commentText.isNotBlank(),
                            onClick = {
                                if (currentUserId != null) {
                                    scope.launch {

                                        val blocked = authRepository.isUserCommentBlocked(currentUserId)
                                        if (blocked) {
                                            Toast.makeText(context, "Tài khoản của bạn đã bị khóa tính năng bình luận.", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }

                                        val user = authRepository.getUserData(currentUserId)
                                        val actualName = user?.fullName ?: "Người dùng"

                                        val text = commentText
                                        val replyTo = replyingToComment
                                        commentText = ""
                                        replyingToComment = null

                                        if (containsVulgarWords(text)) {
                                            authRepository.blockUserFor24h(currentUserId)
                                            Toast.makeText(context, "Bình luận chứa từ ngữ tục tĩu! Bạn bị khóa bình luận 24h.", Toast.LENGTH_LONG).show()
                                            return@launch
                                        }

                                        if (replyTo != null) {
                                            val newReply = Reply(
                                                userId = currentUserId,
                                                userName = actualName,
                                                text = text
                                            )
                                            val replySuccess = communityRepository.addReplyToComment(quiz.id, replyTo.id, newReply)
                                            if (!replySuccess) {
                                                Toast.makeText(context, "Gửi phản hồi thất bại, vui lòng thử lại.", Toast.LENGTH_SHORT).show()
                                            }
                                            // Không cần getComments() — snapshot listener tự cập nhật
                                        } else {
                                            val newComment = Comment(
                                                quizId = quiz.id,
                                                userId = currentUserId,
                                                userName = actualName,
                                                text = text
                                            )
                                            val commentSuccess = communityRepository.addComment(newComment)
                                            if (!commentSuccess) {
                                                Toast.makeText(context, "Gửi bình luận thất bại, vui lòng thử lại.", Toast.LENGTH_SHORT).show()
                                                commentText = text
                                            }
                                            // Không cần getComments() — snapshot listener tự cập nhật
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Gửi", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    isOwner: Boolean,
    isCommentBlocked: Boolean,
    currentUserId: String?,
    communityRepository: CommunityRepository,
    onReply: () -> Unit,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(comment.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(comment.text, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = formatRelativeTime(comment.timestamp),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    if (!isCommentBlocked) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Trả lời",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onReply() }
                        )
                    }
                }
            }
            if (isOwner) {
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Hiển thị replies kèm nút xóa cho chủ sở hữu
        comment.replies.forEach { reply ->
            Row(
                modifier = Modifier.padding(start = 32.dp, top = 6.dp).fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(reply.userName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(reply.text, fontSize = 13.sp)
                    Text(
                        text = formatRelativeTime(reply.timestamp),
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                // Nút xóa reply — chỉ hiện với chủ sở hữu reply
                if (reply.userId == currentUserId) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                val success = communityRepository.deleteReply(comment.quizId, comment.id, reply)
                                if (!success) Toast.makeText(context, "Xóa reply thất bại", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

fun containsVulgarWords(text: String): Boolean {
    val vulgarList = listOf(
        "đm", "dm", "đéo", "deo", "vkl", "vcl", "cl", "vl", "cứt", "cut", "đụ", "du", "lồn", "lon", 
        "buồi", "buoi", "cặc", "cac", "dcm", "đmm", "đcm", "mẹ mày", "me may", "óc chó", "oc cho", 
        "đầu buồi", "dau buoi", "đầu cặc", "dau cac", "phò", "pho", "đĩ", "di", "hãm lồn", "ham lon"
    )
    val lowerText = text.toLowerCase(java.util.Locale.ROOT)
    val words = lowerText.split(Regex("\\s+|\\p{Punct}+"))
    for (word in words) {
        if (vulgarList.contains(word)) return true
    }
    for (phrase in vulgarList) {
        if (phrase.contains(" ") && lowerText.contains(phrase)) return true
    }
    return false
}