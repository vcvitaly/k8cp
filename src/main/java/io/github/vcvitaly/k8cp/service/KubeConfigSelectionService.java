package io.github.vcvitaly.k8cp.service;

import io.github.vcvitaly.k8cp.domain.KubeConfigContainer;
import io.github.vcvitaly.k8cp.exception.IOOperationException;
import io.github.vcvitaly.k8cp.exception.KubeContextExtractionException;
import java.nio.file.Path;
import java.util.List;

public interface KubeConfigSelectionService {

    List<KubeConfigContainer> getConfigChoices(Path kubeFolderPath) throws IOOperationException, KubeContextExtractionException;

    KubeConfigContainer toKubeConfig(Path path) throws KubeContextExtractionException;
}
