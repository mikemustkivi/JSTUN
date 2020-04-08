package de.javawi.jstun.logging;

import java.util.HashMap;
import java.util.Map;

public class LoggerFactory {
    private static Map<String, Logger> classToLoggerMap = new HashMap<>();

    public static <T> Logger getLogger(Class<T> klass) {
        Logger logger = classToLoggerMap.get(klass.getName());
        if (logger == null) {
            logger = new SimpleLogger(klass.getName());
            classToLoggerMap.put(klass.getName(), logger);
        }
        return logger;
    }
}
