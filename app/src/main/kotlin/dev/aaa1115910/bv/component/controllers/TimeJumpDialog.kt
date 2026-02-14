package dev.aaa1115910.bv.component.controllers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.util.toast
import kotlinx.coroutines.delay

@Composable
fun TimeJumpDialog(
    show: Boolean,
    durationMs: Long,
    onDismiss: () -> Unit,
    onGoTime: (Long) -> Unit
) {
    if (!show) return

    val context = LocalContext.current

    var selectedField by remember { mutableIntStateOf(0) } // 0=时, 1=分, 2=秒

    var hValue by remember { mutableIntStateOf(0) }
    var hDirty by remember { mutableStateOf(false) }

    var mValue by remember { mutableIntStateOf(0) }
    var mDirty by remember { mutableStateOf(false) }

    var sValue by remember { mutableIntStateOf(0) }
    var sDirty by remember { mutableStateOf(false) }

    var confirmed by remember { mutableStateOf(false) }

    fun setSelectedDirtyValue(value: Int, dirty: Boolean) {
        when (selectedField) {
            0 -> {
                hValue = value
                hDirty = dirty
            }
            1 -> {
                mValue = value
                mDirty = dirty
            }
            else -> {
                sValue = value
                sDirty = dirty
            }
        }
    }

    fun getSelectedDirtyValue(): Pair<Int, Boolean> {
        return when (selectedField) {
            0 -> hValue to hDirty
            1 -> mValue to mDirty
            else -> sValue to sDirty
        }
    }

    fun selectPrev() {
        selectedField = (selectedField + 2) % 3
    }

    fun selectNext() {
        selectedField = (selectedField + 1) % 3
    }

    fun appendDigit(digit: Int) {
        val (curValue, curDirty) = getSelectedDirtyValue()
        val base = if (!curDirty) 0 else curValue

        val next = when (selectedField) {
            0 -> {
                // 时：最多 3 位
                if (!curDirty) digit
                else if (base <= 99) base * 10 + digit else base
            }
            else -> {
                // 分/秒：追加后 clamp 到 59
                val appended = if (!curDirty) digit else base * 10 + digit
                appended.coerceAtMost(59)
            }
        }

        setSelectedDirtyValue(next, true)
    }

    fun setPreset(preset: Int) {
        val next = when (selectedField) {
            0 -> preset.coerceAtMost(999)
            else -> preset.coerceAtMost(59)
        }
        setSelectedDirtyValue(next, true)
    }

    fun deleteOne() {
        val (curValue, curDirty) = getSelectedDirtyValue()
        if (!curDirty) return

        if (curValue < 10) {
            // 删到空 => 回占位
            setSelectedDirtyValue(0, false)
        } else {
            setSelectedDirtyValue(curValue / 10, true)
        }
    }

    fun confirmAndDismiss() {
        if (confirmed) return

        // 设为 true，阻断后续所有点击
        confirmed = true

        val h = if (hDirty) hValue else 0
        val m = if (mDirty) mValue else 0
        val s = if (sDirty) sValue else 0

        val targetMs = ((h * 3600L) + (m * 60L) + s) * 1000L

        if (targetMs == 0L) {
            onDismiss()
            return
        }

        if (durationMs <= 0L) {
            "未获取到视频时长，无法跳转".toast(context)
            onDismiss()
            return
        }

        if (targetMs > durationMs) {
            "超过视频时长".toast(context)
            onDismiss()
            return
        }

        onGoTime(targetMs)
        onDismiss()
    }

    val firstKeyFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // Dialog 刚弹出时直接 requestFocus 有概率失败，小延迟更稳
        delay(50)
        runCatching { firstKeyFocusRequester.requestFocus() }
    }

    // 尺寸：跟 SoftKeyboard 的观感接近；你也可以只改这里统一调大小
    val keySize = 38.dp
    val gap = 6.dp
    val gridW = keySize * 6 + gap * 5
    val gridH = keySize * 4 + gap * 3
    val totalW = keySize + gap + gridW + gap + keySize
    val shape = RoundedCornerShape(10.dp)

    val grid: List<List<Int?>> = listOf(
        listOf(1, 2, 3, 4, 5, 6),
        listOf(7, 8, 9, 0, 10, 11),
        listOf(12, 15, 20, 25, 30, 35),
        listOf(null, 40, 45, 50, 55, null)
    )

    TvAlertDialog(
        onDismissRequest = { confirmAndDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        ),
        title = { Text(text = "时间轴跳转，退出对话框立刻跳转") },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeJumpField(
                        placeholder = if (hDirty || mDirty || sDirty) "00" else "时",
                        value = hValue,
                        dirty = hDirty,
                        selected = selectedField == 0,
                        modifier = Modifier
                            .width(88.dp)
                            .height(48.dp)
                    )
                    Text(text = ":", fontWeight = FontWeight.Bold)
                    TimeJumpField(
                        placeholder = if (hDirty || mDirty || sDirty) "00" else "分",
                        value = mValue,
                        dirty = mDirty,
                        selected = selectedField == 1,
                        modifier = Modifier
                            .width(88.dp)
                            .height(48.dp)
                    )
                    Text(text = ":", fontWeight = FontWeight.Bold)
                    TimeJumpField(
                        placeholder = if (hDirty || mDirty || sDirty) "00" else "秒",
                        value = sValue,
                        dirty = sDirty,
                        selected = selectedField == 2,
                        modifier = Modifier
                            .width(88.dp)
                            .height(48.dp)
                    )
                }

                // 上删除条（拉长）
                TimeJumpBarKey(
                    text = "删除",
                    modifier = Modifier
                        .width(totalW)
                        .height(keySize),
                    onClick = { deleteOne() }
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 中间：< 长条 + 数字网格 + > 长条
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeJumpBarKey(
                        text = "◀",
                        modifier = Modifier
                            .width(keySize)
                            .height(gridH),
                        onClick = { selectPrev() }
                    )

                    Column(
                        modifier = Modifier.width(gridW),
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        grid.forEachIndexed { rowIndex, row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                                row.forEachIndexed { colIndex, key ->
                                    if (key == null) {
                                        Box(modifier = Modifier.size(keySize))
                                    } else {
                                        val keyModifier =
                                            if (rowIndex == 0 && colIndex == 0) {
                                                Modifier.focusRequester(firstKeyFocusRequester)
                                            } else {
                                                Modifier
                                            }
                                        TimeJumpKey(
                                            text = key.toString(),
                                            modifier = keyModifier.size(keySize),
                                            onClick = {
                                                if (key in 0..9) appendDigit(key) else setPreset(key)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TimeJumpBarKey(
                        text = "▶",
                        modifier = Modifier
                            .width(keySize)
                            .height(gridH),
                        onClick = { selectNext() }
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 下删除条（拉长）
                TimeJumpBarKey(
                    text = "删除",
                    modifier = Modifier
                        .width(totalW)
                        .height(keySize),
                    onClick = { deleteOne() }
                )
            }
        },
        confirmButton = {},
        modifier = Modifier
            .border(1.dp, Color.White.copy(alpha = 0.25f), shape)
            .background(Color.Black.copy(alpha = 0.85f), shape),
    )
}

@Composable
private fun TimeJumpField(
    placeholder: String,
    value: Int,
    dirty: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    val borderColor =
        if (selected) MaterialTheme.colorScheme.inverseSurface else Color.White.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .border(2.dp, borderColor, shape)
            .background(Color.Black.copy(alpha = 0.35f), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (dirty) value.toString() else placeholder,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimeJumpKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TimeJumpBarKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = MaterialTheme.shapes.small),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}