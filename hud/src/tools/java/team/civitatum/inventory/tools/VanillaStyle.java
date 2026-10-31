package team.civitatum.inventory.tools;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/**
 * Restyles vanilla screens in the look of the inventory windows: stone instead of light grey, gold bevels, dark slot
 * recesses. The files go to the mod's built-in pack {@code resourcepacks/civitatum_style}, above vanilla and mods.
 *
 * <p>Block screens are vanilla's own textures with their six greys swapped for ours, pixel for pixel: every slot,
 * arrow and picture stays where the screen's code expects it, and other colours (the beacon's gems, the enchanting
 * table's book) are left alone. Widget sprites are drawn anew, as {@code MorrowindSkin} draws a button, at vanilla's
 * sizes with their own nine-slice borders.
 *
 * <p>Run with {@code ./gradlew :hud:generateVanillaStyle} after the module has been set up once (it reads vanilla's
 * textures from the resources jar that ModDevGradle unpacks), or start {@link #main} with that jar as the argument
 * from the {@code hud} folder. A folder of mod jars as the second argument ({@code -Pmods=<folder>} for Gradle) also
 * restyles {@link #FOREIGN} and the mods' screens ({@link ModStyle}); without it those files are left as they are.
 */
public final class VanillaStyle {
    /** The built-in pack that the mod puts above all mods' resources. */
    static final Path PACK = Path.of("src/main/resources/resourcepacks/civitatum_style");
    private static final Path OUTPUT = PACK.resolve("assets/minecraft/textures/gui");
    private static final String VANILLA = "assets/minecraft/textures/gui/";
    /** Screens of blocks; villager and horse screens belong to entities and keep vanilla's look. */
    static final List<String> CONTAINERS = List.of("generic_54", "shulker_box", "dispenser", "hopper",
        "furnace", "blast_furnace", "smoker", "crafting_table", "brewing_stand", "anvil", "enchanting_table",
        "grindstone", "smithing", "stonecutter", "loom", "cartography_table", "beacon", "crafter");
    /**
     * Sprites drawn over those screens: fills of arrows and flames, scrollers, slot hints, buttons. They carry the same
     * greys plus a few of their own, so a sprite left vanilla would show a light grey patch on the stone.
     */
    private static final List<String> SPRITE_FOLDERS = List.of("container/furnace", "container/blast_furnace",
        "container/smoker", "container/crafter", "container/stonecutter", "container/loom",
        "container/cartography_table", "container/brewing_stand", "container/beacon", "container/smithing",
        "container/anvil", "container/grindstone", "recipe_book");
    /**
     * Other mods' pictures drawn on restyled screens, by the start of their jar's name. Moonlight replaces the loom's
     * banner slot hint with these, cycling between them, when an item of Supplementaries can go into that slot.
     */
    private static final Map<String, String> FOREIGN = Map.of(
        "moonlight-", "assets/moonlight/textures/item/gui_slots/empty_slot_banner.png",
        "supplementaries-", "assets/supplementaries/textures/item/gui_slots/empty_slot_flag.png");
    // the greys of those slot hints
    private static final int HINT_FILL = 0xFFC3C3C3, HINT_EDGE = 0xFF808080;
    /**
     * Where a sprite lies on its screen's background, so its stone grain continues the background's. Vanilla draws a
     * partly filled sprite with the same offset into the sprite as on the screen.
     */
    private static final Map<String, int[]> PLACES = Map.of(
        "burn_progress", new int[]{79, 34}, "lit_progress", new int[]{56, 36},
        "bubbles", new int[]{63, 14}, "brew_progress", new int[]{97, 16});

