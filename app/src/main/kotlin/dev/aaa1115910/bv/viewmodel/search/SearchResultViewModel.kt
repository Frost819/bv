package dev.aaa1115910.bv.viewmodel.search

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.repositories.SearchFilterDuration
import dev.aaa1115910.biliapi.repositories.SearchFilterOrderType
import dev.aaa1115910.biliapi.repositories.SearchRepository
import dev.aaa1115910.biliapi.repositories.SearchType
import dev.aaa1115910.biliapi.repositories.SearchTypePage
import dev.aaa1115910.biliapi.repositories.SearchTypeResult
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.util.Partition
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.fInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class SearchResultViewModel(
    private val searchRepository: SearchRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var keyword by mutableStateOf("")
    var searchType by mutableStateOf(SearchType.Video)

    var videoSearchResult by mutableStateOf(SearchResult(SearchType.Video))
    var mediaBangumiSearchResult by mutableStateOf(SearchResult(SearchType.MediaBangumi))
    var mediaFtSearchResult by mutableStateOf(SearchResult(SearchType.MediaFt))
    var biliUserSearchResult by mutableStateOf(SearchResult(SearchType.BiliUser))

    var selectedOrder by mutableStateOf(SearchFilterOrderType.ComprehensiveSort)
    var selectedDuration by mutableStateOf(SearchFilterDuration.All)
    var selectedPartition: Partition? by mutableStateOf(null)
    var selectedChildPartition: Partition? by mutableStateOf(null)

    private var updating = false
    private val hasMore = true

    var enableProxySearchResult = false

    fun update() {
        resetPages()
        clearResults()
        SearchType.entries.forEach { loadMore(it, true) }
    }

    private fun resetPages() {
        videoSearchResult = videoSearchResult.resetPage()
        mediaBangumiSearchResult = mediaBangumiSearchResult.resetPage()
        mediaFtSearchResult = mediaFtSearchResult.resetPage()
        biliUserSearchResult = biliUserSearchResult.resetPage()
    }

    private fun clearResults() {
        videoSearchResult = videoSearchResult.clear()
        mediaBangumiSearchResult = mediaBangumiSearchResult.clear()
        mediaFtSearchResult = mediaFtSearchResult.clear()
        biliUserSearchResult = biliUserSearchResult.clear()
    }

    fun loadMore(
        searchType: SearchType,
        ignoreUpdating: Boolean = false
    ) {
        if (!hasMore) return
        if (updating && !ignoreUpdating) return

        updating = true
        viewModelScope.launch(Dispatchers.IO) {
            val page = when (searchType) {
                SearchType.Video -> videoSearchResult.page
                SearchType.MediaBangumi -> mediaBangumiSearchResult.page
                SearchType.MediaFt -> mediaFtSearchResult.page
                SearchType.BiliUser -> biliUserSearchResult.page
            }
            logger.fInfo { "Load search result: [keyword=$keyword, type=$searchType, page=${page}]" }
            runCatching {
                val searchResultResponse = searchRepository.searchType(
                    keyword = keyword,
                    type = searchType,
                    page = page,
                    tid = selectedChildPartition?.tid ?: selectedPartition?.tid,
                    order = selectedOrder,
                    duration = selectedDuration,
                    preferApiType = Prefs.apiType,
                    enableProxy = enableProxySearchResult
                )

                val filteredSearchResultResponse = if (searchType == SearchType.Video
                    && dev.aaa1115910.bv.block.BlockManager.isPageEnabled(dev.aaa1115910.bv.block.BlockPage.SearchVideo)
                ) {
                    searchResultResponse.copy(
                        videos = searchResultResponse.videos.filter { v ->
                            !dev.aaa1115910.bv.block.BlockManager.isBlocked(v.mid)
                        }
                    )
                } else {
                    searchResultResponse
                }

                withContext(Dispatchers.Main) {
                    when (searchType) {
                        SearchType.Video -> {
                            videoSearchResult = videoSearchResult.appendSearchResultData(filteredSearchResultResponse)

                        }

                        SearchType.MediaBangumi -> {
                            mediaBangumiSearchResult = mediaBangumiSearchResult.appendSearchResultData(searchResultResponse)
                        }

                        SearchType.MediaFt -> {
                            mediaFtSearchResult = mediaFtSearchResult.appendSearchResultData(searchResultResponse)
                        }

                        SearchType.BiliUser -> {
                            biliUserSearchResult = biliUserSearchResult.appendSearchResultData(searchResultResponse)
                        }
                    }
                }
            }
            updating = false
        }
    }

    data class SearchResult(
        val type: SearchType,
        val videos: List<SearchTypeResult.Video> = emptyList(),
        val mediaBangumis: List<SearchTypeResult.Pgc> = emptyList(),
        val mediaFts: List<SearchTypeResult.Pgc> = emptyList(),
        val biliUsers: List<SearchTypeResult.User> = emptyList(),
        val page: SearchTypePage = SearchTypePage()
    ) {
        val count get() = videos.size + mediaBangumis.size + mediaFts.size + biliUsers.size

        fun resetPage() = copy(page = SearchTypePage())

        fun clear() :SearchResult = copy(
            videos = emptyList(),
            mediaBangumis = emptyList(),
            mediaFts = emptyList(),
            biliUsers = emptyList(),
            page = SearchTypePage()
        )

        fun appendSearchResultData(searchTypeResult: SearchTypeResult): SearchResult {
            return when (type) {
                SearchType.Video -> copy(
                    videos = videos + searchTypeResult.videos,
                    page = searchTypeResult.page
                )
                SearchType.MediaBangumi -> copy(
                    mediaBangumis = mediaBangumis + searchTypeResult.pgcs,
                    page = searchTypeResult.page
                )
                SearchType.MediaFt -> copy(
                    mediaFts = mediaFts + searchTypeResult.pgcs,
                    page = searchTypeResult.page
                )
                SearchType.BiliUser -> copy(
                    biliUsers = biliUsers + searchTypeResult.users,
                    page = searchTypeResult.page
                )
            }
        }

    }
}

enum class SearchResultType(
    val type: String,
    private val strRes: Int
) {
    Video(type = "video", strRes = R.string.search_result_type_name_video),
    MediaBangumi(type = "media_bangumi", R.string.search_result_type_name_media_bangumi),
    MediaFt(type = "media_ft", strRes = R.string.search_result_type_name_media_ft),
    BiliUser(type = "bili_user", strRes = R.string.search_result_type_name_bili_user);

    fun getDisplayName(context: Context) = context.getString(strRes)
}
