package io.github.vcvitaly.k8cp.controller.pane;

import io.github.vcvitaly.k8cp.context.ServiceLocator;
import io.github.vcvitaly.k8cp.domain.BreadCrumbFile;
import io.github.vcvitaly.k8cp.domain.FileInfoContainer;
import io.github.vcvitaly.k8cp.domain.FileManagerItem;
import io.github.vcvitaly.k8cp.enumeration.FileManagerColumn;
import io.github.vcvitaly.k8cp.enumeration.FileType;
import io.github.vcvitaly.k8cp.exception.IOOperationException;
import io.github.vcvitaly.k8cp.util.BoolStatusReturningConsumer;
import io.github.vcvitaly.k8cp.util.ThrowingRunnable;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import org.controlsfx.control.BreadCrumbBar;
import org.slf4j.Logger;

public abstract class PaneController implements Initializable {
    private static final String NO_ROWS_TO_DISPLAY_MSG = "No rows to display";

    protected Label getNoRowsToDisplayLbl() {
        return new Label(NO_ROWS_TO_DISPLAY_MSG);
    }

    protected List<TableColumn<FileManagerItem, String>> getTableColumns() {
        return List.of(
                getTableColumn(FileManagerColumn.NAME),
                getTableColumn(FileManagerColumn.SIZE),
                getTableColumn(FileManagerColumn.TYPE),
                getTableColumn(FileManagerColumn.CHANGED)
        );
    }

    protected TableColumn<FileManagerItem, String> getTableColumn(FileManagerColumn column) {
        TableColumn<FileManagerItem, String> col = new TableColumn<>(column.getColName());
        col.setCellValueFactory(new PropertyValueFactory<>(column.getFileManagerItemFieldName()));
        return col;
    }

    protected abstract TableView<FileManagerItem> getTableView();

    protected abstract BreadCrumbBar<BreadCrumbFile> getBreadcrumbBar();

    protected abstract Button getParentBtn();

    protected abstract Button getRootBtn();

    protected abstract Button getHomeBtn();

    protected abstract Button getRefreshBtn();

    protected abstract Logger getLog();

    protected abstract BoolStatusReturningConsumer<Path> getPathRefSettingConsumer();

    protected void initView() {
        getTableView().setPlaceholder(getNoRowsToDisplayLbl());
        getTableView().getColumns().addAll(getTableColumns());
        refreshCrumbAndItems();
        initViewButtons();
        initViewMouseSelection(getPathRefSettingConsumer());
        initViewEnterKeySelection(getPathRefSettingConsumer());
        initBreadCrumbListener();
    }

    protected abstract void initViewCrumb();

    protected void initViewCrumb(List<BreadCrumbFile> breadCrumbFiles) {
        final TreeItem<BreadCrumbFile> treeItem = ServiceLocator.getView().toTreeItem(breadCrumbFiles);
        getBreadcrumbBar().setSelectedCrumb(treeItem);
    }

    protected abstract void initViewItems();

    protected void initViewItems(List<FileInfoContainer> files) {
        final List<FileManagerItem> fileMangerItems = ServiceLocator.getView().toFileMangerItems(files);
        getTableView().setItems(FXCollections.observableList(fileMangerItems));
    }

    protected void initViewButtons() {
        getParentBtn().setOnAction(e -> onParentBtn());
        getRootBtn().setOnAction(e -> onRootBtn());
        getHomeBtn().setOnAction(e -> onHomeBtn());
        getRefreshBtn().setOnAction(e -> onRefreshBtn());
    }

    protected void onRefreshBtn() {
        onNavigationBtn(this::resolveFilesAndBreadcrumbs);
    }

    protected void refreshCrumbAndItems() {
        initViewCrumb();
        initViewItems();
    }

    protected void handleViewSelectionAction(BoolStatusReturningConsumer<Path> pathRefSettingConsumer, FileManagerItem item) {
        handleViewSelectionActionInternal(pathRefSettingConsumer, item);
    }

    protected void initViewEnterKeySelection(BoolStatusReturningConsumer<Path> pathRefSettingConsumer) {
        getTableView().setOnKeyPressed(e -> {
            if (!getTableView().getSelectionModel().isEmpty() && e.getCode() == KeyCode.ENTER) {
                handleViewSelectionAction(pathRefSettingConsumer, getTableView().getSelectionModel().getSelectedItem());
                e.consume();
            }
        });
    }

    protected void initViewMouseSelection(BoolStatusReturningConsumer<Path> pathRefSettingConsumer) {
        getTableView().setRowFactory(tv -> {
            final TableRow<FileManagerItem> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    handleViewSelectionAction(pathRefSettingConsumer, row.getItem());
                }
            });
            return row;
        });
    }

    protected void initBreadCrumbListener() {
        getBreadcrumbBar().selectedCrumbProperty()
                .addListener((observable, oldValue, newValue) -> onBreadcrumb(getPathRefSettingConsumer(), newValue.getValue()));
    }

    protected abstract void onBreadcrumb(BoolStatusReturningConsumer<Path> pathRefSettingConsumer, BreadCrumbFile selection);

    protected void setCursorWait() {
        setCursor(Cursor.WAIT);
    }

    protected void setCursorDefault() {
        setCursor(Cursor.DEFAULT);
    }

    protected void executeLongRunningAction(
            ThrowingRunnable r, Consumer<Throwable> exceptionHandler, Runnable onSuccessRunnable) {
        setCursorWait();
        final Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                r.run();
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            onSuccessRunnable.run();
            setCursorDefault();
        });
        task.setOnFailed(e -> {
            setCursorDefault();
            exceptionHandler.accept(task.getException());
        });
        Thread.ofVirtual().start(task);
    }

    protected void onBreadcrumbInternal(
            BoolStatusReturningConsumer<Path> pathRefSettingConsumer,
            BreadCrumbFile selection,
            ThrowingRunnable fileResolvingRunnable
    ) {
        if (pathRefSettingConsumer.accept(selection.getPath())) {
            executeLongRunningAction(fileResolvingRunnable, this::handleError, this::initViewItems);
        }
    }

    protected abstract void onParentBtn();

    protected abstract void onHomeBtn();

    protected abstract void onRootBtn();

    protected abstract void resolveFilesAndBreadcrumbs() throws IOOperationException;

    private void setCursor(Cursor cursor) {
        ServiceLocator.getView().getCurrentStage().getScene().setCursor(cursor);
    }

    protected void handleError(Throwable t) {
        getLog().error("Error: ", t);
        ServiceLocator.getView().showErrorModal(t.getMessage());
    }

    protected void onNavigationBtn(ThrowingRunnable longRunningRunnable) {
        executeLongRunningAction(
                longRunningRunnable, this::handleError, this::refreshCrumbAndItems
        );
    }

    private void handleViewSelectionActionInternal(BoolStatusReturningConsumer<Path> pathRefSettingConsumer, FileManagerItem item) {
        final FileType fileType = FileType.ofValueName(item.getFileType());
        if (fileType == FileType.DIRECTORY || fileType == FileType.PARENT_DIRECTORY) {
            if (pathRefSettingConsumer.accept(item.getPath())) {
                executeLongRunningAction(this::resolveFilesAndBreadcrumbs, this::handleError, this::refreshCrumbAndItems);
            }
        } else if (fileType == FileType.FILE || fileType == FileType.SYMLINK) {
            final String fileItemInfo = ServiceLocator.getView().toFileItemInfo(item);
            ServiceLocator.getView().showFileInfoModal(fileItemInfo);
        } else {
            throw new IllegalArgumentException("Unsupported file type " + fileType);
        }
    }
}
