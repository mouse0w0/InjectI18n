package com.github.mouse0w0.injecti18n;

import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public final class I18n {

    private static final Path LANGUAGE_FOLDER = Paths.get(System.getProperty("user.dir"), "lang");
    private static final Path LANGUAGE_FILE = LANGUAGE_FOLDER.resolve(".lang");

    private static final boolean DEBUG = "true".equals(System.getProperty("i18n.debug"));
    private static final boolean WATCH = "true".equals(System.getProperty("i18n.watch"));

    private static Map<String, String> TRANSLATION_MAP = new HashMap<>();

    static {
        reload();
        watch();
    }

    public static String translate(String key) {
        if (DEBUG) {
            System.out.println(key);
        }
        return TRANSLATION_MAP.getOrDefault(key, key);
    }

    private static synchronized void reload() {
        try {
            if (!Files.exists(LANGUAGE_FILE)) {
                initDefault();
            }

            Map<String, String> translationMap = new HashMap<>();
            for (String line : Files.readAllLines(LANGUAGE_FILE, StandardCharsets.UTF_8)) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] split = line.split("=", 2);
                if (split.length != 2) {
                    System.out.println("WARNING: Incorrect language entry \"" + line + "\"");
                    continue;
                }
                translationMap.put(split[0], split[1]);
            }

            if (translationMap.size() == 0) {
                System.out.println("WARNING: Failed to load language file. Nothing read.");
                return;
            }

            TRANSLATION_MAP = translationMap;
            System.out.println("Loaded language file: " + translationMap.size() + " entries.");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("WARNING: Failed to load language file.");
        } catch (NullPointerException e) {
            System.out.println("WARNING: No found language file.");
        }
    }

    private static void initDefault() throws IOException {
        if (!Files.exists(LANGUAGE_FOLDER)) {
            Files.createDirectories(LANGUAGE_FOLDER);
        }

        try (InputStream input = I18n.class.getResourceAsStream("/generated.lang")) {
            Files.copy(input, LANGUAGE_FILE);
        }
    }

    private static void watch() {
        if (!WATCH) return;

        System.out.println("Start watching language file: " + LANGUAGE_FILE.toAbsolutePath());
        Thread thread = new Thread(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                WatchKey key = LANGUAGE_FOLDER.register(watchService,
                        new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE},
                        SensitivityWatchEventModifier.HIGH);
                while (true) {
                    watchService.take();
                    if (key.pollEvents().size() != 0) {
                        reload();
                    }
                    key.reset();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }
}
