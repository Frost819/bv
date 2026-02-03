package dev.aaa1115910.bv.screen.settings.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import dev.aaa1115910.bv.block.BlockPage

/**
 * 屏蔽页面选择 Dialog（专用，不做公共组件）：
 * - 点击 chip 只更新 dialog 内部 pendingSelectedPages
 * - 只有按返回/退出 dialog（onDismissRequest）时才提交 onSubmit
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalTvMaterial3Api::class)
@Composable
fun BlockPageSelectDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    title: String,
    allPages: List<BlockPage> = BlockPage.entries,
    initialSelectedPages: List<BlockPage> = emptyList(),
    onHideDialog: () -> Unit,
    onSubmit: (List<BlockPage>) -> Unit
) {
    if (!show) return

    val pendingSelectedPages = remember { mutableStateListOf<BlockPage>() }
    val defaultFocusRequester = remember { FocusRequester() }

    LaunchedEffect(show) {
        if (show) {
            pendingSelectedPages.clear()
            pendingSelectedPages.addAll(initialSelectedPages)
            if (allPages.isNotEmpty()) defaultFocusRequester.requestFocus()
        }
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            // 退出时才提交
            onSubmit(pendingSelectedPages.toList())
            onHideDialog()
        },
        confirmButton = {},
        title = { Text(text = title) },
        text = {
            FlowRow(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allPages.forEachIndexed { index, page ->
                    val selected = pendingSelectedPages.contains(page)
                    val itemModifier =
                        if (index == 0) Modifier.focusRequester(defaultFocusRequester) else Modifier

                    FilterChip(
                        modifier = itemModifier.onFocusChanged { /* 保持和 FavoriteDialog 一致 */ },
                        selected = selected,
                        onClick = {
                            if (selected) pendingSelectedPages.remove(page)
                            else pendingSelectedPages.add(page)
                        },
                        leadingIcon = {
                            Row {
                                AnimatedVisibility(visible = selected) {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = Icons.Rounded.Done,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    ) {
                        Text(text = page.displayName)
                    }
                }
            }
        }
    )
}