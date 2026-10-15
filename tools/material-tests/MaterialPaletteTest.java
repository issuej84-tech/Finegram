import com.th3nekit.finegram.appearance.material.MaterialPalette;
import java.util.Random;

/** Plain JVM regression tests; no Telegram API credentials or Android device required. */
public final class MaterialPaletteTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static double linear(int channel) {
        double c = channel / 255.0;
        return c <= .04045 ? c / 12.92 : Math.pow((c + .055) / 1.055, 2.4);
    }
    private static double luminance(int rgb) {
        return .2126 * linear((rgb >>> 16) & 255) + .7152 * linear((rgb >>> 8) & 255) + .0722 * linear(rgb & 255);
    }
    private static void contrast(int foreground, int background, String name) {
        double a = luminance(foreground), b = luminance(background);
        double ratio = (Math.max(a, b) + .05) / (Math.min(a, b) + .05);
        check(ratio >= 4.5, name + " contrast " + ratio + " < 4.5");
    }
    public static void main(String[] args) {
        check(MaterialPalette.parseSeed(" #526fb8 ") == 0xff526fb8, "hex normalization");
        check(MaterialPalette.parseSeed("FFFFFF") == 0xffffffff, "white");
        check(MaterialPalette.parseSeed("000000") == 0xff000000, "black");
        for (String invalid : new String[]{"", "#12", "#12345678", "gggggg", "red", "#12345Z", "123 456"}) {
            try { MaterialPalette.parseSeed(invalid); throw new AssertionError("Accepted invalid seed: " + invalid); }
            catch (IllegalArgumentException expected) { checks++; }
        }
        Random random = new Random(614);
        int[] special = {0xff000000, 0xffffffff, 0xffff0000, 0xff00ff00, 0xff0000ff, 0xff526fb8};
        for (int i = 0; i < 518; i++) {
            int seed = i < special.length ? special[i] : random.nextInt() | 0xff000000;
            for (boolean dark : new boolean[]{false, true}) {
                MaterialPalette p = new MaterialPalette(seed, dark);
                contrast(p.onSurface, p.surface, "surface");
                contrast(p.onSurfaceVariant, p.containerLow, "secondary text");
                contrast(p.onPrimary, p.primary, "filled button");
                contrast(p.onPrimaryContainer, p.primaryContainer, "outgoing bubble");
                contrast(p.onSecondaryContainer, p.secondaryContainer, "secondary button");
                contrast(p.onError, p.error, "error");
                int worstPhoto = dark ? 0xffffffff : 0xff000000;
                int scrimBackground = com.th3nekit.finegram.appearance.material.VoiceProgressColors.blend(worstPhoto, p.surface, .80);
                contrast(p.onSurface, scrimBackground, "wallpaper body text");
                contrast(p.onSurfaceVariant, scrimBackground, "wallpaper secondary text");
            }
        }
        System.out.println("PASS: " + checks + " assertions across 518 seeds in light and dark Material You palettes");
    }
}
