package de.pf2craft.core.item;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Loads the shipped item data files into an {@link ItemCatalog} for tests. */
public final class TestCatalog {
    private TestCatalog() {
    }

    private static String resource(String name) {
        try (InputStream in = TestCatalog.class.getResourceAsStream("/pf2craft/items/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ItemCatalog shipped() {
        return new ItemCatalog(ItemCatalogParser.weapons(resource("weapons.json")),
                ItemCatalogParser.armor(resource("armor.json")),
                ItemCatalogParser.shields(resource("shields.json")));
    }
}
