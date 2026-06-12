package com.example.eduqizpro.ui.screens.summary

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.eduqizpro.data.model.SummaryBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Chuyển Summary cũ (summaryText + imageUrls) thành danh sách blocks */
fun buildBlocksFromLegacy(summaryText: String, imageUrls: List<String>): MutableList<SummaryBlock> {
    val list = mutableListOf<SummaryBlock>()
    if (summaryText.isNotBlank()) list.add(SummaryBlock("text", summaryText))
    imageUrls.forEach { list.add(SummaryBlock("image", it)) }
    if (list.isEmpty()) list.add(SummaryBlock("text", ""))
    return list
}

/**
 * Editor block-based dùng chung cho cả SummaryScreen và SummaryDetailScreen.
 * Hiển thị tiêu đề + danh sách block (text/image) xen kẽ với nút chèn.
 */
@Composable
fun SummaryBlockEditor(
    title: String,
    onTitleChange: (String) -> Unit,
    blocks: SnapshotStateList<SummaryBlock>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var insertAtIndex by remember { mutableStateOf(0) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val base64 = withContext(Dispatchers.IO) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val original = BitmapFactory.decodeStream(inputStream) ?: return@withContext null
                        val maxSize = 900
                        val w = original.width; val h = original.height
                        val (tw, th) = if (w > h) maxSize to (maxSize * h / w) else (maxSize * w / h) to maxSize
                        val resized = Bitmap.createScaledBitmap(original, tw, th, true)
                        val out = ByteArrayOutputStream()
                        resized.compress(Bitmap.CompressFormat.JPEG, 75, out)
                        "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                    } catch (e: Exception) { null }
                }
                if (base64 != null) {
                    val idx = insertAtIndex.coerceIn(0, blocks.size)
                    blocks.add(idx, SummaryBlock("image", base64))
                    // Tự thêm text block rỗng sau ảnh nếu chưa có
                    val nextIdx = idx + 1
                    if (nextIdx >= blocks.size || blocks[nextIdx].type == "image") {
                        blocks.add(nextIdx, SummaryBlock("text", ""))
                    }
                }
            }
        }
    }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {

        // === Tiêu đề ===
        Text("Tiêu đề", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(Modifier.height(20.dp))

        Text("Nội dung", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        // === Nút chèn tại đầu ===
        InsertBar(
            onInsertImage = { insertAtIndex = 0; imagePicker.launch("image/*") },
            onInsertText = { blocks.add(0, SummaryBlock("text", "")) }
        )

        // === Danh sách blocks ===
        blocks.forEachIndexed { index, block ->
            when (block.type) {
                "text" -> TextBlockItem(
                    text = block.content,
                    onChange = { blocks[index] = block.copy(content = it) },
                    onDelete = if (blocks.size > 1) ({ blocks.removeAt(index) }) else null
                )
                "image" -> ImageBlockItem(
                    base64 = block.content,
                    onDelete = { blocks.removeAt(index) }
                )
            }

            // Nút chèn SAU mỗi block
            InsertBar(
                onInsertImage = { insertAtIndex = index + 1; imagePicker.launch("image/*") },
                onInsertText = { blocks.add(index + 1, SummaryBlock("text", "")) }
            )
        }

        Spacer(Modifier.height(80.dp)) // padding cho bottom bar
    }
}

// ────────────────────────────────────────────────────────────
// Sub-composables
// ────────────────────────────────────────────────────────────

@Composable
private fun InsertBar(onInsertImage: () -> Unit, onInsertText: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
        AssistChip(
            onClick = onInsertImage,
            label = { Text("Ảnh", fontSize = 11.sp) },
            leadingIcon = {
                Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(14.dp))
            },
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        AssistChip(
            onClick = onInsertText,
            label = { Text("Đoạn văn", fontSize = 11.sp) },
            leadingIcon = {
                Icon(Icons.Default.PostAdd, null, modifier = Modifier.size(14.dp))
            },
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
    }
}

@Composable
private fun TextBlockItem(text: String, onChange: (String) -> Unit, onDelete: (() -> Unit)?) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = if (onDelete != null) 36.dp else 0.dp),
            minLines = 3,
            shape = RoundedCornerShape(10.dp),
            placeholder = { Text("Nhập nội dung...", color = Color.LightGray) }
        )
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ImageBlockItem(base64: String, onDelete: () -> Unit) {
    val bitmap = remember(base64) {
        try {
            val pure = if (base64.contains(",")) base64.split(",")[1] else base64
            val bytes = Base64.decode(pure, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) { null }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.FillWidth
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Xóa ảnh",
                        tint = Color.White,
                        modifier = Modifier.padding(4.dp).size(18.dp)
                    )
                }
            }
        }
    }
}

/** Hiển thị read-only một block ảnh */
@Composable
fun SummaryImageView(base64: String, modifier: Modifier = Modifier) {
    val bitmap = remember(base64) {
        try {
            val pure = if (base64.contains(",")) base64.split(",")[1] else base64
            val bytes = Base64.decode(pure, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) { null }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.FillWidth
        )
    }
}
