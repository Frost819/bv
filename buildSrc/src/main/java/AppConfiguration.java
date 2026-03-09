import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class AppConfiguration {
    public static final String appId = "dev.aaa1115910.bv";
    public static final String applicationId = "dev.frost819.bv";
    public static final int compileSdk = 36;
    public static final int minSdk = 21;
    public static final int targetSdk = 36;
    private static final int major = 0;
    private static final int minor = 3;
    private static final int patch = 13;
    private static final int hotFix = 0;

    public static final int versionCode;
    public static final String versionName;
    public static final String libVLCVersion = "3.0.18";
    public static boolean googleServicesAvailable = true;

    static {
        versionCode = Integer.parseInt(exec("git rev-list --count HEAD"));
        versionName = major + "." + minor + "." + patch
                + (hotFix != 0 ? "." + hotFix : "")
                + ".r" + versionCode + "." + exec("git rev-list HEAD --abbrev-commit --max-count=1");
        initConfigurations();
    }

    private AppConfiguration() {}

    private static void initConfigurations() {
        String googleServicesJsonPath = new File(System.getProperty("user.dir"), "app/google-services.json").getAbsolutePath();
        File googleServicesJsonFile = new File(googleServicesJsonPath);
        if (!googleServicesJsonFile.exists()) {
            googleServicesAvailable = false;
            return;
        }
        String content;
        try {
            content = java.nio.file.Files.readString(googleServicesJsonFile.toPath());
        } catch (IOException e) {
            googleServicesAvailable = false;
            return;
        }
        googleServicesAvailable = content.contains(applicationId)
                && content.contains(applicationId + ".r8test")
                && content.contains(applicationId + ".debug");
    }

    private static String exec(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            byte[] out = process.getInputStream().readAllBytes();
            return new String(out, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
