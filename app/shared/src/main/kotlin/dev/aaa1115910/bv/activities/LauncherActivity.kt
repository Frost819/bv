package dev.aaa1115910.bv.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dev.aaa1115910.bv.entity.InterfaceMode
import dev.aaa1115910.bv.util.DeviceUtil
import dev.aaa1115910.bv.util.Prefs
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 启动器活动
 * 
 * 这个活动是应用的入口点，它会根据设备类型路由到合适的主活动
 */
class LauncherActivity : ComponentActivity() {
    private val logger = KotlinLogging.logger { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        routeToCorrectActivity()
    }
    
    /**
     * 根据设备类型路由到正确的活动
     */
    private fun routeToCorrectActivity() {
        val interfaceMode = Prefs.interfaceMode
        val shouldLaunchTv = when (interfaceMode) {
            InterfaceMode.Auto -> DeviceUtil.isTvDevice(this)
            InterfaceMode.TV -> true
            InterfaceMode.Mobile -> false
        }

        val intent = if (shouldLaunchTv) {
            logger.info { "Launching TV MainActivity, interfaceMode=$interfaceMode" }
            Intent(this, Class.forName("dev.aaa1115910.bv.tv.activities.MainActivity"))
        } else {
            logger.info { "Launching Mobile MainActivity, interfaceMode=$interfaceMode" }
            Intent(this, Class.forName("dev.aaa1115910.bv.mobile.activities.MainActivity"))
        }

        // 传递原始Intent中的所有数据
        intent.putExtras(getIntent())

        // 启动相应的MainActivity
        startActivity(intent)

        // 关闭当前Activity
        finish()
    }

    companion object {
        /**
         * 清空当前任务栈并重新走启动路由，用于界面模式切换后立即生效
         */
        fun actionRestart(context: Context) {
            val intent = Intent(context, LauncherActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
        }
    }
}
