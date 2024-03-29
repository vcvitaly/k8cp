package io.github.vcvitaly.k8cp.controller.init;

import io.github.vcvitaly.k8cp.domain.KubeConfigContainer;
import io.github.vcvitaly.k8cp.exception.IOOperationException;
import io.github.vcvitaly.k8cp.exception.KubeContextExtractionException;
import io.github.vcvitaly.k8cp.model.Model;
import io.github.vcvitaly.k8cp.util.Constants;
import io.github.vcvitaly.k8cp.util.ItemSelectionUtil;
import io.github.vcvitaly.k8cp.view.View;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KubeConfigSelectionController implements Initializable {
    public Label chooseFileLbl;
    public ChoiceBox<KubeConfigContainer> configSelector;
    public Label chooseFromFsLbl;
    public Button fsChooserBtn;
    public Label selectedKubeConfigFileLbl;
    public Button nextBtn;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        nextBtn.setOnAction(e -> onNext());
        fsChooserBtn.setOnAction(e -> onFileSelection());
        try {
            final ObservableList<KubeConfigContainer> kubeConfigList = Model.getKubeConfigList();
            if (!kubeConfigList.isEmpty()) {
                setItemsIfKubeConfigsFound(kubeConfigList);
            } else {
                setItemsIfNoKubeConfigFound();
            }
        } catch (IOOperationException | KubeContextExtractionException e) {
            log.error("Could not get kube config list", e);
            setItemsIfNoKubeConfigFound();
            View.getInstance().showErrorModal(e.getMessage());
        }
    }

    private void onNext() {
        final Stage selectionStage = (Stage) nextBtn.getScene().getWindow();
        View.getInstance().closeStage(selectionStage);
        View.getInstance().showKubeNamespaceSelectionWindow();
    }

    private void onFileSelection() {
        final File file = getFileFromFileChooser();
        if (file != null) {
            try {
                final KubeConfigContainer selection = Model.getKubeConfigSelectionDto(file.toPath());
                setKubeConfigSelection(selection);
                nextBtn.setDisable(false);
                chooseFileLbl.setVisible(false);
                configSelector.setVisible(false);
                chooseFromFsLbl.setVisible(false);
                selectedKubeConfigFileLbl.setText("You selected: " + selection.toString());
            } catch (KubeContextExtractionException ex) {
                View.getInstance().showErrorModal(ex.getMessage());
            }
        }
    }

    private File getFileFromFileChooser() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        return fileChooser.showOpenDialog(View.getInstance().getCurrentStage());
    }

    private void setItemsIfKubeConfigsFound(ObservableList<KubeConfigContainer> kubeConfigList) {
        configSelector.setItems(kubeConfigList);
        final KubeConfigContainer selectedItem = ItemSelectionUtil.getSelectionItem(
                kubeConfigList,
                selection -> selection.fileName().equals(Constants.DEFAULT_CONFIG_FILE_NAME)
        );
        configSelector.setValue(selectedItem);
        setKubeConfigSelection(selectedItem);
        configSelector.valueProperty().addListener(observable -> setKubeConfigSelection());
        nextBtn.setDisable(false);
    }

    private void setItemsIfNoKubeConfigFound() {
        chooseFileLbl.setVisible(false);
        configSelector.setVisible(false);
        chooseFromFsLbl.setText("""
            Could not find any configs in $HOME/.kube
            Please select a config from file system
        """);
    }

    private void setKubeConfigSelection() {
        final KubeConfigContainer selection = configSelector.getValue();
        setKubeConfigSelection(selection);
    }

    private void setKubeConfigSelection(KubeConfigContainer selection) {
        Model.setKubeConfigSelection(selection);
    }
}
