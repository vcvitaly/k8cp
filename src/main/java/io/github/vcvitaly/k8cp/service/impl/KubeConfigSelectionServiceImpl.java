package io.github.vcvitaly.k8cp.service.impl;

import io.github.vcvitaly.k8cp.client.LocalFsClient;
import io.github.vcvitaly.k8cp.domain.KubeConfigContainer;
import io.github.vcvitaly.k8cp.exception.IOOperationException;
import io.github.vcvitaly.k8cp.exception.KubeConfigLoadingException;
import io.github.vcvitaly.k8cp.exception.KubeContextExtractionException;
import io.github.vcvitaly.k8cp.service.KubeConfigSelectionService;
import io.github.vcvitaly.k8cp.service.KubeConfigHelper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class KubeConfigSelectionServiceImpl implements KubeConfigSelectionService {

    private final LocalFsClient localFsClient;
    private final KubeConfigHelper kubeConfigHelper;

    @Override
    public List<KubeConfigContainer> getConfigChoices(Path kubeFolderPath) throws IOOperationException, KubeContextExtractionException {
        final List<KubeConfigContainer> list = new ArrayList<>();
        for (Path path : localFsClient.listFiles(kubeFolderPath)) {
            if (!Files.isDirectory(path) && Files.isReadable(path) && kubeConfigHelper.validate(path.toString())) {
                KubeConfigContainer kubeConfigContainer = toKubeConfig(path);
                list.add(kubeConfigContainer);
            }
        }
        return list;
    }

    @Override
    public KubeConfigContainer toKubeConfig(Path path) throws KubeContextExtractionException {
        final String pathStr = path.toString();
        return KubeConfigContainer.builder()
                .contextName(getContextName(pathStr))
                .fileName(path.getFileName().toString())
                .path(pathStr)
                .build();
    }

    private String getContextName(String kubeConfigPath) throws KubeContextExtractionException {
        try {
            return kubeConfigHelper.extractContextName(kubeConfigPath);
        } catch (IOOperationException | KubeConfigLoadingException e) {
            throw new KubeContextExtractionException("Could not extract context name from " + kubeConfigPath, e);
        }
    }
}
