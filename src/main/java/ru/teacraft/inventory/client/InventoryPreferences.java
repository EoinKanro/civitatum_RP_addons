package ru.teacraft.inventory.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InventoryPreferences {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static InventoryPreferences instance;
    public boolean enabled = true;
    public boolean mapVisible = true;
    public Map<String, SavedPanel> panels = new LinkedHashMap<>();
    public record SavedPanel(float x, float y, int width, int height) {}
    public static InventoryPreferences get() {
        if (instance == null) {
            instance = new InventoryPreferences();
            var path = FMLPaths.CONFIGDIR.get().resolve("civitatum-inventory.json");
            if (Files.isRegularFile(path)) try {
                var loaded = JSON.fromJson(Files.readString(path), InventoryPreferences.class);
                if (loaded != null) { instance = loaded; if (instance.panels == null) instance.panels = new LinkedHashMap<>(); }
            } catch (Exception ex) { LogUtils.getLogger().warn("Civitatum Inventory: could not load layout", ex); }
        }
        return instance;
    }
    public void save() {
        var path = FMLPaths.CONFIGDIR.get().resolve("civitatum-inventory.json");
        var temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(temporary, JSON.toJson(this), StandardCharsets.UTF_8);
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) { LogUtils.getLogger().warn("Civitatum Inventory: could not save layout", ex); }
    }
}
