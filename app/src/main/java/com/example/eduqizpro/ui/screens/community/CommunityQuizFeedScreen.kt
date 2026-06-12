package com.example.eduqizpro.ui.screens.community

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eduqizpro.data.ChatRepository
import com.example.eduqizpro.data.CommunityRepository
import com.example.eduqizpro.data.QuizRepository
import com.example.eduqizpro.data.model.Quiz
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

// ─── ViewModel ────────────────────────────────────────────────────────────────
class CommunityFeedViewModel : ViewModel() {
    val quizRepository = QuizRepository()
    val communityRepository = CommunityRepository()
    val chatRepository = ChatRepository()

    var quizzes by mutableStateOf<List<Quiz>>(emptyList())
    var isLoading by mutableStateOf(true)
    var myFriendIds by mutableStateOf<List<String>>(emptyList())
    var pendingCount by mutableIntStateOf(0)
    var searchQuery by mutableStateOf("")

    val filteredQuizzes: List<Quiz>
        get() = if (searchQuery.isBlank()) {
            quizzes
        } else {
            quizzes.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }

    private var isLoaded = false

    fun loadDataIfNeeded(currentUserId: String) {
        if (isLoaded) return
        forceReload(currentUserId)
    }

    fun forceReload(currentUserId: String, isBackgroundRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isBackgroundRefresh || quizzes.isEmpty()) {
                isLoading = true
            }

            val freshFriendIds = chatRepository.getMyFriendIds()
            myFriendIds = freshFriendIds
            pendingCount = chatRepository.getPendingFriendRequests().size

            val publicQuizzes = communityRepository.getCommunityQuizzes()
            val friendQuizzes = if (freshFriendIds.isNotEmpty() || currentUserId.isNotEmpty()) {
                communityRepository.getFriendsQuizzes(freshFriendIds + currentUserId)
            } else emptyList()

            quizzes = (publicQuizzes + friendQuizzes).distinctBy { it.id }
            isLoading = false
            isLoaded = true
        }
    }

    fun toggleLike(quizId: String, currentUserId: String, isLiked: Boolean) {
        val newLikes = if (isLiked) {
            quizzes.find { it.id == quizId }?.likes?.minus(currentUserId) ?: emptyList()
        } else {
            quizzes.find { it.id == quizId }?.likes?.plus(currentUserId) ?: emptyList()
        }
        quizzes = quizzes.map {
            if (it.id == quizId) it.copy(likes = newLikes) else it
        }
        viewModelScope.launch {
            // Sửa lỗi 1: Đổi thành communityRepository.toggleLike đúng theo hàm gốc của bạn
            communityRepository.toggleLike(quizId, currentUserId, isLiked)
        }
    }

    fun unfriendUserOptimistic(userId: String) {
        myFriendIds = myFriendIds.filter { it != userId }
        quizzes = quizzes.filterNot { it.creatorId == userId && it.visibility == "friends" }
        viewModelScope.launch {
            chatRepository.unfriend(userId)
        }
    }

    fun sendFriendRequestOptimistic(userId: String, userName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = chatRepository.sendFriendRequest(userId, userName)
            onResult(success)
        }
    }

    fun acceptFriendOptimistic(newFriendId: String) {
        if (!myFriendIds.contains(newFriendId)) {
            myFriendIds = myFriendIds + newFriendId
        }
        if (pendingCount > 0) pendingCount -= 1

        viewModelScope.launch {
            val newFriendQuizzes = communityRepository.getFriendsQuizzes(listOf(newFriendId))
            if (newFriendQuizzes.isNotEmpty()) {
                quizzes = (newFriendQuizzes + quizzes).distinctBy { it.id }
            }
        }
    }

    // Sửa lỗi 2 & 3: downloadQuiz truyền vào đối tượng Quiz và hứng về dữ liệu loại Pair
    fun downloadQuiz(quiz: Quiz, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val resultPair = communityRepository.downloadQuiz(quiz)
            onResult(resultPair.second) // Trả message chuỗi về cho UI hiển thị Toast
        }
    }
}

