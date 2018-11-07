module jxclient {

    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.base;
    requires controlsfx;
    requires java.prefs;
    requires java.net.http;

    requires matrix.common.api;
    requires matrix.client.api;
    requires matrix.client.impl;
    requires matrix.support.jackson;
    requires java.ws.rs;

    requires com.fasterxml.jackson.databind;

    opens io.github.ma1uta.jxclient to javafx.graphics;
    opens io.github.ma1uta.jxclient.ui to javafx.fxml;

}
