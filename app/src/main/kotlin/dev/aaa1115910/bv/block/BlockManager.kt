package dev.aaa1115910.bv.block

import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.relation.RelationTag
import dev.aaa1115910.biliapi.http.entity.user.RelationStat
import dev.aaa1115910.bv.util.Prefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 仅做“本地过滤显示”的屏蔽管理器：
 * - 每次启动：刷新 tags 列表、following 总数，并按选择的分组更新成员 mid 缓存（顺序分页 + delay，避免风控）
 * - 运行时：根据 Prefs 的缓存/选择生成 blockedMidsSet，用于 ViewModel 追加前过滤
 */
object BlockManager {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        prettyPrint = false
    }

    private val updateMutex = Mutex()

    @Volatile
    private var blockedMids: Set<Long> = emptySet()

    fun isPageEnabled(page: BlockPage): Boolean = Prefs.blockEnabledPages.contains(page)

    fun isBlocked(mid: Long?): Boolean {
        if (mid == null) return false
        return blockedMids.contains(mid)
    }

    /**
     * ViewModel 追加前过滤：只影响“之后加载/翻页追加”的数据。
     */
    fun <T> filterList(
        page: BlockPage,
        list: List<T>,
        midSelector: (T) -> Long?
    ): List<T> {
        if (!isPageEnabled(page)) return list
        if (blockedMids.isEmpty()) return list
        return list.filter { item -> !isBlocked(midSelector(item)) }
    }

    /**
     * 设置提交后调用（不发网络），让取消/启用立即影响后续追加。
     */
    fun reloadFromPrefs() {
        blockedMids = buildBlockedMidsFromPrefs()
    }

    /**
     * 用户手动触发的“立即更新”：
     * - 刷新 followingTotal/tags 缓存
     * - 对已选分组：只更新“需要更新”的分组成员（无缓存/空 mids/缓存 count != 最新 count）
     * - 支持取消：取消时保留已完成分组的结果，未完成分组不落盘
     */
    suspend fun updateByUser() = updateMutex.withLock {
        // 先从本地缓存初始化一次（即使网络失败也能先过滤）
        blockedMids = buildBlockedMidsFromPrefs()

        val uid = Prefs.uid
        val sessData = Prefs.sessData.takeIf { it.isNotBlank() }
        val accessKey = Prefs.accessToken.takeIf { it.isNotBlank() }
        val hasAuth = (sessData != null || accessKey != null) && uid != 0L
        if (!hasAuth) return

        // followingTotal：用于隐藏“全量分组”（count == followingTotal）
        val relationStat: RelationStat = BiliHttpApi.getRelationStat(
            mid = uid,
            accessKey = accessKey,
            sessData = sessData
        ).getResponseData()
        val followingTotal = relationStat.following
        Prefs.followingTotalCache = followingTotal

        // tags：用于设置页展示（会剔除全量分组后缓存）
        val tags: List<RelationTag> =
            BiliHttpApi.getRelationTags(accessKey = accessKey, sessData = sessData).getResponseData()
        val selectableTags = tags.filter { it.count != followingTotal }
        Prefs.followTagsCacheJson = json.encodeToString(
            FollowTagsCache.serializer(),
            FollowTagsCache(selectableTags.map { TagItem(it.tagid, it.name, it.count) })
        )

        // 更新所选分组成员缓存（只更新“需要更新”的）
        val selectedTagIds = Prefs.blockSelectedTagIds
        if (selectedTagIds.isEmpty()) return

        val tagsById = tags.associateBy { it.tagid }
        val membersCache = readMembersCache().toMutableMap()

        // 仅更新：无缓存 / 空 mids / count 不一致
        val needUpdateTagIds = selectedTagIds.filter { tagId ->
            val latestCount = tagsById[tagId]?.count ?: return@filter true
            val old = membersCache[tagId.toString()]
            old == null || old.mids.isEmpty() || old.count != latestCount
        }

        for (tagId in needUpdateTagIds) {
            val latestCount = tagsById[tagId]?.count ?: continue

            // 一次 tag 完整拉完才提交；中途取消/失败则丢弃本次临时数据
            val mids = fetchAllMembersMidsOrNull(
                tagId = tagId,
                accessKey = accessKey,
                sessData = sessData
            ) ?: continue

            membersCache[tagId.toString()] = TagMembersEntry(
                count = latestCount,
                updatedAt = System.currentTimeMillis(),
                mids = mids
            )

            // 每个分组成功后就立刻落盘，保证“退出取消”时已完成的结果保留
            writeMembersCache(membersCache)
            blockedMids = buildBlockedMidsFromPrefs()
        }
    }

    private suspend fun fetchAllMembersMidsOrNull(
        tagId: Int,
        accessKey: String?,
        sessData: String?
    ): List<Long>? {
        val mids = mutableListOf<Long>()
        val ps = 50
        var pn = 1
        while (true) {
            val pageMembers = try {
                BiliHttpApi.getRelationTagMembers(
                    tagId = tagId,
                    pageNumber = pn,
                    pageSize = ps,
                    orderType = null,
                    accessKey = accessKey,
                    sessData = sessData
                ).getResponseData()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Throwable) {
                return null
            }

            if (pageMembers.isEmpty()) break
            mids.addAll(pageMembers.map { it.mid })
            if (pageMembers.size < ps) break

            pn++
            delay(500) // 固定 500ms，降低风控/限流风险
        }
        return mids
    }

    // -------------------------
    // Prefs JSON/CSV 编解码
    // -------------------------

    private fun buildBlockedMidsFromPrefs(): Set<Long> {
        // 1) 优先 CSV：最快
        Prefs.blockedMidsCsv.takeIf { it.isNotBlank() }?.let { csv ->
            return csv.split(",")
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { it.toLongOrNull() }
                .toSet()
        }

        // 2) fallback：用 membersCacheJson + selectedTagIds 现算
        val selected = Prefs.blockSelectedTagIds.toSet()
        if (selected.isEmpty()) return emptySet()

        val cache = readMembersCache()
        val mids = HashSet<Long>(1024)
        for (tagId in selected) {
            cache[tagId.toString()]?.mids?.let { mids.addAll(it) }
        }
        return mids
    }

    private fun readMembersCache(): Map<String, TagMembersEntry> {
        val raw = Prefs.followTagMembersCacheJson
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            json.decodeFromString(TagMembersCache.serializer(), raw).data
        }.getOrDefault(emptyMap())
    }

    private fun writeMembersCache(map: Map<String, TagMembersEntry>) {
        Prefs.followTagMembersCacheJson = json.encodeToString(
            TagMembersCache.serializer(),
            TagMembersCache(map)
        )

        // 同步生成汇总 CSV（下次启动可直接用 CSV 恢复）
        val selected = Prefs.blockSelectedTagIds.toSet()
        val mids = HashSet<Long>(1024)
        for (tagId in selected) {
            map[tagId.toString()]?.mids?.let { mids.addAll(it) }
        }
        Prefs.blockedMidsCsv = mids.joinToString(",")
    }

    // -------------------------
    // 序列化结构
    // -------------------------

    @Serializable
    data class FollowTagsCache(
        val data: List<TagItem> = emptyList()
    )

    @Serializable
    data class TagItem(
        val tagid: Int,
        val name: String,
        val count: Int
    )

    @Serializable
    data class TagMembersCache(
        val data: Map<String, TagMembersEntry> = emptyMap()
    )

    @Serializable
    data class TagMembersEntry(
        val count: Int,
        @SerialName("updated_at")
        val updatedAt: Long,
        val mids: List<Long> = emptyList()
    )
}