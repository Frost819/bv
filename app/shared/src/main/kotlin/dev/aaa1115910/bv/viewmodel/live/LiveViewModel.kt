package dev.aaa1115910.bv.viewmodel.live

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.live.LiveAreaItem
import dev.aaa1115910.biliapi.entity.live.LiveRoomItem
import dev.aaa1115910.biliapi.repositories.LiveRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

/**
 * 直播页面的显示模式
 */
enum class LiveMode {
    /** 推荐直播 */
    RECOMMEND,
    /** 关注的直播 */
    FOLLOWING,
    /** 分区直播 */
    AREA
}

@KoinViewModel
class LiveViewModel(
    private val liveRepository: LiveRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger("LiveViewModel")

    /**
     * 当前直播模式
     */
    var currentMode by mutableStateOf(LiveMode.RECOMMEND)
        private set

    /**
     * 主分区列表（父分区组）
     */
    val parentAreaGroups = mutableStateListOf<dev.aaa1115910.biliapi.entity.live.LiveAreaGroup>()

    /**
     * 当前选中的主分区组
     */
    var currentParentGroup by mutableStateOf<dev.aaa1115910.biliapi.entity.live.LiveAreaGroup?>(null)

    /**
     * 当前主分区下的子分区列表
     */
    val subAreaList = mutableStateListOf<LiveAreaItem>()

    /**
     * 当前选中的子分区
     */
    var currentSubArea by mutableStateOf<LiveAreaItem?>(null)

    /**
     * 当前分区的直播间列表
     */
    val roomList = mutableStateListOf<LiveRoomItem>()

    /**
     * 是否正在加载
     */
    var loading by mutableStateOf(false)

    /**
     * 是否有下一页
     */
    var hasMore by mutableStateOf(true)

    /**
     * 当前页码
     */
    private var currentPage = 1

    /**
     * 上次聚焦的直播间索引（用于从播放器返回时恢复焦点）
     */
    var lastFocusedRoomIndex by mutableStateOf(0)

    /** 是否已登录（用于判断是否显示关注tab） */
    val isLoggedIn: Boolean
        get() = !liveRepository.sessionData.isNullOrBlank()

    init {
        loadAreas()
        // 默认加载推荐
        loadRooms(refresh = true)
    }

    /**
     * 切换到推荐模式
     */
    fun switchToRecommend() {
        if (currentMode == LiveMode.RECOMMEND) return
        currentMode = LiveMode.RECOMMEND
        currentParentGroup = null
        subAreaList.clear()
        currentSubArea = null
        loadRooms(refresh = true)
    }

    /**
     * 切换到关注模式
     */
    fun switchToFollowing() {
        if (currentMode == LiveMode.FOLLOWING) return
        currentMode = LiveMode.FOLLOWING
        currentParentGroup = null
        subAreaList.clear()
        currentSubArea = null
        loadRooms(refresh = true)
    }

    /**
     * 加载所有分区
     */
    fun loadAreas() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val response = liveRepository.getLiveAreaList()
                if (response.code == 0) {
                    withContext(Dispatchers.Main) {
                        parentAreaGroups.clear()
                        parentAreaGroups.addAll(response.data)
                    }
                    logger.info { "Loaded ${response.data.size} parent area groups" }
                } else {
                    withContext(Dispatchers.Main) {
                        "加载直播分区失败: ${response.message}".toast(BVApp.context)
                    }
                }
            }.onFailure { e ->
                logger.error(e) { "Failed to load live areas" }
                withContext(Dispatchers.Main) {
                    "加载直播分区失败: ${e.message}".toast(BVApp.context)
                }
            }
        }
    }

    /**
     * 切换主分区（进入分区模式）
     */
    fun switchParentArea(group: dev.aaa1115910.biliapi.entity.live.LiveAreaGroup) {
        currentMode = LiveMode.AREA
        if (currentParentGroup?.id == group.id) return
        currentParentGroup = group
        subAreaList.clear()
        // 添加"全部"分区项
        subAreaList.add(LiveAreaItem(id = "0", parentId = group.id.toString(), oldAreaId = "0", name = "全部", pic = "", parentName = group.name, areaType = 0))
        subAreaList.addAll(group.list)
        // 默认选中第一个子分区
        if (subAreaList.isNotEmpty()) {
            currentSubArea = subAreaList[0]
            loadRooms(refresh = true)
        }
    }

    /**
     * 切换子分区
     */
    fun switchSubArea(area: LiveAreaItem) {
        if (currentSubArea?.id == area.id) return
        currentSubArea = area
        loadRooms(refresh = true)
    }

    /**
     * 加载直播间列表
     * @param refresh 是否刷新（清空现有数据）
     */
    fun loadRooms(refresh: Boolean = false) {
        if (loading) return

        viewModelScope.launch(Dispatchers.IO) {
            loading = true
            if (refresh) {
                currentPage = 1
                withContext(Dispatchers.Main) {
                    roomList.clear()
                    hasMore = true
                }
            }

            runCatching {
                when (currentMode) {
                    LiveMode.RECOMMEND -> loadRecommendRooms()
                    LiveMode.FOLLOWING -> loadFollowingRooms()
                    LiveMode.AREA -> loadAreaRooms()
                }
            }.onFailure { e ->
                logger.error(e) { "Failed to load live rooms" }
                withContext(Dispatchers.Main) {
                    "加载直播间列表失败: ${e.message}".toast(BVApp.context)
                }
            }
            loading = false
        }
    }

    private suspend fun loadRecommendRooms() {
        val response = liveRepository.getLiveRecommendList(page = currentPage)
        if (response.code == 0) {
            val newItems = response.data?.recommendRoomList?.map { it.toLiveRoomItem() } ?: emptyList()
            withContext(Dispatchers.Main) {
                val existingIds = roomList.map { it.roomId }.toHashSet()
                val filtered = newItems.filter { it.roomId !in existingIds }
                roomList.addAll(filtered)
                hasMore = filtered.isNotEmpty()
                if (hasMore) currentPage++
            }
            logger.info { "Loaded ${newItems.size} recommend rooms, page $currentPage" }
        } else {
            withContext(Dispatchers.Main) {
                "加载推荐直播失败: ${response.message}".toast(BVApp.context)
            }
        }
    }

    private suspend fun loadFollowingRooms() {
        val response = liveRepository.getLiveFollowingList(page = currentPage, pageSize = 30)
        if (response.code == 0) {
            val data = response.data
            val newItems = data?.list
                ?.map { it.toLiveRoomItem() }
                ?: emptyList()
            withContext(Dispatchers.Main) {
                val existingIds = roomList.map { it.roomId }.toHashSet()
                val filtered = newItems.filter { it.roomId !in existingIds }
                roomList.addAll(filtered)
                // 如果本页数据全部重复，即使还有下一页也停止加载
                hasMore = filtered.isNotEmpty() && data != null && currentPage < data.totalPage
                if (hasMore) currentPage++
            }
            logger.info { "Loaded ${newItems.size} following rooms, page $currentPage" }
        } else {
            withContext(Dispatchers.Main) {
                "加载关注直播失败: ${response.message}".toast(BVApp.context)
            }
        }
    }

    private suspend fun loadAreaRooms() {
        val area = currentSubArea ?: return
        val response = liveRepository.getLiveRoomList(
            parentAreaId = area.parentId,
            areaId = area.id,
            page = currentPage,
            pageSize = 30
        )
        if (response.code == 0) {
            withContext(Dispatchers.Main) {
                val existingIds = roomList.map { it.roomId }.toHashSet()
                val newRooms = response.data.list.filter { it.roomId !in existingIds }
                roomList.addAll(newRooms)
                hasMore = newRooms.isNotEmpty()
                if (hasMore) {
                    currentPage++
                }
            }
            logger.info { "Loaded ${response.data.list.size} rooms for area ${area.name}, page $currentPage" }
        } else {
            withContext(Dispatchers.Main) {
                "加载直播间列表失败: ${response.message}".toast(BVApp.context)
            }
        }
    }

    /**
     * 刷新当前分区的直播间列表
     */
    fun refresh() {
        loadRooms(refresh = true)
    }

    /**
     * 加载更多直播间
     */
    fun loadMore() {
        if (hasMore && !loading) {
            loadRooms(refresh = false)
        }
    }
}