// ─── Screen UI ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityQuizFeedScreen(
    onBack: () -> Unit,
    onQuizClick: (Quiz) -> Unit,
    onCommentClick: (Quiz) -> Unit,
    onNavigateToChat: (String, String) -> Unit,
    viewModel: CommunityFeedViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var showFriendSheet by remember { mutableStateOf(false) }
    var showMyQuizzesDialog by remember { mutableStateOf(false) }
    var myQuizzes by remember { mutableStateOf<List<Quiz>>(emptyList()) }
    var isLoadingMyQuizzes by remember { mutableStateOf(false) }
    var selectedQuizForUpload by remember { mutableStateOf<Quiz?>(null) }
    var showVisibilityDialog by remember { mutableStateOf(false) }
    var selectedVisibility by remember { mutableStateOf("public") }
    var selectedUserForAction by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadDataIfNeeded(currentUserId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kho đề cộng đồng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (viewModel.pendingCount > 0) {
                                Badge(containerColor = Color(0xFFFF1744)) {
                                    Text("${viewModel.pendingCount}", fontSize = 9.sp)
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(onClick = { showFriendSheet = true }) {
                            Icon(Icons.Default.People, contentDescription = "Bạn bè", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        isLoadingMyQuizzes = true
                        showMyQuizzesDialog = true
                        myQuizzes = viewModel.quizRepository.getMyQuizzes()
                        isLoadingMyQuizzes = false
                    }
                },
                containerColor = Color(0xFF6200EE),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload Quiz")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Tìm kiếm bộ đề (VD: Toán)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (viewModel.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (viewModel.filteredQuizzes.isEmpty()) {
                    Text(
                        text = if (viewModel.searchQuery.isEmpty()) "Chưa có bộ đề nào được chia sẻ." else "Không tìm thấy kết quả phù hợp.",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(viewModel.filteredQuizzes, key = { it.id }) { quiz ->
                            val isLiked = quiz.likes.contains(currentUserId)
                            val isOwner = quiz.creatorId == currentUserId || quiz.creatorId == "deleted_$currentUserId"
                            val isFriendsOnly = quiz.visibility == "friends"

                            CommunityQuizItem(
                                quiz = quiz,
                                currentUserId = currentUserId,
                                isLiked = isLiked,
                                isOwner = isOwner,
                                isFriendsOnly = isFriendsOnly,
                                onClick = { onQuizClick(quiz) },
                                onCommentClick = { onCommentClick(quiz) },
                                onCreatorClick = {
                                    if (!isOwner) {
                                        selectedUserForAction = quiz.creatorId to quiz.creatorName
                                    }
                                },
                                onLikeClick = {
                                    viewModel.toggleLike(quiz.id, currentUserId, isLiked)
                                },
                                onRemoveClick = {
                                    scope.launch {
                                        val success = viewModel.communityRepository.updateQuizVisibility(quiz.id, "private")
                                        if (success) {
                                            Toast.makeText(context, "Đã gỡ bộ đề", Toast.LENGTH_SHORT).show()
                                            viewModel.forceReload(currentUserId, isBackgroundRefresh = true)
                                        }
                                    }
                                },
                                onDownloadClick = {
                                    viewModel.downloadQuiz(quiz) { message ->
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Các phần Sheet và Dialog giữ nguyên logic của bạn
    if (showFriendSheet) {
        FriendRequestSheet(
            onDismiss = { showFriendSheet = false },
            onFriendAccepted = { newFriendId ->
                viewModel.acceptFriendOptimistic(newFriendId)
            },
            onChatClick = { id, name ->
                showFriendSheet = false
                onNavigateToChat(id, name)
            }
        )
    }

    if (showMyQuizzesDialog) {
        AlertDialog(
            onDismissRequest = { showMyQuizzesDialog = false },
            title = { Text("Chọn bộ đề để chia sẻ", fontWeight = FontWeight.Bold) },
            text = {
                if (isLoadingMyQuizzes) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (myQuizzes.isEmpty()) {
                    Text("Bạn chưa tạo bộ đề nào.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(myQuizzes) { quiz ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        selectedQuizForUpload = quiz
                                        selectedVisibility = quiz.visibility
                                        showMyQuizzesDialog = false
                                        showVisibilityDialog = true
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = quiz.title, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Hiện tại: ${
                                            when (quiz.visibility) {
                                                "public" -> "Công khai"
                                                "friends" -> "Bạn bè"
                                                else -> "Riêng tư"
                                            }
                                        }",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMyQuizzesDialog = false }) { Text("ĐÓNG") } }
        )
    }

    if (showVisibilityDialog && selectedQuizForUpload != null) {
        AlertDialog(
            onDismissRequest = { showVisibilityDialog = false },
            title = { Text("Chế độ hiển thị", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Chọn đối tượng có thể thấy bộ đề này:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedVisibility == "public", onClick = { selectedVisibility = "public" })
                        Text("Công khai (Ai cũng có thể thấy)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedVisibility == "friends", onClick = { selectedVisibility = "friends" })
                        Text("Bạn bè (Chỉ bạn bè mới thấy)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedVisibility == "private", onClick = { selectedVisibility = "private" })
                        Text("Riêng tư (Gỡ khỏi kho)")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val success = viewModel.communityRepository.updateQuizVisibility(selectedQuizForUpload!!.id, selectedVisibility)
                        if (success) {
                            Toast.makeText(context, "Đã cập nhật", Toast.LENGTH_SHORT).show()
                            viewModel.forceReload(currentUserId, isBackgroundRefresh = true)
                        }
                        showVisibilityDialog = false
                        selectedQuizForUpload = null
                    }
                }) { Text("LƯU") }
            },
            dismissButton = {
                TextButton(onClick = { showVisibilityDialog = false; selectedQuizForUpload = null }) { Text("HỦY") }
            }
        )
    }

    selectedUserForAction?.let { (userId, userName) ->
        val isFriend = viewModel.myFriendIds.contains(userId)
        AlertDialog(
            onDismissRequest = { selectedUserForAction = null },
            title = { Text(userName, fontWeight = FontWeight.Bold) },
            text = { Text("Bạn muốn thực hiện hành động gì?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedUserForAction = null
                    if (isFriend) {
                        viewModel.unfriendUserOptimistic(userId)
                        Toast.makeText(context, "Đã hủy kết bạn", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.sendFriendRequestOptimistic(userId, userName) { success ->
                            if (success) Toast.makeText(context, "Đã gửi yêu cầu", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text(
                        text = if (isFriend) "HỦY KẾT BẠN" else "KẾT BẠN",
                        color = if (isFriend) Color.Red else MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedUserForAction = null
                    onNavigateToChat(userId, userName)
                }) { Text("NHẮN TIN") }
            }
        )
    }
}

// ─── CommunityQuizItem ────────────────────────────────────────────────────────
@Composable
fun CommunityQuizItem(
    quiz: Quiz,
    currentUserId: String,
    isLiked: Boolean,
    isOwner: Boolean,
    isFriendsOnly: Boolean,
    onClick: () -> Unit,
    onCommentClick: () -> Unit,
    onCreatorClick: () -> Unit,
    onLikeClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onDownloadClick: () -> Unit // Thêm callback tải đề
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Gỡ khỏi cộng đồng") },
            text = { Text("Bạn có muốn gỡ bộ đề '${quiz.title}' khỏi kho cộng đồng không?") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    onRemoveClick()
                }) { Text("GỠ XUỐNG", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showRemoveDialog = false }) { Text("HỦY") } }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = quiz.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (isFriendsOnly) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFEDE7F6)) {
                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color(0xFF6200EE))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Bạn bè", fontSize = 10.sp, color = Color(0xFF6200EE), fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (isOwner) {
                    IconButton(onClick = { showRemoveDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.VisibilityOff, contentDescription = "Gỡ khỏi cộng đồng", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onCreatorClick() }) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = quiz.creatorName, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "${quiz.questions.size} câu", fontSize = 12.sp, color = Color(0xFF6200EE))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Thanh điều hướng tính năng: Like, Comment và Thêm nút Download
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLikeClick) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = if (isLiked) Color.Red else Color.Gray)
                        Text(" ${quiz.likes.size}", fontSize = 13.sp)
                    }
                }

                IconButton(onClick = onCommentClick) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bình luận", fontSize = 13.sp)
                    }
                }

                // Không cho phép tải nếu chính mình là người tạo đề (tránh lãng phí xu)
                if (!isOwner) {
                    Button(
                        onClick = onDownloadClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tải đề (-50đ)", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}