package io.github.vcvitaly.k8cp.context;

import io.github.vcvitaly.k8cp.domain.KubeConfigContainer;
import java.util.concurrent.atomic.AtomicReference;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Context {

    public static final AtomicReference<KubeConfigContainer> kubeConfigSelectionRef = new AtomicReference<>();
}
