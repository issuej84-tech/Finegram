/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.th3nekit.finegram.appearance.material;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import com.th3nekit.finegram.preferences.BaseCGPreferencesEntry;

/** Global semantic roles; unrecognised keys retain upstream behaviour (media, errors, brands). */
public final class MaterialAppearance {
    private static MaterialSettings settings;
    private static int cachedSeed;
    private static MaterialPalette light, dark;
    private static android.util.SparseIntArray lightRoles, darkRoles;
    private MaterialAppearance() {}
    public static MaterialSettings settings() {
        if (settings == null && ApplicationLoader.applicationContext != null)
            settings = new MaterialSettings(ApplicationLoader.applicationContext);
        return settings;
    }
    public static MaterialPalette palette() {
        MaterialSettings s = settings();
        int seed = s == null ? MaterialSettings.DEFAULT_SEED : s.seed();
        if (light == null || cachedSeed != seed) {
            cachedSeed = seed;
            light = new MaterialPalette(seed, false);
            dark = new MaterialPalette(seed, true);
            lightRoles = roles(light);
            darkRoles = roles(dark);
        }
        return Theme.getActiveTheme() != null && Theme.isCurrentThemeDark() ? dark : light;
    }
    public static long color(int key) {
        MaterialSettings s = settings();
        // Theme calls getColor during its own class initialization. Never recurse into it.
        if (s == null || !s.enabled() || Theme.getActiveTheme() == null) return Long.MIN_VALUE;
        MaterialPalette p = palette();
        android.util.SparseIntArray map = p.dark ? darkRoles : lightRoles;
        int index = map.indexOfKey(key);
        return index < 0 ? Long.MIN_VALUE : map.valueAt(index) & 0xffffffffL;
    }
    private static android.util.SparseIntArray roles(MaterialPalette p) {
        android.util.SparseIntArray result = new android.util.SparseIntArray(110);
        result.put(Theme.key_windowBackgroundWhite, p.surface);
        result.put(Theme.key_actionBarDefault, p.surface);
        result.put(Theme.key_actionBarDefaultArchived, p.surface);
        result.put(Theme.key_chats_menuBackground, p.surface);
        result.put(Theme.key_chat_messagePanelBackground, p.surface);
        result.put(Theme.key_chat_inBubble, p.surface);
        result.put(Theme.key_dialogBackground, p.surface);
        result.put(Theme.key_dialogBackgroundGray, p.surface);
        result.put(Theme.key_profile_actionBackground, p.surface);
        result.put(Theme.key_windowBackgroundGray, p.containerLow);
        result.put(Theme.key_chats_pinnedOverlay, p.containerLow);
        result.put(Theme.key_chat_inBubbleSelected, p.containerLow);
        result.put(Theme.key_actionBarDefaultSearch, p.container);
        result.put(Theme.key_chat_outBubbleSelected, p.container);
        result.put(Theme.key_listSelector, p.container);
        result.put(Theme.key_chat_messagePanelShadow, p.container);
        result.put(Theme.key_divider, p.containerHigh);
        result.put(Theme.key_switchTrack, p.containerHigh);
        result.put(Theme.key_radioBackground, p.containerHigh);
        result.put(Theme.key_chats_unreadCounterMuted, p.containerHigh);
        result.put(Theme.key_windowBackgroundWhiteBlackText, p.onSurface);
        result.put(Theme.key_actionBarDefaultTitle, p.onSurface);
        result.put(Theme.key_actionBarDefaultIcon, p.onSurface);
        result.put(Theme.key_chats_name, p.onSurface);
        result.put(Theme.key_chats_nameMessage, p.onSurface);
        result.put(Theme.key_chats_menuItemText, p.onSurface);
        result.put(Theme.key_dialogTextBlack, p.onSurface);
        result.put(Theme.key_dialogIcon, p.onSurface);
        result.put(Theme.key_dialogSearchText, p.onSurface);
        result.put(Theme.key_profile_title, p.onSurface);
        result.put(Theme.key_player_actionBarTitle, p.onSurface);
        result.put(Theme.key_player_button, p.onSurface);
        result.put(Theme.key_windowBackgroundWhiteGrayText, p.onSurfaceVariant);
        result.put(Theme.key_windowBackgroundWhiteGrayText2, p.onSurfaceVariant);
        result.put(Theme.key_windowBackgroundWhiteGrayText3, p.onSurfaceVariant);
        result.put(Theme.key_windowBackgroundWhiteGrayText4, p.onSurfaceVariant);
        result.put(Theme.key_windowBackgroundWhiteGrayText5, p.onSurfaceVariant);
        result.put(Theme.key_windowBackgroundWhiteGrayText6, p.onSurfaceVariant);
        result.put(Theme.key_windowBackgroundWhiteGrayText7, p.onSurfaceVariant);
        result.put(Theme.key_windowBackgroundWhiteGrayText8, p.onSurfaceVariant);
        result.put(Theme.key_windowBackgroundWhiteGrayIcon, p.onSurfaceVariant);
        result.put(Theme.key_actionBarDefaultSubtitle, p.onSurfaceVariant);
        result.put(Theme.key_actionBarDefaultSearchPlaceholder, p.onSurfaceVariant);
        result.put(Theme.key_chats_message, p.onSurfaceVariant);
        result.put(Theme.key_chats_date, p.onSurfaceVariant);
        result.put(Theme.key_chat_inTimeText, p.onSurfaceVariant);
        result.put(Theme.key_dialogTextGray, p.onSurfaceVariant);
        result.put(Theme.key_dialogTextGray2, p.onSurfaceVariant);
        result.put(Theme.key_dialogTextGray3, p.onSurfaceVariant);
        result.put(Theme.key_chat_messagePanelHint, p.onSurfaceVariant);
        result.put(Theme.key_chat_messagePanelIcons, p.onSurfaceVariant);
        result.put(Theme.key_windowBackgroundWhiteBlueText, p.primary);
        result.put(Theme.key_windowBackgroundWhiteBlueText2, p.primary);
        result.put(Theme.key_windowBackgroundWhiteBlueText3, p.primary);
        result.put(Theme.key_windowBackgroundWhiteBlueText4, p.primary);
        result.put(Theme.key_windowBackgroundWhiteBlueText5, p.primary);
        result.put(Theme.key_windowBackgroundWhiteBlueText6, p.primary);
        result.put(Theme.key_windowBackgroundWhiteBlueText7, p.primary);
        result.put(Theme.key_windowBackgroundWhiteBlueHeader, p.primary);
        result.put(Theme.key_windowBackgroundWhiteValueText, p.primary);
        result.put(Theme.key_windowBackgroundWhiteLinkText, p.primary);
        result.put(Theme.key_dialogTextBlue, p.primary);
        result.put(Theme.key_dialogTextBlue2, p.primary);
        result.put(Theme.key_dialogTextBlue4, p.primary);
        result.put(Theme.key_dialogButton, p.primary);
        result.put(Theme.key_radioBackgroundChecked, p.primary);
        result.put(Theme.key_checkbox, p.primary);
        result.put(Theme.key_switchTrackChecked, p.primary);
        result.put(Theme.key_switchTrackBlueChecked, p.primary);
        result.put(Theme.key_chats_actionBackground, p.primary);
        result.put(Theme.key_chats_unreadCounter, p.primary);
        result.put(Theme.key_chats_actionMessage, p.primary);
        result.put(Theme.key_chats_sentCheck, p.primary);
        result.put(Theme.key_chats_sentReadCheck, p.primary);
        result.put(Theme.key_chat_messagePanelSend, p.primary);
        result.put(Theme.key_chat_inReplyLine, p.primary);
        result.put(Theme.key_chat_inReplyNameText, p.primary);
        result.put(Theme.key_featuredStickers_addButton, p.primary);
        result.put(Theme.key_player_progress, p.primary);
        result.put(Theme.key_chat_attachIcon, p.primary);
        result.put(Theme.key_chat_outSentCheck, p.primary);
        result.put(Theme.key_chat_outSentCheckRead, p.primary);
        result.put(Theme.key_chats_actionIcon, p.onPrimary);
        result.put(Theme.key_chats_unreadCounterText, p.onPrimary);
        result.put(Theme.key_checkboxCheck, p.onPrimary);
        result.put(Theme.key_featuredStickers_buttonText, p.onPrimary);
        result.put(Theme.key_switchTrackBlueThumbChecked, p.onPrimary);
        result.put(Theme.key_chat_outBubble, p.primaryContainer);
        result.put(Theme.key_windowBackgroundChecked, p.primaryContainer);
        result.put(Theme.key_dialogButtonSelector, p.primaryContainer);
        result.put(Theme.key_actionBarTabSelector, p.primaryContainer);
        result.put(Theme.key_chat_outTimeText, p.onPrimaryContainer);
        result.put(Theme.key_chat_outReplyNameText, p.onPrimaryContainer);
        result.put(Theme.key_chat_outReplyLine, p.onPrimaryContainer);
        result.put(Theme.key_switchTrackBlueThumb, p.onPrimaryContainer);
        result.put(Theme.key_text_RedBold, p.error);
        result.put(Theme.key_text_RedRegular, p.error);
        result.put(Theme.key_chats_sentError, p.error);
        result.put(Theme.key_chat_sentError, p.error);
        result.put(Theme.key_chat_inVoiceSeekbarFill, p.primary);
        result.put(Theme.key_chat_outVoiceSeekbarFill, p.primary);
        result.put(Theme.key_chat_inAudioProgress, p.primary);
        result.put(Theme.key_chat_outAudioProgress, p.primary);
        result.put(Theme.key_chat_inAudioSelectedProgress, p.primary);
        result.put(Theme.key_chat_outAudioSelectedProgress, p.primary);
        result.put(Theme.key_chat_inAudioSeekbarFill, p.primary);
        result.put(Theme.key_chat_outAudioSeekbarFill, p.primary);
        result.put(Theme.key_chat_messageLinkIn, p.primary);
        result.put(Theme.key_chat_messageLinkOut, p.primary);
        result.put(Theme.key_chat_inVoiceSeekbar, p.outlineVariant);
        result.put(Theme.key_chat_outVoiceSeekbar, p.outlineVariant);
        result.put(Theme.key_chat_inVoiceSeekbarSelected, p.outlineVariant);
        result.put(Theme.key_chat_outVoiceSeekbarSelected, p.outlineVariant);
        result.put(Theme.key_chat_inAudioSeekbar, p.outlineVariant);
        result.put(Theme.key_chat_outAudioSeekbar, p.outlineVariant);
        result.put(Theme.key_chat_messageTextIn, p.onSurface);
        result.put(Theme.key_chat_messageTextOut, p.onPrimaryContainer);
        result.put(Theme.key_chat_outAudioDurationText, p.onPrimaryContainer);
        result.put(Theme.key_chat_outAudioTitleText, p.onPrimaryContainer);
        result.put(Theme.key_chat_inAudioDurationText, p.onSurfaceVariant);
        result.put(Theme.key_chat_inAudioTitleText, p.onSurfaceVariant);
        return result;
    }
    public static boolean isEnabled() {
        MaterialSettings s = settings();
        return s != null && s.enabled();
    }
    private static boolean offeredGuide;
    public static void maybeShowGuide(BaseFragment fragment) {
        MaterialSettings s = settings();
        if (s == null || s.guideCompleted() || offeredGuide || fragment.getParentActivity() == null
                || fragment.getParentActivity().isFinishing() || fragment.getFragmentView() == null
                || !fragment.getFragmentView().isShown()) return;
        offeredGuide = true;
        open(fragment.getParentActivity(), true);
    }
    public static void open(Context context, boolean guide) {
        if (context == null) return;
        MaterialStudioActivity.appearanceChanged = MaterialAppearance::applyChanges;
        Intent intent = new Intent(context, MaterialStudioActivity.class);
        intent.putExtra("guide", guide);
        intent.putExtra("dark", Theme.getActiveTheme() != null && Theme.isCurrentThemeDark());
        context.startActivity(intent);
    }
    public static void preloadWallpapers() {
        MaterialSettings s = settings();
        if (s != null && s.wallpaper()) MaterialWallpaper.warm(ApplicationLoader.applicationContext, null);
    }
    public static void setSurface(BaseFragment fragment, View view, int fallback) {
        if (view == null) return;
        MaterialSettings s = settings();
        if (s != null && s.wallpaper() && MaterialWallpaper.applyTo(view, palette().surface, s.scrim())) return;
        Drawable old = view.getBackground();
        if (!(old instanceof ColorDrawable) || ((ColorDrawable) old).getColor() != fallback) view.setBackgroundColor(fallback);
    }
    public static void decorate(BaseFragment fragment, View view) {
        MaterialSettings s = settings();
        if (s == null || view == null || !s.wallpaper()) return;
        String name = fragment.getClass().getSimpleName();
        boolean content = fragment instanceof BaseCGPreferencesEntry || name.equals("DialogsActivity")
                || name.equals("SettingsActivity") || name.equals("ProfileActivity")
                || name.equals("ContactsActivity") || name.equals("CallLogActivity") || name.equals("MainTabsActivity");
        if (!MaterialWallpaper.ready()) {
            MaterialWallpaper.warm(view.getContext(), () -> {
                if (fragment.getClass().getSimpleName().equals("ChatActivity"))
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewWallpapper);
                else decorate(fragment, view);
            });
        }
        // No wallpaper overlays on authentication, camera, media, or payment screens.
        if (!content) return;
        MaterialWallpaper.applyTo(view, palette().surface, s.scrim());
        softenContent(view, palette());
    }
    private static void softenContent(View view, MaterialPalette p) {
        Drawable background = view.getBackground();
        if (background instanceof ColorDrawable) {
            int color = ((ColorDrawable) background).getColor();
            if (color == p.surface || color == p.containerLow || color == p.container)
                view.setBackgroundColor((color & 0x00ffffff) | 0xc7000000);
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) softenContent(group.getChildAt(i), p);
        }
    }
    private static Drawable cachedChatWallpaper;
    private static long wallpaperRevision = -1;
    private static boolean wallpaperDark;
    public static Drawable chatWallpaper() {
        MaterialSettings s = settings();
        if (s == null || !s.wallpaper() || !MaterialWallpaper.ready()) return null;
        MaterialPalette p = palette();
        if (cachedChatWallpaper == null || wallpaperRevision != s.revision() || wallpaperDark != p.dark) {
            wallpaperRevision = s.revision(); wallpaperDark = p.dark;
            cachedChatWallpaper = MaterialWallpaper.drawable(p.surface, 12);
        }
        return cachedChatWallpaper;
    }
    public static void applyChanges() {
        Context c = ApplicationLoader.applicationContext;
        if (c == null) return;
        Theme.reloadAllResources(c);
        Theme.reloadWallpaper(true);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme, false, true);
        if (LaunchActivity.instance != null && LaunchActivity.instance.getActionBarLayout() != null)
            LaunchActivity.instance.getActionBarLayout().rebuildAllFragmentViews(true, true);
    }
}
