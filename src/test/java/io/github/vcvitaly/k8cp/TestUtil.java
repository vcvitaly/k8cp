package io.github.vcvitaly.k8cp;

import io.github.vcvitaly.k8cp.context.Context;
import io.github.vcvitaly.k8cp.context.ServiceLocator;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestUtil {

    public static File getFile(String resourcePath) throws URISyntaxException {
        return new File(getUri(resourcePath));
    }

    public static Path getPath(String resourcePath) {
        try {
            return Paths.get(getUri(resourcePath));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static URI getUri(String resourcePath) throws URISyntaxException {
        return TestUtil.class.getResource(resourcePath).toURI();
    }

    public static void cleanupContext() {
        nullifyAllAtomicReferences(Context.class);
        nullifyAllAtomicReferences(ServiceLocator.class);
    }

    private static void nullifyAllAtomicReferences(Class<?> clazz) {
        Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.getType().isAssignableFrom(AtomicReference.class))
                .forEach(f -> {
                    try {
                        f.setAccessible(true);
                        final AtomicReference ref = (AtomicReference) f.get(null);
                        ref.set(null);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