    // vanilla's greys
    private static final int FACE = 0xFFC6C6C6, RECESS = 0xFF8B8B8B, LIGHT = 0xFFFFFFFF, DARK_EDGE = 0xFF373737;
    private static final int SHADE = 0xFF555555, BLACK = 0xFF000000;
    // the crafter's result frame and the recipe book's craftable rim
    private static final int FRAME_DARK = 0xFF383838, FRAME_MID = 0xFF737373, FRAME_LIGHT = 0xFFADADAD;
    private static final int RIM = 0xFFCCCCCC, DISABLED_RIM = 0xFFB2B2B2;
    // the blue hover of the recipe book and the beacon's buttons
    private static final int HOVER = 0xFF8892C9, BEACON_HOVER = 0xFF7778A0, HOVER_OUTLINE = 0xFF00073E;
    private static final int HOVER_OUTLINE_2 = 0xFF00031E, HOVER_EDGE = 0xFF343E75, HOVER_EDGE_2 = 0xFF5F647F;
    private static final int BEACON_HOVER_EDGE = 0xFF373860, HOVER_SHADE = 0xFF444964;
    private static final int HOVER_LIGHT = 0xFFCFD0F7, HOVER_LIGHT_2 = 0xFFAFB6DA;
    // a recipe that cannot be made, plain and hovered; the disabled beacon button
    /**
     * The lilac of a hovered stonecutter recipe or loom pattern, over buttons of tan {@code A09172}: it turns into
     * that tan lit by our gold, as the blue hover of other screens does.
     */
    private static final int PATTERN_HOVER = 0xFFB688AE, PATTERN_HOVER_LIGHT = 0xFFFAB9EF;
    private static final int PATTERN_HOVER_SHADE = 0xFF654C61, PATTERN = 0xFFA09172, PATTERN_SHADE = 0xFF544C3B;
    private static final double PATTERN_GOLD = 0.5;
    private static final int UNCRAFTABLE = 0xFF936B6B, UNCRAFTABLE_HOVER = 0xFF8D7E99, UNCRAFTABLE_SLOT = 0xFF6A6A6A;
    private static final int DISABLED_DARK = 0xFF2D2D2D, DISABLED_LIGHT = 0xFF494949;
    // scroll thumbs and the brewing arrow
    private static final int THUMB_LIGHT = 0xFFE4E4E4, THUMB_MID = 0xFFC3C3C3, THUMB_DARK = 0xFF9C9C9C;
    private static final int REFUSED = 0x8A2222;
    // the colours of MorrowindSkin.frame and the slot frame of the inventory screen
    private static final int OUTLINE = 0xFF17130E, SHADOW = 0xFF54422C, GOLD = 0xFFB29A6B, LIT_GOLD = 0xFFE2C18A;
    private static final int SLOT_EDGE = 0xFF473B2A, LIT_SHADE = 0x205F4824, THUMB = 0xFFD3B478;
    /**
     * How far a neutral grey may stray from one of vanilla's six and still count as it: mods paint with
     * {@code C7C7C7}, {@code 8C8C8C}, {@code 343434}, {@code 545454} or {@code C2C2C2} where vanilla has its own.
     */
    private static final int GREY_TOLERANCE = 8, GREY_SPREAD = 2;
    private static final int[] VANILLA_GREYS = {FACE, RECESS, LIGHT, DARK_EDGE, SHADE, BLACK};
    /**
     * Vanilla's greys from dark to light. A neutral grey between two of them (a mod's {@code E4E4E4} panel, its
     * {@code 747474} slot, its {@code 9C9C9C} scroller) is laid between what those two become, by its brightness.
     */
    private static final int[] GREY_RAMP = {BLACK, DARK_EDGE, SHADE, RECESS, FACE, LIGHT};
    /**
     * The lighter palette some mods draw whole screens in (Quark's crate, MineColonies, Supplementaries' pulley), the
     * one of vanilla's scroll thumb: an {@code E4E4E4} face, {@code C3C3C3} slots with {@code 808080} edges. Its greys,
     * dark to light, stand for those of {@link #GREY_RAMP}; without the shift the slots would read as the face.
     */
    static final int[] LIGHT_RAMP = {BLACK, 0xFF808080, THUMB_DARK, THUMB_MID, THUMB_LIGHT, LIGHT};
    /** Channels of a grey that is meant as neutral may differ this much; tinted pictures keep their colours. */
    private static final int NEUTRAL_SPREAD = 6;
    /**
     * A cool grey (Create's green grey slot frames, AE2's violet grey) is still grey to the eye while its
     * channels differ by this share of the brightest. Warm ones are not: there it is cream paper or wood.
     */
    private static final double COOL_GREY_SHARE = 0.3;
    /**
     * Vanilla's blue of a hovered button as mods vary it, pressed, dimmed or striped when disabled (Create's buttons),
     * steel blue (Waystones) or pale (Stellaris): blue over red by this much at least, and at most this share of the
     * blue apart from the weakest channel. Only in pictures of buttons and highlights: elsewhere blue is an icon's.
     */
    private static final int MIN_BUTTON_BLUE = 0x18;
    /** Blue over green of a button's blue; teal (an ender eye on a button) is no button's colour. */
    private static final int MIN_BUTTON_BLUE_OVER_GREEN = 0x10;
    private static final double MAX_BUTTON_SATURATION = 0.6;
    private static final Pattern BUTTON = Pattern.compile("(?i)button|widget|hover|highlight|selected|checkbox");
    /** How much of the lit gold a hovered button's blue turns into, over the grey of its brightness. */
    private static final double BUTTON_GOLD = 0.2;
    /** Slots and empty arrows sink this far towards near black, so they read on the dark stone. */
    private static final double RECESS_DEPTH = 0.45;
    private static final int RECESS_BASE = 0x0E0A07;

