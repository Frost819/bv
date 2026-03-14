package dev.aaa1115910.bv.component.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon

@Composable
fun CoinButton(
    modifier: Modifier = Modifier,
    isCoined: Boolean,
    onClick: () -> Unit
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        scale = ButtonDefaults.scale(focusedScale = 1f, pressedScale = 1f),
    ) {
        Icon(
            imageVector = if (isCoined) Icons.Rounded.Paid else Icons.Outlined.Paid,
            contentDescription = null
        )
    }
}

@Preview
@Composable
fun CoinButtonPreview() {
    CoinButton(
        isCoined = false,
        onClick = {}
    )
}