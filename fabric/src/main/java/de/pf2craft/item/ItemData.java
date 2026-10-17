package de.pf2craft.item;

import de.pf2craft.Pf2Craft;
import de.pf2craft.core.item.ArmorData;
import de.pf2craft.core.item.ItemCatalog;
import de.pf2craft.core.item.ItemCatalogParser;
import de.pf2craft.core.item.ShieldData;
import de.pf2craft.core.item.WeaponData;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.server.MinecraftServer;

/** Loads the item catalog: built-in files from the jar, then entries from the server config folder. */
public final class ItemData {
    private static final String BASE = "/pf2craft/items/";
    private static volatile ItemCatalog catalog = new ItemCatalog(List.of(), List.of(), List.of());

    private ItemData() {
    }

    public static ItemCatalog catalog() {
        return catalog;
    }

    public static void load(MinecraftServer server) {
        Path dir = server.getRunDirectory().resolve("config").resolve("pf2craft").resolve("items");
        List<WeaponData> weapons = read(dir, "weapons.json", ItemCatalogParser::weapons);
        List<ArmorData> armor = read(dir, "armor.json", ItemCatalogParser::armor);
        List<ShieldData> shields = read(dir, "shields.json", ItemCatalogParser::shields);
        catalog = new ItemCatalog(weapons, armor, shields);
        Pf2Craft.LOGGER.info("Loaded {} weapon(s), {} armor(s), {} shield(s)",
                weapons.size(), armor.size(), shields.size());
    }

    /** The jar file first, then the config file; later entries override earlier ones by name. */
    private static <T> List<T> read(Path dir, String file, Function<String, List<T>> parser) {
        List<T> out = new ArrayList<>();
        try (InputStream in = ItemData.class.getResourceAsStream(BASE + file)) {
            if (in == null) {
                throw new IOException("missing resource " + file);
            }
            out.addAll(parser.apply(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException | RuntimeException e) {
            Pf2Craft.LOGGER.error("Skipping built-in item file {}: {}", file, e.getMessage());
        }
        Path p = dir.resolve(file);
        if (Files.isRegularFile(p)) {
            try {
                out.addAll(parser.apply(Files.readString(p, StandardCharsets.UTF_8)));
            } catch (IOException | RuntimeException e) {
                Pf2Craft.LOGGER.error("Skipping item file {}: {}", p, e.getMessage());
            }
        }
        return out;
    }
}
