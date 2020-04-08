package de.javawi.jstun.logging;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class SimpleLogger implements Logger {
    private final String prefix;
    public static String PATTERN_DATE_TIME_MS = "yyyy-MM-dd HH:mm:ss.SSS";
    static TimeZone timeZoneGMT2 = TimeZone.getTimeZone("GMT+2");

    public SimpleLogger(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void info(String s, Object... vars) {
        writeToConsole(replaceCurlies(s, vars));
    }

    @Override
    public void error(String s, Exception e) {
        writeToConsole(String.format("%s %s", s, e.toString()));
    }

    @Override
    public void debug(String s, Object... vars) {
        writeToConsole(replaceCurlies(s, vars));
    }

    private String replaceCurlies(String sourceWithCurlies, Object... vars) {
        String result = sourceWithCurlies;
        for (Object var : vars) {
            result = result.replaceFirst("[{][}]", var.toString());
        }
        return result;
    }

    private String nowAsStr() {
        DateFormat df = new SimpleDateFormat(PATTERN_DATE_TIME_MS, Locale.US);
        df.setTimeZone(timeZoneGMT2);
        return df.format(new Date(System.currentTimeMillis()));
    }

    private void writeToConsole(String text) {
        String data = String.format("%s %s %s", nowAsStr(), prefix, text);
        System.out.println(data);
    }
}
