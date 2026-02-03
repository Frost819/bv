package dev.aaa1115910.bv

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.webkit.WebViewCompat
import de.schnettler.datastore.manager.DataStoreManager
import dev.aaa1115910.biliapi.http.BiliHttpProxyApi
import dev.aaa1115910.biliapi.http.util.BiliAppConf
import dev.aaa1115910.biliapi.http.util.BiliWebConf
import dev.aaa1115910.biliapi.repositories.AuthRepository
import dev.aaa1115910.biliapi.repositories.BiliApiModule
import dev.aaa1115910.biliapi.repositories.ChannelRepository
import dev.aaa1115910.bv.dao.AppDatabase
import dev.aaa1115910.bv.network.HttpServer
import dev.aaa1115910.bv.util.LogCatcherUtil
import dev.aaa1115910.bv.util.Prefs
import dev.aaa1115910.bv.block.BlockManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.KoinApplication
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.ksp.generated.module
import org.slf4j.impl.HandroidLoggerAdapter

class BVApp : Application(), KoinComponent {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
            private set

        lateinit var dataStoreManager: DataStoreManager
            private set

        lateinit var koinApplication: KoinApplication
            private set

        @SuppressLint("StaticFieldLeak")
        var instance: BVApp? = null
            private set

        fun getAppDatabase(context: Context = this.context) = AppDatabase.getDatabase(context)
    }

    override fun onCreate() {
        super.onCreate()

        instance = this
        context = this.applicationContext

        initCoreLibraries()
        Prefs.init()
        BlockManager.reloadFromPrefs() // 启动即从本地恢复，保证过滤一直有效
        initDeviceInfo()
        initRepository()
        initProxy()
        HttpServer.startServer()
    }

    private fun initCoreLibraries() {
        HandroidLoggerAdapter.DEBUG = BuildConfig.DEBUG
        LogCatcherUtil.installLogCatcher()

        dataStoreManager = DataStoreManager(applicationContext.dataStore)

        koinApplication = startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@BVApp)
            modules(AppModule().module)
        }
    }

    fun initRepository() {
        val channelRepository: ChannelRepository = get()
        val authRepository: AuthRepository = get()

        channelRepository.initDefaultChannel(Prefs.accessToken, Prefs.buvid)

        authRepository.apply {
            sessionData = Prefs.sessData.takeIf { it.isNotEmpty() }
            biliJct = Prefs.biliJct.takeIf { it.isNotEmpty() }
            accessToken = Prefs.accessToken.takeIf { it.isNotEmpty() }
            mid = Prefs.uid.takeIf { it != 0L }
            buvid3 = Prefs.buvid3
            buvid = Prefs.buvid
        }
    }

    fun initProxy() {
        if (!Prefs.enableProxy) return

        BiliHttpProxyApi.createClient(Prefs.proxyHttpServer)

        runCatching {
            val channelRepository: ChannelRepository = get()
            channelRepository.initProxyChannel(
                Prefs.accessToken,
                Prefs.buvid,
                Prefs.proxyGRPCServer
            )
        }
    }

    fun initDeviceInfo() {
        BiliAppConf.osVersion = Build.VERSION.RELEASE
        BiliAppConf.model = Build.MODEL
        BiliWebConf.webViewVersion = runCatching {
            WebViewCompat.getCurrentLoadedWebViewPackage()?.versionName
                ?.substringBefore(".")?.toInt()
        }.getOrDefault(144) ?: 144
    }
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "Settings")

@Module(includes = [BiliApiModule::class])
@ComponentScan
class AppModule