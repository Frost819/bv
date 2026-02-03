package dev.aaa1115910.bv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.http.BiliHttpApi
import dev.aaa1115910.biliapi.http.entity.video.VideoInfo
import dev.aaa1115910.bv.block.BlockManager
import dev.aaa1115910.bv.block.BlockPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class TagViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TagUiState())
    val uiState = _uiState.asStateFlow()

    private val pageSize = 20
    private var pageNumber = 1
    private var updating = false

    fun setNameAndId(tagName: String, tagId: Int){
        _uiState.update {
            it.copy(
                tagName = tagName,
                tagId = tagId
            )
        }
    }

    fun update() {
        viewModelScope.launch(Dispatchers.IO) {
            updateTagVideos()
        }
    }

    private suspend fun updateTagVideos() {
        if (_uiState.value.noMore || updating) return

        try {
            updating = true
            _uiState.update { it.copy(loading = true) }

            val result = runCatching {
                BiliHttpApi.getTagTopVideos(
                    tagId = _uiState.value.tagId,
                    pageNumber = pageNumber,
                    pageSize = pageSize
                )
            }
            result.onSuccess { response ->
                val newData = response.data

                val filtered = BlockManager.filterList(
                    page = BlockPage.Tag,
                    list = newData
                ) { video -> video.owner.mid }

                _uiState.update {
                    it.copy(
                        videoList = it.videoList + filtered,
                        // 注意：noMore 仍然用“接口返回是否为空”判断，避免因为全被过滤导致误判没有更多页
                        noMore = newData.isEmpty(),
                        loading = false
                    )
                }
                pageNumber++
            }.onFailure { exception ->
                exception.printStackTrace()
                _uiState.update { it.copy(loading = false) }
            }
        } finally {
            updating = false
        }
    }
}

data class TagUiState(
    val tagName: String = "",
    val tagId: Int = 0,
    val noMore: Boolean = false,
    val loading: Boolean = false,
    val videoList: List<VideoInfo> = emptyList(),
)