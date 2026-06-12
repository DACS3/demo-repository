package com.example.eduqizpro.ui.screens.community

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduqizpro.data.ChatRepository
import com.example.eduqizpro.data.model.FriendRequest
import kotlinx.coroutines.launch

private val Purple = Color(0xFF6200EE)
private val PurpleLight = Color(0xFFF3E8FF)
private val GreenAccept = Color(0xFF2E7D32)
private val RedDecline = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestSheet(
    onDismiss: () -> Unit,
    onFriendAccepted: (String) -> Unit = {}, // Báo ID người bạn mới ra ngoài
    onChatClick: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chatRepository = remember { ChatRepository() }

    var pendingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var friends by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        pendingRequests = chatRepository.getPendingFriendRequests()
        friends = chatRepository.getMyFriends()
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDDD8F0))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.People,
                    contentDescription = null,
                    tint = Purple,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Bạn bè",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1A1A2E)
                )
                Spacer(modifier = Modifier.weight(1f))

                if (pendingRequests.isNotEmpty()) {
                    Surface(
                        color = Purple,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${pendingRequests.size} lời mời",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.White,
                contentColor = Purple,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Purple
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Lời mời", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Bạn bè (${friends.size})", fontSize = 13.sp) }
                )
            }

            // Content
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Purple)
                }
            } else {
                when (selectedTab) {
                    0 -> PendingRequestsTab(
                        requests = pendingRequests,
                        onAccept = { request ->
                            // 1. Giao diện biến mất ngay lập tức (Optimistic UI)
                            pendingRequests = pendingRequests.filter { it.fromId != request.fromId }

                            val newFriend = mapOf("friendId" to request.fromId, "friendName" to request.fromName)
                            friends = listOf(newFriend) + friends

                            // 2. Báo hiệu ra màn hình kho đề lấy bộ đề của ông này
                            onFriendAccepted(request.fromId)
                            Toast.makeText(context, "Đã kết bạn với ${request.fromName}", Toast.LENGTH_SHORT).show()

                            // 3. Tác vụ Firebase âm thầm chạy ngầm
                            scope.launch {
                                chatRepository.acceptFriendRequest(request)
                            }
                        },
                        onDecline = { request ->
                            // Biến mất ngay lập tức
                            pendingRequests = pendingRequests.filter { it.fromId != request.fromId }
                            Toast.makeText(context, "Đã từ chối", Toast.LENGTH_SHORT).show()

                            scope.launch {
                                chatRepository.declineFriendRequest(request)
                            }
                        },
                        onChatClick = onChatClick
                    )
                    1 -> FriendListTab(
                        friends = friends,
                        onChatClick = onChatClick
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingRequestsTab(
    requests: List<FriendRequest>,
    onAccept: (FriendRequest) -> Unit,
    onDecline: (FriendRequest) -> Unit,
    onChatClick: (String, String) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyStateView(Icons.Default.PersonSearch, "Không có lời mời nào")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(requests, key = { it.fromId }) { request ->
                FriendRequestCard(request, { onAccept(request) }, { onDecline(request) }, { onChatClick(request.fromId, request.fromName) })
            }
        }
    }
}

@Composable
private fun FriendRequestCard(request: FriendRequest, onAccept: () -> Unit, onDecline: () -> Unit, onChatClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { onChatClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PurpleLight)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Purple
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = request.fromName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.fromName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color(0xFF1A1A2E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Muốn kết bạn",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            IconButton(
                onClick = onDecline,
                modifier = Modifier.size(36.dp).background(Color(0xFFFFEBEE), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = RedDecline, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onAccept,
                modifier = Modifier.size(36.dp).background(Color(0xFFE8F5E9), CircleShape)
            ) {
                Icon(Icons.Default.Check, null, tint = GreenAccept, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun FriendListTab(friends: List<Map<String, Any>>, onChatClick: (String, String) -> Unit) {
    if (friends.isEmpty()) {
        EmptyStateView(Icons.Default.GroupAdd, "Chưa có bạn bè nào")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(friends, key = { it["friendId"] as? String ?: "" }) { friend ->
                FriendCard(friend, onChatClick = {
                    val id = friend["friendId"] as? String ?: return@FriendCard
                    val name = friend["friendName"] as? String ?: "Người dùng"
                    onChatClick(id, name)
                })
            }
        }
    }
}

@Composable
private fun FriendCard(friend: Map<String, Any>, onChatClick: () -> Unit) {
    val name = friend["friendName"] as? String ?: "Người dùng"
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onChatClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF8FF))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Purple) {
                Box(contentAlignment = Alignment.Center) {
                    Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Bạn bè", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun EmptyStateView(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
            Text(text, color = Color.Gray, fontSize = 14.sp)
        }
    }
}