package team.civitatum.inventory.tools;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;

/**
 * Mods' screens restyled as vanilla's. Every picture of a mod's interface that is a good part grey is recoloured as
 * vanilla's are: greys of any shade laid onto our colours by their brightness, everything else left alone. Not only
 * the big backgrounds: slot sprites, buttons, progress fills, scrollers and panels, of any mod, so no grey is left in
 * a window, whatever its own style. Icons, HUD parts, signs, cursors and flags keep their colours.
 *
 * <p>Screens that use these pictures are found in the bytecode and listed in
 * {@code civitatum_hud/restyled_screens.txt}; there the mod draws dark labels light and the slot highlight in gold.
 * A screen is listed when it names a restyled picture (or one of vanilla's restyled screens) in any of the ways of
 * {@link PictureNames}, or names a class that holds such names, as Create's {@code AllGuiTextures} or Doggy Talents'
 * {@code Resources} do for all their screens. A screen that names no picture at all gets its background from the
 * code that opens it, and is listed when that code names a restyled picture (Ars Elemental's curio bags).
 *
 * <p>Jars inside jars (libraries a mod bundles, such as Create's Ponder) are treated as mods of their own.
 */
final class ModStyle {
    /** Share of vanilla-like greys among the opaque pixels, and the light grey face in pixels, of a vanilla screen. */
    private static final double MIN_GREYS = 0.5;
    private static final int MIN_FACE = 200;
    /** Folders that hold interface pictures besides {@code textures/gui}, as some mods name them (AE2's guis). */
    private static final List<String> INTERFACE_FOLDERS =
        List.of("gui", "guis", "screen", "screens", "container", "menu");
    /** Opaque pixels of a screen background; a smaller picture in vanilla's greys is a button, not a screen. */
    private static final int MIN_SCREEN = 4000;
    /**
     * Screens that show paper with writing on it: books, guides, a notice board, a clipboard. The writing is dark on
     * the paper, and drawing it light would lose it; the screen's own labels stay dark then, the lesser harm. A recipe
     * book is a panel like the others.
     */
    private static final java.util.regex.Pattern PAPER =
        java.util.regex.Pattern.compile("(?i)(?<!recipe)book|bestiary|guide|notice|clipboard|documentation");
    private static final int FACE = 0xFFC6C6C6;
    /**
     * Share of the face and of the slots of {@link VanillaStyle#LIGHT_RAMP} that marks a picture drawn in that lighter
     * palette, when it has less of vanilla's face than of its own.
     */
    private static final double MIN_LIGHT_PALETTE = 0.05;
    /**
     * Palettes of mods that draw their screens in greys of their own, by namespace: each colour and the vanilla grey
     * it stands for. Brightness alone gets them wrong. AE2 draws violet greys, its slots ({@code ADB0C4}) closer to
     * the face ({@code CBCCD4}) than to vanilla's recess, and its sky stone chest is dark with slots lighter than
     * the face.
     */
    private static final Map<String, Map<Integer, Integer>> PALETTES = Map.of("ae2", Map.ofEntries(
        Map.entry(0xFFCBCCD4, 0xFFC6C6C6), Map.entry(0xFFADB0C4, 0xFF8B8B8B), Map.entry(0xFF9A9FB4, 0xFF373737),
        Map.entry(0xFFF2F2F2, 0xFFFFFFFF), Map.entry(0xFF878FA5, 0xFF373737), Map.entry(0xFF413F54, 0xFF000000),
        Map.entry(0xFF696D88, 0xFF555555), Map.entry(0xFF4D4D67, 0xFF373737),
        Map.entry(0xFF2D2F2D, 0xFFC6C6C6), Map.entry(0xFF454D45, 0xFF8B8B8B), Map.entry(0xFF323835, 0xFF373737),
        Map.entry(0xFF575E54, 0xFFFFFFFF), Map.entry(0xFF393E39, 0xFFFFFFFF), Map.entry(0xFF1F2221, 0xFF555555),
        Map.entry(0xFF161717, 0xFF000000)));
    private static final int LIGHT_FACE = 0xE4, LIGHT_SLOT = 0xC3, LIGHT_TOLERANCE = 3, LIGHT_SPREAD = 2;
    /** Share of greys of any shade that makes a picture part of a screen, to be recoloured. */
    private static final double MIN_BACKING = 0.15;
    /**
     * Share of a picture that the recolouring must darken for its screens to be listed, in a mod with no screen in
     * vanilla's style, and the picture must be as big as a screen's background ({@link #MIN_SCREEN}): a dark panel
     * then, not paper with a grey button or ornament on it, where light labels would be lost.
     */
    private static final double MIN_DARKENED = 0.5;
    /**
     * Parts of paths of pictures that are no screen's background: icons, HUD, signs, cursors, title screens,
     * panoramas and logos, banners and drawings, books, whose dark text is written on their paper (the recipe book is
     * a screen like the others), flags, brand logos of social media, and pictures of items and hearts that mods keep
     * outside their icon folders. And colour pickers, whose greys are the colours they offer.
     */
    private static final List<String> NOT_SCREEN = List.of("icon", "hud", "abilities", "penalties", "hanging_signs",
        "cursors", "panorama", "title/", "main_menu", "logo", "banner", "patreon", "drawing", "bestiary", "book",
        "flags/", "media/", "professions/", "personalities/", "heart", "color");
    static final Path SCREENS = Path.of("src/main/resources/civitatum_hud/restyled_screens.txt");

