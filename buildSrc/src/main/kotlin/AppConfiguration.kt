import java.io.File

object AppConfiguration {
    const val appId = "dev.aaa1115910.bv"
    const val applicationId = "dev.frost819.bv" //由于小米电视屏蔽原包名，此包名仅用于apk打包
    const val compileSdk = 36
    const val minSdk = 21
    const val targetSdk = 36
    private const val major = 0
    private const val minor = 3
    private const val patch = 11
    private const val hotFix = 0

    @Suppress("KotlinConstantConditions")
    val versionName: String by lazy {
        "$major.$minor.$patch${".$hotFix".takeIf { hotFix != 0 } ?: ""}" +
                ".r${versionCode}.${"git rev-list HEAD --abbrev-commit --max-count=1".exec()}"
    }
    val versionCode: Int by lazy { "git rev-list --count HEAD".exec().toInt() }
    const val libVLCVersion = "3.0.18"
    var googleServicesAvailable = true

    init {
        initConfigurations()
    }

    private fun initConfigurations() {
        val googleServicesJsonPath = File(System.getProperty("user.dir"), "app/google-services.json").absolutePath
        val googleServicesJsonFile = File(googleServicesJsonPath)
        googleServicesAvailable =
            googleServicesJsonFile.exists() && googleServicesJsonFile.readText().let {
                it.contains(applicationId) && it.contains("$applicationId.r8test") && it.contains("$applicationId.debug")
            }
    }
}

fun String.exec() = String(Runtime.getRuntime().exec(this).inputStream.readBytes()).trim()