package io.github.vcvitaly.k8cp.view;

import io.github.vcvitaly.k8cp.controller.ErrorController;
import io.github.vcvitaly.k8cp.enumeration.FxmlView;
import io.github.vcvitaly.k8cp.util.Constants;
import io.github.vcvitaly.k8cp.util.FxmlLoaderUtil;
import io.github.vcvitaly.k8cp.util.ResourceUtil;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Slf4j
public class ViewFactory {

    private static final String MAIN_ICON_PATH = "/images/k8cp_icon.png";

    public void closeStage(Stage stage) {
        stage.close();
    }

    public void showMainWindow() {
        createStageAndShow(
                StageCreationParam.builder()
                        .fxmlView(FxmlView.MAIN)
                        .build()
        );
    }

    public void showAboutModal() {
        createStageAndShow(
                StageCreationParam.builder()
                        .fxmlView(FxmlView.ABOUT)
                        .modality(Modality.APPLICATION_MODAL)
                        .build()
        );
    }

    public void showErrorModal(String errorMsg) {
        createStageAndShow(
                StageCreationParam.builder()
                        .fxmlView(FxmlView.ERROR)
                        .modality(Modality.APPLICATION_MODAL)
                        .title("%s %s".formatted(Constants.TITLE, Constants.ERROR_TITLE_SUFFIX))
                        .controller(new ErrorController(errorMsg))
                        .build()
        );
    }

    public void showKubeConfigSelectionWindow() {
        createStageAndShow(
                StageCreationParam.builder()
                        .fxmlView(FxmlView.KUBE_CONFIG_SELECTION)
                        .build()
        );
    }

    private void createStageAndShow(StageCreationParam param) {
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
        stage.show();
        log.info("Shown " + fxmlView);
    }

    private Image getMainIcon() {
        return new Image(
                ResourceUtil.getResource(MAIN_ICON_PATH).toString()
        );
    }
}
