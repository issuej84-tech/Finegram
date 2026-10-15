/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.th3nekit.finegram.appearance.material;

import com.google.android.material.color.utilities.Hct;
import com.google.android.material.color.utilities.MaterialDynamicColors;
import com.google.android.material.color.utilities.SchemeTonalSpot;

/** Pixel-style Material You tonal roles, generated in HCT rather than tinting RGB. */
public final class MaterialPalette {
    public final int primary, onPrimary, primaryContainer, onPrimaryContainer;
    public final int secondary, secondaryContainer, onSecondaryContainer;
    public final int surface, containerLow, container, containerHigh, onSurface;
    public final int onSurfaceVariant, outline, outlineVariant, error, onError;
    public final boolean dark;

    public MaterialPalette(int seed, boolean dark) {
        this.dark = dark;
        SchemeTonalSpot scheme = new SchemeTonalSpot(Hct.fromInt(seed | 0xff000000), dark, 0);
        MaterialDynamicColors c = new MaterialDynamicColors();
        primary = c.primary().getArgb(scheme);
        onPrimary = c.onPrimary().getArgb(scheme);
        primaryContainer = c.primaryContainer().getArgb(scheme);
        onPrimaryContainer = c.onPrimaryContainer().getArgb(scheme);
        secondary = c.secondary().getArgb(scheme);
        secondaryContainer = c.secondaryContainer().getArgb(scheme);
        onSecondaryContainer = c.onSecondaryContainer().getArgb(scheme);
        surface = c.surface().getArgb(scheme);
        containerLow = c.surfaceContainerLow().getArgb(scheme);
        container = c.surfaceContainer().getArgb(scheme);
        containerHigh = c.surfaceContainerHigh().getArgb(scheme);
        onSurface = c.onSurface().getArgb(scheme);
        onSurfaceVariant = c.onSurfaceVariant().getArgb(scheme);
        outline = c.outline().getArgb(scheme);
        outlineVariant = c.outlineVariant().getArgb(scheme);
        error = c.error().getArgb(scheme);
        onError = c.onError().getArgb(scheme);
    }

    /** Accept exactly six hex digits; malformed or alpha input must not be saved. */
    public static int parseSeed(String input) {
        String hex = input.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (!hex.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Use #RRGGBB");
        return (int) Long.parseLong(hex, 16) | 0xff000000;
    }
}
