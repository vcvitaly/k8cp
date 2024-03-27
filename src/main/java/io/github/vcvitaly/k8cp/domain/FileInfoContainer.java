package io.github.vcvitaly.k8cp.domain;

import io.github.vcvitaly.k8cp.enumeration.FileSizeUnit;
import io.github.vcvitaly.k8cp.enumeration.FileType;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileInfoContainer implements Comparable<FileInfoContainer> {
    private Path path;
    private String name;
    private Long sizeBytes;
    private Integer size;
    private FileSizeUnit sizeUnit;
    private FileType fileType;
    private LocalDateTime changedAt;

    @Override
    public int compareTo(FileInfoContainer fileInfoContainer) {
        return Comparator.comparing((FileInfoContainer container) -> container.getFileType().getPriority())
                .thenComparing(FileInfoContainer::getName)
                .compare(this, fileInfoContainer);
    }
}
