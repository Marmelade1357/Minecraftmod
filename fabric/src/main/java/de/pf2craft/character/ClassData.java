package de.pf2craft.character;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.pf2craft.Pf2Craft;
import de.pf2craft.core.progression.ClassProgression;
import de.pf2craft.core.progression.ClassProgressionParser;
import de.pf2craft.core.progression.ClassRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;

/** Loads class progression data: built-in files from the jar, plus overrides from the server config folder. */
public final class ClassData {
    private static final String BASE = "/pf2craft/classes/";
    private static volatile ClassRegistry registry = new ClassRegistry(List.of());

    private ClassData() {
    }

    public static ClassRegistry registry() {
        return registry;
    }

    public static void load(MinecraftServer server) {
        Map<String, ClassProgression> classes = new LinkedHashMap<>();
        for (String file : readIndex()) {
            try (InputStream in = ClassData.class.getResourceAsStream(BASE + file)) {
                if (in == null) {
                    throw new IOException("missing resource " + file);
                }
                put(classes, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException | RuntimeException e) {
                Pf2Craft.LOGGER.error("Skipping built-in class file {}: {}", file, e.getMessage());
            }
        }

        Path dir = server.getRunDirectory().resolve("config").resolve("pf2craft").resolve("classes");
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                for (Path p : files.filter(f -> f.getFileName().toString().endsWith(".json")).sorted().toList()) {
                    try {
                        put(classes, Files.readString(p, StandardCharsets.UTF_8));
                    } catch (IOException | RuntimeException e) {
                        Pf2Craft.LOGGER.error("Skipping class file {}: {}", p, e.getMessage());
                    }
                }
            } catch (IOException e) {
                Pf2Craft.LOGGER.error("Cannot read {}: {}", dir, e.getMessage());
            }
        }
        registry = new ClassRegistry(new ArrayList<>(classes.values()));
        Pf2Craft.LOGGER.info("Loaded {} class definition(s)", registry.size());
    }

    private static void put(Map<String, ClassProgression> classes, String json) {
        ClassProgression c = ClassProgressionParser.parse(json);
        classes.put(c.name().toLowerCase(Locale.ROOT), c);
    }

    private static List<String> readIndex() {
        try (InputStream in = ClassData.class.getResourceAsStream(BASE + "index.json")) {
            if (in == null) {
                Pf2Craft.LOGGER.error("Built-in class index is missing");
                return List.of();
            }
            JsonElement root = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            List<String> names = new ArrayList<>();
            root.getAsJsonArray().forEach(e -> names.add(e.getAsString()));
            return names;
        } catch (IOException | RuntimeException e) {
            Pf2Craft.LOGGER.error("Cannot read built-in class index: {}", e.getMessage());
            return List.of();
        }
    }
}
