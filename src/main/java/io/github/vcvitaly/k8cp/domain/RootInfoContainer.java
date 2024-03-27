package io.github.vcvitaly.k8cp.domain;

import java.nio.file.Path;

public record RootInfoContainer(Path path, String name) {

    @Override
    public String toString() {
        return name;
    }
}
