package io.github.vcvitaly.k8cp.client;

import io.github.vcvitaly.k8cp.exception.IOOperationException;
import java.nio.file.Path;
import java.util.List;

public interface LocalFsClient {

    List<Path> listFiles(Path path) throws IOOperationException;

}
