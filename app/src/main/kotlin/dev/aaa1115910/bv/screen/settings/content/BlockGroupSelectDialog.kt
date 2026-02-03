package dev.aaa1115910.bv.screen.settings.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import androidx.compose.ui.platform.LocalContext
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.bv.util.Prefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 屏蔽分组选择 Dialog（专用，不做公共组件）：
 * - 点击 chip：
 *   - 若本地已有旧 mids：直接选中（count 不一致则显示“需更新”）
 *   - 否则入队串行拉取成员（显示 0/total -> x/total），完成后才选中
 * - 退出 dialog：
 *   - 立刻停止拉取
 *   - 丢弃未完成数据（不写入 Prefs）
 *   - 只提交最终 selectedTagIds + 本次 dialog 新拉取完成的 upserts
 *
 * 额外规则（防止等同全量分组）：
 * - 当 tags.size >= 2 且 已选数 == 总数 - 1 时，剩余未选分组禁止获得焦点（无法被选中）
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun BlockGroupSelectDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    tags: List<BlockTagItem>,
    // 进入 dialog 时的已选分组（来自 Prefs.blockSelectedTagIds）
    initialSelectedTagIds: List<Int>,
    // 进入 dialog 时的本地缓存快照：tagid -> (count, mids)
    initialMembersCache: Map<Int, CachedMembers>,
    onHideDialog: () -> Unit,
    // 退出 dialog 时一次性提交：selectedTagIds + 本次新增/更新的成员缓存（仅 upserts）
    onSubmit: (selectedTagIds: List<Int>, newOrUpdatedCache: Map<Int, CachedMembers>) -> Unit
) {
    if (!show) return

    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    // 仅在用户确实操作过（点击过）时才提示，避免“打开即退/仅进入就提示”的干扰
    var userInteracted by remember { mutableStateOf(false) }

    // dialog 内部状态：允许反悔（取消选中不删缓存），退出时才统一提交 + prune
    val selected = remember { mutableStateListOf<Int>() } // 当前“已选”的 tagid（含 Selected/SelectedStale）
    val desired = remember { mutableStateMapOf<Int, Boolean>() } // 用户意图：true 表示希望选中（含 queued/loading）

    // 当已选数达到“总数-1”时提示用户不得全选（最后一个会被禁止聚焦/禁止选中）
    LaunchedEffect(userInteracted, selected.size, tags.size) {
        if (!userInteracted) return@LaunchedEffect
        if (tags.size >= 2 && selected.size == tags.size - 1) {
            "为避免等同全量分组，禁止全选关注分组".toast(context)
        }
    }

    // 进度与队列状态
    val uiState = remember { mutableStateMapOf<Int, TagUiState>() }
    val queue = remember { ArrayDeque<Int>() }
    var workerJob by remember { mutableStateOf<Job?>(null) }

    // 本次 dialog 拉取成功的数据（只记录“完成”的，未完成不进入这里）
    val fetchedCache = remember { mutableStateMapOf<Int, CachedMembers>() }

    // 焦点：每个 tag 一个 FocusRequester，便于在“最后一个不可聚焦”时主动迁移焦点
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }
    fun requesterFor(tagId: Int): FocusRequester =
        focusRequesters.getOrPut(tagId) { FocusRequester() }

    var focusedTagId by remember { mutableStateOf<Int?>(null) }

    fun shouldBlockLastUnselectedFocus(): Boolean =
        tags.size >= 2 && selected.size == tags.size - 1

    fun remainingUnselectedTagId(): Int? {
        if (!shouldBlockLastUnselectedFocus()) return null
        return tags.firstOrNull { !selected.contains(it.tagid) }?.tagid
    }

    fun requestFocusToAnyFocusable() {
        if (tags.isEmpty()) return
        val blocked = remainingUnselectedTagId()
        val targetId = tags.firstOrNull { it.tagid != blocked }?.tagid ?: return
        requesterFor(targetId).requestFocus()
    }

    // 初始化：已选分组必须保留；缓存新旧只影响 Selected/SelectedStale
    LaunchedEffect(show) {
        selected.clear()
        desired.clear()
        uiState.clear()
        queue.clear()
        workerJob?.cancel()
        fetchedCache.clear()

        tags.forEach { tag ->
            val tagId = tag.tagid
            val isInitiallySelected = initialSelectedTagIds.contains(tagId)
            val cached = initialMembersCache[tagId]
            val cacheUsable = cached != null && cached.mids.isNotEmpty()
            val cacheFresh = cacheUsable && cached!!.count == tag.count

            if (isInitiallySelected) {
                selected.add(tagId)
                desired[tagId] = true
                uiState[tagId] = if (cacheFresh) TagUiState.Selected else TagUiState.SelectedStale
            } else {
                desired[tagId] = false
                uiState[tagId] = TagUiState.Idle
            }
        }

        requestFocusToAnyFocusable()
    }

    // 若当前焦点正好落在“最后一个不可聚焦”的分组上，主动把焦点挪走
    LaunchedEffect(selected.size) {
        val blocked = remainingUnselectedTagId()
        if (blocked != null && focusedTagId == blocked) {
            requestFocusToAnyFocusable()
        }
    }

    fun startWorkerIfNeeded() {
        if (workerJob?.isActive == true) return
        workerJob = scope.launch {
            while (isActive && queue.isNotEmpty()) {
                val tagId = queue.removeFirst()
                val tag = tags.firstOrNull { it.tagid == tagId } ?: continue

                // 若用户在排队期间取消意图：直接丢弃
                if (desired[tagId] != true) {
                    uiState[tagId] = TagUiState.Idle
                    continue
                }

                uiState[tagId] = TagUiState.Loading(fetched = 0)

                val mids = mutableListOf<Long>()
                val ps = 50
                var pn = 1
                var ok = true
                while (isActive) {
                    val pageMembers = try {
                        BiliHttpApi.getRelationTagMembers(
                            tagId = tagId,
                            pageNumber = pn,
                            pageSize = ps,
                            orderType = null,
                            accessKey = Prefs.accessToken.takeIf { it.isNotBlank() },
                            sessData = Prefs.sessData.takeIf { it.isNotBlank() }
                        ).getResponseData()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Throwable) {
                        ok = false
                        break
                    }

                    if (pageMembers.isEmpty()) break
                    mids.addAll(pageMembers.map { it.mid })
                    uiState[tagId] = TagUiState.Loading(fetched = mids.size)
                    if (pageMembers.size < ps) break

                    pn++
                    delay(700) // 固定 700ms，避免风控/限流
                }

                if (!ok || !isActive) {
                    uiState[tagId] = TagUiState.Idle
                    desired[tagId] = false
                    continue
                }

                // 拉取完成：先写入 dialog 内的 fetchedCache，便于后续“秒选中”
                fetchedCache[tagId] = CachedMembers(count = tag.count, mids = mids)

                // 防止“等同全量”：若此时已选数已经是总数-1，则不自动把最后一个也选上
                if (shouldBlockLastUnselectedFocus() && !selected.contains(tagId)) {
                    uiState[tagId] = TagUiState.Idle
                    desired[tagId] = false

                    continue
                }

                if (!selected.contains(tagId)) selected.add(tagId)
                desired[tagId] = true
                uiState[tagId] = TagUiState.Selected
            }
        }
    }

    fun enqueue(tagId: Int) {
        if (queue.contains(tagId)) return
        queue.addLast(tagId)
        startWorkerIfNeeded()
    }

    fun dismissAndSubmit() {
        // 1) 立刻停止拉取
        workerJob?.cancel()
        workerJob = null

        // 2) 未完成（Queued/Loading）的全部丢弃并标记为未选中
        tags.forEach { tag ->
            when (uiState[tag.tagid]) {
                is TagUiState.Queued,
                is TagUiState.Loading -> {
                    uiState[tag.tagid] = TagUiState.Idle
                    desired[tag.tagid] = false
                }
                else -> Unit
            }
        }
        queue.clear()

        // 3) 只提交 selected 的 tagId；缓存只提交“本次完成拉取”的 upserts（避免打开即退也写 Prefs）
        val finalSelected = selected.toList().sorted()
        val upserts = fetchedCache
            .filter { (tagId, _) -> finalSelected.contains(tagId) }
            .toMap()

        onSubmit(finalSelected, upserts)
        onHideDialog()
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = { dismissAndSubmit() },
        confirmButton = {},
        title = { Text(text = title) },
        text = {
            FlowRow(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    val state = uiState[tag.tagid] ?: TagUiState.Idle
                    val isSelected = state == TagUiState.Selected || state == TagUiState.SelectedStale

                    val trailingText = when (state) {
                        TagUiState.Idle -> "${tag.count}"
                        TagUiState.Selected -> "${tag.count}"
                        TagUiState.SelectedStale -> "${tag.count} 需更新"
                        TagUiState.Queued -> "0/${tag.count}"
                        is TagUiState.Loading -> "${state.fetched.coerceAtMost(tag.count)}/${tag.count}"
                    }

                    // tags.size>=2 且 selected==total-1 时，唯一未选中的那个禁止聚焦
                    val blockFocus = shouldBlockLastUnselectedFocus() && !isSelected

                    FilterChip(
                        modifier = Modifier
                            .focusRequester(requesterFor(tag.tagid))
                            .focusProperties { canFocus = !blockFocus }
                            .onFocusChanged { if (it.hasFocus) focusedTagId = tag.tagid },
                        selected = isSelected,
                        onClick = {
                            userInteracted = true

                            // 若已选数已经是“总数-1”，则禁止再选（否则等同全量分组），并 toast 提示
                            val isSelectedState =
                                state == TagUiState.Selected || state == TagUiState.SelectedStale
                            if (tags.size >= 2 && selected.size >= tags.size - 1 && !isSelectedState) {
                                "禁止全选关注分组".toast(context)
                                return@FilterChip
                            }

                            // 双保险：即使某些场景下仍触发点击，也不允许选满全量
                            if (shouldBlockLastUnselectedFocus() && !isSelected) return@FilterChip

                            // cache 优先：本次 dialog 拉取完成的 > 进入 dialog 前的旧缓存
                            val cached = fetchedCache[tag.tagid] ?: initialMembersCache[tag.tagid]
                            val cacheUsable = cached != null && cached.mids.isNotEmpty()
                            val cacheFresh = cacheUsable && cached!!.count == tag.count

                            when (state) {
                                // 已选：允许取消（不清缓存，允许反悔）
                                TagUiState.Selected,
                                TagUiState.SelectedStale -> {
                                    selected.remove(tag.tagid)
                                    desired[tag.tagid] = false
                                    uiState[tag.tagid] = TagUiState.Idle
                                }

                                // 排队/拉取中：最小实现为忽略（不做二次点击取消）
                                TagUiState.Queued,
                                is TagUiState.Loading -> Unit

                                // 未选：有旧 mids 则秒选中；否则入队串行拉取
                                TagUiState.Idle -> {
                                    desired[tag.tagid] = true
                                    if (cacheUsable) {
                                        if (!selected.contains(tag.tagid)) selected.add(tag.tagid)
                                        uiState[tag.tagid] =
                                            if (cacheFresh) TagUiState.Selected else TagUiState.SelectedStale
                                    } else {
                                        uiState[tag.tagid] = TagUiState.Queued
                                        enqueue(tag.tagid)
                                    }
                                }
                            }
                        },
                        leadingIcon = {
                            Row {
                                AnimatedVisibility(visible = isSelected) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Rounded.Done,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = tag.name)
                            Text(text = trailingText)
                        }
                    }
                }
            }
        }
    )
}

data class BlockTagItem(
    val tagid: Int,
    val name: String,
    val count: Int
)

data class CachedMembers(
    val count: Int,
    val mids: List<Long>
)

private sealed interface TagUiState {
    data object Idle : TagUiState
    data object Queued : TagUiState
    data class Loading(val fetched: Int) : TagUiState
    data object Selected : TagUiState
    data object SelectedStale : TagUiState
}