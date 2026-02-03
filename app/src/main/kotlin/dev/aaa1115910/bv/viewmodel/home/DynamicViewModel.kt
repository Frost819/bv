package dev.aaa1115910.bv.viewmodel.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.aaa1115910.biliapi.entity.user.DynamicVideo
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.repositories.UserRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.addAllWithMainContext
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.aaa1115910.bv.repository.UserRepository as BvUserRepository
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class DynamicViewModel(
    private val bvUserRepository: BvUserRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val logger = KotlinLogging.logger {}
    val dynamicList = mutableStateListOf<DynamicVideo>()

    private var currentPage = 0
    var loading by mutableStateOf(false)
        private set

    var hasMore by mutableStateOf(true)
        private set

    private var historyOffset: String? = null
    private var updateBaseline: String? = null
    val isLogin get() = bvUserRepository.isLogin

    suspend fun loadMore() {
        if (!loading) loadData()
    }

    private suspend fun loadData() {
        if (!hasMore || !bvUserRepository.isLogin) return
        if (loading) return

        loading = true
        val nextPage = currentPage + 1

        try {
            logger.fInfo { "Load dynamic page: $nextPage, offset=$historyOffset" }

            val data = userRepository.getDynamicVideos(
                page = nextPage,
                offset = historyOffset.orEmpty(),
                updateBaseline = updateBaseline.orEmpty(),
                preferApiType = Prefs.apiType
            )

            currentPage = nextPage
            val filtered = dev.aaa1115910.bv.block.BlockManager.filterList(
                page = dev.aaa1115910.bv.block.BlockPage.Dynamics,
                list = data.videos
            ) { it.authorMid }
            dynamicList.addAllWithMainContext(filtered)

            historyOffset = data.historyOffset
            updateBaseline = data.updateBaseline
            hasMore = data.hasMore

            logger.fInfo { "Loaded page=$currentPage size=${data.videos.size}" }

        } catch (e: Exception) {
            logger.fWarn { "Load dynamic failed: ${e.stackTraceToString()}" }

            when (e) {
                is AuthFailureException -> {
                    withContext(Dispatchers.Main) {
                        BVApp.context.getString(R.string.exception_auth_failure)
                            .toast(BVApp.context)
                    }
                    if (!BuildConfig.DEBUG) bvUserRepository.logout()
                }

                else -> {
                    withContext(Dispatchers.Main) {
                        "加载动态失败: ${e.localizedMessage}".toast(BVApp.context)
                    }
                }
            }

        } finally {
            loading = false
        }
    }

    fun clearData() {
        dynamicList.clear()
        currentPage = 0
        loading = false
        hasMore = true
        historyOffset = null
    }
}