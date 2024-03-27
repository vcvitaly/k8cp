package io.github.vcvitaly.k8cp.client.impl;

import io.github.vcvitaly.k8cp.client.LocalFsClient;
import io.github.vcvitaly.k8cp.exception.IOOperationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class LocalFsClientImpl implements LocalFsClient {
    @Override
    public List<Path> listFiles(Path path) throws IOOperationException {
        try (final Stream<Path> pathStream = Files.list(path)) {
            return pathStream.toList();
        } catch (IOException e) {
            throw new IOOperationException("An error while listing files in " + path, e);
        }
    }

}
