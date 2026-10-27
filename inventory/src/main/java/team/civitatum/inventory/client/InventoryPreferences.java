package team.civitatum.inventory.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InventoryPreferences {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "civitatum-inventory.json";
    private static final double DEFAULT_TRANSPARENCY = 0.60;
    private static final double DEFAULT_SLOT_SCALE = 1.0;
    private static InventoryPreferences instance;
    public boolean enabled = true;
    public double transparency = DEFAULT_TRANSPARENCY;
    public double slotScale = DEFAULT_SLOT_SCALE;
    public String selectedAbility = "civitatum:racial_gift";
    public boolean powersVisible = true;
    public Map<String, SavedPanel> panels = new LinkedHashMap<>();
    public record SavedPanel(float x, float y, int width, int height) {}

    /**
     * Loaded once. Only the file can hold out-of-range values: the sliders write values inside their own range,
     * so the bounds are applied on load and not on every call.
     */
    public static InventoryPreferences get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static InventoryPreferences load() {
        var result = new InventoryPreferences();
        var path = path();
        if (Files.isRegularFile(path)) try {
            var loaded = JSON.fromJson(Files.readString(path), InventoryPreferences.class);
            if (loaded != null) {
                result = loaded;
                if (result.panels == null) result.panels = new LinkedHashMap<>();
            }
        } catch (Exception ex) {
            LogUtils.getLogger().warn("Civitatum Inventory: could not load layout", ex);
        }
        result.transparency = bounded(result.transparency, 0, 0.9, DEFAULT_TRANSPARENCY);
        result.slotScale = bounded(result.slotScale, 1, 2, DEFAULT_SLOT_SCALE);
        return result;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE);
    }

    private static double bounded(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }

    public void save() {
        var path = path();
        var temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(temporary, JSON.toJson(this), StandardCharsets.UTF_8);
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            LogUtils.getLogger().warn("Civitatum Inventory: could not save layout", ex);
        }
    }
}
