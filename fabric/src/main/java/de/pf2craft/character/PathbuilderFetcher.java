package de.pf2craft.character;

import de.pf2craft.core.character.CharacterImportException;
import de.pf2craft.core.character.CharacterSheet;
import de.pf2craft.core.character.PathbuilderResponse;
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

    public static CompletableFuture<CharacterSheet> fetch(String id) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://pathbuilder2e.com/json.php?id=" + id))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .handle((response, error) -> {
                    if (error != null) {
                        throw new CompletionException(
                                new CharacterImportException("Pathbuilder is not reachable", error));
                    }
                    try {
                        return PathbuilderResponse.toSheet(response.statusCode(), response.body());
                    } catch (CharacterImportException e) {
                        throw new CompletionException(e);
                    }
                });
    }
}
