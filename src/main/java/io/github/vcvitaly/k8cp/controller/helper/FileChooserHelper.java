package io.github.vcvitaly.k8cp.controller.helper;

import java.io.File;
import javafx.stage.Stage;

public interface FileChooserHelper {

    File getFile(String chooserTitle, Stage stage);
}
