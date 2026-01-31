package dev.aaa1115910.bv.viewmodel.user

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.entity.user.SpaceVideoPage
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.addWithMainContext
import dev.aaa1115910.bv.util.fInfo
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
import kotlin.random.Random

@KoinViewModel
class UpInfoViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var upName by mutableStateOf("")
    var upMid by mutableLongStateOf(0L)
    var spaceVideos = mutableStateListOf<VideoCardData>()

    // Web: page.count; App: count。取不到时为 null
    var totalCount by mutableStateOf<Int?>(null)
        private set

    // 输入：raw 立即更新；debounced 900ms 后才更新（用于真正过滤）
    var rawQuery by mutableStateOf("")
        private set
    var debouncedQuery by mutableStateOf("")
        private set

    // 自动加载控制：输入时暂停（false），停输入防抖结束后恢复（true）
    var autoLoadEnabled by mutableStateOf(true)
        private set

    // 顶部展示用
    var isAutoLoading by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set

    private var page = SpaceVideoPage()
    private var updating = false
    val noMore get() = !page.hasNext

    // 防止并发请求、并做按 avid 去重
    private val requestMutex = Mutex()
    private val loadedAvids = mutableSetOf<Long>()

    private var queryDebounceJob: Job? = null
    private var autoLoadJob: Job? = null

    // 进入页面时冻结一次，避免 Web/App 混用导致分页游标混乱
    private var preferApiType: ApiType = Prefs.apiType

    fun reset(mid: Long, name: String) {
        upMid = mid
        upName = name

        preferApiType = Prefs.apiType

        rawQuery = ""
        debouncedQuery = ""
        autoLoadEnabled = true

        isAutoLoading = false
        loading = false
        totalCount = null

        page = SpaceVideoPage()
        spaceVideos.clear()
        loadedAvids.clear()

        queryDebounceJob?.cancel()
        autoLoadJob?.cancel()
        queryDebounceJob = null
        autoLoadJob = null
    }

    fun onQueryChange(newText: String) {
        rawQuery = newText
        pauseAutoLoad()

        queryDebounceJob?.cancel()
        queryDebounceJob = viewModelScope.launch {
            delay(900)
            debouncedQuery = rawQuery
            resumeAutoLoad()
        }
    }

    fun onSearchAction() {
        // 用户按“搜索”视为输入完成：立即应用过滤并恢复自动加载
        queryDebounceJob?.cancel()
        debouncedQuery = rawQuery
        resumeAutoLoad()
    }

    private fun pauseAutoLoad() {
        autoLoadEnabled = false
    }

    private fun resumeAutoLoad() {
        autoLoadEnabled = true
        startAutoLoad()
    }

    fun startAutoLoad() {
        if (autoLoadJob?.isActive == true) return

        autoLoadJob = viewModelScope.launch(Dispatchers.Default) {
            isAutoLoading = true

            var backoffMs = 0L
            while (isActive && !noMore) {
                // 输入时暂停：允许当前请求跑完，但不进入下一轮
                while (isActive && !autoLoadEnabled) {
                    delay(100)
                }
                if (!isActive || noMore) break

                val ok = loadNextPage()
                if (noMore) break

                if (ok) {
                    backoffMs = 0L
                    // 保守节流 + 抖动，降低风控风险
                    delay(Random.nextLong(1500L, 3000L))
                } else {
                    // 失败退避：5s 起步，指数增长，上限 60s
                    backoffMs = if (backoffMs == 0L) 5_000L else (backoffMs * 2).coerceAtMost(60_000L)
                    delay(backoffMs)
                }
            }

            isAutoLoading = false
        }
    }

    fun update() {
        viewModelScope.launch(Dispatchers.Default) {
            updateSpaceVideos()
        }
    }

    suspend fun loadNextPage(): Boolean = requestMutex.withLock {
        if (loading || noMore) return false

        loading = true
        return try {
            logger.fInfo { "Updating up [mid=$upMid] space videos from page $page (api=$preferApiType)" }

            val spaceVideoData = withContext(Dispatchers.IO) {
                userRepository.getSpaceVideos(
                    mid = upMid,
                    page = page,
                    preferApiType = preferApiType
                )
            }

            // totalCount 取不到就保留旧值
            spaceVideoData.totalCount?.takeIf { it > 0 }?.let { totalCount = it }

            // 构造 card 列表
            val newCards = spaceVideoData.videos.map { spaceVideoItem ->
                VideoCardData(
                    avid = spaceVideoItem.aid,
                    title = spaceVideoItem.title,
                    // TODO 这里在改造 app 端接口时，没找到在空间内显示为合集样式封面的UP,没法进一步测试接口
                    cover = spaceVideoItem.cover,
                    upName = spaceVideoItem.author,
                    play = with(spaceVideoItem.play) { if (this == -1) null else this },
                    danmaku = with(spaceVideoItem.danmaku) { if (this == -1) null else this },
                    time = spaceVideoItem.duration * 1000L,
                    pubTime = spaceVideoItem.pubTime
                )
            }

            // 去重 + 主线程 add
            withContext(Dispatchers.Main) {
                newCards.forEach { card ->
                    if (loadedAvids.add(card.avid)) {
                        spaceVideos.add(card)
                    }
                }
            }

            page = spaceVideoData.page
            true
        } catch (t: Throwable) {
            logger.fInfo { "Update up space videos failed: ${t.stackTraceToString()}" }
            false
        } finally {
            loading = false
        }
    }

    private suspend fun updateSpaceVideos() {
        if (updating || noMore) return
        logger.fInfo { "Updating up [mid=$upMid] space videos from page $page" }
        updating = true
        runCatching {
            val spaceVideoData = userRepository.getSpaceVideos(
                mid = upMid,
                page = page,
                preferApiType = Prefs.apiType
            )
            spaceVideoData.videos.forEach { spaceVideoItem ->
                spaceVideos.addWithMainContext(
                    VideoCardData(
                        avid = spaceVideoItem.aid,
                        title = spaceVideoItem.title,
                        //TODO 这里在改造 app 端接口时，没找到在空间内显示为合集样式封面的UP,没法进一步测试接口
                        cover = spaceVideoItem.cover,
                        upName = spaceVideoItem.author,
                        play = with(spaceVideoItem.play) { if (this == -1) null else this },
                        danmaku = with(spaceVideoItem.danmaku) { if (this == -1) null else this },
                        time = spaceVideoItem.duration * 1000L,
                        pubTime = spaceVideoItem.pubTime
                    )
                )
            }
            page = spaceVideoData.page
            logger.fInfo { "Update up space videos success" }
        }.onFailure {
            logger.fInfo { "Update up space videos failed: ${it.stackTraceToString()}" }
        }
        updating = false
    }
}