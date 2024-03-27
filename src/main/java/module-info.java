module io.github.vcvitaly.k8cp {
    requires javafx.controls;
    requires javafx.fxml;

    requires java.annotation;
//    requires jsr305;

    requires org.controlsfx.controls;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;

    requires org.slf4j;
    requires ch.qos.logback.core;
    requires ch.qos.logback.classic;

    requires static lombok;
    requires io.kubernetes.client.java;
    requires io.kubernetes.client.java.api;

    requires org.apache.commons.io;
    requires org.yaml.snakeyaml;
    requires com.google.gson;
    requires kotlin.stdlib;
    requires org.apache.commons.lang3;
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.util;
    requires jdk.crypto.ec;

    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.fontawesome;
    requires org.kordamp.ikonli.material2;

//    requires jdk.jdwp.agent;

    opens io.github.vcvitaly.k8cp to javafx.fxml;
    exports io.github.vcvitaly.k8cp;
    exports io.github.vcvitaly.k8cp.controller;
    exports io.github.vcvitaly.k8cp.controller.menu;
    exports io.github.vcvitaly.k8cp.controller.pane;
    exports io.github.vcvitaly.k8cp.controller.init;
    exports io.github.vcvitaly.k8cp.domain;
    exports io.github.vcvitaly.k8cp.enumeration;
    exports io.github.vcvitaly.k8cp.util;
    exports io.github.vcvitaly.k8cp.exception;
    exports io.github.vcvitaly.k8cp.context;
    opens io.github.vcvitaly.k8cp.controller to javafx.fxml;
    opens io.github.vcvitaly.k8cp.controller.menu to javafx.fxml;
    opens io.github.vcvitaly.k8cp.controller.init to javafx.fxml;
    opens io.github.vcvitaly.k8cp.controller.pane to javafx.fxml;
    opens io.github.vcvitaly.k8cp.util to javafx.fxml;
}