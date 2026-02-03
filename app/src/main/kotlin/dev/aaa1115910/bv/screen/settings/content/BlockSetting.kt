package dev.aaa1115910.bv.screen.settings.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.component.settings.SettingListItem
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.toast
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BlockSetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val scope = rememberCoroutineScope()
    var updating by remember { mutableStateOf(false) }
    var updateJob by remember { mutableStateOf<Job?>(null) }

    // 更新开始后，把焦点强行挪到“屏蔽页面”（因为“屏蔽分组/立即更新”会被禁用）
    val pageFocusRequester = remember { FocusRequester() }

    LaunchedEffect(updating) {
        if (updating) pageFocusRequester.requestFocus()
    }

    // 退出设置页（离开 composition）就停止更新；已完成分组的结果已在 BlockManager 内逐个落盘
    DisposableEffect(Unit) {
        onDispose {
            updateJob?.cancel()
            updateJob = null
            updating = false
        }
    }

    var showGroupDialog by remember { mutableStateOf(false) }
    var showPageDialog by remember { mutableStateOf(false) }

    val groupSelectedCount = Prefs.blockSelectedTagIds.size
    val pagesSelected = Prefs.blockEnabledPages

    // 用于：
    // 1) “屏蔽分组”对话框候选
    // 2) “立即更新”显示 需要更新：N 个分组
    val tags = rememberTagsFromPrefs() // TagItem(tagid,name,count)
    val cachedMembers = rememberMembersCacheFromPrefs()
    val needUpdateCount = calcNeedUpdateCount(
        selectedTagIds = Prefs.blockSelectedTagIds,
        tags = tags,
        cached = cachedMembers
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_item_block),
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 更新进行中：禁用“屏蔽分组”入口（不可聚焦/不可点击）
        SettingListItem(
            modifier = if (updating || tags.isEmpty()) Modifier.focusProperties { canFocus = false } else Modifier,
            title = stringResource(R.string.block_setting_groups_title),
            supportText = if (tags.isEmpty()) {
                "未获取分组，请先点击立即更新"
            } else {
                "已选择 $groupSelectedCount / ${tags.size} 个分组"
            },
            onClick = {
                if (updating || tags.isEmpty()) return@SettingListItem
                showGroupDialog = true
            }
        )

        // “屏蔽页面”不禁用，但会作为更新期间的默认落点
        SettingListItem(
            modifier = Modifier.focusRequester(pageFocusRequester),
            title = stringResource(R.string.block_setting_pages_title),
            supportText = if (pagesSelected.isEmpty()) {
                stringResource(R.string.block_setting_pages_support_empty)
            } else {
                context.getString(
                    R.string.block_setting_pages_support,
                    pagesSelected.joinToString(",") { it.displayName }
                )
            },
            onClick = { showPageDialog = true }
        )

        // 立即更新：刷新 tags + 更新已选分组成员；更新中不可聚焦/不可点击
        SettingListItem(
            modifier = if (updating) Modifier.focusProperties { canFocus = false } else Modifier,
            title = stringResource(R.string.block_setting_update_now_title),
            supportText = if (updating) {
                stringResource(R.string.block_setting_update_now_support_updating)
            } else {
                "已选择 ${needUpdateCount} 个分组"
            },
            onClick = {
                if (updating) return@SettingListItem

                // onClick 里不要用 stringResource（它是 @Composable）
                val hasAuth = Prefs.uid != 0L && (Prefs.sessData.isNotBlank() || Prefs.accessToken.isNotBlank())
                if (!hasAuth) {
                    context.getString(R.string.block_setting_update_now_need_login).toast(context)
                    return@SettingListItem
                }

                // 先把焦点挪到“屏蔽页面”那一项，再进入 updating（否则当前按钮禁焦点会把焦点弹飞到左侧菜单）
                pageFocusRequester.requestFocus()

                updating = true
                updateJob = scope.launch(Dispatchers.IO) {
                    try {
                        dev.aaa1115910.bv.block.BlockManager.updateByUser()

                        withContext(Dispatchers.Main) {
                            // updateByUser 可能因为 auth/网络等原因没拿到 tags，这里做一次兜底判断
                            if (Prefs.followTagsCacheJson.isBlank()) {
                                "更新失败：未获取到分组列表".toast(context)
                            } else {
                                val total = getCachedTagTotalCount()
                                "更新完成：已获取 $total 个分组".toast(context)
                            }
                        }
                    } catch (e: CancellationException) {
                        // 退出设置页触发的取消：不提示、不报错；按你的要求“已完成的保留”
                    } catch (t: Throwable) {
                        withContext(Dispatchers.Main) {
                            ("更新失败: " + (t.localizedMessage ?: t.javaClass.simpleName)).toast(context)
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            updating = false
                            updateJob = null
                        }
                    }
                }
            }
        )
    }

    // ---------- 分组多选（串行拉取 + 进度显示 + 秒选中缓存）----------
    run {
        // 把 TagItem 转为 Dialog 需要的 BlockTagItem
        val dialogTags = tags.map { BlockTagItem(it.tagid, it.name, it.count) }

        BlockGroupSelectDialog(
            show = showGroupDialog,
            title = stringResource(R.string.block_setting_groups_dialog_title),
            tags = dialogTags,
            initialSelectedTagIds = Prefs.blockSelectedTagIds,
            initialMembersCache = cachedMembers,
            onHideDialog = { showGroupDialog = false },
            onSubmit = submit@{ finalSelectedTagIds, newOrUpdatedCache ->
                val oldSelected = Prefs.blockSelectedTagIds
                if (oldSelected == finalSelectedTagIds && newOrUpdatedCache.isEmpty()) {
                    // 打开即退：不写 Prefs，不 prune，不更新时间戳 => 保持原状态
                    return@submit
                }

                // 退出 dialog 才提交：
                // 1) 写入选中 tagid
                // 2) 写入成员缓存（仅包含最终选中的分组；未选中的全部清空）
                // 3) reload 让后续加载/翻页追加生效
                Prefs.blockSelectedTagIds = finalSelectedTagIds
                BlockPrefsUtilV2.writeMembersCacheAndPruneUnselected(
                    selectedTagIds = finalSelectedTagIds.toSet(),
                    upserts = newOrUpdatedCache
                )
                dev.aaa1115910.bv.block.BlockManager.reloadFromPrefs()
                "已保存，仅对之后加载的内容生效".toast(context)
            }
        )
    }

    // ---------- 页面多选（退出才提交，不拉取） ----------
    run {
        BlockPageSelectDialog(
            show = showPageDialog,
            title = stringResource(R.string.block_setting_pages_dialog_title),
            initialSelectedPages = Prefs.blockEnabledPages,
            onHideDialog = { showPageDialog = false },
            onSubmit = { pages ->
                if (Prefs.blockEnabledPages == pages) return@BlockPageSelectDialog
                Prefs.blockEnabledPages = pages
                dev.aaa1115910.bv.block.BlockManager.reloadFromPrefs()
                "已保存，仅对之后加载的内容生效".toast(context)
            }
        )
    }
}

