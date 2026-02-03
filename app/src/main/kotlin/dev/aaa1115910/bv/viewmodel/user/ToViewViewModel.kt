package dev.aaa1115910.bv.viewmodel.user

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aaa1115910.biliapi.http.entity.AuthFailureException
import dev.aaa1115910.biliapi.repositories.ToViewRepository
import dev.aaa1115910.bv.BVApp
import dev.aaa1115910.bv.BuildConfig
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.repository.UserRepository
import dev.aaa1115910.bv.ui.effect.UiEffect
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.addWithMainContext
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.fWarn
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ToViewViewModel(
    private val userRepository: UserRepository,
    private val ToViewRepository: ToViewRepository
) : ViewModel() {
    private val _uiEffect = MutableSharedFlow<UiEffect>()
    val uiEvent = _uiEffect.asSharedFlow()

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var histories = mutableStateListOf<VideoCardData>()
    var noMore by mutableStateOf(false)

    private var cursor = 0L
    private var updating = false

    private var updateJob: Job? = null

    fun update() {
        updateJob = viewModelScope.launch(Dispatchers.IO) {
            updateToView()
        }
    }

    fun addToView(aid: Long, bvid: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ToViewRepository.addToView(aid = aid, bvid = bvid)
            }.onSuccess {
                _uiEffect.emit(UiEffect.ShowToast("添加到稍后再看"))
            }.onFailure {
                logger.fWarn { "Add toview failed: ${it.stackTraceToString()}" }
                _uiEffect.emit(UiEffect.ShowToast("添加到稍后再看失败"))
            }
        }
    }
    fun delToView(aid: Long, viewed: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ToViewRepository.delToView(viewed = viewed, aid = aid)
            }.onSuccess {
                histories.removeAll { it.avid == aid }
                _uiEffect.emit(UiEffect.ShowToast("删除稍后再看"))
            }.onFailure {
                logger.fWarn { "Delete toview failed: ${it.stackTraceToString()}" }
                _uiEffect.emit(UiEffect.ShowToast("删除稍后再看失败"))
            }
        }
    }

    fun clearData() {
        updateJob?.cancel()
        histories.clear()
        cursor = 0
        noMore = false
        updating = false
    }
    private suspend fun updateToView(context: Context = BVApp.context) {
        if (updating || noMore) return
        logger.fInfo { "Updating histories with params [cursor=$cursor, apiType=${Prefs.apiType}]" }
        updating = true
        runCatching {
            val data = ToViewRepository.getToView(
                cursor = cursor,
                preferApiType = Prefs.apiType
            )

            data.data.forEach { ToViewItem ->
                if (dev.aaa1115910.bv.block.BlockManager.isPageEnabled(dev.aaa1115910.bv.block.BlockPage.ToView)
                    && dev.aaa1115910.bv.block.BlockManager.isBlocked(ToViewItem.mid)
                ) return@forEach

                histories.addWithMainContext(
                    VideoCardData(
                        avid = ToViewItem.oid,
                        title = ToViewItem.title,
                        cover = ToViewItem.cover,
                        upName = ToViewItem.author,
                        upMid = ToViewItem.mid,
                        timeString = if (ToViewItem.progress == -1) context.getString(R.string.play_time_finish)
                        else context.getString(
                            R.string.play_time_history,
                            (ToViewItem.progress * 1000L).formatHourMinSec(),
                            (ToViewItem.duration * 1000L).formatHourMinSec()
                        )
                    )
                )
            }
            //update cursor
            cursor = data.cursor
            logger.fInfo { "Update toview cursor: [cursor=$cursor]" }
            logger.fInfo { "Update histories success" }
            if (cursor == 0L) {
                withContext(Dispatchers.Main) { noMore = true }
                logger.fInfo { "No more toview" }
            }
        }.onFailure {
            logger.fWarn { "Update histories failed: ${it.stackTraceToString()}" }
            when (it) {
                is AuthFailureException -> {
                    withContext(Dispatchers.Main) {
                        BVApp.context.getString(R.string.exception_auth_failure)
                            .toast(BVApp.context)
                    }
                    logger.fInfo { "User auth failure" }
                    if (!BuildConfig.DEBUG) userRepository.logout()
                }

                else -> {}
            }
        }
        updating = false
    }
}