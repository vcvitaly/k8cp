package io.github.vcvitaly.k8cp.view;

import io.github.vcvitaly.k8cp.controller.ErrorController;
import io.github.vcvitaly.k8cp.controller.FileInfoController;
import io.github.vcvitaly.k8cp.domain.BreadCrumbFile;
import io.github.vcvitaly.k8cp.domain.FileInfoContainer;
import io.github.vcvitaly.k8cp.domain.FileManagerItem;
import io.github.vcvitaly.k8cp.enumeration.FxmlView;
import io.github.vcvitaly.k8cp.util.Constants;
import io.github.vcvitaly.k8cp.util.DateTimeUtil;
import io.github.vcvitaly.k8cp.util.FxmlLoaderUtil;
import io.github.vcvitaly.k8cp.util.ResourceUtil;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.BreadCrumbBar;

@Slf4j
public class View {

    private static final String MAIN_ICON_PATH = "/images/k8cp_icon.png";

    private final AtomicReference<Stage> currentStage;

    private View() {
        currentStage = new AtomicReference<>();
    }

    public Stage getCurrentStage() {
        return currentStage.get();
    }

    /* Windows section */
    public void closeStage(Stage stage) {
        stage.close();
    }

    public void showMainWindow() {
        initStageAndShow(
                StageCreationParam.builder()
                        .fxmlView(FxmlView.MAIN)
                        .build()
        );
    }

    public void showAboutModal() {
        initStageAndShow(
                StageCreationParam.builder()
                        .fxmlView(FxmlView.ABOUT)
                        .modality(Modality.APPLICATION_MODAL)
                        .resizeable(false)
                        .build()
        );
    }

    public void showErrorModal(String errorMsg) {
        initStageAndShow(
                StageCreationParam.builder()
                        .fxmlView(FxmlView.ERROR)
                        .modality(Modality.APPLICATION_MODAL)
                        .title("%s - %s".formatted(Constants.TITLE, Constants.ERROR_TITLE_SUFFIX))
                        .controller(new ErrorController(errorMsg))
                        .resizeable(false)
                        .build()
        );
    }

    public void showKubeConfigSelectionWindow() {
        initStageAndShow(
                StageCreationParam.builder()
                        .fxmlView(FxmlView.KUBE_CONFIG_SELECTION)
                        .resizeable(false)
                        .build()
        );
    }

    public void showKubeNamespaceSelectionWindow() {
        initStageAndShow(
                StageCreationParam.builder()
                        .fxmlView(FxmlView.KUBE_NAMESPACE_SELECTION)
                        .resizeable(false)
                        .build()
        );
    }

    public void showKubePodSelectionWindow() {
        initStageAndShow(
                StageCreationParam.builder()
                        .fxmlView(FxmlView.KUBE_POD_SELECTION)
                        .resizeable(false)
                        .build()
        );
    }

    public void showFileInfoModal(String fileInfo) {
        initStageAndShow(
                StageCreationParam.builder()
                        .fxmlView(FxmlView.FILE_INFO)
                        .modality(Modality.APPLICATION_MODAL)
                        .title("%s - %s".formatted(Constants.TITLE, Constants.FILE_INFO_TITLE_SUFFIX))
                        .controller(new FileInfoController(fileInfo))
                        .resizeable(false)
                        .build()
        );
    }

    /* Other public methods */
    public List<FileManagerItem> toFileMangerItems(List<FileInfoContainer> fileInfoContainers) {
        return fileInfoContainers.stream()
                .map(this::toFileManagerItem)
                .toList();
    }

    public TreeItem<BreadCrumbFile> toTreeItem(List<BreadCrumbFile> breadCrumbFiles) {
        return BreadCrumbBar.buildTreeModel(breadCrumbFiles.toArray(BreadCrumbFile[]::new));
    }

    public String toFileItemInfo(FileManagerItem fileManagerItem) {
        return """
                Name: %s
                Path: %s
                Size: %s
                Type: %s
                Changed: %s
                """.formatted(
                fileManagerItem.getName(),
                fileManagerItem.getPath(),
                fileManagerItem.getSize(),
                fileManagerItem.getFileType(),
                fileManagerItem.getChangedAt()
        );
    }

    /* Private methods */
    private void initStageAndShow(StageCreationParam param) {
        final FxmlView fxmlView = param.getFxmlView();
        final FXMLLoader loader = FxmlLoaderUtil.createFxmlLoader(fxmlView);
        final Initializable controller = param.getController();
        if (controller != null) {
            loader.setController(controller);
        }
        final Scene scene;
        try {
            scene = new Scene(loader.load());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final Stage stage = new Stage();
        stage.setScene(scene);
        stage.getIcons().add(getMainIcon());
        stage.setTitle(param.getTitle() != null ? param.getTitle() : Constants.TITLE);
        if (param.getModality() != null) {
            stage.initModality(param.getModality());
        }
        final Boolean resizeable = param.getResizeable();
        if (resizeable != null) {
            stage.setResizable(resizeable);
        }
        stage.show();
        setCurrentStage(stage);
        log.info("Shown " + fxmlView);
    }

    private Image getMainIcon() {
        return new Image(
                ResourceUtil.getResource(MAIN_ICON_PATH).toString()
        );
    }

    private void setCurrentStage(Stage stage) {
        currentStage.set(stage);
    }


    private FileManagerItem toFileManagerItem(FileInfoContainer fileInfoContainer) {
        return FileManagerItem.builder()
                .path(fileInfoContainer.getPath())
                .name(fileInfoContainer.getName())
                .size(fileInfoContainer.getSize())
                .sizeUnit(fileInfoContainer.getSizeUnit())
                .fileType(fileInfoContainer.getFileType())
                .changedAt(DateTimeUtil.toString(fileInfoContainer.getChangedAt()))
                .build();
    }

    /* Holder section */
    public static View getInstance() {
        return ViewFactoryHolder.view;
    }

    private static class ViewFactoryHolder {
        private static final View view = new View();
    }
}
