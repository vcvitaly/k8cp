package io.github.vcvitaly.k8cp.service.impl;

import io.github.vcvitaly.k8cp.client.LocalFsClient;
import io.github.vcvitaly.k8cp.domain.FileInfoContainer;
import io.github.vcvitaly.k8cp.domain.FileSizeContainer;
import io.github.vcvitaly.k8cp.domain.RootInfoContainer;
import io.github.vcvitaly.k8cp.enumeration.FileType;
import io.github.vcvitaly.k8cp.enumeration.OsFamily;
import io.github.vcvitaly.k8cp.exception.IOOperationException;
import io.github.vcvitaly.k8cp.service.LocalFsService;
import io.github.vcvitaly.k8cp.service.LocalRootResolver;
import io.github.vcvitaly.k8cp.service.SizeConverter;
import io.github.vcvitaly.k8cp.util.DateTimeUtil;
import io.github.vcvitaly.k8cp.util.LocalFileUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalFsServiceImpl implements LocalFsService {

    private final LocalFsClient localFsClient;
    private final SizeConverter sizeConverter;
    private final LocalRootResolver localRootResolver;

    @Override
    public List<FileInfoContainer> listFiles(Path path, boolean showHidden) throws IOOperationException {
        return listFilesInternal(path, showHidden);
    }

    @Override
    public List<RootInfoContainer> listWindowsRoots() {
        return localRootResolver.listWindowsRoots();
    }

    @Override
    public List<RootInfoContainer> listLinuxRoots() throws IOOperationException {
        return localRootResolver.listLinuxRoots();
    }

    @Override
    public List<RootInfoContainer> listMacosRoots() throws IOOperationException {
        return localRootResolver.listMacosRoots();
    }

    @Override
    public RootInfoContainer getMainRoot(OsFamily osFamily) {
        return localRootResolver.getMainRoot(osFamily);
    }

    private List<FileInfoContainer> listFilesInternal(Path path, boolean showHidden) throws IOOperationException {
        final List<Path> pathsUnfiltered = listPathsInternal(path);
        final List<Path> paths = pathsUnfiltered.stream()
                .filter(p -> LocalFileUtil.shouldBeShownBasedOnHiddenFlag(p, showHidden))
                .toList();
        final List<FileInfoContainer> list = new ArrayList<>();
        for (Path p : paths) {
            final FileInfoContainer fileInfoContainer = toFileInfoContainer(p);
            list.add(fileInfoContainer);
        }
        return list;
    }

    private List<Path> listPathsInternal(Path path) throws IOOperationException {
        return localFsClient.listFiles(path);
    }

    private FileInfoContainer toFileInfoContainer(Path path) throws IOOperationException {
        try {
            final long size = Files.size(path);
            final FileSizeContainer fileSizeContainer = sizeConverter.toFileSizeDto(size);
            final BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            return FileInfoContainer.builder()
                    .path(path)
                    .name(LocalFileUtil.getPathFilename(path))
                    .sizeBytes(size)
                    .size(fileSizeContainer.sizeInUnit())
                    .sizeUnit(fileSizeContainer.unit())
                    .fileType(Files.isDirectory(path) ? FileType.DIRECTORY : FileType.FILE)
                    .changedAt(toLocalDateTime(attrs.lastModifiedTime()))
                    .build();
        } catch (IOException e) {
            throw new IOOperationException("An error while reading attributes for " + path, e);
        }
    }

    private LocalDateTime toLocalDateTime(FileTime fileTime) {
        return DateTimeUtil.toLocalDateTime(fileTime);
    }
}
