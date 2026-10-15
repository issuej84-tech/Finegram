/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.th3nekit.finegram.appearance.material;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.animation.PathInterpolator;
import android.widget.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.telegram.messenger.R;
import java.util.Locale;

/** Native, independently previewable personalisation flow. No network or account access. */
public class MaterialStudioActivity extends Activity {
    public static Runnable appearanceChanged;
    private static final int PICK_WALLPAPER = 6104;
    private static final int[] SEEDS = {0xff526fb8, 0xff9a82db, 0xff5d8b68, 0xffca8557, 0xffb96587, 0xff368a91};
    private static final int[] COLOUR_NAMES = {R.string.FGM_ColourBlue, R.string.FGM_ColourLilac, R.string.FGM_ColourGreen, R.string.FGM_ColourPeach, R.string.FGM_ColourRose, R.string.FGM_ColourTeal};
    private MaterialSettings settings;
    private MaterialPalette palette;
    private LinearLayout root, page;
    private ScrollView scroll;
    private int step;
    private boolean guide, sounds, changed, importing;
    private ToneGenerator tones;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        settings = new MaterialSettings(this);
        guide = state == null ? getIntent().getBooleanExtra("guide", !settings.guideCompleted()) : state.getBoolean("guide");
        step = state == null ? 0 : state.getInt("step");
        changed = state != null && state.getBoolean("changed");
        // Sound permission is deliberately session-local; recreation always starts silently.
        render(false);
        MaterialWallpaper.warm(this, () -> { if (!isFinishing() && !isDestroyed()) render(false); });
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("guide", guide); state.putInt("step", step); state.putBoolean("changed", changed);
        super.onSaveInstanceState(state);
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private boolean dark() { if (getIntent().hasExtra("dark")) return getIntent().getBooleanExtra("dark", false); return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES; }
    private boolean motion() { return Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled(); }
    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private TextView text(CharSequence value, float size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setTextDirection(View.TEXT_DIRECTION_LOCALE); t.setLineSpacing(dp(2), 1.05f); return t;
    }
    private TextView text(int resource, float size, int color) { return text(getString(resource), size, color); }
    private void gap(LinearLayout target, int height) { View v = new View(this); target.addView(v, new LinearLayout.LayoutParams(1, dp(height))); }
    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private MaterialButton button(int title, boolean filled, Runnable click) {
        MaterialButton b = new MaterialButton(this); b.setText(title); b.setAllCaps(false);
        b.setTextSize(15); b.setMinHeight(dp(52)); b.setMinimumHeight(dp(52)); b.setCornerRadius(dp(28));
        b.setBackgroundTintList(ColorStateList.valueOf(filled ? palette.primary : palette.secondaryContainer));
        b.setTextColor(filled ? palette.onPrimary : palette.onSecondaryContainer);
        b.setOnClickListener(v -> click.run()); return b;
    }
    private void addButton(LinearLayout target, int title, boolean filled, Runnable click) {
        target.addView(button(title, filled, click), new LinearLayout.LayoutParams(-1, -2));
    }
    private void heading(int title, int description) {
        gap(page, 24);
        TextView t = text(title, 22, palette.onSurface); t.setTypeface(null, Typeface.BOLD); page.addView(t);
        gap(page, 8); page.addView(text(description, 15, palette.onSurfaceVariant)); gap(page, 16);
    }
    private void render(boolean animate) {
        if (isDestroyed()) return;
        int oldScroll = scroll == null ? 0 : scroll.getScrollY();
        palette = new MaterialPalette(settings.seed(), dark());
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(palette.surface);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        root = vertical(); root.setBackgroundColor(palette.surface);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(dp(20) + safe.left, safe.top, dp(20) + safe.right, dp(12) + safe.bottom);
            } else view.setPadding(dp(20), insets.getSystemWindowInsetTop(), dp(20), dp(12) + insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root); root.requestApplyInsets();
        if (Build.VERSION.SDK_INT >= 30 && getWindow().getInsetsController() != null) {
            int lightBars = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            getWindow().getInsetsController().setSystemBarsAppearance(dark() ? 0 : lightBars, lightBars);
        } else if (Build.VERSION.SDK_INT < 30) {
            getWindow().getDecorView().setSystemUiVisibility(dark() ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        if (settings.wallpaper()) {
            android.graphics.drawable.Drawable wallpaper = MaterialWallpaper.drawable(palette.surface, settings.scrim());
            if (wallpaper != null) root.setBackground(wallpaper);
        }
        LinearLayout toolbar = new LinearLayout(this); toolbar.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(guide ? getString(R.string.FGM_Step, step + 1, 4) : getString(R.string.FGM_Title), 16, palette.onSurfaceVariant);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1)); title.setGravity(Gravity.CENTER_VERTICAL);
        MaterialButton close = button(guide ? R.string.FGM_SkipShort : R.string.FGM_Close, false, () -> {
            if (guide) { settings.completeGuide(); guide = false; render(false); } else finish();
        });
        close.setContentDescription(getString(guide ? R.string.FGM_Skip : R.string.FGM_Close));
        toolbar.addView(close, new LinearLayout.LayoutParams(-2, -2)); root.addView(toolbar);
        scroll = new ScrollView(this); scroll.setFillViewport(false); scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        page = vertical(); page.setPadding(0, dp(12), 0, dp(24)); scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        if (guide) buildGuide(); else buildStudio();
        if (guide) buildNavigation();
        if (animate && motion()) {
            page.setAlpha(0); page.setTranslationY(dp(24)); page.setScaleX(.98f); page.setScaleY(.98f);
            page.animate().alpha(1).translationY(0).scaleX(1).scaleY(1).setDuration(420)
                .setInterpolator(new PathInterpolator(.2f, 0, 0, 1)).start();
        } else if (!animate) scroll.post(() -> scroll.scrollTo(0, oldScroll));
    }
    private void hero(int title, int description) {
        MaterialCardView card = new MaterialCardView(this); card.setRadius(dp(32)); card.setCardElevation(0);
        card.setStrokeWidth(0); card.setCardBackgroundColor(palette.primaryContainer);
        LinearLayout body = vertical(); body.setPadding(dp(24), dp(24), dp(24), dp(28)); card.addView(body);
        TextView eyebrow = text(R.string.FGM_Eyebrow, 12, palette.onPrimaryContainer); eyebrow.setLetterSpacing(.11f); body.addView(eyebrow);
        gap(body, 24); TextView star = text("✦", 56, palette.onPrimaryContainer); star.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); body.addView(star);
        gap(body, 16); TextView headline = text(title, 34, palette.onPrimaryContainer); headline.setTypeface(null, Typeface.BOLD); body.addView(headline);
        gap(body, 12); body.addView(text(description, 16, palette.onPrimaryContainer)); page.addView(card, new LinearLayout.LayoutParams(-1, -2));
    }
    private void preview() {
        gap(page, 24); TextView label = text(R.string.FGM_Preview, 12, palette.onSurfaceVariant); label.setLetterSpacing(.1f); page.addView(label); gap(page, 12);
        MaterialCardView card = new MaterialCardView(this); card.setCardElevation(0); card.setRadius(dp(28));
        card.setCardBackgroundColor(palette.containerLow); card.setStrokeColor(palette.outlineVariant); card.setStrokeWidth(dp(1));
        LinearLayout body = vertical(); body.setPadding(dp(20), dp(20), dp(20), dp(20)); card.addView(body);
        TextView name = text(R.string.FGM_PreviewName, 17, palette.onSurface); name.setTypeface(null, Typeface.BOLD); body.addView(name); gap(body, 18);
        TextView received = text(R.string.FGM_PreviewMessage, 16, palette.onSurface); received.setPadding(dp(16), dp(12), dp(16), dp(12)); received.setBackground(rounded(palette.containerHigh, 20));
        LinearLayout.LayoutParams incoming = new LinearLayout.LayoutParams(-2, -2); incoming.setMarginEnd(dp(24)); body.addView(received, incoming); gap(body, 12);
        TextView sent = text(R.string.FGM_PreviewReply, 16, palette.onPrimaryContainer); sent.setPadding(dp(16), dp(12), dp(16), dp(12)); sent.setBackground(rounded(palette.primaryContainer, 20));
        LinearLayout.LayoutParams outgoing = new LinearLayout.LayoutParams(-2, -2); outgoing.gravity = Gravity.END; outgoing.setMarginStart(dp(24)); body.addView(sent, outgoing);
        page.addView(card, new LinearLayout.LayoutParams(-1, -2));
    }
    private void colours() {
        heading(R.string.FGM_Accent, R.string.FGM_AccentDesc);
        ChipGroup group = new ChipGroup(this); group.setChipSpacingHorizontal(dp(8)); group.setChipSpacingVertical(dp(8)); group.setSingleSelection(true);
        for (int i = 0; i < SEEDS.length; i++) {
            final int seed = SEEDS[i]; Chip chip = new Chip(this); chip.setId(View.generateViewId());
            chip.setText(COLOUR_NAMES[i]); chip.setCheckable(true); chip.setChecked(seed == settings.seed());
            MaterialPalette sample = new MaterialPalette(seed, dark());
            chip.setChipBackgroundColor(ColorStateList.valueOf(sample.primaryContainer)); chip.setTextColor(sample.onPrimaryContainer);
            chip.setRippleColor(ColorStateList.valueOf((sample.primary & 0x00ffffff) | 0x22000000));
            chip.setChipStrokeColor(ColorStateList.valueOf(sample.primary)); chip.setChipStrokeWidth(seed == settings.seed() ? dp(2) : 0);
            chip.setMinHeight(dp(48)); chip.setEnsureMinTouchTargetSize(true); chip.setCheckedIconTint(ColorStateList.valueOf(sample.onPrimaryContainer));
            chip.setOnClickListener(v -> { settings.seed(seed); changed = true; chime(); render(false); }); group.addView(chip);
        }
        page.addView(group); gap(page, 12); addButton(page, R.string.FGM_Custom, false, this::customColour);
        TextView hex = text(String.format(Locale.ROOT, "#%06X", settings.seed() & 0xffffff), 14, palette.onSurfaceVariant); hex.setGravity(Gravity.END); page.addView(hex);
    }
    private void customColour() {
        TextInputLayout field = new TextInputLayout(this); field.setHint(getString(R.string.FGM_CustomLabel));
        TextInputEditText edit = new TextInputEditText(field.getContext()); edit.setSingleLine(true);
        edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        edit.setText(String.format(Locale.ROOT, "#%06X", settings.seed() & 0xffffff)); field.addView(edit); field.setBoxStrokeColor(palette.primary);
        field.setHintTextColor(ColorStateList.valueOf(palette.primary)); edit.setTextColor(palette.onSurface);
        LinearLayout wrapper = vertical(); wrapper.setPadding(dp(24), dp(8), dp(24), 0); wrapper.addView(field);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setBackground(rounded(palette.containerHigh, 28)).setTitle(R.string.FGM_Custom).setView(wrapper).setNegativeButton(R.string.FGM_Cancel, null).setPositiveButton(R.string.FGM_Apply, null).create();
        dialog.setOnShowListener(d -> {
            tintDialog(dialog);
            dialog.getButton(-1).setOnClickListener(v -> {
            try { settings.seed(MaterialPalette.parseSeed(String.valueOf(edit.getText()))); changed = true; dialog.dismiss(); chime(); render(false); }
            catch (IllegalArgumentException invalid) { field.setError(getString(R.string.FGM_InvalidColor)); }
        }); }); dialog.show();
    }
    private void wallpaper() {
        heading(R.string.FGM_Wallpaper, R.string.FGM_WallpaperDesc);
        MaterialButton choose = button(importing ? R.string.FGM_WallpaperBusy : R.string.FGM_ChooseWallpaper, true, () -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
            try { startActivityForResult(pick, PICK_WALLPAPER); }
            catch (android.content.ActivityNotFoundException missing) { Toast.makeText(this, R.string.FGM_WallpaperError, Toast.LENGTH_LONG).show(); }
        }); choose.setEnabled(!importing); page.addView(choose, new LinearLayout.LayoutParams(-1, -2));
        if (settings.wallpaper()) {
            addButton(page, R.string.FGM_RemoveWallpaper, false, () -> { settings.wallpaper(false); changed = true; render(false); });
            gap(page, 12); TextView scrimLabel = text(getString(R.string.FGM_Scrim) + " · " + settings.scrim() + "%", 15, palette.onSurfaceVariant); page.addView(scrimLabel);
            Slider slider = new Slider(this); slider.setValueFrom(80); slider.setValueTo(95); slider.setStepSize(1); slider.setValue(settings.scrim());
            slider.setContentDescription(getString(R.string.FGM_Scrim)); slider.setTrackInactiveTintList(ColorStateList.valueOf(palette.containerHigh)); slider.setHaloTintList(ColorStateList.valueOf((palette.primary & 0x00ffffff) | 0x33000000));
            slider.setTrackActiveTintList(ColorStateList.valueOf(palette.primary)); slider.setThumbTintList(ColorStateList.valueOf(palette.primary));
            slider.addOnChangeListener((s, value, user) -> {
                if (!user) return; settings.scrim(Math.round(value)); changed = true;
                scrimLabel.setText(getString(R.string.FGM_Scrim) + " · " + Math.round(value) + "%");
                android.graphics.drawable.Drawable image = MaterialWallpaper.drawable(palette.surface, settings.scrim()); if (image != null) root.setBackground(image);
            }); page.addView(slider, new LinearLayout.LayoutParams(-1, dp(56)));
        }
    }
    private void buildStudio() {
        hero(R.string.FGM_Hero, R.string.FGM_HeroDesc); preview(); heading(R.string.FGM_Material, R.string.FGM_MaterialDesc);
        SwitchMaterial material = new SwitchMaterial(this); material.setText(R.string.FGM_Material); material.setTextSize(16); material.setTextColor(palette.onSurface); material.setMinHeight(dp(56)); material.setChecked(settings.enabled());
        material.setUseMaterialThemeColors(false);
        int[][] switchStates = {{android.R.attr.state_checked}, {}};
        material.setThumbTintList(new ColorStateList(switchStates, new int[]{palette.primary, palette.outline}));
        material.setTrackTintList(new ColorStateList(switchStates, new int[]{palette.primaryContainer, palette.containerHigh}));
        material.setOnCheckedChangeListener((b, checked) -> { settings.enabled(checked); changed = true; }); page.addView(material, new LinearLayout.LayoutParams(-1, -2));
        colours(); wallpaper(); gap(page, 24);
        addButton(page, R.string.FGM_Replay, false, () -> { guide = true; step = 0; sounds = false; render(true); });
        addButton(page, R.string.FGM_Reset, false, () -> showThemedDialog(new MaterialAlertDialogBuilder(this).setTitle(R.string.FGM_Reset).setMessage(R.string.FGM_ResetBody)
            .setNegativeButton(R.string.FGM_Cancel, null).setPositiveButton(R.string.FGM_Reset, (d, w) -> { settings.reset(); changed = true; render(false); })));
    }
    private void buildGuide() {
        LinearLayout progress = new LinearLayout(this);
        for (int i = 0; i < 4; i++) { View segment = new View(this); segment.setBackground(rounded(i <= step ? palette.primary : palette.containerHigh, 8)); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(4), 1); lp.setMargins(dp(2), 0, dp(2), dp(16)); progress.addView(segment, lp); }
        page.addView(progress);
        int[] titles = {R.string.FGM_GuideWelcome, R.string.FGM_GuideColour, R.string.FGM_GuideWallpaper, R.string.FGM_GuideDone};
        int[] descriptions = {R.string.FGM_GuideWelcomeDesc, R.string.FGM_GuideColourDesc, R.string.FGM_GuideWallpaperDesc, R.string.FGM_GuideDoneDesc};
        hero(titles[step], descriptions[step]);
        if (step == 1) { colours(); preview(); } else if (step == 2) wallpaper(); else preview();
        gap(page, 16); addButton(page, sounds ? R.string.FGM_SoundOn : R.string.FGM_SoundOff, false, () -> {
            if (sounds) { stopSound(); render(false); } else soundConsent(false);
        });
    }
    private void buildNavigation() {
        LinearLayout navigation = new LinearLayout(this); navigation.setGravity(Gravity.CENTER_VERTICAL);
        if (step > 0) {
            MaterialButton back = button(R.string.FGM_Back, false, () -> { step--; chime(); render(true); });
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, -2, 1); bp.setMarginEnd(dp(12)); navigation.addView(back, bp);
        }
        MaterialButton next = button(step == 0 ? R.string.FGM_Start : step == 3 ? R.string.FGM_Done : R.string.FGM_Next, true, () -> {
            if (step == 0 && !sounds) { soundConsent(true); return; }
            advance();
        }); navigation.addView(next, new LinearLayout.LayoutParams(0, -2, 1)); root.addView(navigation);
    }
    private void advance() {
        if (step == 3) { settings.completeGuide(); guide = false; stopSound(); render(true); return; }
        step++; chime(); render(true); page.announceForAccessibility(getString(R.string.FGM_Step, step + 1, 4));
    }
    private void soundConsent(boolean advance) {
        showThemedDialog(new MaterialAlertDialogBuilder(this).setTitle(R.string.FGM_SoundTitle).setMessage(R.string.FGM_SoundBody)
            .setNegativeButton(R.string.FGM_WithoutSound, (d, w) -> { stopSound(); if (advance) advance(); else render(false); })
            .setPositiveButton(R.string.FGM_WithSound, (d, w) -> { sounds = true; if (advance) advance(); else render(false); }));
    }
    private void tintDialog(androidx.appcompat.app.AlertDialog dialog) {
        for (int which : new int[]{-1, -2, -3}) {
            android.widget.Button button = dialog.getButton(which);
            if (button != null) button.setTextColor(palette.primary);
        }
    }
    private void showThemedDialog(MaterialAlertDialogBuilder builder) {
        androidx.appcompat.app.AlertDialog dialog = builder.setBackground(rounded(palette.containerHigh, 28)).create();
        dialog.setOnShowListener(d -> tintDialog(dialog)); dialog.show();
    }
    private void chime() {
        if (!sounds) return;
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        NotificationManager notifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (audio == null || audio.getRingerMode() != AudioManager.RINGER_MODE_NORMAL || audio.getMode() != AudioManager.MODE_NORMAL) return;
        if (notifications != null && notifications.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL) return;
        try { if (tones == null) tones = new ToneGenerator(AudioManager.STREAM_MUSIC, 22); tones.startTone(ToneGenerator.TONE_PROP_BEEP, 45); }
        catch (RuntimeException unavailable) { stopSound(); }
    }
    private void stopSound() { sounds = false; if (tones != null) { tones.release(); tones = null; } }
    @Override protected void onPause() { stopSound(); super.onPause(); }
    @Override protected void onDestroy() {
        stopSound(); if (isFinishing() && changed && appearanceChanged != null) appearanceChanged.run();
        super.onDestroy();
    }
    @Override public void onBackPressed() {
        if (guide && step > 0) { step--; render(true); } else super.onBackPressed();
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_WALLPAPER || result != RESULT_OK || data == null || data.getData() == null) return;
        importing = true; render(false);
        MaterialWallpaper.importImage(this, data.getData(), success -> {
            if (isDestroyed() || isFinishing()) return;
            importing = false;
            if (success) changed = true; else Toast.makeText(this, R.string.FGM_WallpaperError, Toast.LENGTH_LONG).show();
            render(false);
        });
    }
}
