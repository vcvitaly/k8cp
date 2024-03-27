package io.github.vcvitaly.k8cp.model;

import io.github.vcvitaly.k8cp.domain.BreadCrumbFile;
import io.github.vcvitaly.k8cp.domain.FileInfoContainer;
import io.github.vcvitaly.k8cp.domain.KubeConfigContainer;
import io.github.vcvitaly.k8cp.domain.KubeNamespace;
import io.github.vcvitaly.k8cp.domain.KubePod;
import io.github.vcvitaly.k8cp.domain.RootInfoContainer;
import io.github.vcvitaly.k8cp.enumeration.FileType;
import io.github.vcvitaly.k8cp.enumeration.OsFamily;
import io.github.vcvitaly.k8cp.exception.IOOperationException;
import io.github.vcvitaly.k8cp.exception.KubeApiException;
import io.github.vcvitaly.k8cp.exception.KubeContextExtractionException;
import io.github.vcvitaly.k8cp.context.Context;
import io.github.vcvitaly.k8cp.service.KubeConfigSelectionService;
import io.github.vcvitaly.k8cp.service.KubeService;
import io.github.vcvitaly.k8cp.service.LocalFsService;
import io.github.vcvitaly.k8cp.service.LocalOsFamilyDetector;
import io.github.vcvitaly.k8cp.service.PathProvider;
import io.github.vcvitaly.k8cp.util.Constants;
import io.github.vcvitaly.k8cp.util.LocalFileUtil;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Model {
    private final AtomicReference<KubeNamespace> kubeNamespaceSelectionRef = new AtomicReference<>();
    private final AtomicReference<KubePod> kubePodSelectionRef = new AtomicReference<>();
    @Getter
    private final AtomicReference<Path> localPathRef;
    @Getter
    private final AtomicReference<Path> remotePathRef;
    private final AtomicReference<List<BreadCrumbFile>> localBreadcrumbTree = new AtomicReference<>();
    private final AtomicReference<List<BreadCrumbFile>> remoteBreadcrumbTree = new AtomicReference<>();
    private final AtomicReference<List<FileInfoContainer>> localFiles = new AtomicReference<>();
    private final AtomicReference<List<FileInfoContainer>> remoteFiles = new AtomicReference<>();
    private final Supplier<KubeService> kubeServiceSupplier;
    private final LocalFsService localFsService;
    private final KubeConfigSelectionService kubeConfigSelectionService;
    private final PathProvider pathProvider;
    private final LocalOsFamilyDetector localOsFamilyDetector;

    @Builder
    public Model(
            Supplier<KubeService> kubeServiceSupplier,
            LocalFsService localFsService,
            KubeConfigSelectionService kubeConfigSelectionService,
            PathProvider pathProvider,
            LocalOsFamilyDetector localOsFamilyDetector
    ) {
        this.kubeServiceSupplier = kubeServiceSupplier;
        this.localFsService = localFsService;
        this.kubeConfigSelectionService = kubeConfigSelectionService;
        this.pathProvider = pathProvider;
        this.localOsFamilyDetector = localOsFamilyDetector;
        localPathRef = new AtomicReference<>(this.pathProvider.provideLocalHomePath());
        remotePathRef = new AtomicReference<>(this.pathProvider.provideRemoteRootPath());
    }

    public ObservableList<KubeConfigContainer> getKubeConfigList() throws IOOperationException, KubeContextExtractionException {
        final Path homePath = pathProvider.provideLocalHomePath();
        final List<KubeConfigContainer> configChoices = kubeConfigSelectionService
                .getConfigChoices(homePath.resolve(Constants.KUBE_FOLDER));
        return FXCollections.observableList(configChoices);
    }

    public KubeConfigContainer getKubeConfigSelectionDto(Path path) throws KubeContextExtractionException {
        return kubeConfigSelectionService.toKubeConfig(path);
    }

    public List<KubeNamespace> getKubeNamespaces() throws KubeApiException {
        return kubeServiceSupplier.get().getNamespaces();
    }

    public List<KubePod> getKubePods() throws KubeApiException {
        if (kubeNamespaceSelectionRef.get() == null) {
            throw logAndReturnRuntimeException(new IllegalStateException("A kube namespace has to be selected first"));
        }
        return kubeServiceSupplier.get().getPods(kubeNamespaceSelectionRef.get().name());
    }

    public FileInfoContainer getLocalParentDirectory() {
        final Path parentPath = getLocalParentPath();
        return getParentDirectory(parentPath);
    }

    public void resolveLocalFiles() throws IOOperationException {
        final Path currentPath = getLocalPath();
        final List<FileInfoContainer> files = new ArrayList<>(
                localFsService.listFiles(currentPath, false)
        );
        if (!LocalFileUtil.isRoot(currentPath)) {
            files.add(getLocalParentDirectory());
        }
        files.sort(Comparator.naturalOrder());
        localFiles.set(files);
        log.info("Resolved the local files for [%s]".formatted(currentPath));
    }

    public void resolveLocalBreadcrumbTree() {
        final Path currentPath = getLocalPath();
        final List<BreadCrumbFile> tree = resolveBreadCrumbFiles(currentPath);
        localBreadcrumbTree.set(tree);
        log.info("Resolved the local breadcrumb tree for [%s] to [%s]".formatted(currentPath, tree));
    }

    public List<RootInfoContainer> listLocalRoots() throws IOOperationException {
        final OsFamily osFamily = localOsFamilyDetector.detectOsFamily();
        return switch (osFamily) {
            case WINDOWS -> localFsService.listWindowsRoots();
            case LINUX -> localFsService.listLinuxRoots();
            case MACOS -> localFsService.listMacosRoots();
        };
    }

    public RootInfoContainer getMainRoot() {
        final OsFamily osFamily = localOsFamilyDetector.detectOsFamily();
        return localFsService.getMainRoot(osFamily);
    }

    public FileInfoContainer getRemoteParentDirectory() {
        final Path parentPath = getRemoteParentPath();
        return getParentDirectory(parentPath);
    }

    public void resolveRemoteFiles() throws IOOperationException {
        final Path currentPath = getRemotePath();
        final List<FileInfoContainer> files = new ArrayList<>(
                kubeServiceSupplier.get().listFiles(
                        kubeNamespaceSelectionRef.get().name(),
                        kubePodSelectionRef.get().name(),
                        currentPath,
                        false
                )
        );
        if (!LocalFileUtil.isRoot(currentPath)) {
            files.add(getRemoteParentDirectory());
        }
        files.sort(Comparator.naturalOrder());
        remoteFiles.set(files);
        log.info("Resolved the remote files for [%s]".formatted(currentPath));
    }

    public void resolveRemoteBreadcrumbTree() {
        final Path currentPath = getRemotePath();
        final List<BreadCrumbFile> tree = resolveBreadCrumbFiles(currentPath);
        remoteBreadcrumbTree.set(tree);
        log.info("Resolved the remote breadcrumb tree for [%s] to [%s]".formatted(currentPath, tree));
    }

    /* Setters */
    public void setKubeConfigSelection(KubeConfigContainer selection) {
        Context.kubeConfigSelectionRef.set(selection);
        log.info("Set the kube config selection to [{}]", selection);
    }

    public void setKubeNamespaceSelection(KubeNamespace selection) {
        kubeNamespaceSelectionRef.set(selection);
        log.info("Set the kube namespace selection to [{}]", selection);
    }

    public void setKubePodSelection(KubePod selection) {
        kubePodSelectionRef.set(selection);
        log.info("Set the kube pod selection to [{}]", selection);
    }

    public boolean setLocalPathRef(Path path) {
        final boolean comparedAndSet = compareAndSetLocalPathRef(path);
        if (comparedAndSet) {
            log.info("Set local path ref to [{}]", path);
        }
        return comparedAndSet;
    }

    public boolean setLocalPathRefToParent() {
        final Path parent = getLocalParentPath();
        final boolean comparedAndSet = compareAndSetLocalPathRef(parent);
        if (comparedAndSet) {
            log.info("Set local path ref to parent [{}]", parent);
        }
        return comparedAndSet;
    }

    public void setLocalPathRefToHome() {
        final Path home = pathProvider.provideLocalHomePath();
        if (compareAndSetLocalPathRef(home)) {
            log.info("Set local path ref to home path [{}]", home);
        }
    }

    public void setLocalPathRefToRoot() {
        final Path root = pathProvider.provideLocalRootPath();
        if (compareAndSetLocalPathRef(root)) {
            log.info("Set local path ref to root path [{}]", root);
        }
    }

    public boolean setRemotePathRef(Path path) {
        final boolean comparedAndSet = compareAndSetRemotePathRef(path);
        if (comparedAndSet) {
            log.info("Set remote path ref to [{}]", path);
        }
        return comparedAndSet;
    }

    public boolean setRemotePathRefToParent() {
        final Path parent = getRemoteParentPath();
        final boolean comparedAndSet = compareAndSetRemotePathRef(parent);
        if (comparedAndSet) {
            log.info("Set remote path ref to parent [{}]", parent);
        }
        return comparedAndSet;
    }

    public void setRemotePathRefToHome() throws IOOperationException {
        final Path home = LocalFileUtil.getPath(
                kubeServiceSupplier.get().getHomeDir(
                        kubeNamespaceSelectionRef.get().name(),
                        kubePodSelectionRef.get().name()
                )
        );
        if (compareAndSetRemotePathRef(home)) {
            log.info("Set remote path ref to home path [{}]", home);
        }
    }

    public void setRemotePathRefToRoot() {
        final Path rootPath = pathProvider.provideRemoteRootPath();
        if (compareAndSetRemotePathRef(rootPath)) {
            log.info("Set remote path ref to root path [{}]", rootPath);
        }
    }

    /* Getters */
    public synchronized Path getLocalPath() {
        return localPathRef.get();
    }

    public synchronized Path getRemotePath() {
        return remotePathRef.get();
    }

    // TODO should it be synchronized?
    public List<BreadCrumbFile> getRemoteBreadcrumbTree() {
        return remoteBreadcrumbTree.get();
    }

    public List<FileInfoContainer> getRemoteFiles() {
        return remoteFiles.get();
    }

    public List<BreadCrumbFile> getLocalBreadcrumbTree() {
        return localBreadcrumbTree.get();
    }

    public List<FileInfoContainer> getLocalFiles() {
        return localFiles.get();
    }

    /* Private methods */
    private BreadCrumbFile toBreadCrumbFile(Path path) {
        final String pathName = LocalFileUtil.getPathFilename(path);
        return new BreadCrumbFile(path, pathName);
    }

    private Path getLocalParentPath() {
        final Path path = getLocalPath();
        return getParentOrItself(path);
    }

    private synchronized boolean compareAndSetLocalPathRef(Path path) {
        return compareAndSetRef(getLocalPathRef(), path);
    }

    private synchronized boolean compareAndSetRemotePathRef(Path path) {
        return compareAndSetRef(getRemotePathRef(), path);
    }

    private <T> boolean compareAndSetRef(AtomicReference<T> ref, T t) {
        final T cur = ref.get();
        if (!t.equals(cur)) {
            ref.set(t);
            return true;
        }
        return false;
    }

    private FileInfoContainer getParentDirectory(Path parentPath) {
        return FileInfoContainer.builder()
                .path(parentPath)
                .name(Constants.PARENT_DIR_NAME)
                .fileType(FileType.PARENT_DIRECTORY)
                .build();
    }

    private Path getRemoteParentPath() {
        final Path curPath = getRemotePath();
        return getParentOrItself(curPath);
    }

    private RuntimeException logAndReturnRuntimeException(RuntimeException e) {
        log.error("Error: ", e);
        return e;
    }

    private List<BreadCrumbFile> resolveBreadCrumbFiles(Path tmpPath) {
        final Queue<BreadCrumbFile> reversedTree = new LinkedList<>();
        while (tmpPath != null) {
            reversedTree.add(toBreadCrumbFile(tmpPath));
            tmpPath = tmpPath.getParent();
        }
        return reversedTree.stream().toList().reversed();
    }

    private Path getParentOrItself(Path path) {
        if (LocalFileUtil.isRoot(path)) {
            return path;
        }
        return path.getParent();
    }
}