@Serializable
private data class FollowTagsCache(val data: List<TagItem> = emptyList())

@Serializable
private data class TagItem(val tagid: Int, val name: String, val count: Int)

@Composable
private fun rememberTagsFromPrefs(): List<TagItem> {
    val raw = Prefs.followTagsCacheJson
    if (raw.isBlank()) return emptyList()
    val json = remember { Json { ignoreUnknownKeys = true; coerceInputValues = true } }
    return remember(raw) {
        runCatching { json.decodeFromString(FollowTagsCache.serializer(), raw).data }
            .getOrDefault(emptyList())
    }
}

private object BlockPrefsUtil {
    @Serializable
    private data class TagMembersCache(val data: Map<String, TagMembersEntry> = emptyMap())

    @Serializable
    private data class TagMembersEntry(
        val count: Int,
        @kotlinx.serialization.SerialName("updated_at")
        val updatedAt: Long,
        val mids: List<Long> = emptyList()
    )

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun removeMembersCacheForTagIds(tagIds: Set<Int>) {
        val raw = Prefs.followTagMembersCacheJson
        if (raw.isBlank()) return
        val cache = runCatching { json.decodeFromString(TagMembersCache.serializer(), raw) }
            .getOrElse { return }
        val newMap = cache.data.toMutableMap()
        tagIds.forEach { newMap.remove(it.toString()) }
        Prefs.followTagMembersCacheJson = json.encodeToString(
            TagMembersCache.serializer(),
            TagMembersCache(newMap)
        )
        // 清空汇总 CSV，交给 BlockManager.reloadFromPrefs 重算
        Prefs.blockedMidsCsv = ""
    }
}

