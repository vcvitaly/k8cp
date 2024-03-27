package io.github.vcvitaly.k8cp.context;

import io.github.vcvitaly.k8cp.client.KubeClient;
import io.github.vcvitaly.k8cp.client.LocalFsClient;
import io.github.vcvitaly.k8cp.client.impl.KubeClientImpl;
import io.github.vcvitaly.k8cp.client.impl.LocalFsClientImpl;
import io.github.vcvitaly.k8cp.controller.helper.FileChooserHelper;
import io.github.vcvitaly.k8cp.controller.helper.FileChooserHelperImpl;
import io.github.vcvitaly.k8cp.domain.KubeConfigContainer;
import io.github.vcvitaly.k8cp.model.Model;
import io.github.vcvitaly.k8cp.service.KubeConfigHelper;
import io.github.vcvitaly.k8cp.service.KubeConfigSelectionService;
import io.github.vcvitaly.k8cp.service.KubeService;
import io.github.vcvitaly.k8cp.service.LocalFsService;
import io.github.vcvitaly.k8cp.service.LocalOsFamilyDetector;
import io.github.vcvitaly.k8cp.service.PathProvider;
import io.github.vcvitaly.k8cp.service.RootInfoConverter;
import io.github.vcvitaly.k8cp.service.LocalRootResolver;
import io.github.vcvitaly.k8cp.service.SizeConverter;
import io.github.vcvitaly.k8cp.service.impl.KubeConfigHelperImpl;
import io.github.vcvitaly.k8cp.service.impl.KubeConfigSelectionServiceImpl;
import io.github.vcvitaly.k8cp.service.impl.KubeServiceImpl;
import io.github.vcvitaly.k8cp.service.impl.LocalFsServiceImpl;
import io.github.vcvitaly.k8cp.service.impl.LocalOsFamilyDetectorImpl;
import io.github.vcvitaly.k8cp.service.impl.PathProviderImpl;
import io.github.vcvitaly.k8cp.service.impl.RootInfoConverterImpl;
import io.github.vcvitaly.k8cp.service.impl.LocalRootResolverImpl;
import io.github.vcvitaly.k8cp.service.impl.SizeConverterImpl;
import io.github.vcvitaly.k8cp.util.Constants;
import io.github.vcvitaly.k8cp.view.View;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ServiceLocator {

    private static AtomicReference<Model> modelRef = new AtomicReference<>();
    private static AtomicReference<FileChooserHelper> fileChooserHelperRef = new AtomicReference<>();
    private static AtomicReference<View> viewRef = new AtomicReference<>();

    private static void logCreatedNewInstanceOf(Object o) {
        log.info(Constants.NEW_INSTANCE_OF_MSG.formatted(o.getClass().getSimpleName()));
    }

    private static RuntimeException logAndReturnRuntimeException(RuntimeException e) {
        log.error("Error: ", e);
        return e;
    }

    /* Getters and setters */
    public static synchronized Model getModel() {
        if (modelRef.get() == null) {
            modelRef.set(ModelHolder.instance);
        }
        return modelRef.get();
    }

    public static void setModel(Model model) {
        modelRef.set(model);
    }

    public static synchronized FileChooserHelper getFileChooserHelper() {
        if (fileChooserHelperRef.get() == null) {
            fileChooserHelperRef.set(FileChooserHelperHolder.instance);
        }
        return fileChooserHelperRef.get();
    }

    public static void setFileChooserHelper(FileChooserHelper fileChooserHelper) {
        fileChooserHelperRef.set(fileChooserHelper);
    }

    public static synchronized View getView() {
        if (viewRef.get() == null) {
            viewRef.set(View.getInstance());
        }
        return viewRef.get();
    }

    public static void setView(View view) {
        viewRef.set(view);
    }

    private static class KubeClientHolder {
        private static final KubeClient instance = createInstance();

        private static KubeClient createInstance() {
            final KubeConfigContainer kubeConfigContainer = Context.kubeConfigSelectionRef.get();
            if (kubeConfigContainer == null) {
                throw logAndReturnRuntimeException(new IllegalStateException("Kube config initialization has to be done first"));
            }
            try {
                final String configYml = Files.readString(Paths.get(kubeConfigContainer.path()));
                final KubeClient instance = new KubeClientImpl(configYml);
                logCreatedNewInstanceOf(instance);
                return instance;
            } catch (Exception e) {
                throw new RuntimeException("Could not read kube config due to - %s".formatted(e.getMessage()), e);
            }
        }
    }

    private static class SizeConverterHolder {
        private static final SizeConverter instance = createInstance();

        private static SizeConverter createInstance() {
            final SizeConverter sizeConverter = new SizeConverterImpl();
            logCreatedNewInstanceOf(sizeConverter);
            return sizeConverter;
        }
    }

    private static class KubeServiceHolder {
        private static final KubeService instance = createInstance();

        private static KubeService createInstance() {
            final KubeServiceImpl instance = new KubeServiceImpl(
                    KubeClientHolder.instance, SizeConverterHolder.instance
            );
            logCreatedNewInstanceOf(instance);
            return instance;
        }

        private static KubeService getInstance() {
            return instance;
        }
    }

    private static class LocalFsClientHolder {
        private static final LocalFsClient instance = createInstance();

        private static LocalFsClient createInstance() {
            final LocalFsClientImpl instance = new LocalFsClientImpl();
            logCreatedNewInstanceOf(instance);
            return instance;
        }
    }

    private static class LocalFsServiceHolder {
        private static final LocalFsService instance = createInstance();

        private static LocalFsService createInstance() {
            final LocalFsServiceImpl instance = new LocalFsServiceImpl(
                    LocalFsClientHolder.instance,
                    SizeConverterHolder.instance,
                    LocalRootResolverHolder.instance
            );
            logCreatedNewInstanceOf(instance);
            return instance;
        }
    }

    private static class KubeConfigHelperHolder {
        private static final KubeConfigHelper instance = createInstance();

        private static KubeConfigHelper createInstance() {
            final KubeConfigHelper instance = new KubeConfigHelperImpl();
            logCreatedNewInstanceOf(instance);
            return instance;
        }
    }

    private static class PathProviderHolder {
        private static final PathProvider instance = createInstance();

        private static PathProvider createInstance() {
            final PathProvider instance = new PathProviderImpl(LocalOsFamilyDetectorHolder.instance);
            logCreatedNewInstanceOf(instance);
            return instance;
        }
    }

    public static class KubeConfigSelectionServiceHolder {
        private static final KubeConfigSelectionService instance = createInstance();

        private static KubeConfigSelectionService createInstance() {
            final KubeConfigSelectionService instance = new KubeConfigSelectionServiceImpl(
                    LocalFsClientHolder.instance, KubeConfigHelperHolder.instance
            );
            logCreatedNewInstanceOf(instance);
            return instance;
        }
    }

    private static class LocalOsFamilyDetectorHolder {
        private static final LocalOsFamilyDetector instance = createInstance();

        private static LocalOsFamilyDetector createInstance() {
            final LocalOsFamilyDetector instance = new LocalOsFamilyDetectorImpl();
            logCreatedNewInstanceOf(instance);
            return instance;
        }
    }

    private static class LocalRootResolverHolder {
        private static final LocalRootResolver instance = createInstance();

        private static LocalRootResolver createInstance() {
            final LocalRootResolver instance = new LocalRootResolverImpl(
                    LocalFsClientHolder.instance, RootInfoConverterHolder.instance
            );
            logCreatedNewInstanceOf(instance);
            return instance;
        }
    }

    private static class RootInfoConverterHolder {
        private static final RootInfoConverter instance = createInstance();

        private static RootInfoConverter createInstance() {
            final RootInfoConverter instance = new RootInfoConverterImpl();
            logCreatedNewInstanceOf(instance);
            return instance;
        }
    }

    private static class ModelHolder {
        private static final Model instance = createInstance();

        private static Model createInstance() {
            return Model.builder()
                    .kubeServiceSupplier(() -> KubeServiceHolder.instance)
                    .localFsService(LocalFsServiceHolder.instance)
                    .kubeConfigSelectionService(KubeConfigSelectionServiceHolder.instance)
                    .pathProvider(PathProviderHolder.instance)
                    .localOsFamilyDetector(LocalOsFamilyDetectorHolder.instance)
                    .build();
        }
    }

    private static class FileChooserHelperHolder {
        private static final FileChooserHelper instance = createInstance();

        private static FileChooserHelper createInstance() {
            final FileChooserHelper instance = new FileChooserHelperImpl();
            logCreatedNewInstanceOf(instance);
            return instance;
        }
    }
}
