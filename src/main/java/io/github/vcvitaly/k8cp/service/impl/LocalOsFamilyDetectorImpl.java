package io.github.vcvitaly.k8cp.service.impl;

import io.github.vcvitaly.k8cp.enumeration.OsFamily;
import io.github.vcvitaly.k8cp.service.LocalOsFamilyDetector;

public class LocalOsFamilyDetectorImpl implements LocalOsFamilyDetector {
    private static final String OS_NAME_PROP_NAME = "os.name";
    private static final String WINDOWS_NAME_PREFIX = "Windows";
    private static final String MACOS_NAME_PREFIX = "Mac";

    @Override
    public OsFamily detectOsFamily() {
        final String osNameProp = System.getProperty(OS_NAME_PROP_NAME);
        if (osNameProp.contains(WINDOWS_NAME_PREFIX)) {
            return OsFamily.WINDOWS;
        }
        if (osNameProp.contains(MACOS_NAME_PREFIX)) {
            return OsFamily.MACOS;
        }
        return OsFamily.LINUX;
    }
}
