package dev.aaa1115910.bv.viewmodel.live

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.http.BiliLiveHttpApi
import dev.aaa1115910.biliapi.http.entity.live.LiveAreaGroup
import dev.aaa1115910.biliapi.http.entity.live.LiveAreaItem
import dev.aaa1115910.biliapi.http.entity.live.LiveRoomItem
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class LiveViewModel : ViewModel() {
    private val logger = KotlinLogging.logger { }

    var areaGroups = mutableStateListOf<LiveAreaGroup>()
    
    var currentParentAreaId by mutableStateOf(-1)
    var currentAreaId by mutableStateOf(-1)

    var currentSubAreas = mutableStateListOf<LiveAreaItem>()

    var roomList = mutableStateListOf<LiveRoomItem>()
    var page = 1
    var hasMore = true
    var updating by mutableStateOf(false)

    init {
        fetchAreaList()
    }

    private fun fetchAreaList() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val response = BiliLiveHttpApi.getAreaList()
                withContext(Dispatchers.Main) {
                    areaGroups.clear()
                    areaGroups.addAll(response.data)

                    if (areaGroups.isNotEmpty()) {
                        val firstGroup = areaGroups.first()
                        onParentAreaSelect(firstGroup.id)
                    }
                }
            }.onFailure {
                logger.error(it) { "Failed to fetch area list" }
            }
        }
    }

    fun onParentAreaSelect(parentId: Int) {
        if (currentParentAreaId == parentId) return
        currentParentAreaId = parentId
        
        val group = areaGroups.find { it.id == parentId }
        currentSubAreas.clear()
        group?.list?.let { currentSubAreas.addAll(it) }

        if (currentSubAreas.isNotEmpty()) {
            val firstSubAreaId = currentSubAreas.first().id.toIntOrNull() ?: 0
            onAreaSelect(firstSubAreaId)
        }
    }

    fun onAreaSelect(areaId: Int) {
        if (currentAreaId == areaId) return
        currentAreaId = areaId
        page = 1
        hasMore = true
        roomList.clear()
        fetchRoomList()
    }

    fun fetchRoomList() {
        if (updating || !hasMore) return
        updating = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val response = BiliLiveHttpApi.getRoomList(
                    parentAreaId = currentParentAreaId,
                    areaId = currentAreaId,
                    page = page
                )
                if (response.data.isEmpty()) {
                    hasMore = false
                } else {
                    roomList.addAll(response.data)
                    page++
                }
            }.onFailure {
                logger.error(it) { "Failed to fetch room list" }
            }.onSuccess {
                updating = false
            }
        }
    }
}
