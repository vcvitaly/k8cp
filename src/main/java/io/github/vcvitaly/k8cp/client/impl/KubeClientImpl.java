package io.github.vcvitaly.k8cp.client.impl;

import io.github.vcvitaly.k8cp.client.KubeClient;
import io.github.vcvitaly.k8cp.domain.KubeNamespace;
import io.github.vcvitaly.k8cp.domain.KubePod;
import io.github.vcvitaly.k8cp.exception.KubeApiException;
import io.github.vcvitaly.k8cp.exception.KubeExecException;
import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

import static java.util.Collections.emptyList;

@Slf4j
public class KubeClientImpl implements KubeClient {

    private static final int WAIT_TIMEOUT_MS = 250;
    public static final String UNKNOWN_OBJECT_NAME = "UNKNOWN";
    private final Exec exec;
    private final CoreV1Api api;

    public KubeClientImpl(String configYml) {
        ApiClient client;
        try (final StringReader sr = new StringReader(configYml)) {
            client = Config.fromConfig(sr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Configuration.setDefaultApiClient(client);
        exec = new Exec();
        api = new CoreV1Api();
    }

    @Override
    public List<String> execAndReturnOut(String namespace, String podName, String[] cmdParts) throws KubeExecException {
        try {
            return executeAndReturnOutInternal(namespace, podName, cmdParts);
        } catch (IOException | ApiException | InterruptedException e) {
            throw new KubeExecException(
                    "Could not execute %s".formatted(Arrays.toString(cmdParts)), e
            );
        }
    }

    @Override
    public List<KubeNamespace> getNamespaces() throws KubeApiException {
        try {
            final V1NamespaceList v1NamespaceList = api.listNamespace().execute();
            return v1NamespaceList.getItems().stream()
                    .map(this::extractNamespaceName)
                    .map(KubeNamespace::new)
                    .toList();
        } catch (ApiException e) {
            throw new KubeApiException("Could not get a list of namespaces", e);
        }
    }

    @Override
    public List<KubePod> getPods(String namespace) throws KubeApiException {
        try {
            final V1PodList v1PodList = api.listNamespacedPod(namespace).execute();
            return v1PodList.getItems().stream()
                    .map(this::extractPodName)
                    .map(KubePod::new)
                    .toList();
        } catch (ApiException e) {
            throw new KubeApiException("Could not get a list of pods in [%s] namespace".formatted(namespace), e);
        }
    }

    private List<String> executeAndReturnOutInternal(String namespace, String podName, String[] cmdParts) throws IOException, ApiException, InterruptedException {
        boolean tty = System.console() != null;
        final Process proc =
                exec.exec(namespace, podName, cmdParts, true, tty);
        final var ref = new Object() {
            List<String> outLines = new ArrayList<>();
            List<String> errLines = new ArrayList<>();
        };

        final Thread out = Thread.ofVirtual().start(() -> {
            ref.outLines.addAll(readStream(proc.getInputStream(), "output"));
        });

        final Thread err = Thread.ofVirtual().start(() -> {
            ref.errLines.addAll(readStream(proc.getErrorStream(), "error"));
        });

        proc.waitFor();

        out.join();
        err.join();

        proc.destroy();

        if (!ref.errLines.isEmpty()) {
            throw new IOException("Err output: %s".formatted(ref.errLines));
        }

        final int exitValue = proc.exitValue();
        if (exitValue != 0) {
            log.error("Exit code [%d] while running %s".formatted(exitValue, Arrays.toString(cmdParts)));
        }

        return ref.outLines;
    }

    private List<String> readStream(InputStream is, String streamType) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            return readStream(br);
        } catch (IOException | InterruptedException ex) {
            log.error("An error while reading %s stream: ".formatted(streamType), ex);
        }
        return emptyList();
    }

    private List<String> readStream(BufferedReader br) throws IOException, InterruptedException {
        List<String> out = new ArrayList<>();

        int i = 0;

        boolean readingInProgress = false;
        while (true) {
            if (br.ready()) {
                String line = br.readLine();
                out.add(line);
                readingInProgress = true;
            } else {
                if (readingInProgress) {
                    break;
                }
                Thread.sleep(1);
                i++;
                if (i >= WAIT_TIMEOUT_MS) {
                    break;
                }
            }
        }

        return out;
    }

    private String extractNamespaceName(V1Namespace v1Namespace) {
        return extractObjectName(v1Namespace.getMetadata());
    }

    private String extractObjectName(V1ObjectMeta metadata) {
        if (metadata == null) {
            return UNKNOWN_OBJECT_NAME;
        } else {
            final String name = metadata.getName();
            return name == null ? UNKNOWN_OBJECT_NAME : name;
        }
    }

    private String extractPodName(V1Pod v1Pod) {
        return extractObjectName(v1Pod.getMetadata());
    }
}
