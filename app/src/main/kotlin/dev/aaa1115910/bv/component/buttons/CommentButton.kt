package dev.aaa1115910.bv.component.buttons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Comment
import androidx.compose.runtime.Composable
import androidx.tv.material3.Button
import androidx.tv.material3.Icon

@Composable
fun CommentButton(
    onClick: () -> Unit
) {

    Button(
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.Rounded.Comment,
            contentDescription = null
        )
    }
}