package io.github.vcvitaly.k8cp.service.impl;

import io.github.vcvitaly.k8cp.enumeration.OsFamily;
import io.github.vcvitaly.k8cp.service.LocalOsFamilyDetector;
import io.github.vcvitaly.k8cp.service.PathProvider;
import io.github.vcvitaly.k8cp.util.Constants;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PathProviderImpl implements PathProvider {
    private static final String USER_HOME_PROP_NAME = "user.home";

    private final LocalOsFamilyDetector localOsFamilyDetector;

    @Override
    public Path provideLocalHomePath() {
        return Paths.get(System.getProperty(USER_HOME_PROP_NAME));
    }

    @Override
    public Path provideLocalRootPath() {
        final OsFamily osFamily = localOsFamilyDetector.detectOsFamily();
        return Paths.get(
                switch (osFamily) {
                    case WINDOWS -> Constants.WINDOWS_ROOT;
                    case LINUX, MACOS -> Constants.UNIX_ROOT;
                }
        );
    }

    @Override
    public Path provideRemoteRootPath() {
        return Paths.get(Constants.UNIX_ROOT);
    }
}
