package dev.aaa1115910.bv.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.ContentScale
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.user.CoAuthor
import dev.aaa1115910.bv.tv.component.TvAlertDialog

@Stable
class CoAuthorsDialogState internal constructor() {
    var visible by mutableStateOf(false)
        private set

    var authors by mutableStateOf<List<CoAuthor>>(emptyList())
        private set

    fun open(input: List<CoAuthor>) {
        val dedup = input.distinctBy { it.mid } // 稳定去重：保留第一次出现
        authors = dedup
        visible = dedup.size > 1
    }

    fun dismiss() {
        visible = false
    }
}

@Composable
fun rememberCoAuthorsDialogState(): CoAuthorsDialogState = remember { CoAuthorsDialogState() }

private data class CoAuthorGroup(
    val title: String,
    val members: List<CoAuthor>
)

private fun buildGroups(authors: List<CoAuthor>): List<CoAuthorGroup> {
    // 稳定去重
    val dedup = authors.distinctBy { it.mid }

    // 按 title 稳定分组（保持 title 首次出现顺序）
    val map = LinkedHashMap<String, MutableList<CoAuthor>>()
    for (a in dedup) {
        map.getOrPut(a.title) { mutableListOf() }.add(a)
    }
    val groups = map.entries.map { CoAuthorGroup(it.key, it.value) }.toMutableList()

    // 投稿成员(UP主)分组置顶（title 原文显示）
    val upIndex = groups.indexOfFirst { it.title == "UP主" }
    if (upIndex > 0) {
        val up = groups.removeAt(upIndex)
        groups.add(0, up)
    }
    return groups
}

@Composable
fun CoAuthorsDialogHost(
    state: CoAuthorsDialogState,
    onClickAuthor: (mid: Long, name: String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "联合投稿"
) {
    if (!state.visible) return

    val groups = remember(state.authors) { buildGroups(state.authors) }

    // 找到弹窗里第一个可聚焦成员（UP主组已在 buildGroups 里置顶）
    val firstMemberMid = remember(groups) { groups.firstOrNull()?.members?.firstOrNull()?.mid }
    val firstMemberFocusRequester = remember { FocusRequester() }

    LaunchedEffect(firstMemberMid) {
        if (firstMemberMid == null) return@LaunchedEffect
        // Dialog 刚弹出时直接 requestFocus 有概率失败，小延迟更稳
        delay(60)
        runCatching { firstMemberFocusRequester.requestFocus() }
    }

    TvAlertDialog(
        onDismissRequest = { state.dismiss() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        ),
        title = { Text(text = title) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(groups, key = { it.title }) { group ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = group.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(items = group.members, key = { it.mid }) { member ->
                                val itemModifier =
                                    if (member.mid == firstMemberMid) {
                                        Modifier.focusRequester(firstMemberFocusRequester)
                                    } else {
                                        Modifier
                                    }

                                CoAuthorItem(
                                    author = member,
                                    modifier = itemModifier,
                                    onClick = {
                                        onClickAuthor(member.mid, member.name)
                                        state.dismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        modifier = modifier
    )
}

@Composable
private fun CoAuthorItem(
    author: CoAuthor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 外层固定最大尺寸：保证“名字不动”
    val outerSize = 86.dp
    val idleInnerSize = 72.dp

    var focused by remember { mutableStateOf(false) }
    val innerSize by animateDpAsState(
        targetValue = if (focused) outerSize else idleInnerSize,
        label = "coAuthorAvatarSize"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        // 名字贴着描边（避免看起来被描边挡住）
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(outerSize)
                .onFocusChanged { focused = it.isFocused },
            shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
            // 透明背景
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                pressedContainerColor = Color.Transparent
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(3.dp, Color(0xFFFE7297)),
                    shape = CircleShape
                )
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    modifier = Modifier
                        .size(innerSize)
                        .clip(CircleShape),
                    model = author.face,
                    contentDescription = author.name,
                    contentScale = ContentScale.Crop
                )
            }
        }

        Text(
            text = author.name,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 给“up主页”按钮用的统一点击处理：
 * - 单作者：直接跳转
 * - 多作者：打开弹窗（由 state 管理）
 */
fun handleUpHomeClick(
    authors: List<CoAuthor>,
    state: CoAuthorsDialogState,
    onNavigateSingle: (mid: Long, name: String) -> Unit
) {
    val dedup = authors.distinctBy { it.mid }
    if (dedup.size <= 1) {
        val only = dedup.firstOrNull() ?: return
        onNavigateSingle(only.mid, only.name)
    } else {
        state.open(dedup)
    }
}