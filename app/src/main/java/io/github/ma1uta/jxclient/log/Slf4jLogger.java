package io.github.ma1uta.jxclient.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

public class Slf4jLogger implements System.Logger {

    private final String name;
    private final Logger logger;

    public Slf4jLogger(String name, Module module) {
        this.name = name;
        logger = LoggerFactory.getLogger(module.getName() + "-" + name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isLoggable(Level level) {
        switch (level) {
            case ALL:
                return true;
            case DEBUG:
                return logger.isDebugEnabled();
            case INFO:
                return logger.isInfoEnabled();
            case ERROR:
                return logger.isErrorEnabled();
            case TRACE:
                return logger.isTraceEnabled();
            case WARNING:
                return logger.isWarnEnabled();
            case OFF:
            default:
                return false;
        }
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
        switch (level) {
            case ALL:
            case TRACE:
                logger.trace(msg, thrown);
                break;
            case DEBUG:
                logger.debug(msg, thrown);
                break;
            case ERROR:
                logger.error(msg, thrown);
                break;
            case INFO:
                logger.info(msg, thrown);
                break;
            case WARNING:
                logger.warn(msg, thrown);
                break;
            case OFF:
            default:
                // nothing
        }
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) {
        switch (level) {
            case ALL:
            case TRACE:
                logger.trace(format, params);
                break;
            case DEBUG:
                logger.debug(format, params);
                break;
            case ERROR:
                logger.error(format, params);
                break;
            case INFO:
                logger.info(format, params);
                break;
            case WARNING:
                logger.warn(format, params);
                break;
            case OFF:
            default:
                // nothing
        }
    }
}
