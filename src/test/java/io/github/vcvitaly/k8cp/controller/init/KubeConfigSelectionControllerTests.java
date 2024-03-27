package io.github.vcvitaly.k8cp.controller.init;

import io.github.vcvitaly.k8cp.TestUtil;
import io.github.vcvitaly.k8cp.client.impl.LocalFsClientImpl;
import io.github.vcvitaly.k8cp.context.Context;
import io.github.vcvitaly.k8cp.context.ServiceLocator;
import io.github.vcvitaly.k8cp.controller.TestFxTest;
import io.github.vcvitaly.k8cp.controller.helper.FileChooserHelper;
import io.github.vcvitaly.k8cp.domain.KubeConfigContainer;
import io.github.vcvitaly.k8cp.model.Model;
import io.github.vcvitaly.k8cp.service.KubeConfigSelectionService;
import io.github.vcvitaly.k8cp.service.PathProvider;
import io.github.vcvitaly.k8cp.service.impl.KubeConfigHelperImpl;
import io.github.vcvitaly.k8cp.service.impl.KubeConfigSelectionServiceImpl;
import io.github.vcvitaly.k8cp.view.View;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KubeConfigSelectionControllerTests {

    private static final String CONTEXT_NAME = "kind-kind";
    private static final String KUBE_CONFIG_YML_FILE_NAME = "kube_config.yml";
    private static final String CONTEXT_COPY_NAME = "kind-copy";
    private static final String KUBE_CONFIG_YML_FILE_COPY_NAME = "kube_config_copy.yml";
    private static KubeConfigContainer EXPECTED_CONFIG_CONTAINER = new KubeConfigContainer(
            CONTEXT_NAME, KUBE_CONFIG_YML_FILE_NAME, null
    );

    @Nested
    @ExtendWith(ApplicationExtension.class)
    class KubeConfigSelectionControllerChoiceBoxTest extends TestFxTest {

        private final View viewMock = mock(View.class);

        private void mockViewGetStage() {
            when(viewMock.getCurrentStage()).thenReturn(mock(Stage.class));
        }

        @Start
        private void start(Stage stage) {
            mockViewGetStage();
            ServiceLocator.setView(viewMock);
            final PathProvider pathProvider = mock(PathProvider.class);
            final Path homePath = TestUtil.getPath("/kubeconfig/ok");
            when(pathProvider.provideLocalHomePath()).thenReturn(homePath);
            ServiceLocator.setModel(
                    Model.builder()
                            .pathProvider(pathProvider)
                            .kubeConfigSelectionService(new KubeConfigSelectionServiceImpl(
                                    new LocalFsClientImpl(), new KubeConfigHelperImpl()
                            ))
                            .build()
            );
            View.getInstance().showKubeConfigSelectionWindow();
        }

        @Test
        void kubeConfigChoiceBoxIsLoadedSuccessfully(FxRobot robot) {
            final ChoiceBox<KubeConfigContainer> choiceBox = robot.lookup("#configSelector").queryAs(ChoiceBox.class);
            assertThat(choiceBox.getValue())
                    .usingRecursiveComparison()
                    .ignoringFields("path")
                    .isEqualTo(EXPECTED_CONFIG_CONTAINER);
            assertThat(choiceBox.getItems())
                    .usingRecursiveFieldByFieldElementComparator(
                            RecursiveComparisonConfiguration.builder()
                                    .withIgnoredFields("path")
                                    .build()
                    ).containsExactlyInAnyOrderElementsOf(List.of(
                            new KubeConfigContainer(CONTEXT_NAME, KUBE_CONFIG_YML_FILE_NAME, null),
                            new KubeConfigContainer(CONTEXT_COPY_NAME, KUBE_CONFIG_YML_FILE_COPY_NAME, null)
                    ));
            assertThat(robot.lookup("#nextBtn").queryButton().isDisable()).isFalse();
            assertThat(Context.kubeConfigSelectionRef.get())
                    .usingRecursiveComparison()
                    .ignoringFields("path")
                    .isEqualTo(EXPECTED_CONFIG_CONTAINER);

            robot.clickOn("#nextBtn");
            verify(viewMock).showKubeNamespaceSelectionWindow();
        }
    }

    @Nested
    @ExtendWith(ApplicationExtension.class)
    class KubeConfigSelectionControllerChooserTest extends TestFxTest {

        private final View viewMock = mock(View.class);

        private void mockViewGetStage() {
            when(viewMock.getCurrentStage()).thenReturn(mock(Stage.class));
        }

        @Start
        private void start(Stage stage) throws Exception {
            mockViewGetStage();
            ServiceLocator.setView(viewMock);
            final PathProvider pathProvider = mock(PathProvider.class);
            final Path somePath = Paths.get("some_path");
            when(pathProvider.provideLocalHomePath()).thenReturn(somePath);
            final KubeConfigSelectionService kubeConfigSelectionService = mock(KubeConfigSelectionService.class);
            when(kubeConfigSelectionService.getConfigChoices(somePath)).thenReturn(Collections.emptyList());
            when(kubeConfigSelectionService.toKubeConfig(any(Path.class)))
                    .thenReturn(EXPECTED_CONFIG_CONTAINER);
            ServiceLocator.setModel(
                    Model.builder()
                            .pathProvider(pathProvider)
                            .kubeConfigSelectionService(kubeConfigSelectionService)
                            .build()
            );
            final FileChooserHelper fileChooserHelper = mock(FileChooserHelper.class);
            when(fileChooserHelper.getFile(anyString(), any(Stage.class)))
                    .thenReturn(TestUtil.getFile("/kubeconfig/ok/.kube/%s".formatted(KUBE_CONFIG_YML_FILE_NAME)));
            ServiceLocator.setFileChooserHelper(fileChooserHelper);
            View.getInstance().showKubeConfigSelectionWindow();
        }

        @Test
        void kubeConfigChoiceBoxIsHiddenAndChooserIsExecuted(FxRobot robot) {
            assertThat(robot.lookup("#nextBtn").queryButton().isDisable()).isTrue();
            assertThat(robot.lookup("#configSelector").queryAs(ChoiceBox.class).isShowing()).isFalse();
            assertThat(robot.lookup("#chooseFileLbl").queryAs(Label.class).isVisible()).isFalse();

            robot.clickOn("#fsChooserBtn");

            assertThat(robot.lookup("#selectedKubeConfigFileLbl").queryAs(Label.class).getText())
                    .isEqualTo("You selected: %s - %s".formatted(CONTEXT_NAME, KUBE_CONFIG_YML_FILE_NAME));
            assertThat(robot.lookup("#nextBtn").queryButton().isDisable()).isFalse();

            assertThat(Context.kubeConfigSelectionRef.get())
                    .isEqualTo(EXPECTED_CONFIG_CONTAINER);

            robot.clickOn("#nextBtn");
            verify(viewMock).showKubeNamespaceSelectionWindow();
        }
    }
}