package io.github.vcvitaly.k8cp.client.impl;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import io.github.vcvitaly.k8cp.TestUtil;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.tinyzip.TinyZip;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFsClientImplTest {

    private final LocalFsClientImpl localFsClient = new LocalFsClientImpl();

    @Test
    void listsFilesTest() throws Exception {
        final Configuration configuration = Configuration.forCurrentPlatform().toBuilder().setWorkingDirectory("/").build();
        try (final FileSystem fs = Jimfs.newFileSystem(configuration)) {
            final Path root = fs.getPath("/");

            TinyZip.unzip(TestUtil.getPath("/test_fs_1.zip"), root);

            final List<Path> paths = localFsClient.listFiles(root);
            assertThat(paths.stream().map(p -> p.getFileName().toString()))
                    .containsExactlyInAnyOrderElementsOf(List.of("root", "home", "etc"));
        }
    }
}