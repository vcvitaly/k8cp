package io.github.vcvitaly.k8cp.controller.pane;

import io.github.vcvitaly.k8cp.domain.BreadCrumbFile;
import io.github.vcvitaly.k8cp.domain.FileInfoContainer;
import io.github.vcvitaly.k8cp.domain.FileManagerItem;
import io.github.vcvitaly.k8cp.domain.RootInfoContainer;
import io.github.vcvitaly.k8cp.exception.IOOperationException;
import io.github.vcvitaly.k8cp.context.ServiceLocator;
import io.github.vcvitaly.k8cp.util.BoolStatusReturningConsumer;
import io.github.vcvitaly.k8cp.util.LocalFileUtil;
import io.github.vcvitaly.k8cp.util.ItemSelectionUtil;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableView;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.control.BreadCrumbBar;
import org.slf4j.Logger;

@Slf4j
public class LocalPaneController extends PaneController {
    public Button leftParentBtn;
    public Button leftRootBtn;
    public Button leftHomeBtn;
    public Button leftRefreshBtn;
    public Button leftCopyBtn;
    public Button leftMoveBtn;
    public Button leftDeleteBtn;
    public Button leftRenameBtn;
    public ChoiceBox<RootInfoContainer> localRootSelector;
    public BreadCrumbBar<BreadCrumbFile> leftBreadcrumbBar;
    public TableView<FileManagerItem> leftView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            resolveFilesAndBreadcrumbs();
            initView();
            initLocalRootSelector();
        } catch (Exception e) {
            log.error("Could not init the local view", e);
            ServiceLocator.getView().showErrorModal(e.getMessage());
        }
    }

    @Override
    protected TableView<FileManagerItem> getTableView() {
        return leftView;
    }

    @Override
    protected BreadCrumbBar<BreadCrumbFile> getBreadcrumbBar() {
        return leftBreadcrumbBar;
    }

    @Override
    protected Button getParentBtn() {
        return leftParentBtn;
    }

    @Override
    protected Button getRootBtn() {
        return leftRootBtn;
    }

    @Override
    protected Button getHomeBtn() {
        return leftHomeBtn;
    }

    @Override
    protected Button getRefreshBtn() {
        return leftRefreshBtn;
    }

    @Override
    protected Logger getLog() {
        return log;
    }

    @Override
    protected BoolStatusReturningConsumer<Path> getPathRefSettingConsumer() {
        return ServiceLocator.getModel()::setLocalPathRef;
    }

    @Override
    protected void initViewCrumb() {
        final List<BreadCrumbFile> localBreadcrumbTree = ServiceLocator.getModel().getLocalBreadcrumbTree();
        initViewCrumb(localBreadcrumbTree);
    }

    @Override
    protected void initViewItems() {
        final List<FileInfoContainer> localFiles = ServiceLocator.getModel().getLocalFiles();
        initViewItems(localFiles);
    }

    private void initLocalRootSelector() throws IOOperationException {
        final List<RootInfoContainer> roots = ServiceLocator.getModel().listLocalRoots();
        localRootSelector.setItems(FXCollections.observableList(roots));
        localRootSelector.setValue(ItemSelectionUtil.getSelectionItem(roots, rootInfoContainer -> true));
        localRootSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                onLocalRootSelection();
            }
        });
    }

    @Override
    protected void onParentBtn() {
        onNavigationBtn(() -> {
            if (ServiceLocator.getModel().setLocalPathRefToParent()) {
                resolveFilesAndBreadcrumbs();
            }
        });
    }

    @Override
    protected void onHomeBtn() {
        onNavigationBtn(() -> {
            ServiceLocator.getModel().setLocalPathRefToHome();
            resolveFilesAndBreadcrumbs();
        });
        localRootSelector.setValue(ServiceLocator.getModel().getMainRoot());
    }

    @Override
    protected void onRootBtn() {
        onNavigationBtn(() -> {
            ServiceLocator.getModel().setLocalPathRefToRoot();
            resolveFilesAndBreadcrumbs();
        });
        localRootSelector.setValue(ServiceLocator.getModel().getMainRoot());
    }

    @Override
    protected void resolveFilesAndBreadcrumbs() throws IOOperationException {
        ServiceLocator.getModel().resolveLocalBreadcrumbTree();
        ServiceLocator.getModel().resolveLocalFiles();
    }

    @Override
    protected void onBreadcrumb(BoolStatusReturningConsumer<Path> pathRefSettingConsumer, BreadCrumbFile selection) {
        onBreadcrumbInternal(pathRefSettingConsumer, selection, ServiceLocator.getModel()::resolveLocalFiles);
    }

    private void onLocalRootSelection() {
        final RootInfoContainer root = localRootSelector.getValue();
        final Path rootPath = root.path();
        if (!LocalFileUtil.isInTheSameRoot(rootPath, ServiceLocator.getModel().getLocalPath())) {
            if (ServiceLocator.getModel().setLocalPathRef(rootPath)) {
                executeLongRunningAction(this::resolveFilesAndBreadcrumbs, this::handleError, this::refreshCrumbAndItems);
            }
        }
    }
}
