package de.pf2craft.spell;

import de.pf2craft.Pf2Craft;
import de.pf2craft.core.spell.SpellCatalog;
import de.pf2craft.core.spell.SpellCatalogParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;

/** Loads the spell catalog: built-in spells from the jar, then custom files from the server config folder. */
public final class SpellData {
    private static final String JAR_FILE = "/pf2craft/spells/spells.json";
    private static volatile SpellCatalog catalog = SpellCatalog.empty();

    private SpellData() {
    }

    public static SpellCatalog catalog() {
        return catalog;
    }

    public static void load(MinecraftServer server) {
        SpellCatalog all = SpellCatalog.empty();
        try (InputStream in = SpellData.class.getResourceAsStream(JAR_FILE)) {
            if (in == null) {
                throw new IOException("missing resource " + JAR_FILE);
            }
            all = all.merged(parse(new String(in.readAllBytes(), StandardCharsets.UTF_8), JAR_FILE));
        } catch (IOException | RuntimeException e) {
            Pf2Craft.LOGGER.error("Skipping built-in spell file {}: {}", JAR_FILE, e.getMessage());
        }
        Path dir = server.getRunDirectory().resolve("config").resolve("pf2craft").resolve("spells");
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                for (Path p : files.filter(f -> f.getFileName().toString().endsWith(".json")).sorted().toList()) {
                    try {
                        all = all.merged(parse(Files.readString(p, StandardCharsets.UTF_8), p.toString()));
                    } catch (IOException | RuntimeException e) {
                        Pf2Craft.LOGGER.error("Skipping spell file {}: {}", p, e.getMessage());
                    }
                }
            } catch (IOException e) {
                Pf2Craft.LOGGER.error("Cannot list {}: {}", dir, e.getMessage());
            }
        }
        catalog = all;
        Pf2Craft.LOGGER.info("Loaded {} spell(s)", all.all().size());
    }

    private static SpellCatalog parse(String json, String source) {
        SpellCatalogParser.ParseResult result = SpellCatalogParser.parse(json);
        result.errors().forEach(e -> Pf2Craft.LOGGER.error("Spell file {}: {}", source, e));
        return result.catalog();
    }
}
