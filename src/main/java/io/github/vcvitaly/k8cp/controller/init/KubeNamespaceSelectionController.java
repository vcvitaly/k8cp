package io.github.vcvitaly.k8cp.controller.init;

import io.github.vcvitaly.k8cp.domain.KubeNamespace;
import io.github.vcvitaly.k8cp.exception.KubeApiException;
import io.github.vcvitaly.k8cp.context.ServiceLocator;
import io.github.vcvitaly.k8cp.util.Constants;
import io.github.vcvitaly.k8cp.util.ItemSelectionUtil;
import io.github.vcvitaly.k8cp.view.View;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KubeNamespaceSelectionController implements Initializable {
    public ChoiceBox<KubeNamespace> namespaceSelector;
    public Label errorLbl;
    public Button prevBtn;
    public Button nextBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        prevBtn.setOnAction(e -> onPrev());
        nextBtn.setOnAction(e -> onNext());
        try {
            final List<KubeNamespace> namespaces = ServiceLocator.getModel().getKubeNamespaces();
            if (!namespaces.isEmpty()) {
                namespaceSelector.setItems(FXCollections.observableList(namespaces));
                final KubeNamespace selectedItem = ItemSelectionUtil.getSelectionItem(
                        namespaces,
                        selection -> selection.name().equals(Constants.DEFAULT_NAMESPACE_NAME)
                );
                namespaceSelector.setValue(selectedItem);
                setKubeNamespaceSelection(selectedItem);
                namespaceSelector.valueProperty().addListener(observable -> setKubeNamespaceSelection());
                nextBtn.setDisable(false);
            } else {
                errorLbl.setText("There are no namespaces in this cluster");
            }
        } catch (KubeApiException e) {
            log.error("Could not get namespaces list", e);
            errorLbl.setText("Could not get namespaces list");
            ServiceLocator.getView().showErrorModal(e.getMessage());
        }
    }

    private void onNext() {
        final Stage selectionStage = (Stage) nextBtn.getScene().getWindow();
        ServiceLocator.getView().closeStage(selectionStage);
        ServiceLocator.getView().showKubePodSelectionWindow();
    }

    private void onPrev() {
        final Stage selectionStage = (Stage) prevBtn.getScene().getWindow();
        ServiceLocator.getView().closeStage(selectionStage);
        ServiceLocator.getView().showKubeConfigSelectionWindow();
    }

    private void setKubeNamespaceSelection() {
        final KubeNamespace selection = namespaceSelector.getValue();
        setKubeNamespaceSelection(selection);
    }

    private void setKubeNamespaceSelection(KubeNamespace selection) {
        ServiceLocator.getModel().setKubeNamespaceSelection(selection);
    }
}
