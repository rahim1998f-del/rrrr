package dev.rahim.morphmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Shared state. In singleplayer the client and the integrated server live in one JVM,
 * so plain static fields are visible to both sides. Not suitable for multiplayer.
 */
public final class MorphState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<Identifier> UNLOCKED = new LinkedHashSet<>();

    private static volatile UUID owner;
    private static volatile EntityType<?> active;

    private MorphState() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("morphmod.json");
    }

    public static synchronized void load() {
        UNLOCKED.clear();
        Path f = file();
        if (!Files.exists(f)) return;
        try {
            JsonObject o = GSON.fromJson(Files.readString(f), JsonObject.class);
            if (o == null || !o.has("unlocked")) return;
            for (JsonElement e : o.getAsJsonArray("unlocked")) {
                Identifier id = Identifier.tryParse(e.getAsString());
                if (id != null) UNLOCKED.add(id);
            }
        } catch (IOException | RuntimeException ex) {
            MorphMod.LOGGER.error("Failed to load morph list", ex);
        }
    }

    private static void save() {
        try {
            JsonObject o = new JsonObject();
            JsonArray a = new JsonArray();
            for (Identifier id : UNLOCKED) a.add(id.toString());
            o.add("unlocked", a);
            Files.writeString(file(), GSON.toJson(o));
        } catch (IOException ex) {
            MorphMod.LOGGER.error("Failed to save morph list", ex);
        }
    }

    public static synchronized boolean isUnlocked(EntityType<?> type) {
        return UNLOCKED.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    /** @return true if this morph was new. */
    public static synchronized boolean unlock(EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (!UNLOCKED.add(id)) return false;
        save();
        return true;
    }

    public static synchronized List<EntityType<?>> unlocked() {
        List<EntityType<?>> out = new ArrayList<>();
        for (Identifier id : UNLOCKED) {
            BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(out::add);
        }
        out.sort(Comparator.comparing(t -> t.getDescription().getString()));
        return out;
    }

    public static void setActive(UUID player, EntityType<?> type) {
        owner = type == null ? null : player;
        active = type;
    }

    public static void clearActive() {
        owner = null;
        active = null;
    }

    /** Active morph type for this player, or null if the player is not morphed. */
    public static EntityType<?> getActive(UUID player) {
        EntityType<?> t = active;
        UUID o = owner;
        return (t != null && player != null && player.equals(o)) ? t : null;
    }
}
