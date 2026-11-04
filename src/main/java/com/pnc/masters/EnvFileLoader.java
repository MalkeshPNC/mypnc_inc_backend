package com.pnc.masters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads {@code .env} or {@code .env.staging} into system properties before Spring starts.
 * A variable already set in the process environment is left alone.
 */
final class EnvFileLoader {

    private EnvFileLoader() {
    }

    static void load(String[] args) {
        String fileName = stagingRequested(args) ? ".env.staging" : ".env";
        Path file = find(fileName);
        if (file == null) {
            System.err.println("No " + fileName + " file found. Using application.yml defaults.");
            return;
        }
        try {
            apply(parse(Files.readAllLines(file, StandardCharsets.UTF_8)));
            System.out.println("Loaded environment from " + file.toAbsolutePath());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + file.toAbsolutePath(), exception);
        }
    }

    static boolean stagingRequested(String[] args) {
        String profiles = System.getenv("SPRING_PROFILES_ACTIVE");
        String property = System.getProperty("spring.profiles.active");
        if (property != null && !property.isBlank()) {
            profiles = property;
        }
        if (args != null) {
            for (String arg : args) {
                if (arg != null && arg.startsWith("--spring.profiles.active=")) {
                    profiles = arg.substring("--spring.profiles.active=".length());
                }
            }
        }
        if (profiles == null || profiles.isBlank()) {
            return false;
        }
        for (String profile : profiles.split(",")) {
            if ("staging".equalsIgnoreCase(profile.trim())) {
                return true;
            }
        }
        return false;
    }

    static Map<String, String> parse(List<String> lines) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (trimmed.startsWith("export ")) {
                trimmed = trimmed.substring("export ".length()).trim();
            }
            int split = trimmed.indexOf('=');
            if (split <= 0) {
                continue;
            }
            String key = trimmed.substring(0, split).trim();
            String value = unquote(trimmed.substring(split + 1).trim());
            if (!key.isEmpty()) {
                values.put(key, value);
            }
        }
        return values;
    }

    static void apply(Map<String, String> values) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (System.getenv(entry.getKey()) != null) {
                continue;
            }
            if (System.getProperty(entry.getKey()) != null) {
                continue;
            }
            System.setProperty(entry.getKey(), entry.getValue());
        }
    }

    private static Path find(String fileName) {
        Path direct = Path.of(fileName);
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        Path nested = Path.of("dev-backend").resolve(fileName);
        if (Files.isRegularFile(nested)) {
            return nested;
        }
        return null;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
