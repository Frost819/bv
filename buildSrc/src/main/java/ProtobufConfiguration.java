import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ProtobufConfiguration {
    private static final Set<String> usedProtoFiles = Set.of(
            "bilibili/app/archive/middleware/v1/preload.proto",
            "bilibili/app/archive/v1/archive.proto",
            "bilibili/app/card/v1/ad.proto",
            "bilibili/app/card/v1/card.proto",
            "bilibili/app/card/v1/common.proto",
            "bilibili/app/card/v1/single.proto",
            "bilibili/app/dynamic/v2/dynamic.proto",
            "bilibili/app/interfaces/v1/history.proto",
            "bilibili/app/interfaces/v1/search.proto",
            "bilibili/app/playeronline/v1/playeronline.proto",
            "bilibili/app/playerunite/v1/playerunite.proto",
            "bilibili/app/show/popular/v1/popular.proto",
            "bilibili/app/view/v1/view.proto",
            "bilibili/community/service/dm/v1/dm.proto",
            "bilibili/dagw/component/avatar/common/common.proto",
            "bilibili/dagw/component/avatar/v1/avatar.proto",
            "bilibili/dagw/component/avatar/v1/plugin.proto",
            "bilibili/main/community/reply/v1/reply.proto",
            "bilibili/metadata/device/device.proto",
            "bilibili/metadata/locale/locale.proto",
            "bilibili/metadata/metadata.proto",
            "bilibili/metadata/network/network.proto",
            "bilibili/pagination/pagination.proto",
            "bilibili/pgc/gateway/player/v2/playurl.proto",
            "bilibili/playershared/playershared.proto",
            "bilibili/polymer/app/search/v1/search.proto",
            "bilibili/rpc/status.proto",
            "common/ErrorProto.proto"
    );

    public static final Set<String> excludeProtoFiles = calculateExcludeProtoFiles();

    private ProtobufConfiguration() {}

    private static Set<String> calculateExcludeProtoFiles() {
        Set<String> all = getAllProtoFiles();
        Set<String> result = new LinkedHashSet<>(all);
        result.removeAll(usedProtoFiles);
        return result;
    }

    private static Set<String> getAllProtoFiles() {
        File rootDir = new File("bili-api-grpc/proto");
        Set<String> protoFiles = new LinkedHashSet<>();
        if (!rootDir.exists()) {
            return protoFiles;
        }
        Path rootPath = rootDir.toPath();
        File[] files = rootDir.listFiles();
        if (files == null) return protoFiles;

        java.util.ArrayDeque<File> stack = new java.util.ArrayDeque<>();
        for (File file : files) stack.push(file);

        while (!stack.isEmpty()) {
            File file = stack.pop();
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) stack.push(child);
                }
            } else if (file.isFile() && file.getName().endsWith(".proto")) {
                String rel = rootPath.relativize(file.toPath()).toString().replace(File.separatorChar, '/');
                protoFiles.add(rel);
            }
        }
        return protoFiles;
    }
}
