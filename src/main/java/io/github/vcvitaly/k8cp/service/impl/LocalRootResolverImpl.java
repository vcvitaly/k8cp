package io.github.vcvitaly.k8cp.service.impl;

import io.github.vcvitaly.k8cp.client.LocalFsClient;
import io.github.vcvitaly.k8cp.domain.RootInfoContainer;
import io.github.vcvitaly.k8cp.enumeration.OsFamily;
import io.github.vcvitaly.k8cp.exception.IOOperationException;
import io.github.vcvitaly.k8cp.service.LocalRootResolver;
import io.github.vcvitaly.k8cp.service.RootInfoConverter;
import io.github.vcvitaly.k8cp.util.Constants;
import io.github.vcvitaly.k8cp.util.LocalFileUtil;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalRootResolverImpl implements LocalRootResolver {

    private static final String MNT_DIR = "/mnt";
    private static final String VOLUMES_DIR = "/Volumes";

    private final LocalFsClient localFsClient;
    private final RootInfoConverter rootInfoConverter;

    @Override
    public List<RootInfoContainer> listWindowsRoots() {
        final File[] roots = File.listRoots();
        return rootInfoConverter.convert(
                Arrays.stream(roots)
                        .map(File::toPath)
                        .toList()
        );
    }

    @Override
    public List<RootInfoContainer> listLinuxRoots() throws IOOperationException {
        return listUnixRoots(MNT_DIR);
    }

    @Override
    public List<RootInfoContainer> listMacosRoots() throws IOOperationException {
        return listUnixRoots(VOLUMES_DIR);
    }

    @Override
    public RootInfoContainer getMainRoot(OsFamily osFamily) {
        return switch (osFamily) {
            case WINDOWS -> new RootInfoContainer(
                    LocalFileUtil.getPath(Constants.WINDOWS_ROOT),
                    LocalFileUtil.normalizeRootPath(Paths.get(Constants.WINDOWS_ROOT))
            );
            case LINUX, MACOS -> new RootInfoContainer(LocalFileUtil.getPath(Constants.UNIX_ROOT), Constants.UNIX_ROOT);
        };
    }

    private List<RootInfoContainer> listUnixRoots(String rootsDir) throws IOOperationException {
        final List<RootInfoContainer> roots = new ArrayList<>();
        roots.add(new RootInfoContainer(LocalFileUtil.getPath(Constants.UNIX_ROOT), Constants.UNIX_ROOT));
        final List<Path> paths = localFsClient.listFiles(Paths.get(rootsDir));
        roots.addAll(rootInfoConverter.convert(paths));
        return roots;
    }
}
