package io.github.vcvitaly.k8cp.controller.helper;

import java.io.File;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class FileChooserHelperImpl implements FileChooserHelper {
    @Override
    public File getFile(String chooserTitle, Stage stage) {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(chooserTitle);
        return fileChooser.showOpenDialog(stage);
    }
}
