package seraphina.silent_love_sword_tria.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public final class PlayerDef {
    private static final Gson GSON = new Gson();
    private static final Path CONFIG_FILE = FMLPaths.GAMEDIR.get().resolve("silent_love_sword_trial/def/player.json");
    private static List<String> protectedPlayers;

    static {
        loadConfig();
    }

    public static void addDef(Object object) {
        if (object instanceof Player player) {
            String playerName = player.getGameProfile().getName();
            if (!protectedPlayers.contains(playerName)) {
                protectedPlayers.add(playerName);
                saveConfig();
            }
        }
    }

    public static boolean isDef(Object object) {
        if (!(object instanceof Player player) || player.gameProfile == null) {
            return false;
        }
        String playerName = player.getGameProfile().getName();
        return protectedPlayers.contains(playerName) || FinalValue.SILENT_LOCAL_PLAYER.equals(object.getClass()) || FinalValue.SILENT_SERVER_PLAYER.equals(object.getClass());
    }

    public static void loadConfig() {
        if (!Files.exists(CONFIG_FILE)) {
            try {
                Files.createDirectories(CONFIG_FILE.getParent());
            } catch (IOException e) {
                e.printStackTrace();
            }
            protectedPlayers = new ArrayList<>();
            saveConfig();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            protectedPlayers = GSON.fromJson(reader, new TypeToken<List<String>>(){}.getType());
            if (protectedPlayers == null) {
                protectedPlayers = new ArrayList<>();
            }
        } catch (IOException e) {
            e.printStackTrace();
            protectedPlayers = new ArrayList<>();
        }
    }

    public static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(protectedPlayers, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addPlayerByName(String name) {
        if (name != null && !name.isEmpty() && !protectedPlayers.contains(name)) {
            protectedPlayers.add(name);
            saveConfig();
        }
    }

    public static void removeDef(Object object) {
        if (object instanceof Player player) {
            String playerName = player.getGameProfile().getName();
            protectedPlayers.remove(playerName);
            saveConfig();
        }
    }

    public static List<String> getProtectedPlayers() {
        return new ArrayList<>(protectedPlayers);
    }
}
