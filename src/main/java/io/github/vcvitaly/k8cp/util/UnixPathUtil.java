package io.github.vcvitaly.k8cp.util;

import java.nio.file.Path;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UnixPathUtil {

    public static String concatPaths(String path, String fileName) {
        return "%s/%s".formatted(StringUtil.stripEndingSlash(path), StringUtil.stripEndingSlash(fileName));
    }

    public static boolean isRoot(String path) {
        return path.equals(Constants.UNIX_ROOT);
    }

    public static String getParentPath(String path) {
        if (isRoot(path)) {
            return path;
        }
        path = StringUtil.stripEndingSlash(path);
        final int lastIndexOfFwSlash = path.lastIndexOf('/');
        if (lastIndexOfFwSlash == 0) {
            return Constants.UNIX_ROOT;
        }
        return path.substring(0, lastIndexOfFwSlash);
    }

    public static String getFilename(String path) {
        if (isRoot(path)) {
            return path;
        }
        path = StringUtil.stripEndingSlash(path);
        final int lastIndexOfFwSlash = path.lastIndexOf('/');
        return StringUtil.stripBeginningSlash(path.substring(lastIndexOfFwSlash));
    }

    public static String normalizePathToString(Path path) {
        return path.toString().replaceAll(Constants.WINDOWS_SEPARATOR_REGEX, Constants.UNIX_SEPARATOR);
    }
}
