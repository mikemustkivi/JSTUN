package de.javawi.jstun.logging;


public interface Logger {
    void info(String s, Object... vars);

    void error(String s, Exception e);

    void debug(String s, Object... vars);
}
