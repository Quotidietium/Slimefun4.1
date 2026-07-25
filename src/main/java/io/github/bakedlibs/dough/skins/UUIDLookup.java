package io.github.bakedlibs.dough.skins;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.plugin.Plugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Version-compatible replacement for dough's {@code UUIDLookup}.
 * <p>
 * Resolves a Minecraft username to its {@link UUID} via the playerdb.co API.
 *
 * @see PlayerSkin#fromPlayerUUID(Plugin, UUID)
 */
public final class UUIDLookup {

    private static final Pattern NAME_PATTERN = Pattern.compile("\\w{3,16}");
    private static final JsonParser JSON_PARSER = new JsonParser();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private UUIDLookup() {}

    /**
     * Returns a {@link CompletableFuture} with the {@link UUID} for the given username.
     *
     * @param plugin
     *            The plugin invoking this function.
     * @param name
     *            The username of the player.
     *
     * @return A {@link CompletableFuture} with the {@link UUID}, or {@code null} if not found.
     */
    @ParametersAreNonnullByDefault
    public static @Nonnull CompletableFuture<UUID> getUuidFromUsername(Plugin plugin, String name) {
        Validate.notNull(plugin, "The plugin instance must not be null!");
        Validate.notNull(name, "The username cannot be null!");

        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("\"" + name + "\" is not a valid Minecraft Username!");
        }

        String targetUrl = "https://playerdb.co/api/player/minecraft/" + name;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofMinutes(2))
                .header("user-agent", "Mozilla/5.0 Dough (+https://github.com/baked-libs/dough)")
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(s -> JSON_PARSER.parse(s).getAsJsonObject())
                .thenApply(jsonObject -> {
                    if (jsonObject.get("success").getAsBoolean()) {
                        JsonObject data = jsonObject.getAsJsonObject("data");
                        JsonObject player = data.getAsJsonObject("player");
                        return UUID.fromString(player.get("id").getAsString());
                    } else {
                        return null;
                    }
                });
    }
}
