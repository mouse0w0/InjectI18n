package com.github.mouse0w0.injecti18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class I18n {

    private static final Map<String, String> TRANSLATION_MAP = new HashMap<>();

    static {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                I18n.class.getResourceAsStream("/inject_i18n.lang"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] split = line.split("=", 2);
                if (split.length != 2) {
                    System.out.println("WARNING: Incorrect language entry \"" + line + "\"");
                    continue;
                }
                TRANSLATION_MAP.put(split[0], split[1]);
            }
            System.out.println("Loaded language file: " + TRANSLATION_MAP.size() + " entries.");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("WARNING: No found language file.");
        }
    }

    public static String translate(String key) {
        return TRANSLATION_MAP.getOrDefault(key, key);
    }
}
