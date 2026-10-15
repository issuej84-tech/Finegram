/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.th3nekit.finegram.appearance.material;

/** Contrast guard for imported themes as well as Material You. Pure JVM, allocation-free. */
public final class VoiceProgressColors {
    private VoiceProgressColors() {}
    private static double linear(int c) {
        double value = c / 255.0;
        return value <= .04045 ? value / 12.92 : Math.pow((value + .055) / 1.055, 2.4);
    }
    private static double luminance(int color) {
        return .2126 * linear((color >>> 16) & 255) + .7152 * linear((color >>> 8) & 255) + .0722 * linear(color & 255);
    }
    public static double contrast(int a, int b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + .05) / (Math.min(x, y) + .05);
    }
    public static int blend(int background, int foreground, double alpha) {
        int r = (int) Math.round(((background >>> 16) & 255) * (1 - alpha) + ((foreground >>> 16) & 255) * alpha);
        int g = (int) Math.round(((background >>> 8) & 255) * (1 - alpha) + ((foreground >>> 8) & 255) * alpha);
        int b = (int) Math.round((background & 255) * (1 - alpha) + (foreground & 255) * alpha);
        return 0xff000000 | r << 16 | g << 8 | b;
    }
    private static int composite(int color, int background) { return blend(background, color, (color >>> 24) / 255.0); }
    public static int fill(int requested, int bubble) {
        int visible = composite(requested, bubble);
        if (contrast(visible, bubble) >= 4.5) return visible;
        return contrast(0xff000000, bubble) >= contrast(0xffffffff, bubble) ? 0xff000000 : 0xffffffff;
    }
    public static int track(int requested, int fill, int bubble) {
        int visible = composite(requested, bubble);
        if (contrast(visible, fill) >= 3) return visible;
        // Keep an expressive hint of the fill, but never let played and unplayed merge.
        for (int percent = 24; percent >= 0; percent -= 2) {
            int candidate = blend(bubble, fill, percent / 100.0);
            if (contrast(candidate, fill) >= 3) return candidate;
        }
        return bubble | 0xff000000;
    }
}
