package dev.aaa1115910.bv.entity

import android.content.Context
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.util.stringRes

enum class InterfaceMode(private val strRes: Int) {
    Auto(R.string.interface_mode_auto),
    TV(R.string.interface_mode_tv),
    Mobile(R.string.interface_mode_mobile);

    fun getDisplayName(context: Context): String = strRes.stringRes(context)
}