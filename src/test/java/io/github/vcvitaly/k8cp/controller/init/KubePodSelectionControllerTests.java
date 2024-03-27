package io.github.vcvitaly.k8cp.controller.init;

import io.github.vcvitaly.k8cp.context.ServiceLocator;
import io.github.vcvitaly.k8cp.controller.TestFxTest;
import io.github.vcvitaly.k8cp.domain.KubeNamespace;
import io.github.vcvitaly.k8cp.domain.KubePod;
import io.github.vcvitaly.k8cp.exception.KubeApiException;
import io.github.vcvitaly.k8cp.model.Model;
import io.github.vcvitaly.k8cp.service.KubeService;
import io.github.vcvitaly.k8cp.service.PathProvider;
import io.github.vcvitaly.k8cp.view.View;
import java.util.List;
import javafx.scene.control.ChoiceBox;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubePodSelectionControllerTests {

    private static final String DEFAULT_NAMESPACE_NAME = "default";
    private static final KubePod NGINX_POD = new KubePod("nginx");
    private static final KubePod APP_POD = new KubePod("app");
    private static final List<KubePod> PODS = List.of(NGINX_POD, APP_POD);
    private static final String KUBE_API_ERROR_MSG = "Error!";

    @Nested
    @ExtendWith(ApplicationExtension.class)
    class KubePodSelectionControllerSuccessTest extends TestFxTest {
        private final View viewMock = mock(View.class);

        private Model model;

        @Start
        private void start(Stage stage) throws Exception {
            ServiceLocator.setView(viewMock);
            final KubeService kubeService = mock(KubeService.class);
            when(kubeService.getPods(DEFAULT_NAMESPACE_NAME)).thenReturn(PODS);
            model = spy(
                    Model.builder()
                            .pathProvider(mock(PathProvider.class))
                            .kubeServiceSupplier(() -> kubeService)
                            .build()
            );
            model.setKubeNamespaceSelection(new KubeNamespace(DEFAULT_NAMESPACE_NAME));
            ServiceLocator.setModel(model);
            View.getInstance().showKubePodSelectionWindow();
        }

        @Test
        void podChoiceBoxIsLoadedSuccessfully(FxRobot robot) {
            final ChoiceBox<KubePod> choiceBox = robot.lookup("#podSelector").queryAs(ChoiceBox.class);
            assertThat(choiceBox.getValue()).isEqualTo(NGINX_POD);
            assertThat(choiceBox.getItems()).containsExactlyInAnyOrderElementsOf(PODS);
            verify(model).setKubePodSelection(NGINX_POD);
            assertThat(robot.lookup("#prevBtn").queryButton().isDisable()).isFalse();
            assertThat(robot.lookup("#nextBtn").queryButton().isDisable()).isFalse();

            robot.clickOn("#nextBtn");
            verify(viewMock).showMainWindow();
        }

        @Test
        void onChoosingPodSelectionIsChanged(FxRobot robot) {
            final ChoiceBox<KubePod> choiceBox = robot.lookup("#podSelector").queryAs(ChoiceBox.class);
            assertThat(choiceBox.getValue()).isEqualTo(NGINX_POD);
            assertThat(choiceBox.getItems()).containsExactlyInAnyOrderElementsOf(PODS);
            verify(model).setKubePodSelection(NGINX_POD);
            assertThat(robot.lookup("#prevBtn").queryButton().isDisable()).isFalse();
            assertThat(robot.lookup("#nextBtn").queryButton().isDisable()).isFalse();

            robot.clickOn("#podSelector");
            robot.type(KeyCode.DOWN);
            robot.type(KeyCode.ENTER);

            assertThat(choiceBox.getValue()).isEqualTo(APP_POD);
            verify(model).setKubePodSelection(APP_POD);

            robot.clickOn("#nextBtn");
            verify(viewMock).showMainWindow();
        }
    }

    @Nested
    @ExtendWith(ApplicationExtension.class)
    class KubePodSelectionControllerFailureTest extends TestFxTest {

        private final View viewMock = mock(View.class);
        private Model model;

        @Start
        private void start(Stage stage) throws Exception {
            ServiceLocator.setView(viewMock);
            final KubeService kubeService = mock(KubeService.class);
            doThrow(new KubeApiException(KUBE_API_ERROR_MSG)).when(kubeService).getPods(DEFAULT_NAMESPACE_NAME);
            model = spy(
                    Model.builder()
                            .pathProvider(mock(PathProvider.class))
                            .kubeServiceSupplier(() -> kubeService)
                            .build()
            );
            model.setKubeNamespaceSelection(new KubeNamespace(DEFAULT_NAMESPACE_NAME));
            ServiceLocator.setModel(model);
            View.getInstance().showKubePodSelectionWindow();
        }

        @Test
        void podChoiceBoxInitializationThrows(FxRobot robot) {
            final ChoiceBox<KubePod> choiceBox = robot.lookup("#podSelector").queryAs(ChoiceBox.class);
            assertThat(choiceBox.getValue()).isNull();
            assertThat(choiceBox.getItems()).isEmpty();
            verify(model, never()).setKubePodSelection(any());
            assertThat(robot.lookup("#prevBtn").queryButton().isDisable()).isFalse();
            assertThat(robot.lookup("#nextBtn").queryButton().isDisable()).isTrue();
            verify(viewMock).showErrorModal(argThat(msg -> msg.equals(KUBE_API_ERROR_MSG)));
        }
    }
}