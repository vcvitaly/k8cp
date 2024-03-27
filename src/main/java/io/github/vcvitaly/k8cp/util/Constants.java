package io.github.vcvitaly.k8cp.util;

import io.github.vcvitaly.k8cp.domain.FileManagerItem;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
    public static final String TITLE = "K8CP";

    public static final String ERROR_TITLE_SUFFIX = "Error";

    public static final String FILE_INFO_TITLE_SUFFIX = "file info";

    public static final String KUBE_FOLDER = ".kube";

    public static final String DEFAULT_CONFIG_FILE_NAME = "config";

    public static final String DEFAULT_NAMESPACE_NAME = "default";

    public static final Class<?> FILE_MANAGER_ITEM_CLAZZ = FileManagerItem.class;

    public static final String PARENT_DIR_NAME = "..";

    public static final String UNIX_SEPARATOR = "/";
    public static final String UNIX_ROOT = UNIX_SEPARATOR;

    // PC stands for personal computer
    public static final String WINDOWS_ROOT = "C:\\";
    public static final String WINDOWS_DRIVE_LETTER_SUFFIX = ":\\";
    public static final String WINDOWS_SEPARATOR_REGEX = "\\\\";
    public static final String WINDOWS_SEPARATOR = "\\";

    public static final String NEW_INSTANCE_OF_MSG = "Created a new instance of %s";
}
