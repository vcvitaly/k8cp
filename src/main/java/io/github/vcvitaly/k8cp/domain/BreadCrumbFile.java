package io.github.vcvitaly.k8cp.domain;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BreadCrumbFile {
    private Path path;
    private String name;

    @Override
    public String toString() {
        return name;
    }
}