    private final BufferedImage stone = StoneTexture.generate();

    VanillaStyle() {}

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("Pass vanilla's resources jar and, optionally, a folder of mod jars");
        }
        if (!Files.isDirectory(Path.of("src/main/resources"))) {
            throw new IllegalStateException("Run from the hud module folder (working directory is "
                + Path.of("").toAbsolutePath() + ")");
        }
        var style = new VanillaStyle();
        // a full run writes the whole pack anew, so pictures of mods removed since then do not linger
        if (args.length == 2) clear(PACK.resolve("assets"));
        style.run(Path.of(args[0]));
        if (args.length == 2) {
            style.foreign(Path.of(args[1]));
            new ModStyle(style).run(Path.of(args[1]));
        } else {
            System.out.println("No mods folder: " + FOREIGN.values() + " and mods' screens left as they are");
        }
    }

    private static void clear(Path folder) throws IOException {
        if (!Files.exists(folder)) return;
        try (var files = Files.walk(folder)) {
            for (Path file : files.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(file);
        }
    }

    private void run(Path jar) throws IOException {
        try (var zip = new ZipFile(jar.toFile())) {
            for (String name : CONTAINERS) restyle(zip, "container/" + name + ".png");
            restyle(zip, "recipe_book.png");
            for (var entry : zip.stream().toList()) {
                String name = entry.getName();
                if (!name.endsWith(".png") || !name.startsWith(VANILLA + "sprites/")) continue;
                String sprite = name.substring(VANILLA.length() + "sprites/".length());
                String folder = sprite.substring(0, sprite.lastIndexOf('/'));
                if (SPRITE_FOLDERS.contains(folder)) restyle(zip, "sprites/" + sprite);
            }
        }
        // border sizes are ours: the bevels need three pixels, vanilla's disabled button and slider keep only one
        sprite("button", button(GOLD, 0), 200, 20, 3);
        sprite("button_highlighted", button(LIT_GOLD, LIT_SHADE), 200, 20, 3);
        sprite("button_disabled", disabled(), 200, 20, 3);
        sprite("slider", button(GOLD, 0), 200, 20, 3);
        sprite("slider_highlighted", button(LIT_GOLD, 0), 200, 20, 3);
        sprite("slider_handle", handle(GOLD, 0), 8, 20, 2);
        sprite("slider_handle_highlighted", handle(LIT_GOLD, LIT_SHADE), 8, 20, 2);
        sprite("text_field", field(GOLD), 200, 20, 1);
        sprite("text_field_highlighted", field(LIT_GOLD), 200, 20, 1);
        sprite("scroller", scroller(), 6, 32, 1);
        sprite("scroller_background", track(), 6, 32, 1);
        // drawn anew rather than recoloured: vanilla's raised grey plate would turn into plain stone
        write(plate(), OUTPUT.resolve("sprites/container/crafter/disabled_slot.png"));
    }

    private void foreign(Path mods) throws IOException {
        for (var entry : FOREIGN.entrySet()) {
            Path jar;
            try (var jars = Files.list(mods)) {
                jar = jars.filter(f -> f.getFileName().toString().startsWith(entry.getKey())).findFirst()
                    .orElseThrow(() -> new IOException("No " + entry.getKey() + "*.jar in " + mods));
            }
            BufferedImage image;
            try (var zip = new ZipFile(jar.toFile())) {
                try (InputStream in = zip.getInputStream(zip.getEntry(entry.getValue()))) {
                    image = ImageIO.read(in);
                }
            }
            var out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    // as vanilla's banner slot hint: the shape sunk into the slot, its outline in the slot edge colour
                    out.setRGB(x, y, argb == HINT_FILL ? recess(x, y) : argb == HINT_EDGE ? SLOT_EDGE : argb);
                }
            }
            write(out, PACK.resolve(entry.getValue()));
        }
    }

    /**
     * The crafter's switched-off slot: a flat gold plate over the slot, lit on the top and left, shaded on the bottom
     * and right. A dark rim keeps it apart from the gold edges of the slots around it; no stone grain, so it reads as
     * a pressed button rather than a hole in the background.
     */
    private static BufferedImage plate() {
        var image = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 18; y++) {
            for (int x = 0; x < 18; x++) {
                int colour;
                if (x == 0 || y == 0 || x == 17 || y == 17) colour = OUTLINE;
                else if (x == 16 || y == 16) colour = SHADOW;
                else if (x == 1 || y == 1) colour = LIT_GOLD;
                else colour = GOLD;
                image.setRGB(x, y, colour);
            }
        }
        return image;
    }

    private void restyle(ZipFile zip, String name) throws IOException {
        var entry = zip.getEntry(VANILLA + name);
        if (entry == null) throw new IOException("No " + name + " in " + zip.getName());
        BufferedImage image;
        try (InputStream in = zip.getInputStream(entry)) {
            image = ImageIO.read(in);
        }
        String file = name.substring(name.lastIndexOf('/') + 1, name.length() - ".png".length());
        int[] place = PLACES.getOrDefault(file, new int[]{0, 0});
        var out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                out.setRGB(x, y, recolour(image.getRGB(x, y), x + place[0], y + place[1], file));
            }
        }
        write(out, OUTPUT.resolve(name));
        // the scaling of a sprite is read from the pack that gives its picture, so vanilla's metadata comes along
        var meta = zip.getEntry(VANILLA + name + ".mcmeta");
        if (meta != null) {
            try (InputStream in = zip.getInputStream(meta)) {
                Files.write(OUTPUT.resolve(name + ".mcmeta"), in.readAllBytes());
            }
        }
    }

    /** One pixel of a vanilla screen or sprite {@code file} in our palette; {@code x, y} place the stone grain. */
    int recolour(int argb, int x, int y, String file) {
        int alpha = argb >>> 24;
        if (alpha == 0) return argb;
        // a see-through grey (a shadow, a dimmed panel) is the same grey: recoloured, at its own translucency
        if (alpha != 0xFF) return alpha << 24 | recolour(argb | 0xFF000000, x, y, file) & 0xFFFFFF;
        if (file.startsWith("scroller") || file.equals("brew_progress")) {
            int thumb = thumb(argb, file.endsWith("disabled"));
            if (thumb != argb) return thumb;
        }
        boolean lit = file.endsWith("highlighted"), off = file.contains("disabled");
        return switch (argb) {
            case FACE -> stone(x, y);
            case RECESS -> recess(x, y);
            case LIGHT -> lit ? LIT_GOLD : GOLD;
            // the recipe book's page and a switched-off button are wells, not slot edges
            case DARK_EDGE -> file.equals("recipe_book") || off ? recess(x, y) : SLOT_EDGE;
            case SHADE, FRAME_MID, DISABLED_LIGHT, HOVER_SHADE, BEACON_HOVER_EDGE -> SHADOW;
            case BLACK, HOVER_OUTLINE, HOVER_OUTLINE_2, DISABLED_DARK -> OUTLINE;
            case FRAME_DARK, HOVER_EDGE, HOVER_EDGE_2 -> SLOT_EDGE;
            case FRAME_LIGHT, RIM, DISABLED_RIM -> GOLD;
            case HOVER, BEACON_HOVER -> mix(stone(x, y), LIT_GOLD, 0.25);
            case HOVER_LIGHT, HOVER_LIGHT_2 -> LIT_GOLD;
            case PATTERN_HOVER -> mix(PATTERN, LIT_GOLD, PATTERN_GOLD);
            case PATTERN_HOVER_LIGHT -> mix(LIT_GOLD, LIGHT, PATTERN_GOLD);
            case PATTERN_HOVER_SHADE -> mix(PATTERN_SHADE, LIT_GOLD, PATTERN_GOLD / 2);
            case UNCRAFTABLE -> mix(recess(x, y), REFUSED, 0.4);
            // its red rim stays: it is what tells the slot apart
            case UNCRAFTABLE_SLOT -> mix(recess(x, y), REFUSED, 0.2);
            case UNCRAFTABLE_HOVER -> mix(stone(x, y), REFUSED, 0.4);
            default -> {
                int grey = vanillaGrey(argb);
                if (grey != 0 && grey != argb) yield recolour(grey, x, y, file);
                if (buttonBlue(argb) && BUTTON.matcher(file).find()) {
                    yield mix(ramp(grey(argb), x, y, file + "_highlighted"), LIT_GOLD, BUTTON_GOLD);
                }
                yield ramp(argb, x, y, file);
            }
        };
    }

    /** Any other neutral grey between the two vanilla greys around it; other colours unchanged. */
    private int ramp(int argb, int x, int y, String file) {
        if (!neutral(argb)) return argb;
        int g = grey(argb) & 0xFF;
        for (int i = 1; i < GREY_RAMP.length; i++) {
            int dark = GREY_RAMP[i - 1] & 0xFF, light = GREY_RAMP[i] & 0xFF;
            if (g > light) continue;
            double t = (g - dark) / (double) (light - dark);
            return blend(recolour(GREY_RAMP[i - 1], x, y, file), recolour(GREY_RAMP[i], x, y, file), t);
        }
        return argb;
    }

    /**
     * Whether a colour is a grey: its channels differ by no more than {@link #NEUTRAL_SPREAD}, or, when it is not
     * warm, by no more than {@link #COOL_GREY_SHARE} of the brightest.
     */
    static boolean neutral(int argb) {
        if (argb >>> 24 == 0) return false;
        int r = argb >> 16 & 0xFF, g = argb >> 8 & 0xFF, b = argb & 0xFF;
        int max = Math.max(r, Math.max(g, b)), spread = max - Math.min(r, Math.min(g, b));
        boolean warm = r > b + NEUTRAL_SPREAD && r >= g;
        return spread <= NEUTRAL_SPREAD || !warm && spread <= COOL_GREY_SHARE * max;
    }

    /** Whether an opaque colour is one of the blues of a hovered or pressed button, see {@link #MIN_BUTTON_BLUE}. */
    private static boolean buttonBlue(int argb) {
        int r = argb >> 16 & 0xFF, g = argb >> 8 & 0xFF, b = argb & 0xFF;
        return b - r >= MIN_BUTTON_BLUE && b - g >= MIN_BUTTON_BLUE_OVER_GREEN
            && b - Math.min(r, g) <= MAX_BUTTON_SATURATION * b;
    }

    /**
     * A grey of the {@link #LIGHT_RAMP} as the vanilla grey it stands for, laid between them by brightness as
     * {@link #ramp} does; other colours unchanged, and so is the translucency.
     */
    static int vanillaFromLight(int argb) {
        if (!neutral(argb)) return argb;
        int g = grey(argb) & 0xFF;
        for (int i = 1; i < LIGHT_RAMP.length; i++) {
            int dark = LIGHT_RAMP[i - 1] & 0xFF, light = LIGHT_RAMP[i] & 0xFF;
            if (g > light) continue;
            double t = (g - dark) / (double) (light - dark);
            int from = GREY_RAMP[i - 1] & 0xFF, to = GREY_RAMP[i] & 0xFF;
            return argb & 0xFF000000 | (int) Math.round(from + (to - from) * t) * 0x010101;
        }
        return argb;
    }

    /** The neutral grey of an opaque colour's brightness. */
    private static int grey(int argb) {
        int level = ((argb >> 16 & 0xFF) + (argb >> 8 & 0xFF) + (argb & 0xFF)) / 3;
        return 0xFF000000 | level * 0x010101;
    }

    /** Two opaque colours mixed, {@code t} of the way from the first to the second. */
    private static int blend(int from, int to, double t) {
        int argb = 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int a = from >> shift & 0xFF, b = to >> shift & 0xFF;
            argb |= (int) Math.round(a + (b - a) * t) << shift;
        }
        return argb;
    }

    /** The vanilla grey that an opaque neutral grey stands for, or 0 when it is not close to any of them. */
    static int vanillaGrey(int argb) {
        if (argb >>> 24 != 0xFF) return 0;
        int r = argb >> 16 & 0xFF, g = argb >> 8 & 0xFF, b = argb & 0xFF;
        if (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > GREY_SPREAD) return 0;
        for (int grey : VANILLA_GREYS) {
            if (Math.abs((grey & 0xFF) - g) <= GREY_TOLERANCE) return grey;
        }
        return 0;
    }

    /** A scroll thumb or the brewing arrow in the gold of the inventory's scroll bars; dark while it cannot move. */
    private static int thumb(int argb, boolean off) {
        return switch (argb) {
            case LIGHT -> off ? 0xFF6E5B3E : LIT_GOLD;
            case THUMB_LIGHT -> THUMB;
            case THUMB_MID -> off ? SHADOW : GOLD;
            case THUMB_DARK -> off ? OUTLINE : SHADOW;
            default -> argb;
        };
    }

    private int recess(int x, int y) {
        return mix(stone(x, y), RECESS_BASE, RECESS_DEPTH);
    }

    /** The stone of the windows, opaque, tiled from the top left corner. */
    private int stone(int x, int y) {
        return 0xFF000000 | stone.getRGB(x % stone.getWidth(), y % stone.getHeight()) & 0xFFFFFF;
    }

    /** The stone as the windows draw it by default, at its own translucency. */
    private int glass(int x, int y) {
        return stone.getRGB(x % stone.getWidth(), y % stone.getHeight());
    }

    /** A button or slider track: stone in a frame, gold on the lit sides, shaded on the others. */
    private BufferedImage button(int gold, int shade) {
        var image = new BufferedImage(200, 20, BufferedImage.TYPE_INT_ARGB);
        frame(image, gold, shade);
        return image;
    }

    /** As {@link #button}, dimmed: the gold bevel turns into the shadow's colour. */
    private BufferedImage disabled() {
        var image = new BufferedImage(200, 20, BufferedImage.TYPE_INT_ARGB);
        frame(image, SHADOW, 0x9918130E);
        return image;
    }

    private BufferedImage handle(int gold, int shade) {
        var image = new BufferedImage(8, 20, BufferedImage.TYPE_INT_ARGB);
        frame(image, gold, shade);
        return image;
    }

    /** A text field: a dark well with a one-pixel rim, gold while it is focused. */
    private BufferedImage field(int rim) {
        var image = new BufferedImage(200, 20, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 200; x++) {
                boolean edge = x == 0 || y == 0 || x == 199 || y == 19;
                image.setRGB(x, y, edge ? rim : 0xF0100D0A);
            }
        }
        return image;
    }

    /** The scroll thumb in the gold of the inventory's scroll bars, with a dark rim. */
    private BufferedImage scroller() {
        var image = new BufferedImage(6, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 6; x++) {
                boolean edge = x == 0 || y == 0 || x == 5 || y == 31;
                image.setRGB(x, y, edge ? SHADOW : THUMB);
            }
        }
        return image;
    }

    private BufferedImage track() {
        var image = new BufferedImage(6, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 6; x++) {
                boolean edge = x == 0 || y == 0 || x == 5 || y == 31;
                image.setRGB(x, y, edge ? OUTLINE : 0xC0130E09);
            }
        }
        return image;
    }

    /** {@code MorrowindSkin.frame} around {@code MorrowindSkin.body}, with an optional shade over the stone. */
    private void frame(BufferedImage image, int gold, int shade) {
        int w = image.getWidth(), h = image.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int colour;
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) colour = OUTLINE;
                else if (x == w - 2 || y == h - 2) colour = SHADOW;
                else if (x == 1 || y == 1) colour = gold;
                else colour = over(glass(x, y), shade);
                image.setRGB(x, y, colour);
            }
        }
    }

    /** {@code top} drawn over {@code bottom} with its own alpha, as a GUI fill would. */
    static int over(int bottom, int top) {
        double a = (top >>> 24) / 255.0, b = (bottom >>> 24) / 255.0;
        double alpha = a + b * (1 - a);
        if (alpha == 0) return 0;
        int argb = (int) Math.round(alpha * 255) << 24;
        for (int shift = 0; shift <= 16; shift += 8) {
            double channel = ((top >> shift & 0xFF) * a + (bottom >> shift & 0xFF) * b * (1 - a)) / alpha;
            argb |= (int) Math.round(channel) << shift;
        }
        return argb;
    }

    /** {@code colour} moved {@code amount} of the way to {@code target}, keeping its alpha. */
    static int mix(int colour, int target, double amount) {
        int argb = colour & 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int from = colour >> shift & 0xFF, to = target >> shift & 0xFF;
            argb |= (int) Math.round(from + (to - from) * amount) << shift;
        }
        return argb;
    }

    private static void sprite(String name, BufferedImage image, int width, int height, int border)
            throws IOException {
        Path file = OUTPUT.resolve("sprites/widget/" + name + ".png");
        write(image, file);
        // our own metadata: vanilla's would be read with our picture and its borders do not fit our bevels
        String meta = """
            {
              "gui": {
                "scaling": {"type": "nine_slice", "width": %d, "height": %d, "border": %d}
              }
            }
            """.formatted(width, height, border);
        Files.writeString(file.resolveSibling(name + ".png.mcmeta"), meta, StandardCharsets.UTF_8);
    }

    private static void write(BufferedImage image, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "png", file.toFile());
        System.out.println("Generated " + file);
    }
}