    private final VanillaStyle style;
    private final TreeSet<String> screens = new TreeSet<>();
    /** Jars found inside jars, done after all the mods themselves. */
    private final Map<String, byte[]> bundled = new LinkedHashMap<>();
    /** Pictures written so far: a mod's own copy wins over an older one bundled in another mod. */
    private final Set<String> written = new HashSet<>();
    private final List<String> report = new ArrayList<>();

    ModStyle(VanillaStyle style) {
        this.style = style;
    }

    void run(Path mods) throws IOException {
        List<Path> jars;
        try (var files = Files.list(mods)) {
            jars = files.filter(f -> f.toString().endsWith(".jar")).sorted().toList();
        }
        for (Path jar : jars) {
            try (InputStream in = Files.newInputStream(jar)) {
                restyle(jar.getFileName().toString(), in);
            }
        }
        while (!bundled.isEmpty()) {
            var inner = bundled.entrySet().iterator().next();
            bundled.remove(inner.getKey());
            restyle(inner.getKey(), new ByteArrayInputStream(inner.getValue()));
        }
        var lines = new ArrayList<String>();
        lines.add("# Screens drawn on restyled pictures, written by ./gradlew :hud:generateVanillaStyle.");
        lines.add("# Their dark labels are drawn light and slot highlights in gold. One class name per line.");
        lines.addAll(screens);
        Files.createDirectories(SCREENS.getParent());
        Files.write(SCREENS, lines, StandardCharsets.UTF_8);
        report.forEach(System.out::println);
        System.out.println(screens.size() + " screens listed in " + SCREENS);
    }

