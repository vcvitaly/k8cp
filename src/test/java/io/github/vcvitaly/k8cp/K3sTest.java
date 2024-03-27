package io.github.vcvitaly.k8cp;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Yaml;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class K3sTest {

    @Container
    protected static final K3sContainer K3S = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.27.4-k3s1"));
    protected static final String DEFAULT_NAMESPACE = "default";
    protected static V1Pod nginxPod;
    protected static String nginxPodName;

    @BeforeAll
    static void beforeK3sTest() throws Exception {
        final ApiClient client = getClient(K3S);
        final CoreV1Api api = getApi(client);
        nginxPod = createNginxPod(api);
        nginxPodName = nginxPod.getMetadata().getName();
        for (int i = 0; i < 120; i++) {
            final V1PodList list = api.listNamespacedPod(DEFAULT_NAMESPACE).execute();
            final V1Pod foundNginxPod = list.getItems().stream().filter(pod -> pod.getMetadata().getName().equals(nginxPodName))
                    .findFirst().get();
            final String phase = foundNginxPod.getStatus().getPhase();
            if (phase.equals("Running")) {
                break;
            }
            Thread.sleep(1_000);
            System.out.printf("Waiting for %s to be started%n", nginxPodName);
        }
        System.out.println("Nginx pod is started");
    }

    private static ApiClient getClient(K3sContainer k3s) throws IOException {
        final String kubeConfigYaml = k3s.getKubeConfigYaml();
        return Config.fromConfig(new StringReader(kubeConfigYaml));
    }

    private static CoreV1Api getApi(ApiClient client) throws IOException {
        return new CoreV1Api(client);
    }

    private static V1Pod createPod(CoreV1Api api, String namespace, String yamlPath) throws URISyntaxException, IOException, ApiException {
        File file = TestUtil.getFile(yamlPath);
        V1Pod yamlPod = (V1Pod) Yaml.load(file);
        yamlPod.getSpec().setOverhead(null);
        return api.createNamespacedPod(namespace, yamlPod).execute();
    }

    private static V1Pod createNginxPod(CoreV1Api coreApi) throws URISyntaxException, IOException, ApiException {
        return createPod(coreApi, DEFAULT_NAMESPACE, "/nginx.yml");
    }
}
