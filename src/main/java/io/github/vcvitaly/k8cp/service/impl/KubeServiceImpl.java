package io.github.vcvitaly.k8cp.service.impl;

import io.github.vcvitaly.k8cp.client.KubeClient;
import io.github.vcvitaly.k8cp.domain.FileInfoContainer;
import io.github.vcvitaly.k8cp.domain.FileSizeContainer;
import io.github.vcvitaly.k8cp.domain.KubeNamespace;
import io.github.vcvitaly.k8cp.domain.KubePod;
import io.github.vcvitaly.k8cp.enumeration.FileType;
import io.github.vcvitaly.k8cp.exception.IOOperationException;
import io.github.vcvitaly.k8cp.exception.KubeApiException;
import io.github.vcvitaly.k8cp.exception.KubeExecException;
import io.github.vcvitaly.k8cp.service.KubeService;
import io.github.vcvitaly.k8cp.service.SizeConverter;
import io.github.vcvitaly.k8cp.util.Constants;
import io.github.vcvitaly.k8cp.util.DateTimeUtil;
import io.github.vcvitaly.k8cp.util.LocalFileUtil;
import io.github.vcvitaly.k8cp.util.StringUtil;
import io.github.vcvitaly.k8cp.util.UnixPathUtil;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KubeServiceImpl implements KubeService {

    private static final List<String> LS_PARTS = List.of("ls", "--full-time", "-l");
    private static final List<String> ECHO_HOME_PARTS = List.of("sh", "-c", "echo $HOME");
    private static final String DIRECTORY_MODIFIER = "d";
    private static final String SYMLINK_MODIFIER = "l";

    private final KubeClient kubeClient;
    private final SizeConverter sizeConverter;

    @Override
    public List<FileInfoContainer> listFiles(String namespace, String podName, Path path, boolean showHidden) throws IOOperationException {
        final ArrayList<String> partsList = new ArrayList<>(LS_PARTS);
        partsList.add(UnixPathUtil.normalizePathToString(path));
        final String[] cmdParts  = partsList.toArray(String[]::new);
        try {
            final List<String> lines = kubeClient.execAndReturnOut(namespace, podName, cmdParts);
            return lines.stream()
                    .filter(line -> !line.startsWith("total"))
                    .map(line -> toFileInfoContainer(path, line))
                    .filter(container -> showBeShownBasedOnHiddenFlag(container, showHidden))
                    .toList();
        } catch (KubeExecException e) {
            throw new IOOperationException("Could not get a list of files at [%s@%s]".formatted(podName, path), e);
        }
    }

    @Override
    public List<KubeNamespace> getNamespaces() throws KubeApiException {
        return kubeClient.getNamespaces();
    }

    @Override
    public List<KubePod> getPods(String namespace) throws KubeApiException {
        return kubeClient.getPods(namespace);
    }

    @Override
    public String getHomeDir(String namespace, String podName) throws IOOperationException {
        final String[] cmdParts  = ECHO_HOME_PARTS.toArray(String[]::new);
        try {
            final List<String> lines = kubeClient.execAndReturnOut(namespace, podName, cmdParts);
            if (!lines.isEmpty()) {
                return lines.getFirst();
            }
            return Constants.UNIX_ROOT;
        } catch (KubeExecException e) {
            throw new IOOperationException("Could not get the home dir for [%s]".formatted(podName), e);
        }
    }

    private FileInfoContainer toFileInfoContainer(Path path, String lsLine) {
        final String[] parts = lsLine.split("\\s+");
        final String attrs = parts[0];
        final long size = Long.parseLong(parts[4]);
        final String date = parts[5];
        final String time = parts[6];
        final String nameRaw = parts[8];
        final Path fullPath = LocalFileUtil.concatPaths(path, LocalFileUtil.getPath(nameRaw));
        final String name = StringUtil.stripEndingSlash(nameRaw);
        final FileSizeContainer fileSizeContainer = sizeConverter.toFileSizeDto(size);
        return FileInfoContainer.builder()
                .path(fullPath)
                .name(name)
                .sizeBytes(size)
                .size(fileSizeContainer.sizeInUnit())
                .sizeUnit(fileSizeContainer.unit())
                .fileType(getType(attrs))
                .changedAt(DateTimeUtil.toLocalDate(date, time))
                .build();
    }

    private FileType getType(String attrs) {
        if (attrs.startsWith(DIRECTORY_MODIFIER)) {
            return FileType.DIRECTORY;
        }
        if (attrs.startsWith(SYMLINK_MODIFIER)) {
            return FileType.SYMLINK;
        }
        return FileType.FILE;
    }

    private boolean showBeShownBasedOnHiddenFlag(FileInfoContainer fileInfoContainer, boolean showHidden) {
        if (showHidden) {
            return true;
        }
        return !fileInfoContainer.getName().startsWith(".");
    }
}
