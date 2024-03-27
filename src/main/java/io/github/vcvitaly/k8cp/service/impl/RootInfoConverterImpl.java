package io.github.vcvitaly.k8cp.service.impl;

import io.github.vcvitaly.k8cp.domain.RootInfoContainer;
import io.github.vcvitaly.k8cp.service.RootInfoConverter;
import io.github.vcvitaly.k8cp.util.LocalFileUtil;
import java.nio.file.Path;
import java.util.List;

public class RootInfoConverterImpl implements RootInfoConverter {

    @Override
    public List<RootInfoContainer> convert(List<Path> paths) {
        return paths.stream()
                .map(this::toRootInfoContainer)
                .toList();
    }

    private RootInfoContainer toRootInfoContainer(Path path) {
        return new RootInfoContainer(path, LocalFileUtil.normalizeRootPath(path));
    }
}
