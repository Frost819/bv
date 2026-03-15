package dev.aaa1115910.bv.tv.util

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import dev.aaa1115910.bv.entity.carddata.VideoCardData

@Stable
class TvLazyListFocusRestorer internal constructor(
    val fallbackFocusRequester: FocusRequester
) {
    fun containerModifier(modifier: Modifier = Modifier): Modifier {
        return modifier.focusRestorer(fallbackFocusRequester)
    }

    fun firstItemModifier(index: Int, modifier: Modifier = Modifier): Modifier {
        return if (index == 0) {
            modifier.focusRequester(fallbackFocusRequester)
        } else {
            modifier
        }
    }
}

@Composable
fun rememberTvLazyListFocusRestorer(
    fallbackFocusRequester: FocusRequester = remember { FocusRequester() }
): TvLazyListFocusRestorer {
    return remember(fallbackFocusRequester) {
        TvLazyListFocusRestorer(fallbackFocusRequester)
    }
}

fun VideoCardData.stableItemKey(): Any {
    return when {
        seasonId != null -> "season-$seasonId-${epId ?: 0}-$upId"
        avid > 0 -> "av-$avid-$upId"
        else -> "$title|$upId"
    }
}

fun Modifier.blockDownFocusExitAtGridEnd(
    currentIndex: Int,
    itemCount: Int,
    columnCount: Int
): Modifier {
    return onPreviewKeyEvent { event ->
        val nativeEvent = event.nativeKeyEvent
        if (nativeEvent.keyCode != KeyEvent.KEYCODE_DPAD_DOWN) return@onPreviewKeyEvent false
        if (nativeEvent.action != KeyEvent.ACTION_DOWN && nativeEvent.action != KeyEvent.ACTION_UP) {
            return@onPreviewKeyEvent false
        }

        val hasNextRow = itemCount > 0 && currentIndex >= 0 && currentIndex + columnCount < itemCount
        !hasNextRow
    }
}