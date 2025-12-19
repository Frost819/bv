package dev.aaa1115910.bv.component

import com.kuaishou.akdanmaku.data.DanmakuItemData

// B站弹幕扩展常量
const val RETAINER_BILIBILI = 0

// B站弹幕类型映射
fun Int.toDanmakuMode(): Int = when (this) {
    4 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
    5 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
    else -> DanmakuItemData.DANMAKU_MODE_ROLLING
}