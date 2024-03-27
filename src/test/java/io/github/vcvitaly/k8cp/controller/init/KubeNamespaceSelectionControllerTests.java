package io.github.vcvitaly.k8cp.controller.init;

import io.github.vcvitaly.k8cp.context.ServiceLocator;
import io.github.vcvitaly.k8cp.controller.TestFxTest;
import io.github.vcvitaly.k8cp.domain.KubeNamespace;
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

public class KubeNamespaceSelectionControllerTests {

    private static final String DEFAULT_NAMESPACE_NAME = "default";
    private static final String CONTROL_PLANE_NAMESPACE_NAME = "control-plane";
    private static final KubeNamespace DEFAULT_NAMESPACE = new KubeNamespace(DEFAULT_NAMESPACE_NAME);
    private static final KubeNamespace CONTROL_PLANE_NAMESPACE = new KubeNamespace(CONTROL_PLANE_NAMESPACE_NAME);
    private static final List<KubeNamespace> KUBE_NAMESPACE_LIST =
            List.of(DEFAULT_NAMESPACE, CONTROL_PLANE_NAMESPACE);
    private static final String KUBE_API_ERROR_MSG = "Error!";

    @Nested
    @ExtendWith(ApplicationExtension.class)
    class KubeNamespaceSelectionControllerSuccessTest extends TestFxTest {
        private final View viewMock = mock(View.class);

        private Model model;

        @Start
        private void start(Stage stage) throws Exception {
            ServiceLocator.setView(viewMock);
            final KubeService kubeService = mock(KubeService.class);
            when(kubeService.getNamespaces()).thenReturn(KUBE_NAMESPACE_LIST);
            model = spy(
                    Model.builder()
                            .pathProvider(mock(PathProvider.class))
                            .kubeServiceSupplier(() -> kubeService)
                            .build()
            );
            ServiceLocator.setModel(model);
            View.getInstance().showKubeNamespaceSelectionWindow();
        }

        @Test
        void namespaceChoiceBoxIsLoadedSuccessfully(FxRobot robot) {
            final ChoiceBox<KubeNamespace> choiceBox = robot.lookup("#namespaceSelector").queryAs(ChoiceBox.class);
            assertThat(choiceBox.getValue()).isEqualTo(DEFAULT_NAMESPACE);
            assertThat(choiceBox.getItems()).containsExactlyInAnyOrderElementsOf(KUBE_NAMESPACE_LIST);
            verify(model).setKubeNamespaceSelection(DEFAULT_NAMESPACE);
            assertThat(robot.lookup("#prevBtn").queryButton().isDisable()).isFalse();
            assertThat(robot.lookup("#nextBtn").queryButton().isDisable()).isFalse();

            robot.clickOn("#nextBtn");
            verify(viewMock).showKubePodSelectionWindow();
        }

        @Test
        void onChoosingNamespaceSelectionIsChanged(FxRobot robot) {
            final ChoiceBox<KubeNamespace> choiceBox = robot.lookup("#namespaceSelector").queryAs(ChoiceBox.class);
            assertThat(choiceBox.getValue()).isEqualTo(DEFAULT_NAMESPACE);
            assertThat(choiceBox.getItems()).containsExactlyInAnyOrderElementsOf(KUBE_NAMESPACE_LIST);
            verify(model).setKubeNamespaceSelection(DEFAULT_NAMESPACE);
            assertThat(robot.lookup("#prevBtn").queryButton().isDisable()).isFalse();
            assertThat(robot.lookup("#nextBtn").queryButton().isDisable()).isFalse();

            robot.clickOn("#namespaceSelector");
            robot.type(KeyCode.DOWN);
            robot.type(KeyCode.ENTER);

            assertThat(choiceBox.getValue()).isEqualTo(CONTROL_PLANE_NAMESPACE);
            verify(model).setKubeNamespaceSelection(CONTROL_PLANE_NAMESPACE);

            robot.clickOn("#nextBtn");
            verify(viewMock).showKubePodSelectionWindow();
        }
    }

    @Nested
    @ExtendWith(ApplicationExtension.class)
    class KubeNamespaceSelectionControllerFailureTest extends TestFxTest {

        private final View viewMock = mock(View.class);
        private Model model;

        @Start
        private void start(Stage stage) throws Exception {
            ServiceLocator.setView(viewMock);
            final KubeService kubeService = mock(KubeService.class);
            doThrow(new KubeApiException(KUBE_API_ERROR_MSG)).when(kubeService).getNamespaces();
            model = spy(
                    Model.builder()
                            .pathProvider(mock(PathProvider.class))
                            .kubeServiceSupplier(() -> kubeService)
                            .build()
            );
            ServiceLocator.setModel(model);
            View.getInstance().showKubeNamespaceSelectionWindow();
        }

        @Test
        void namespaceChoiceBoxInitializationThrows(FxRobot robot) {
            final ChoiceBox<KubeNamespace> choiceBox = robot.lookup("#namespaceSelector").queryAs(ChoiceBox.class);
            assertThat(choiceBox.getValue()).isNull();
            assertThat(choiceBox.getItems()).isEmpty();
            verify(model, never()).setKubeNamespaceSelection(any());
            assertThat(robot.lookup("#prevBtn").queryButton().isDisable()).isFalse();
            assertThat(robot.lookup("#nextBtn").queryButton().isDisable()).isTrue();
            verify(viewMock).showErrorModal(argThat(msg -> msg.equals(KUBE_API_ERROR_MSG)));
        }
    }
}