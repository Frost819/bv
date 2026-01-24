package dev.aaa1115910.bv.player.entity

import android.content.Context
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.player.entity.DefaultStartPosition as PlayerDefaultStartPositionType

/**
 * 播放默认开始位置
 */
enum class PlayerDefaultStartPosition(val value: Int) {
    /** 从历史位置开始 */
    History(0),
    /** 从开头开始 */
    Beginning(1);

    fun displayName(context: Context): String = when (this) {
        History -> context.getString(R.string.settings_player_default_start_position_history)
        Beginning -> context.getString(R.string.settings_player_default_start_position_beginning)
    }

    fun toPlayerType(): PlayerDefaultStartPositionType = when (this) {
        History -> PlayerDefaultStartPositionType.History
        Beginning -> PlayerDefaultStartPositionType.Beginning
    }

    companion object {
        fun fromValue(value: Int): PlayerDefaultStartPosition = entries.find { it.value == value } ?: History
    }
}

