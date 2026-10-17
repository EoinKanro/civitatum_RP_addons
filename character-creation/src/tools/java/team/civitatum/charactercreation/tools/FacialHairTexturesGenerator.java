package team.civitatum.charactercreation.tools;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Generates the beard and mustache textures, {@code textures/character/facial_hair/beard_1..5.png} and
 * {@code mustache_1..5.png}: 64x64 skin overlays in grey, which the mod tints with the player's hair colour.
 *
 * <p>The artwork is the pixel patterns below. In a pattern every character is one pixel: {@code .} is transparent, a
 * digit 2-6 is a shade of grey ({@code digit * 25 + 35}, darker to lighter).
 *
 * <p>Run with {@code ./gradlew :character-creation:generateFacialHair}, or start {@link #main} straight from the IDE:
 * without arguments it writes into the module's resources, relative to the working directory (the
 * {@code character-creation} module folder, the IDE's default). An argument overrides the output folder.
 */
public final class FacialHairTexturesGenerator {
    private static final int SIZE = 64;
    private static final Path DEFAULT_OUTPUT = Path.of("src/main/resources/assets/civitatum/textures/character/facial_hair");

    /** Beards: rows at the bottom of the face, then rows continuing down the chest (longer styles). */
    private static final List<Style> BEARDS = List.of(
            new Style(List.of("2......2", "243..342"), List.of("..2323..")),
            new Style(List.of("34....43", "34655463"), List.of(".346643.", "..4564..", "..3453..", "...34...")),
            new Style(List.of("...23...", "..3443.."), List.of("...34...", "...23...")),
            new Style(List.of("34....43", "34566543"),
                    List.of("..4564..", "..3453..", "...45...", "...34...", "...23...")),
            new Style(List.of("34....43", "34566543"),
                    List.of(".345543.", "..4564..", "..3443..", "...45...", "...34...", "...45...", "...34...", "...23...")));

    /** Mustaches: rows at the bottom of the face only. */
    private static final List<Style> MUSTACHES = List.of(
            new Style(List.of(".234432.", "...22..."), List.of()),
            new Style(List.of(".342243.", "2......2"), List.of()),
            new Style(List.of(".23..32.", "........"), List.of()),
            new Style(List.of(".345543.", ".234432."), List.of()),
            new Style(List.of("..2332..", "........"), List.of()));

    /** Bottom rows of the face pattern and the rows on the chest. */
    private record Style(List<String> face, List<String> chest) {
    }

    private FacialHairTexturesGenerator() {
    }

    public static void main(String[] args) throws IOException {
        Path output = args.length > 0 ? Path.of(args[0]) : DEFAULT_OUTPUT;
        if (args.length == 0 && !Files.isDirectory(Path.of("src/main/resources"))) {
            throw new IllegalStateException("Run from the character-creation module folder (working directory is "
                    + Path.of("").toAbsolutePath() + ") or pass the output folder as an argument");
        }
        Files.createDirectories(output);
        for (int i = 0; i < BEARDS.size(); i++) {
            write(output, "beard_" + (i + 1), beard(i + 1, BEARDS.get(i)));
        }
        for (int i = 0; i < MUSTACHES.size(); i++) {
            write(output, "mustache_" + (i + 1), mustache(MUSTACHES.get(i)));
        }
        System.out.println("Generated " + BEARDS.size() + " beards and " + MUSTACHES.size() + " mustaches in " + output);
    }

    static BufferedImage beard(int number, Style style) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        // the first beard is stubble: slightly see-through on the face
        drawFace(image, style.face(), number == 1 ? 205 : 255);
        if (number != 3) {
            // sides of the face, on the neighbouring faces of the head
            for (int x : new int[] {7, 16}) {
                for (int y = 12; y < 16; y++) {
                    put(image, x, y, 110 + (x + y) % 3 * 25, 255);
                }
            }
            // underside of the jaw, on the bottom face of the head
            for (int y = 1; y < 7; y++) {
                for (int x = 17; x < 23; x++) {
                    put(image, x, y, 90 + (x * 3 + y * 7) % 5 * 24, 255);
                }
            }
        }
        drawChest(image, style.chest());
        return image;
    }

    static BufferedImage mustache(Style style) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        drawFace(image, style.face(), 255);
        return image;
    }

    /** The face of the head is the 8x8 square at (8, 8); the rows fill its bottom, so the eye rows stay untouched. */
    private static void drawFace(BufferedImage image, List<String> rows, int alpha) {
        draw(image, rows, 8, 16 - rows.size(), alpha);
    }

    /** The front of the body starts at (20, 20). */
    private static void drawChest(BufferedImage image, List<String> rows) {
        draw(image, rows, 20, 20, 255);
    }

    private static void draw(BufferedImage image, List<String> rows, int left, int top, int alpha) {
        for (int y = 0; y < rows.size(); y++) {
            String row = rows.get(y);
            for (int x = 0; x < row.length(); x++) {
                char c = row.charAt(x);
                if (c != '.') {
                    put(image, left + x, top + y, (c - '0') * 25 + 35, alpha);
                }
            }
        }
    }

    private static void put(BufferedImage image, int x, int y, int grey, int alpha) {
        image.setRGB(x, y, alpha << 24 | grey << 16 | grey << 8 | grey);
    }

    private static void write(Path folder, String name, BufferedImage image) throws IOException {
        ImageIO.write(image, "png", folder.resolve(name + ".png").toFile());
    }
}