@Composable
private fun rememberMembersCacheFromPrefs(): Map<Int, CachedMembers> {
    val raw = Prefs.followTagMembersCacheJson
    if (raw.isBlank()) return emptyMap()
    val json = remember { Json { ignoreUnknownKeys = true; coerceInputValues = true } }
    return remember(raw) {
        runCatching {
            val cache = json.decodeFromString(BlockPrefsUtilV2.TagMembersCache.serializer(), raw)
            cache.data.mapNotNull { (k, v) ->
                val tagId = k.toIntOrNull() ?: return@mapNotNull null
                tagId to CachedMembers(count = v.count, mids = v.mids)
            }.toMap()
        }.getOrDefault(emptyMap())
    }
}

private object BlockPrefsUtilV2 {
    @Serializable
    data class TagMembersCache(val data: Map<String, TagMembersEntry> = emptyMap())

    @Serializable
    data class TagMembersEntry(
        val count: Int,
        @kotlinx.serialization.SerialName("updated_at")
        val updatedAt: Long,
        val mids: List<Long> = emptyList()
    )

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun writeMembersCacheAndPruneUnselected(
        selectedTagIds: Set<Int>,
        upserts: Map<Int, CachedMembers>
    ) {
        val raw = Prefs.followTagMembersCacheJson
        val old = if (raw.isBlank()) TagMembersCache() else runCatching {
            json.decodeFromString(TagMembersCache.serializer(), raw)
        }.getOrDefault(TagMembersCache())

        val newMap = old.data.toMutableMap()

        // 1) 先删：未选中的全部清空
        val keepKeys = selectedTagIds.map { it.toString() }.toSet()
        newMap.keys.toList().forEach { key ->
            if (!keepKeys.contains(key)) newMap.remove(key)
        }

        // 2) 再写：选中的缓存（包含秒选中的原缓存 + 本次拉取的 upserts）
        upserts.forEach { (tagId, cache) ->
            if (!selectedTagIds.contains(tagId)) return@forEach
            newMap[tagId.toString()] = TagMembersEntry(
                count = cache.count,
                updatedAt = System.currentTimeMillis(),
                mids = cache.mids
            )
        }

        Prefs.followTagMembersCacheJson = json.encodeToString(
            TagMembersCache.serializer(),
            TagMembersCache(newMap)
        )

        // 同步重建 blockedMidsCsv（让 BlockManager.reloadFromPrefs 快速生效）
        val mids = HashSet<Long>(1024)
        selectedTagIds.forEach { tagId ->
            newMap[tagId.toString()]?.mids?.let { mids.addAll(it) }
        }
        Prefs.blockedMidsCsv = mids.joinToString(",")
    }
}

private fun calcNeedUpdateCount(
    selectedTagIds: List<Int>,
    tags: List<TagItem>,
    cached: Map<Int, CachedMembers>
): Int {
    if (selectedTagIds.isEmpty()) return 0
    val tagMap = tags.associateBy { it.tagid }
    var n = 0
    for (tagId in selectedTagIds) {
        val latest = tagMap[tagId]
        val entry = cached[tagId]

        // tags 缓存没有该分组（或 tags 还没刷新过）：保守起见算“需更新”
        if (latest == null) {
            n++
            continue
        }

        // 无缓存/空 mids/count 不一致 => 需更新
        if (entry == null || entry.mids.isEmpty() || entry.count != latest.count) {
            n++
        }
    }
    return n
}

private fun getCachedTagTotalCount(): Int {
    val raw = Prefs.followTagsCacheJson
    if (raw.isBlank()) return 0
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    return runCatching {
        json.decodeFromString(FollowTagsCache.serializer(), raw).data.size
    }.getOrDefault(0)
}
