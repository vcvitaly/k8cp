package io.github.vcvitaly.k8cp.service.impl;

import io.github.vcvitaly.k8cp.TestUtil;
import io.github.vcvitaly.k8cp.exception.KubeConfigLoadingException;
import java.io.File;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KubeConfigHelperImplTest {

    private final KubeConfigHelperImpl kubeConfigHelper = new KubeConfigHelperImpl();

    @Test
    void extractContextNameTest_success() throws Exception {
        final File resource = TestUtil.getFile("/kubeconfig/ok/.kube/kube_config.yml");
        final String contextName = kubeConfigHelper.extractContextName(resource.toString());

        assertThat(contextName).isEqualTo("kind-kind");
    }

    @Test
    void extractContextNameTest_error() throws Exception {
        final File resource = TestUtil.getFile("/kubeconfig/broken/broken_config.yml");
        assertThatThrownBy(() -> kubeConfigHelper.extractContextName(resource.toString()))
                .isInstanceOf(KubeConfigLoadingException.class);
    }
}