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

@KoinViewModel
class LiveViewModel(
    private val liveRepository: LiveRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger("LiveViewModel")

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

    init {
        loadAreas()
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
                        // 默认选中第一个主分区和其第一个子分区
                        if (response.data.isNotEmpty() && currentParentGroup == null) {
                            val firstGroup = response.data[0]
                            currentParentGroup = firstGroup
                            subAreaList.clear()
                            subAreaList.addAll(firstGroup.list)
                            if (firstGroup.list.isNotEmpty()) {
                                currentSubArea = firstGroup.list[0]
                                loadRooms(refresh = true)
                            }
                        }
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
     * 切换主分区
     */
    fun switchParentArea(group: dev.aaa1115910.biliapi.entity.live.LiveAreaGroup) {
        if (currentParentGroup?.id == group.id) return
        currentParentGroup = group
        subAreaList.clear()
        subAreaList.addAll(group.list)
        // 默认选中第一个子分区
        if (group.list.isNotEmpty()) {
            currentSubArea = group.list[0]
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
        val area = currentSubArea ?: return
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
                val response = liveRepository.getLiveRoomList(
                    parentAreaId = area.parentId,
                    areaId = area.id,
                    page = currentPage,
                    pageSize = 30
                )
                if (response.code == 0) {
                    withContext(Dispatchers.Main) {
                        roomList.addAll(response.data)
                        hasMore = response.data.size >= 30
                        if (hasMore) {
                            currentPage++
                        }
                    }
                    logger.info { "Loaded ${response.data.size} rooms for area ${area.name}, page $currentPage" }
                } else {
                    withContext(Dispatchers.Main) {
                        "加载直播间列表失败: ${response.message}".toast(BVApp.context)
                    }
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
