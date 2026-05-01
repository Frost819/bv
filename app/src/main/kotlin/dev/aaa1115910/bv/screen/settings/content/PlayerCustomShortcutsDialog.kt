package dev.aaa1115910.bv.screen.settings.content

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import dev.aaa1115910.bv.component.settings.SettingsMenuSelectItem
import dev.aaa1115910.bv.entity.PlayerCustomShortcut
import dev.aaa1115910.bv.entity.PlayerCustomShortcutActionEntry
import dev.aaa1115910.bv.entity.PlayerCustomShortcutCatalog
import dev.aaa1115910.bv.entity.PlayerCustomShortcutKeys
import dev.aaa1115910.bv.entity.PlayerCustomShortcutsStore
import dev.aaa1115910.bv.util.toast

@Composable
fun PlayerCustomShortcutsDialog(
    onDismiss: () -> Unit,
    onShortcutsChanged: (List<PlayerCustomShortcut>) -> Unit
) {
    val context = LocalContext.current
    var shortcuts by remember { mutableStateOf(PlayerCustomShortcutsStore.get()) }
    var stage by remember { mutableStateOf<PlayerCustomShortcutsDialogStage>(PlayerCustomShortcutsDialogStage.Main) }

    fun updateShortcuts(next: List<PlayerCustomShortcut>) {
        shortcuts = next
        onShortcutsChanged(next)
    }

    when (val currentStage = stage) {
        PlayerCustomShortcutsDialogStage.Main -> {
            PlayerCustomShortcutsMainDialog(
                shortcuts = shortcuts,
                onDismiss = onDismiss,
                onAdd = { stage = PlayerCustomShortcutsDialogStage.CaptureKey },
                onClear = { stage = PlayerCustomShortcutsDialogStage.ConfirmClear },
                onEdit = { shortcut ->
                    stage = PlayerCustomShortcutsDialogStage.PickAction(shortcut.keyCode)
                }
            )
        }

        PlayerCustomShortcutsDialogStage.CaptureKey -> {
            PlayerCustomShortcutKeyCaptureDialog(
                onDismiss = { stage = PlayerCustomShortcutsDialogStage.Main },
                onCaptured = { keyCode ->
                    stage = PlayerCustomShortcutsDialogStage.PickAction(keyCode)
                }
            )
        }

        is PlayerCustomShortcutsDialogStage.PickAction -> {
            val currentShortcut = shortcuts.firstOrNull { it.keyCode == currentStage.keyCode }
            PlayerCustomShortcutActionPickerDialog(
                keyCode = currentStage.keyCode,
                currentShortcut = currentShortcut,
                onDismiss = { stage = PlayerCustomShortcutsDialogStage.Main },
                onSelect = { entry ->
                    val next = PlayerCustomShortcutsStore.upsert(currentStage.keyCode, entry.action)
                    updateShortcuts(next)
                    stage = PlayerCustomShortcutsDialogStage.Main
                    "已绑定 ${PlayerCustomShortcutKeys.getDisplayName(currentStage.keyCode)}".toast(context)
                },
                onRemove = {
                    val next = PlayerCustomShortcutsStore.remove(currentStage.keyCode)
                    updateShortcuts(next)
                    stage = PlayerCustomShortcutsDialogStage.Main
                    "已删除绑定".toast(context)
                }
            )
        }

        PlayerCustomShortcutsDialogStage.ConfirmClear -> {
            PlayerCustomShortcutClearConfirmDialog(
                onDismiss = { stage = PlayerCustomShortcutsDialogStage.Main },
                onConfirm = {
                    val next = PlayerCustomShortcutsStore.clear()
                    updateShortcuts(next)
                    stage = PlayerCustomShortcutsDialogStage.Main
                    "已清空自定义播放快捷键".toast(context)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerCustomShortcutsMainDialog(
    shortcuts: List<PlayerCustomShortcut>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onClear: () -> Unit,
    onEdit: (PlayerCustomShortcut) -> Unit
) {
    val context = LocalContext.current

    PlayerCustomShortcutsDialogSurface(
        onDismiss = onDismiss
    ) { maxHeightModifier ->
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "自定义播放快捷键",
                style = MaterialTheme.typography.titleLarge
            )
            LazyColumn(
                modifier = maxHeightModifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (shortcuts.isEmpty()) {
                    item {
                        Text(
                            modifier = Modifier.padding(12.dp),
                            text = "暂无绑定",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    items(shortcuts, key = { it.keyCode }) { shortcut ->
                        SettingsMenuSelectItem(
                            text = "${PlayerCustomShortcutKeys.getDisplayName(shortcut.keyCode)}：${
                                PlayerCustomShortcutCatalog.getActionDisplayName(context, shortcut.action)
                            }",
                            selected = false,
                            onClick = { onEdit(shortcut) }
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onAdd) {
                    Text("新增")
                }
                Button(
                    enabled = shortcuts.isNotEmpty(),
                    onClick = onClear
                ) {
                    Text("清空")
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        }
    }
}

@Composable
private fun PlayerCustomShortcutKeyCaptureDialog(
    onDismiss: () -> Unit,
    onCaptured: (Int) -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    PlayerCustomShortcutsDialogSurface(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent true
                if (event.nativeKeyEvent.repeatCount != 0) return@onPreviewKeyEvent true

                val keyCode = event.nativeKeyEvent.keyCode
                when {
                    PlayerCustomShortcutKeys.isCancelKeyCode(keyCode) -> {
                        onDismiss()
                    }

                    PlayerCustomShortcutKeys.isAllowedKeyCode(keyCode) -> {
                        onCaptured(keyCode)
                    }

                    else -> {
                        "该按键不能绑定".toast(context)
                    }
                }
                true
            },
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "按下要绑定的按键",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "返回、ESC、手柄 B、确认和 Enter 不可绑定",
                style = MaterialTheme.typography.bodyLarge
            )
            OutlinedButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    }
}

@Composable
private fun PlayerCustomShortcutActionPickerDialog(
    keyCode: Int,
    currentShortcut: PlayerCustomShortcut?,
    onDismiss: () -> Unit,
    onSelect: (PlayerCustomShortcutActionEntry) -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val actionEntries = remember(context) { PlayerCustomShortcutCatalog.entries(context) }

    PlayerCustomShortcutsDialogSurface(
        onDismiss = onDismiss
    ) { maxHeightModifier ->
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "选择动作：${PlayerCustomShortcutKeys.getDisplayName(keyCode)}",
                style = MaterialTheme.typography.titleLarge
            )
            LazyColumn(
                modifier = maxHeightModifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentShortcut != null) {
                    item {
                        SettingsMenuSelectItem(
                            text = "删除当前绑定",
                            selected = false,
                            onClick = onRemove
                        )
                    }
                }
                items(actionEntries, key = { it.displayName }) { entry ->
                    SettingsMenuSelectItem(
                        text = entry.displayName,
                        selected = currentShortcut?.action == entry.action,
                        onClick = { onSelect(entry) }
                    )
                }
            }
            OutlinedButton(onClick = onDismiss) {
                Text("返回")
            }
        }
    }
}

@Composable
private fun PlayerCustomShortcutClearConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    PlayerCustomShortcutsDialogSurface(
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "清空全部绑定？",
                style = MaterialTheme.typography.titleLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onConfirm) {
                    Text("清空")
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerCustomShortcutsDialogSurface(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val maxHeightDp = with(density) {
        (windowInfo.containerSize.height * 0.6f).toDp()
    }

    BasicAlertDialog(
        modifier = Modifier.padding(vertical = 24.dp),
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            content(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeightDp)
            )
        }
    }
}

private sealed interface PlayerCustomShortcutsDialogStage {
    data object Main : PlayerCustomShortcutsDialogStage
    data object CaptureKey : PlayerCustomShortcutsDialogStage
    data class PickAction(val keyCode: Int) : PlayerCustomShortcutsDialogStage
    data object ConfirmClear : PlayerCustomShortcutsDialogStage
}
