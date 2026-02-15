package dev.aaa1115910.bv.component.controllers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DenseListItem
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CollectionParentItem(
    val key: Long,
    val title: String,
    // 三态：null=未加载/失败；empty=无子项；notEmpty=有子项
    val children: List<CollectionChildItem>? = emptyList(),
    val extra: Any? = null
)

data class CollectionChildItem(
    val key: Long,
    val title: String,
    val extra: Any? = null
)

@Composable
fun CollectionListController(
    modifier: Modifier = Modifier,
    show: Boolean,
    active: Boolean,
    parents: List<CollectionParentItem>,
    selectedParentKey: Long?,
    selectedChildKey: Long?,
    pinnedParentKey: Long?,
    onEnsureChildrenLoaded: (CollectionParentItem) -> Unit,
    onParentClick: (CollectionParentItem) -> Unit,
    onChildClick: (CollectionParentItem, CollectionChildItem) -> Unit,
) {
    val listState = rememberLazyListState()

    // 宽度策略：
    // - “选择合集”（存在子项）固定 300.dp：与 VideoListController 完全一致，同时避免测量大量子项标题导致卡顿/闪屏
    // - “选择章节”（无子项）才按父项标题自适应宽度
    val hasAnyChildren = remember(parents) { parents.any { it.children?.isNotEmpty() == true } }

    val contentWidth: androidx.compose.ui.unit.Dp = if (hasAnyChildren) {
        300.dp
    } else {
        val textMeasurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val textStyle = MaterialTheme.typography.bodyLarge

        val maxPx: Int = parents.maxOfOrNull { p ->
            textMeasurer.measure(
                text = AnnotatedString(p.title),
                style = textStyle,
                maxLines = 1
            ).size.width
        } ?: 0

        (with(density) { maxPx.toDp() } + 16.dp * 2 + 32.dp)
            .coerceAtLeast(120.dp)
    }

    var didInitialPosition by remember { mutableStateOf(false) }
    var pendingFocusKey by remember { mutableStateOf<Long?>(null) }
    var pendingPrefetchKey by remember { mutableStateOf<Long?>(null) }

    val scope = rememberCoroutineScope()
    var ensureParentVisibleJob by remember { mutableStateOf<Job?>(null) }
    var focusedParentKey by remember { mutableStateOf<Long?>(null) }

    // 只在“进入内容区(active=true)”时设置 pendingFocus（避免导航聚焦时被内容抢焦点）
    LaunchedEffect(show, active) {
        if (!show) {
            pendingFocusKey = null
            pendingPrefetchKey = null
            didInitialPosition = false
            return@LaunchedEffect
        }
        if (active) {
            pendingFocusKey = selectedChildKey ?: selectedParentKey
        } else {
            pendingFocusKey = null
        }
    }

    val pinnedParent = remember(parents, pinnedParentKey, selectedParentKey, selectedChildKey) {
        parents.firstOrNull { it.key == (pinnedParentKey ?: selectedParentKey) }
            ?: parents.firstOrNull { p ->
                selectedChildKey != null && (p.children?.any { it.key == selectedChildKey } == true)
            }
    }

    // 初次定位：只在 active=true 时执行
    LaunchedEffect(show, active, parents.size) {
        if (!show) return@LaunchedEffect
        if (didInitialPosition) return@LaunchedEffect
        if (parents.isEmpty()) return@LaunchedEffect

        val targetIndex = parents.indexOfFirst { p ->
            p.key == selectedParentKey ||
                    (selectedChildKey != null && (p.children?.any { it.key == selectedChildKey } == true))
        }
        if (targetIndex != -1) {
            listState.scrollToItem(targetIndex)
            kotlinx.coroutines.android.awaitFrame()
            listState.scrollToItem(targetIndex, scrollOffset = -80)
        }
        didInitialPosition = true
    }

    // 延迟预取：保留能力，但仅 active 时触发
    LaunchedEffect(show, active, pendingPrefetchKey) {
        if (!show) return@LaunchedEffect
        if (!active) return@LaunchedEffect
        val key = pendingPrefetchKey ?: return@LaunchedEffect
        delay(200)
        if (pendingPrefetchKey == key) {
            parents.firstOrNull { it.key == key }?.let { onEnsureChildrenLoaded(it) }
        }
    }

    LaunchedEffect(show, active, focusedParentKey, parents.size) {
        if (!show) return@LaunchedEffect
        val key = focusedParentKey ?: return@LaunchedEffect

        val index = parents.indexOfFirst { it.key == key }
        if (index < 0) return@LaunchedEffect

        kotlinx.coroutines.android.awaitFrame()
        if (focusedParentKey != key) return@LaunchedEffect

        val layout = listState.layoutInfo
        val viewportStart = layout.viewportStartOffset
        val viewportEnd = layout.viewportEndOffset

        val info = layout.visibleItemsInfo.firstOrNull { it.index == index }
        val itemStart = info?.offset
        val itemEnd = info?.let { it.offset + it.size }

        val actuallyVisible =
            (itemStart != null && itemEnd != null && itemEnd > viewportStart && itemStart < viewportEnd)

        if (!actuallyVisible) {
            listState.scrollToItem(index)
            kotlinx.coroutines.android.awaitFrame()
            if (focusedParentKey != key) return@LaunchedEffect
            listState.scrollToItem(index, scrollOffset = -80)
        }
    }

    AnimatedVisibility(
        visible = show,
        enter = expandHorizontally(),
        exit = shrinkHorizontally()
    ) {
        Surface(
            modifier = modifier.fillMaxHeight(),
            colors = SurfaceDefaults.colors(containerColor = Color.Black.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .width(contentWidth)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 60.dp)
                ) {
                    items(items = parents, key = { it.key }) { parent ->
                        val isParentSelected = parent.key == selectedParentKey
                        val children = parent.children
                        val childrenLoaded = children != null
                        val hasChildren = children?.isNotEmpty() == true
                        val isPinned = pinnedParent?.key == parent.key ||
                                isParentSelected ||
                                (selectedChildKey != null && (children?.any { it.key == selectedChildKey } == true))

                        var expanded by remember(parent.key) { mutableStateOf(false) }
                        var didAutoExpand by remember(parent.key) { mutableStateOf(false) }

                        LaunchedEffect(show, active, isPinned) {
                            if (!show) return@LaunchedEffect
                            if (!active) return@LaunchedEffect
                            if (isPinned && !didAutoExpand && hasChildren) {
                                expanded = true
                                didAutoExpand = true
                            }
                        }

                        var groupHasFocus by remember(parent.key) { mutableStateOf(false) }
                        var collapseToken by remember(parent.key) { mutableIntStateOf(0) }

                        fun scheduleCollapseIfNeeded() {
                            if (isPinned) return
                            collapseToken++
                        }

                        LaunchedEffect(active, collapseToken) {
                            if (!active) return@LaunchedEffect
                            if (collapseToken > 0) {
                                kotlinx.coroutines.android.awaitFrame()
                                if (!groupHasFocus && !isPinned) expanded = false
                            }
                        }

                        val parentFocusRequester = remember(parent.key) { FocusRequester() }
                        val parentBringIntoViewRequester =
                            remember(parent.key) { androidx.compose.foundation.relocation.BringIntoViewRequester() }

                        Column(modifier = Modifier.animateContentSize()) {
                            // pendingFocus 消耗：只在 active 时执行
                            LaunchedEffect(show, active, pendingFocusKey) {
                                if (!show) return@LaunchedEffect
                                if (!active) return@LaunchedEffect
                                val wantKey = pendingFocusKey ?: return@LaunchedEffect

                                val shouldHandleThisGroup =
                                    (parent.key == wantKey) || (childrenLoaded && (children?.any { it.key == wantKey } == true))
                                if (!shouldHandleThisGroup) return@LaunchedEffect

                                val wantChild = children?.any { it.key == wantKey } == true
                                if (wantChild) expanded = true

                                val parentIndex = parents.indexOfFirst { it.key == parent.key }
                                if (parentIndex != -1) {
                                    listState.scrollToItem(parentIndex, scrollOffset = -80)
                                    kotlinx.coroutines.android.awaitFrame()
                                }

                                if (parent.key == wantKey) {
                                    parentFocusRequester.requestFocus()
                                    kotlinx.coroutines.android.awaitFrame()
                                    kotlinx.coroutines.coroutineScope {
                                        launch { parentBringIntoViewRequester.bringIntoView() }
                                    }
                                    pendingFocusKey = null
                                    return@LaunchedEffect
                                }

                                if (!childrenLoaded) {
                                    onEnsureChildrenLoaded(parent)
                                    parentFocusRequester.requestFocus()
                                    kotlinx.coroutines.android.awaitFrame()
                                    kotlinx.coroutines.coroutineScope {
                                        launch { parentBringIntoViewRequester.bringIntoView() }
                                    }
                                    return@LaunchedEffect
                                }
                            }

                            DenseListItem(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .focusRequester(parentFocusRequester)
                                    .bringIntoViewRequester(parentBringIntoViewRequester)
                                    .onFocusChanged { state ->
                                        if (state.hasFocus) {
                                            groupHasFocus = true
                                            pendingPrefetchKey = parent.key

                                            // 仅当父项已经在屏幕外时，才瞬移滚动把它带回可视区。
                                            focusedParentKey = parent.key
                                            ensureParentVisibleJob?.cancel()
                                            ensureParentVisibleJob = scope.launch {
                                                if (!show) return@launch

                                                kotlinx.coroutines.android.awaitFrame()
                                                if (focusedParentKey != parent.key) return@launch

                                                val index = parents.indexOfFirst { it.key == parent.key }
                                                if (index < 0) return@launch

                                                val layout = listState.layoutInfo
                                                val viewportStart = layout.viewportStartOffset
                                                val viewportEnd = layout.viewportEndOffset

                                                val info = layout.visibleItemsInfo.firstOrNull { it.index == index }
                                                val itemStart = info?.offset
                                                val itemEnd = info?.let { it.offset + it.size }

                                                val actuallyVisible =
                                                    (itemStart != null && itemEnd != null && itemEnd > viewportStart && itemStart < viewportEnd)

                                                if (!actuallyVisible) {
                                                    listState.scrollToItem(index, scrollOffset = -80)
                                                    kotlinx.coroutines.android.awaitFrame()

                                                    if (focusedParentKey == parent.key) {
                                                        parentBringIntoViewRequester.bringIntoView()
                                                    }
                                                }
                                            }
                                        } else {
                                            groupHasFocus = false
                                            if (pendingPrefetchKey == parent.key) pendingPrefetchKey = null
                                            scheduleCollapseIfNeeded()

                                            if (focusedParentKey == parent.key) focusedParentKey = null
                                        }
                                    },
                                selected = isParentSelected && (selectedChildKey == null),
                                onClick = {
                                    if (hasChildren) {
                                        expanded = !expanded
                                        return@DenseListItem
                                    }
                                    onParentClick(parent)
                                },
                                headlineContent = {
                                    Text(
                                        text = parent.title,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                trailingContent = {
                                    if (hasChildren) {
                                        Icon(
                                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            )

                            if (expanded && !childrenLoaded) {
                                Column(
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    SimpleListItem(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .focusProperties { canFocus = false },
                                        text = "加载中……",
                                        selected = false,
                                        textAlign = TextAlign.Start,
                                        onClick = {}
                                    )
                                }
                            }

                            if (expanded && hasChildren) {
                                Column(
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    children?.forEach { child ->
                                        key(child.key) {
                                            val isChildSelected = child.key == selectedChildKey

                                            val childFocusRequester = remember(child.key) { FocusRequester() }
                                            val childBringIntoViewRequester =
                                                remember(child.key) { androidx.compose.foundation.relocation.BringIntoViewRequester() }

                                            LaunchedEffect(show, active, pendingFocusKey, expanded, childrenLoaded) {
                                                if (!show) return@LaunchedEffect
                                                if (!active) return@LaunchedEffect
                                                if (!expanded) return@LaunchedEffect
                                                if (!childrenLoaded) return@LaunchedEffect

                                                val wantKey = pendingFocusKey ?: return@LaunchedEffect
                                                if (wantKey != child.key) return@LaunchedEffect

                                                val parentIndex = parents.indexOfFirst { it.key == parent.key }
                                                if (parentIndex != -1) {
                                                    listState.scrollToItem(parentIndex, scrollOffset = -80)
                                                    kotlinx.coroutines.android.awaitFrame()
                                                }

                                                childFocusRequester.requestFocus()
                                                kotlinx.coroutines.android.awaitFrame()

                                                kotlinx.coroutines.coroutineScope {
                                                    launch { childBringIntoViewRequester.bringIntoView() }
                                                }

                                                pendingFocusKey = null
                                            }

                                            SimpleListItem(
                                                modifier = Modifier
                                                    .padding(horizontal = 16.dp)
                                                    .focusRequester(childFocusRequester)
                                                    .bringIntoViewRequester(childBringIntoViewRequester),
                                                text = child.title,
                                                selected = isChildSelected,
                                                textAlign = TextAlign.Start,
                                                onFocus = { groupHasFocus = true },
                                                onClick = {
                                                    if (!isChildSelected) onChildClick(parent, child)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            LaunchedEffect(active, expanded) {
                                if (!active) return@LaunchedEffect
                                if (!expanded && isParentSelected) {
                                    parentFocusRequester.requestFocus()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleListItem(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    textAlign: TextAlign = TextAlign.Center,
    onFocus: () -> Unit = {},
    onClick: () -> Unit
) {
    DenseListItem(
        modifier = modifier.onFocusChanged { if (it.hasFocus) onFocus() },
        selected = selected,
        onClick = onClick,
        headlineContent = { Text(text = text, textAlign = textAlign) }
    )
}