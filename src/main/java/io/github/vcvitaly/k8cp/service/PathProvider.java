package io.github.vcvitaly.k8cp.service;

import java.nio.file.Path;

public interface PathProvider {

    Path provideLocalHomePath();

    Path provideLocalRootPath();

    Path provideRemoteRootPath();
}
