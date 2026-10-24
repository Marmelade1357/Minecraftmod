package de.pf2craft.character;

import de.pf2craft.core.character.CharacterImportException;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.PathbuilderResponse;
import de.pf2craft.core.item.ItemCatalog;
import de.pf2craft.core.spell.PathbuilderSpells;
import de.pf2craft.core.spell.Spellbook;
import de.pf2craft.item.ItemData;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Downloads a character export from pathbuilder2e.com. The id must already be validated. */
public final class PathbuilderFetcher {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private PathbuilderFetcher() {
    }

    /** The imported sheet together with the spellbook of the same Pathbuilder build. */
    public record Imported(CharacterSheet sheet, Spellbook spellbook) {
    }

    public static CompletableFuture<Imported> fetch(String id) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://pathbuilder2e.com/json.php?id=" + id))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        ItemCatalog catalog = ItemData.catalog();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((response, error) -> {
                    if (error != null) {
                        throw new CompletionException(
                                new CharacterImportException("Pathbuilder is not reachable", error));
                    }
                    try {
                        CharacterSheet sheet = PathbuilderResponse.toSheet(response.statusCode(), response.body(), catalog);
                        return new Imported(sheet, PathbuilderSpells.parseResponse(response.body()));
                    } catch (CharacterImportException e) {
                        throw new CompletionException(e);
                    }
                });
    }
}
