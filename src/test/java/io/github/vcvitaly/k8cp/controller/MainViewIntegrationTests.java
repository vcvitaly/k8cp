package io.github.vcvitaly.k8cp.controller;

import io.github.vcvitaly.k8cp.K3sTest;
import io.github.vcvitaly.k8cp.TestUtil;
import io.github.vcvitaly.k8cp.client.KubeClient;
import io.github.vcvitaly.k8cp.client.LocalFsClient;
import io.github.vcvitaly.k8cp.client.impl.KubeClientImpl;
import io.github.vcvitaly.k8cp.client.impl.LocalFsClientImpl;
import io.github.vcvitaly.k8cp.context.ServiceLocator;
import io.github.vcvitaly.k8cp.domain.FileManagerItem;
import io.github.vcvitaly.k8cp.domain.KubeNamespace;
import io.github.vcvitaly.k8cp.domain.KubePod;
import io.github.vcvitaly.k8cp.domain.RootInfoContainer;
import io.github.vcvitaly.k8cp.enumeration.FileSizeUnit;
import io.github.vcvitaly.k8cp.enumeration.FileType;
import io.github.vcvitaly.k8cp.enumeration.OsFamily;
import io.github.vcvitaly.k8cp.exception.IOOperationException;
import io.github.vcvitaly.k8cp.model.Model;
import io.github.vcvitaly.k8cp.service.KubeService;
import io.github.vcvitaly.k8cp.service.LocalFsService;
import io.github.vcvitaly.k8cp.service.LocalOsFamilyDetector;
import io.github.vcvitaly.k8cp.service.PathProvider;
import io.github.vcvitaly.k8cp.service.LocalRootResolver;
import io.github.vcvitaly.k8cp.service.SizeConverter;
import io.github.vcvitaly.k8cp.service.impl.KubeServiceImpl;
import io.github.vcvitaly.k8cp.service.impl.LocalFsServiceImpl;
import io.github.vcvitaly.k8cp.service.impl.LocalOsFamilyDetectorImpl;
import io.github.vcvitaly.k8cp.service.impl.SizeConverterImpl;
import io.github.vcvitaly.k8cp.util.Constants;
import io.github.vcvitaly.k8cp.util.LocalFileUtil;
import io.github.vcvitaly.k8cp.view.View;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.zeroturnaround.zip.ZipUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class MainViewIntegrationTests extends K3sTest {

    private static final String TEST_FS_1 = "test_fs_1";
    private static final String TEST_FS_2 = "test_fs_2";
    private static final Path TEST_FS_1_PATH;
    private static final Path TEST_FS_2_PATH;
    private static final FileManagerItem PARENT_DIRECTORY_ITEM = FileManagerItem.builder()
            .name("..")
            .fileType(FileType.PARENT_DIRECTORY)
            .build();
    private static final List<RootInfoContainer> ROOTS;

    static {
        Function<String, Path> tmpPathCreator = dirName -> {
            try {
                return Paths.get(Files.createTempDirectory(dirName + "_").toString(), dirName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        TEST_FS_1_PATH = tmpPathCreator.apply(TEST_FS_1);
        TEST_FS_2_PATH = tmpPathCreator.apply(TEST_FS_2);
        Function<Path, RootInfoContainer> rootCreator = p -> new RootInfoContainer(p, p.getFileName().toString());
        ROOTS = List.of(
                rootCreator.apply(TEST_FS_1_PATH),
                rootCreator.apply(TEST_FS_2_PATH)
        );
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        ZipUtil.unpack(TestUtil.getFile("/%s.zip".formatted(TEST_FS_1)), TEST_FS_1_PATH.toFile());
        ZipUtil.unpack(TestUtil.getFile("/%s.zip".formatted(TEST_FS_2)), TEST_FS_2_PATH.toFile());
    }

    @AfterAll
    static void afterAll() throws Exception {
        FileUtils.deleteDirectory(TEST_FS_1_PATH.toFile());
        FileUtils.deleteDirectory(TEST_FS_2_PATH.toFile());
    }

    @Nested
    @ExtendWith(ApplicationExtension.class)
    class SuccessTest extends TestFxTest {
        private Model model;

        @Start
        private void start(Stage stage) throws IOOperationException {
            final LocalOsFamilyDetector localOsFamilyDetector = new LocalOsFamilyDetectorImpl();
            final PathProvider pathProvider = mock(PathProvider.class);
            when(pathProvider.provideRemoteRootPath()).thenReturn(LocalFileUtil.getPath(Constants.UNIX_ROOT));
            when(pathProvider.provideLocalRootPath()).thenReturn(TEST_FS_1_PATH);
            when(pathProvider.provideLocalHomePath()).thenReturn(TEST_FS_1_PATH.resolve("home").resolve("user"));
            final LocalFsClient localFsClient = new LocalFsClientImpl();
            final SizeConverter sizeConverter = new SizeConverterImpl();
            final LocalRootResolver localRootResolver = mock(LocalRootResolver.class);
            when(localRootResolver.listWindowsRoots()).thenReturn(ROOTS);
            when(localRootResolver.listLinuxRoots()).thenReturn(ROOTS);
            when(localRootResolver.listMacosRoots()).thenReturn(ROOTS);
            when(localRootResolver.getMainRoot(any(OsFamily.class)))
                    .thenReturn(ROOTS.getFirst());
            final LocalFsService localFsService = new LocalFsServiceImpl(
                    localFsClient, sizeConverter, localRootResolver
            );
            final KubeClient kubeClient = new KubeClientImpl(K3S.getKubeConfigYaml());
            final KubeService kubeService = new KubeServiceImpl(kubeClient, sizeConverter);
            model = spy(
                    Model.builder()
                            .kubeServiceSupplier(() -> kubeService)
                            .pathProvider(pathProvider)
                            .localFsService(localFsService)
                            .localOsFamilyDetector(localOsFamilyDetector)
                            .build()
            );
            model.setKubeNamespaceSelection(new KubeNamespace(DEFAULT_NAMESPACE));
            model.setKubePodSelection(new KubePod(nginxPodName));
            ServiceLocator.setModel(model);
            View.getInstance().showMainWindow();
        }

        @Test
        void localPaneIsLoadedSuccessfully_navigateToParentViaTableCell_navigateBackHome(FxRobot robot) {
            // Initial assert
            final TableView<FileManagerItem> localView = robot.lookup("#leftView").queryAs(TableView.class);
            assertLocalHomeFiles(localView);

            // Navigate to parent via table cell click
            robot.doubleClickOn(
                    (Node) robot.lookup(".table-row-cell")
                            .match(
                                    (TableRow<FileManagerItem> row) -> {
                                        final FileManagerItem item = row.getItem();
                                        return item != null && item.getName().equals("..") &&
                                                row.getTableView().getItems().stream().anyMatch(fmi -> fmi.getName().equals("bigfile"));
                                    }
                            ).query()
            );

            // Assert
            assertThat(localView.getItems()).usingRecursiveFieldByFieldElementComparator(
                    RecursiveComparisonConfiguration.builder()
                            .withIgnoredFields("path", "changedAt")
                            .build()
            ).containsExactlyInAnyOrderElementsOf(List.of(
                    PARENT_DIRECTORY_ITEM,
                    FileManagerItem.builder()
                            .name("user")
                            .fileType(FileType.DIRECTORY)
                            .build()
            ));

            // Navigate back home
            robot.clickOn("#leftHomeBtn");

            // Assert
            assertLocalHomeFiles(localView);
        }

        @Test
        void remotePaneIsLoadedSuccessfully_navigateToRootHome_navigateBackToRoot(FxRobot robot) {
            // Initial assert
            final TableView<FileManagerItem> remoteView = robot.lookup("#rightView").queryAs(TableView.class);
            assertRemoteRootFiles(remoteView);
            int itemsHashCode = remoteView.getItems().hashCode();

            // Navigate to root via table cell click
            robot.doubleClickOn(
                    (Node) robot.lookup(".table-row-cell")
                            .match(
                                    (TableRow<FileManagerItem> row) -> {
                                        final FileManagerItem item = row.getItem();
                                        return item != null && item.getName().equals("root") &&
                                                row.getTableView().getItems().stream().anyMatch(fmi -> fmi.getName().equals("mnt"));
                                    }
                            ).query()
            );
            waitForItemsToChange(remoteView, itemsHashCode);

            // Assert
            assertThat(remoteView.getItems()).usingRecursiveFieldByFieldElementComparator(
                    RecursiveComparisonConfiguration.builder()
                            .withIgnoredFields("path", "changedAt")
                            .build()
            ).containsExactlyInAnyOrderElementsOf(List.of(
                    PARENT_DIRECTORY_ITEM
            ));

            // Navigate back to root
            itemsHashCode = remoteView.getItems().hashCode();
            robot.clickOn("#rightRootBtn");
            waitForItemsToChange(remoteView, itemsHashCode);

            // Assert
            assertRemoteRootFiles(remoteView);
        }

        @Test
        void localPaneIsLoadedSuccessfully_navigateToAnotherRoot_navigateBackHome(FxRobot robot) {
            // Initial assert
            final TableView<FileManagerItem> localView = robot.lookup("#leftView").queryAs(TableView.class);
            assertLocalHomeFiles(localView);
            final ChoiceBox<RootInfoContainer> choiceBox = robot.lookup("#localRootSelector").queryAs(ChoiceBox.class);
            assertThat(choiceBox.getValue()).isEqualTo(ROOTS.getFirst());
            assertThat(choiceBox.getItems()).containsExactlyInAnyOrderElementsOf(ROOTS);
            final int itemsHashCode = localView.getItems().hashCode();

            // Navigate to another root
            robot.clickOn("#localRootSelector");
            robot.type(KeyCode.DOWN);
            robot.type(KeyCode.ENTER);

            // Assert
            assertThat(choiceBox.getValue()).isEqualTo(ROOTS.getLast());
            assertThat(localView.getItems()).usingRecursiveFieldByFieldElementComparator(
                    RecursiveComparisonConfiguration.builder()
                            .withIgnoredFields("path", "changedAt")
                            .build()
            ).containsExactlyInAnyOrderElementsOf(List.of(
                    FileManagerItem.builder()
                            .name("Games")
                            .fileType(FileType.DIRECTORY)
                            .build(),
                    FileManagerItem.builder()
                            .name("Videos")
                            .fileType(FileType.DIRECTORY)
                            .build()
            ));

            // Navigate back home
            robot.clickOn("#leftHomeBtn");

            // Assert
            assertThat(choiceBox.getValue()).isEqualTo(ROOTS.getFirst());
            assertLocalHomeFiles(localView);
        }

        private static void assertLocalHomeFiles(TableView<FileManagerItem> localView) {
            assertThat(localView.getItems()).usingRecursiveFieldByFieldElementComparator(
                    RecursiveComparisonConfiguration.builder()
                            .withIgnoredFields("path", "changedAt")
                            .build()
            ).containsExactlyInAnyOrderElementsOf(List.of(
                    PARENT_DIRECTORY_ITEM,
                    FileManagerItem.builder()
                            .name("bigfile")
                            .size(2)
                            .sizeUnit(FileSizeUnit.MB)
                            .fileType(FileType.FILE)
                            .build()
            ));
        }

        private static void assertRemoteRootFiles(TableView<FileManagerItem> remoteView) {
            assertThat(remoteView.getItems()).usingRecursiveFieldByFieldElementComparator(
                    RecursiveComparisonConfiguration.builder()
                            .withIgnoredFields("path", "changedAt", "size")
                            .build()
            ).containsAll(List.of(
                    FileManagerItem.builder()
                            .name("root")
                            .fileType(FileType.DIRECTORY)
                            .build(),
                    FileManagerItem.builder()
                            .name("home")
                            .fileType(FileType.DIRECTORY)
                            .build(),
                    FileManagerItem.builder()
                            .name("etc")
                            .fileType(FileType.DIRECTORY)
                            .build()
            ));
        }
    }

    private static void waitForItemsToChange(TableView<FileManagerItem> view, int oldItemsHashCode) {
        for (int i = 0; i < 100; i++) {
            if (view.getItems().hashCode() != oldItemsHashCode) {
                break;
            }
            System.out.println("Waiting for the items to change after double click action");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}