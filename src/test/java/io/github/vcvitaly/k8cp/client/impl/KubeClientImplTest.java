package io.github.vcvitaly.k8cp.client.impl;

import io.github.vcvitaly.k8cp.K3sTest;
import io.github.vcvitaly.k8cp.domain.KubeNamespace;
import io.github.vcvitaly.k8cp.domain.KubePod;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KubeClientImplTest extends K3sTest {

    private final KubeClientImpl kubeClient = new KubeClientImpl(K3S.getKubeConfigYaml());

    @Test
    void getNamespacesTest() throws Exception {
        final List<KubeNamespace> namespaces = kubeClient.getNamespaces();

        assertThat(namespaces)
                .contains(new KubeNamespace(DEFAULT_NAMESPACE));
    }

    @Test
    void getPodsTest() throws Exception {
        final List<KubePod> pods = kubeClient.getPods(DEFAULT_NAMESPACE);

        assertThat(pods)
                .contains(new KubePod(nginxPodName));
    }

    @Test
    void execAndReturnOutTest() throws Exception {
        final List<String> lines = kubeClient.execAndReturnOut(DEFAULT_NAMESPACE, nginxPodName, new String[]{"ls", "/"});

        assertThat(lines).contains("root", "home");
    }
}