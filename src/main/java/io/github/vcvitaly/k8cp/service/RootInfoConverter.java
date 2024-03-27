package io.github.vcvitaly.k8cp.service;

import io.github.vcvitaly.k8cp.domain.RootInfoContainer;
import java.nio.file.Path;
import java.util.List;

public interface RootInfoConverter {

    List<RootInfoContainer> convert(List<Path> paths);
}