    /** One jar: its pictures, then its screens; the jars bundled in it wait for their turn. */
    private void restyle(String jar, InputStream in) throws IOException {
        var pictures = new LinkedHashMap<String, byte[]>();
        var metadata = new HashMap<String, byte[]>();
        var classes = new HashMap<String, byte[]>();
        var zip = new ZipInputStream(in);
        for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            String name = entry.getName();
            if (interfacePicture(name)) pictures.put(name, zip.readAllBytes());
            else if (name.endsWith(".png.mcmeta")) metadata.put(name, zip.readAllBytes());
            else if (name.endsWith(".class")) classes.put(name.substring(0, name.length() - ".class".length()),
                zip.readAllBytes());
            else if (name.endsWith(".jar")) bundled.put(jar + "!" + name, zip.readAllBytes());
        }
        var images = new LinkedHashMap<String, BufferedImage>();
        var backgrounds = new HashSet<String>();
        var lightPalette = new HashSet<String>();
        var styled = new HashSet<String>();
        for (var picture : pictures.entrySet()) {
            BufferedImage image;
            try {
                image = ImageIO.read(new ByteArrayInputStream(picture.getValue()));
            } catch (IOException ex) {
                continue;
            }
            if (image == null) continue;
            var greys = Greys.of(image);
            if (greys.screen()) styled.add(namespace(picture.getKey()));
            if (greys.visible() >= MIN_SCREEN) backgrounds.add(picture.getKey());
            if (greys.lightPalette()) lightPalette.add(picture.getKey());
            if (greys.screen() || greys.backing() && !greys.fill() && partOfScreen(picture.getKey())) {
                images.put(picture.getKey(), image);
            }
        }
        // what classes may name: this mod's restyled pictures, and vanilla's screens
        var names = new PictureNames();
        for (String name : VanillaStyle.CONTAINERS) {
            names.add("minecraft", "textures/gui/container/" + name + ".png", false);
        }
        int restyled = 0;
        for (var picture : images.entrySet()) {
            String path = picture.getKey();
            double darkened = recolour(path, picture.getValue(), metadata, lightPalette.contains(path));
            if (darkened == 0) continue;
            restyled++;
            if (styled.contains(namespace(path)) || darkened >= MIN_DARKENED && backgrounds.contains(path)) {
                names.add(namespace(path), path.substring(path.indexOf("/textures/") + 1), true);
            }
        }
        if (restyled > 0) report.add(restyled + " pictures from " + jar);
        findScreens(classes, names);
    }

    /**
     * Writes the picture recoloured, with its metadata; one drawn in the {@code light} palette or in a mod's own
     * ({@link #PALETTES}) is first shifted to vanilla's. Returns the share of its visible pixels that changed, 0 when
     * it has no grey to swap; 1 when a mod written earlier had the picture, which is then left as it was written.
     */
    private double recolour(String path, BufferedImage image, Map<String, byte[]> metadata, boolean light)
            throws IOException {
        if (written.contains(path)) return 1;
        String file = path.substring(path.lastIndexOf('/') + 1, path.length() - ".png".length());
        var out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int visible = 0, changed = 0, width = image.getWidth(), height = image.getHeight();
        var palette = PALETTES.getOrDefault(namespace(path), Map.of());
        java.util.function.IntBinaryOperator pixels = (px, py) -> {
            int argb = image.getRGB(px, py);
            Integer own = palette.get(argb | 0xFF000000);
            if (own != null) return argb & 0xFF000000 | own & 0xFFFFFF;
            return light ? VanillaStyle.vanillaFromLight(argb) : argb;
        };
        boolean[][] slots = VanillaStyle.slots(pixels, width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y), detail = style.detail(pixels, width, height, x, y, slots);
                int recoloured = detail != 0 ? detail : style.recolour(pixels.applyAsInt(x, y), x, y, file);
                if (argb >>> 24 != 0) visible++;
                if (recoloured != argb) changed++;
                out.setRGB(x, y, recoloured);
            }
        }
        if (changed == 0) return 0;
        written.add(path);
        Path target = VanillaStyle.PACK.resolve(path);
        Files.createDirectories(target.getParent());
        ImageIO.write(out, "png", target.toFile());
        byte[] meta = metadata.get(path + ".mcmeta");
        if (meta != null) Files.write(VanillaStyle.PACK.resolve(path + ".mcmeta"), meta);
        return changed / (double) visible;
    }

    /** Screens naming a restyled picture; other classes naming one hold the names for the screens that use them. */
    private void findScreens(Map<String, byte[]> classes, PictureNames names) {
        var parents = new HashMap<String, String>();
        var constants = new HashMap<String, String>();
        for (var type : classes.entrySet()) {
            String parent = ClassNames.parent(type.getValue());
            if (parent != null) parents.put(type.getKey(), parent);
            constants.put(type.getKey(), new String(type.getValue(), StandardCharsets.ISO_8859_1));
        }
        var holders = new ArrayList<String>();
        for (var type : classes.entrySet()) {
            if (!names.namedBy(ClassNames.strings(type.getValue()))) continue;
            if (screen(type.getKey(), parents)) list(type.getKey());
            else holders.add(type.getKey());
        }
        for (var type : classes.keySet()) {
            String text = constants.get(type);
            if (!screen(type, parents)) continue;
            if (holders.stream().anyMatch(text::contains)) list(type);
            // a screen naming no picture at all is handed its background by whoever opens it: a holder naming it
            else if (!text.contains("textures/") && !text.contains(".png")
                    && holders.stream().anyMatch(h -> constants.get(h).contains(type))) {
                list(type);
            }
        }
    }

    /** Lists a screen, unless its name or package tells of paper (Ars Nouveau's {@code documentation}). */
    private void list(String type) {
        if (!PAPER.matcher(type).find()) screens.add(type.replace('/', '.'));
    }

    /**
     * Whether the class is a screen: its parents, within the mod, lead to Minecraft's screens, or to a screen of a
     * library outside the mod, which is only known by its name (Let's Do's {@code StationRecipeBookScreen}).
     */
    private static boolean screen(String type, Map<String, String> parents) {
        for (String parent = parents.get(type); parent != null; parent = parents.get(parent)) {
            if (parent.startsWith("net/minecraft/client/gui/screens/")) return true;
            if (!parents.containsKey(parent) && !parent.startsWith("java/")) {
                return parent.endsWith("Screen") || parent.endsWith("Gui");
            }
        }
        return false;
    }

    private static String namespace(String path) {
        return path.split("/")[1];
    }

    /**
     * A picture of a mod's interface; vanilla's own are restyled by {@link VanillaStyle}. HUD pictures are drawn over
     * the world, not on a screen, and keep their look.
     */
    private static boolean interfacePicture(String path) {
        if (!path.startsWith("assets/") || path.startsWith("assets/minecraft/") || !path.endsWith(".png")) return false;
        int textures = path.indexOf("/textures/");
        if (textures < 0) return false;
        String inTextures = path.substring(textures + "/textures/".length());
        if (inTextures.contains("hud")) return false;
        // by folder, not by file name: a block of the world may well be called a screen
        var folders = List.of(inTextures.substring(0, Math.max(0, inTextures.lastIndexOf('/'))).split("/"));
        return folders.getFirst().equals("gui") || folders.stream().anyMatch(INTERFACE_FOLDERS::contains);
    }

    /** Whether no folder or name on the picture's path marks it as an icon, a HUD part, a sign or a cursor. */
    private static boolean partOfScreen(String path) {
        String inTextures = path.substring(path.indexOf("/textures/") + "/textures/".length());
        String parts = inTextures.replace("recipe_book", "recipes");
        return NOT_SCREEN.stream().noneMatch(parts::contains);
    }

    /**
     * Opaque pixels of a picture, those of vanilla's greys and the face alone among them; visible pixels, and those of
     * any grey and of white among them.
     */
    private record Greys(int opaque, int greys, int face, int visible, int neutral, int light, int exactFace,
            int lightFace, int lightSlot) {
        static Greys of(BufferedImage image) {
            int opaque = 0, greys = 0, face = 0, visible = 0, neutral = 0, light = 0;
            int exactFace = 0, lightFace = 0, lightSlot = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    if (argb >>> 24 == 0) continue;
                    visible++;
                    if (VanillaStyle.neutral(argb)) neutral++;
                    if ((argb & 0xFFFFFF) == 0xFFFFFF) light++;
                    if (argb >>> 24 != 0xFF) continue;
                    opaque++;
                    int grey = VanillaStyle.vanillaGrey(argb);
                    if (grey != 0) greys++;
                    if (grey == FACE) face++;
                    int r = argb >> 16 & 0xFF, g = argb >> 8 & 0xFF, b = argb & 0xFF;
                    if (Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > LIGHT_SPREAD) continue;
                    if (Math.abs(g - (FACE & 0xFF)) <= LIGHT_SPREAD) exactFace++;
                    if (Math.abs(g - LIGHT_FACE) <= LIGHT_TOLERANCE) lightFace++;
                    if (Math.abs(g - LIGHT_SLOT) <= 1) lightSlot++;
                }
            }
            return new Greys(opaque, greys, face, visible, neutral, light, exactFace, lightFace, lightSlot);
        }

        /** A screen background in vanilla's style: mostly its greys, with a real face. */
        boolean screen() {
            return opaque >= MIN_SCREEN && face >= MIN_FACE && greys >= MIN_GREYS * opaque;
        }

        /**
         * A plain white picture, all of it light: the mod tints it at run time to fill shapes of any colour (Veil's
         * {@code white.png}), and gold there would tint them all.
         */
        boolean fill() {
            return visible > 0 && light == visible;
        }

        /** A screen in the lighter palette of {@link VanillaStyle#LIGHT_RAMP}: its face and slots, few of vanilla's. */
        boolean lightPalette() {
            return lightFace >= MIN_LIGHT_PALETTE * opaque && lightSlot >= MIN_LIGHT_PALETTE * opaque
                && exactFace < lightFace;
        }

        /** A sprite drawn on such a screen, a slot, a fill, a scroller or a panel: a good part of it grey. */
        boolean backing() {
            return visible > 0 && neutral >= MIN_BACKING * visible;
        }
    }
}
