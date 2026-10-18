package de.pf2craft.npc;

import de.pf2craft.Pf2Craft;
import de.pf2craft.core.monster.MonsterCatalog;
import de.pf2craft.core.monster.MonsterData;
import de.pf2craft.core.monster.MonsterParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;

/** Loads the monster catalog: Monster Core from the jar, then templates from the server config folder. */
public final class MonsterDataLoader {
    private static final String JAR_FILE = "/pf2craft/monsters/monster-core.json";
    private static volatile MonsterCatalog catalog = new MonsterCatalog(List.of());

    private MonsterDataLoader() {
    }

    public static MonsterCatalog catalog() {
        return catalog;
    }

    public static void load(MinecraftServer server) {
        List<MonsterData> all = new ArrayList<>();
        try (InputStream in = MonsterDataLoader.class.getResourceAsStream(JAR_FILE)) {
            if (in == null) {
                throw new IOException("missing resource " + JAR_FILE);
            }
            all.addAll(MonsterParser.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException | RuntimeException e) {
            Pf2Craft.LOGGER.error("Skipping built-in monster file {}: {}", JAR_FILE, e.getMessage());
        }
        Path dir = server.getRunDirectory().resolve("config").resolve("pf2craft").resolve("monsters");
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                for (Path p : files.filter(f -> f.getFileName().toString().endsWith(".json")).sorted().toList()) {
                    try {
                        all.addAll(MonsterParser.parse(Files.readString(p, StandardCharsets.UTF_8)));
                    } catch (IOException | RuntimeException e) {
                        Pf2Craft.LOGGER.error("Skipping monster file {}: {}", p, e.getMessage());
                    }
                }
            } catch (IOException e) {
                Pf2Craft.LOGGER.error("Cannot list {}: {}", dir, e.getMessage());
            }
        }
        catalog = new MonsterCatalog(all);
        Pf2Craft.LOGGER.info("Loaded {} monster(s)", catalog.size());
    }
}
