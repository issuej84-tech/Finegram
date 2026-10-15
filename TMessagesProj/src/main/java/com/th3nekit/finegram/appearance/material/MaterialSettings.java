/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.th3nekit.finegram.appearance.material;

import android.content.Context;
import android.content.SharedPreferences;

/** Device-local preferences. No account, message, or sound-consent data is uploaded. */
public final class MaterialSettings {
    public static final int DEFAULT_SEED = 0xff526fb8;
    public static final int GUIDE_VERSION = 1;
    private final SharedPreferences preferences;

    public MaterialSettings(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences("finegram_material", Context.MODE_PRIVATE);
    }
    public boolean enabled() { return preferences.getBoolean("enabled", true); }
    public int seed() { return preferences.getInt("seed", DEFAULT_SEED); }
    public boolean wallpaper() { return preferences.getBoolean("wallpaper", false); }
    public int scrim() { return Math.max(80, Math.min(95, preferences.getInt("scrim", 82))); }
    public boolean guideCompleted() { return preferences.getInt("guide_version", 0) >= GUIDE_VERSION; }
    public long revision() { return preferences.getLong("revision", 0); }
    private SharedPreferences.Editor edit() {
        return preferences.edit().putLong("revision", revision() + 1);
    }
    public void enabled(boolean value) { edit().putBoolean("enabled", value).apply(); }
    public void seed(int value) { edit().putInt("seed", value | 0xff000000).apply(); }
    public void wallpaper(boolean value) { edit().putBoolean("wallpaper", value).apply(); }
    public void scrim(int value) { edit().putInt("scrim", Math.max(80, Math.min(95, value))).apply(); }
    public void completeGuide() { preferences.edit().putInt("guide_version", GUIDE_VERSION).apply(); }
    public void reset() {
        edit().remove("seed").remove("scrim").putBoolean("wallpaper", false).putBoolean("enabled", true).apply();
    }
}
