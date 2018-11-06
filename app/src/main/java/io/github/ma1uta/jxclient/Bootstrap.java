package io.github.ma1uta.jxclient;

import io.github.ma1uta.jxclient.splash.Splash;
import javafx.application.Application;

/**
 * Bootstrap class.
 */
public class Bootstrap {

    private Bootstrap() {
        // singleton
    }

    /**
     * App entry point.
     *
     * @param args program arguments
     */
    public static void main(String[] args) {
        System.setProperty("javafx.preloader", Splash.class.getName());
        System.setProperty("jdk.internal.httpclient.debug", "true");
        Application.launch(Client.class, args);
    }
}
