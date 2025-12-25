package dev.aaa1115910.bv.component.controllers.playermenu

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.LocalVideoPlayerControllerData
import dev.aaa1115910.bv.component.controllers.MenuFocusState
import dev.aaa1115910.bv.component.controllers.playermenu.component.MenuListItem
import dev.aaa1115910.bv.component.ifElse

@Composable
fun PlaySpeedMenuList(
    modifier: Modifier = Modifier,
    onSelectedPlaySpeedItemChange: (PlaySpeedItem) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onFocusStateChange: (MenuFocusState) -> Unit
) {
    val context = LocalContext.current
    val data = LocalVideoPlayerControllerData.current
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp) {
                        if (listOf(Key.Enter, Key.DirectionCenter).contains(it.key)) {
                            return@onPreviewKeyEvent false
                        }
                        return@onPreviewKeyEvent true
                    }
                    if (it.key == Key.DirectionRight)
                        onFocusStateChange(MenuFocusState.MenuNav)
                    false
                }
                .focusRestorer(focusRequester),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            itemsIndexed(PlaySpeedItem.entries.toMutableList()) { index, item ->
                MenuListItem(
                    modifier = Modifier
                        .ifElse(
                            index == data.currentSelectedPlaySpeedItem.ordinal,
                            Modifier.focusRequester(focusRequester)
                        ),
                    text = item.getDisplayName(context),
                    selected = data.currentSelectedPlaySpeedItem == item,
                    onClick = {
                        onPlaySpeedChange(item.speed)
                        onSelectedPlaySpeedItemChange(item)
                    },
                )
            }
        }
    }


}

enum class PlaySpeedItem(val code: Int, private val strRes: Int, val speed: Float) {
    x0_5(0, R.string.play_speed_x0_5, 0.5f),
    x1(1, R.string.play_speed_x1, 1f),
    x1_25(2, R.string.play_speed_x1_25, 1.25f),
    x1_5(3, R.string.play_speed_x1_5, 1.5f),
    x2(4, R.string.play_speed_x2, 2f);

    companion object {
        fun fromCode(code: Int): PlaySpeedItem {
            return entries.find { it.code == code } ?: x1
        }
    }

    fun getDisplayName(context: Context) = context.getString(strRes)
}