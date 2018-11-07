package io.github.ma1uta.jxclient.splash;

import javafx.application.Preloader;

/**
 * Error notification.
 */
public class ErrorNotification implements Preloader.PreloaderNotification {

    private final Throwable throwable;

    public ErrorNotification(Throwable throwable) {
        this.throwable = throwable;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
