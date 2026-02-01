package dev.aaa1115910.bv.viewmodel.user

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.FavoriteFolderMetadata
import dev.aaa1115910.biliapi.entity.FavoriteItemType
import dev.aaa1115910.biliapi.repositories.FavoriteRepository
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.addWithMainContext
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.swapList
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import kotlin.inc
import kotlin.random.Random
import kotlin.times

@KoinViewModel
class FavoriteViewModel(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var favoriteFolderMetadataList = mutableStateListOf<FavoriteFolderMetadata>()
    var favorites = mutableStateListOf<VideoCardData>()

    var currentFavoriteFolderMetadata: FavoriteFolderMetadata? by mutableStateOf(null)

    private val pageSize = 20
    private var pageNumber = 1
    private var hasMore = true

    private var updatingFolders = false
    private var updatingFolderItems = false

    private var updateFoldersJob: Job? = null
    private var updateItemsJob: Job? = null

    // 1s 悬停 gate + 后台渐进加载
    // UI 在“Tab 按钮停留满 1s”后才会置 true
    var allowAutoLoad by mutableStateOf(false)

    // Dialog 打开时暂停加载；关闭后继续
    var loadingPaused by mutableStateOf(false)
        private set

    fun updateLoadingPaused(paused: Boolean) {
        loadingPaused = paused
    }

    var isAutoLoading by mutableStateOf(false)
        private set

    private var autoLoadJob: Job? = null
    private val requestMutex = Mutex()

    init {
        updateFoldersInfo()
    }

    fun clearData(){
        updateFoldersJob?.cancel()
        updateItemsJob?.cancel()
        autoLoadJob?.cancel()

        favoriteFolderMetadataList.clear()
        favorites.clear()
        currentFavoriteFolderMetadata = null
        updatingFolders = false
        updatingFolderItems = false
        allowAutoLoad = false
        isAutoLoading = false
        resetPageNumber()
    }

    fun stopAutoLoad() {
        // 切换收藏夹时：必须立刻停
        autoLoadJob?.cancel()
        updateItemsJob?.cancel()
        autoLoadJob = null
        updateItemsJob = null

        isAutoLoading = false
        updatingFolderItems = false

        // 退出/切换时确保不处于暂停态
        loadingPaused = false

        // gate 由 UI 再次满足 1s 后开启
        allowAutoLoad = false
    }

    fun updateFoldersInfo() {
        if (updatingFolders) return
        updatingFolders = true
        logger.fInfo { "Updating favorite folders" }
        updateFoldersJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val favoriteFolderMetadataList =
                    favoriteRepository.getAllFavoriteFolderMetadataList(
                        mid = Prefs.uid,
                        preferApiType = Prefs.apiType
                    )
                withContext(Dispatchers.Main) {
                    this@FavoriteViewModel.favoriteFolderMetadataList
                        .swapList(favoriteFolderMetadataList)
                    currentFavoriteFolderMetadata = favoriteFolderMetadataList.firstOrNull()
                }
                logger.fInfo { "Update favorite folders success: ${favoriteFolderMetadataList.map { it.id }}" }
            }.onFailure {
                logger.fWarn { "Update favorite folders failed: ${it.stackTraceToString()}" }
                //这里返回的数据并不会有用户认证失败的错误返回，没必要做身份验证失败提示
            }.onSuccess {
                updateFolderItems()
            }
            updatingFolders = false
        }
    }

    fun startAutoLoad() {
        if (!allowAutoLoad) return
        if (autoLoadJob?.isActive == true) return

        val expectedFolderId = currentFavoriteFolderMetadata?.id ?: return

        autoLoadJob = viewModelScope.launch(Dispatchers.Default) {
            isAutoLoading = true

            var backoffMs = 0L
            while (isActive) {
                // Dialog 打开时暂停：不发起新的请求，也不进入退避
                while (isActive && loadingPaused) {
                    delay(100)
                }

                if (!allowAutoLoad) {
                    delay(100)
                    continue
                }

                // 切换收藏夹后必须立刻停：folder id 不一致就退出
                if (currentFavoriteFolderMetadata?.id != expectedFolderId) break
                if (!hasMore) break

                val ok = loadNextPage(expectedFolderId = expectedFolderId)
                if (currentFavoriteFolderMetadata?.id != expectedFolderId) break
                if (!hasMore) break

                if (ok) {
                    backoffMs = 0L
                    // 保守节流 + 抖动
                    delay(Random.nextLong(1000L, 3000L))
                } else {
                    // 失败退避：5s 起步，指数增长，上限 60s
                    backoffMs = if (backoffMs == 0L) 5_000L else (backoffMs * 2).coerceAtMost(60_000L)
                    delay(backoffMs)
                }
            }

            isAutoLoading = false
        }
    }

    fun updateFolderItems(force: Boolean = false) {
        if (force) {
            updateItemsJob?.cancel()
            resetPageNumber()
            updatingFolderItems = false
        }

        // Dialog 打开时暂停：不允许从任何入口触发新的请求
        if (loadingPaused) return

        val expectedFolderId = currentFavoriteFolderMetadata?.id ?: return

        // 这里保留原来的“单次请求”入口（即使目前不再用 snapshotFlow 触底）
        updateItemsJob = viewModelScope.launch(Dispatchers.Default) {
            loadNextPage(expectedFolderId = expectedFolderId)
        }
    }

        /*
        if (updatingFolderItems || !hasMore) return
        updatingFolderItems = true
        logger.fInfo { "Updating favorite folder items with media id: ${currentFavoriteFolderMetadata?.id}" }
        updateItemsJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val favoriteFolderData = favoriteRepository.getFavoriteFolderData(
                    mediaId = currentFavoriteFolderMetadata!!.id,
                    pageSize = pageSize,
                    pageNumber = pageNumber,
                    preferApiType = Prefs.apiType
                )
                favoriteFolderData.medias.forEach { favoriteItem ->
                    if (favoriteItem.type != FavoriteItemType.Video) return@forEach
                    favorites.addWithMainContext(
                        VideoCardData(
                            avid = favoriteItem.id,
                            title = favoriteItem.title,
                            cover = favoriteItem.cover,
                            upName = favoriteItem.upper.name,
                            upMid = favoriteItem.upper.mid,
                            time = favoriteItem.duration * 1000L
                        )
                    )
                }
                hasMore = favoriteFolderData.hasMore
                logger.fInfo { "Update favorite items success" }
            }.onFailure {
                logger.fInfo { "Update favorite items failed: ${it.stackTraceToString()}" }
            }.onSuccess {
                pageNumber++
            }
            updatingFolderItems = false
        }
        */

    fun resetPageNumber() {
        pageNumber = 1
        hasMore = true
    }
    private suspend fun loadNextPage(expectedFolderId: Long): Boolean = requestMutex.withLock {
        if (loadingPaused) return false

        val folder = currentFavoriteFolderMetadata ?: return false
        if (folder.id != expectedFolderId) return false

        if (updatingFolderItems || !hasMore) return false
        updatingFolderItems = true

        logger.fInfo { "Updating favorite folder items with media id: ${folder.id}" }

        return try {
            val favoriteFolderData = withContext(Dispatchers.IO) {
                favoriteRepository.getFavoriteFolderData(
                    mediaId = folder.id,
                    pageSize = pageSize,
                    pageNumber = pageNumber,
                    preferApiType = Prefs.apiType
                )
            }

            // 只在“仍然是同一个 folder”时才落地数据，避免切换收藏夹后串数据
            withContext(Dispatchers.Main) {
                if (currentFavoriteFolderMetadata?.id != expectedFolderId) return@withContext

                favoriteFolderData.medias.forEach { favoriteItem ->
                    if (favoriteItem.type != FavoriteItemType.Video) return@forEach
                    favorites.addWithMainContext(
                        VideoCardData(
                            avid = favoriteItem.id,
                            title = favoriteItem.title,
                            cover = favoriteItem.cover,
                            upName = favoriteItem.upper.name,
                            upMid = favoriteItem.upper.mid,
                            time = favoriteItem.duration * 1000L
                        )
                    )
                }
            }

            hasMore = favoriteFolderData.hasMore
            pageNumber++

            logger.fInfo { "Update favorite items success" }
            true
        } catch (t: Throwable) {
            logger.fInfo { "Update favorite items failed: ${t.stackTraceToString()}" }
            false
        } finally {
            updatingFolderItems = false
        }
    }
}