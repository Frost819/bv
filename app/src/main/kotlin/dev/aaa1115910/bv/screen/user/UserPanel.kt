package dev.aaa1115910.bv.component

import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.AccountBox
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.util.requestFocus

private val lineHeight = 80.dp

@Composable
fun UserPanel(
    modifier: Modifier = Modifier,
    username: String,
    face: String,
    level: Int,
    currentExp: Int,
    nextLevelExp: Int,
    onHide: () -> Unit,
    onGoUserSwitch: () -> Unit,
    onGoFollowingUp: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var inIncognitoMode by remember { mutableStateOf(Prefs.incognitoMode) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus(scope)
    }

    Box(
        modifier = modifier
            .onPreviewKeyEvent {
                when (it.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_BACK -> {
                        if (it.nativeKeyEvent.action == KeyEvent.ACTION_UP) onHide()
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
    ) {
        Column(
            modifier = Modifier.onPreviewKeyEvent {
                when (it.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                        return@onPreviewKeyEvent true
                    }
                }
                false
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            UserPanelMyItem(
                modifier = Modifier
                    .width(300.dp),
                username = username,
                face = face,
                level = level,
                currentExp = currentExp,
                nextLevelExp = nextLevelExp,
            )

            val buttonWidth = 120.dp
            Row {
                UserPanelSmallItem(
                    modifier = Modifier
                        .width(buttonWidth)
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent {
                            when (it.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_DPAD_LEFT -> {
                                    return@onPreviewKeyEvent true
                                }
                            }
                            false
                        },
                    title = if (inIncognitoMode) "隐身开启" else "隐身关闭",
                    icon = if (inIncognitoMode) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    onClick = {
                        inIncognitoMode = !inIncognitoMode
                        Prefs.incognitoMode = inIncognitoMode
                    }
                )
                UserPanelSmallItem(
                    modifier = Modifier
                        .width(buttonWidth),
                    title = "正在关注",
                    icon = Icons.AutoMirrored.Rounded.ListAlt,
                    onClick = {
                        onGoFollowingUp()
                        onHide()
                    }
                )
                UserPanelSmallItem(
                    modifier = Modifier
                        .width(buttonWidth)
                        .onPreviewKeyEvent {
                            when (it.nativeKeyEvent.keyCode) {
                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    return@onPreviewKeyEvent true
                                }
                            }
                            false
                        },
                    title = "账号管理",
                    icon = Icons.Rounded.AccountBox,
                    onClick = {
                        onGoUserSwitch()
                        onHide()
                    }
                )
            }
        }
    }
}

@Composable
private fun UserPanelMyItem(
    modifier: Modifier = Modifier,
    username: String,
    face: String,
    level: Int,
    currentExp: Int,
    nextLevelExp: Int
) {
    val progress = currentExp.toFloat() / nextLevelExp.coerceAtLeast(1)

    Surface(
        modifier = modifier
            .padding(4.dp)
            .fillMaxWidth()
            .focusable(false)
            .height(lineHeight),
        shape = MaterialTheme.shapes.medium,
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = username,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lv.$level",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                )
            }

            AsyncImage(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                model = face,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
    }
}


@Composable
private fun UserPanelSmallItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .padding(4.dp)
            .height(lineHeight),
        onClick = onClick,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
            pressedContainerColor = MaterialTheme.colorScheme.inverseSurface
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart),
                imageVector = icon,
                contentDescription = null
            )
            Text(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.BottomStart),
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        }

    }
}


@Preview(device = "id:tv_1080p")
@Composable
private fun UserPanelPreview() {
    BVTheme {
        UserPanel(
            username = "abcde",
            face = "",
            onHide = {},
            onGoUserSwitch = {},
            onGoFollowingUp = {},
            level = 5,
            currentExp = 100,
            nextLevelExp = 200,
        )
    }
}