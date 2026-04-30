package com.noah.minecraftagent.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public final class SecureLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("MinecraftAgent");
    private static final Pattern BEARER = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]{8,}");
    private static final Pattern AUTH_HEADER = Pattern.compile("(?i)(authorization\\s*[:=]\\s*)[^,}\\s]+(?:\\s+[^,}\\s]+)?");
    private static final Pattern API_KEY = Pattern.compile("(?i)((api[_-]?key|x-api-key|key|token|secret)(\\\"?\\s*[:=]\\s*\\\"?))[^\\\",&\\s}]{6,}");
    private static final Pattern QUERY_SECRET = Pattern.compile("(?i)([?&](?:api[_-]?key|key|token|secret)=)[^&\\s]+ ");
    private static final Pattern PROXY_PASSWORD = Pattern.compile("(?i)(https?://[^:/\\s]+:)[^@/\\s]+(@)");

    private SecureLog() {
    }

    public static void info(String message, Object... args) {
        LOGGER.info(mask(message), maskArgs(args));
    }

    public static void warn(String message, Object... args) {
        LOGGER.warn(mask(message), maskArgs(args));
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.error(mask(message), throwable);
    }

    public static String mask(String value) {
        if (value == null) {
            return null;
        }
        String result = BEARER.matcher(value).replaceAll("$1***");
        result = AUTH_HEADER.matcher(result).replaceAll("$1***");
        result = API_KEY.matcher(result).replaceAll("$1***");
        result = QUERY_SECRET.matcher(result + " ").replaceAll("$1***").trim();
        result = PROXY_PASSWORD.matcher(result).replaceAll("$1***$2");
        return result;
    }

    private static Object[] maskArgs(Object[] args) {
        Object[] masked = new Object[args.length];
        for (int index = 0; index < args.length; index++) {
            masked[index] = args[index] instanceof String text ? mask(text) : args[index];
        }
        return masked;
    }
}
