/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.th3nekit.finegram.appearance.material;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A single sampled private bitmap, shared by drawable instances; never decode on the UI thread. */
public final class MaterialWallpaper {
    public interface Result { void done(boolean success); }
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile Bitmap cached;
    private static boolean loading;
    private static final java.util.ArrayList<Runnable> WAITERS = new java.util.ArrayList<>();
    private MaterialWallpaper() {}
    private static File file(Context c) { return new File(c.getFilesDir(), "material-wallpaper.jpg"); }

    public static boolean ready() { return cached != null; }
    static Object identity() { return cached; }

    public static void warm(Context context, Runnable ready) {
        Context c = context.getApplicationContext();
        if (cached != null) { if (ready != null) ready.run(); return; }
        synchronized (MaterialWallpaper.class) {
            if (!file(c).exists()) return;
            if (ready != null) WAITERS.add(ready);
            if (loading) return;
            loading = true;
        }
        IO.execute(() -> {
            try {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inPreferredConfig = Bitmap.Config.RGB_565;
                cached = BitmapFactory.decodeFile(file(c).getPath(), o);
            } catch (RuntimeException | OutOfMemoryError ignored) {
                // Keep the opaque theme fallback when storage is corrupt or memory is low.
            } finally {
                java.util.ArrayList<Runnable> callbacks;
                synchronized (MaterialWallpaper.class) {
                    loading = false; callbacks = new java.util.ArrayList<>(WAITERS); WAITERS.clear();
                }
                MAIN.post(() -> { if (cached != null) for (Runnable callback : callbacks) callback.run(); });
            }
        });
    }

    public static void importImage(Context context, Uri uri, Result result) {
        Context c = context.getApplicationContext();
        IO.execute(() -> {
            boolean success = false;
            Bitmap decoded = null, oriented = null;
            File temporary = new File(c.getFilesDir(), "material-wallpaper.tmp");
            try {
                String mime = c.getContentResolver().getType(uri);
                if (mime != null && !mime.startsWith("image/")) throw new IOException("Not an image");
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                    BitmapFactory.decodeStream(in, null, bounds);
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Invalid image");
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 2048) options.inSampleSize *= 2;
                try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                    decoded = BitmapFactory.decodeStream(in, null, options);
                }
                if (decoded == null) throw new IOException("Cannot decode image");
                Matrix matrix = new Matrix();
                try (InputStream in = c.getContentResolver().openInputStream(uri)) {
                    ExifInterface exif = new ExifInterface(in);
                    if (exif.isFlipped()) matrix.postScale(-1, 1);
                    matrix.postRotate(exif.getRotationDegrees());
                } catch (IOException ignored) { /* Non-EXIF formats remain usable. */ }
                oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), matrix, true);
                try (FileOutputStream out = new FileOutputStream(temporary)) {
                    if (!oriented.compress(Bitmap.CompressFormat.JPEG, 90, out)) throw new IOException("Cannot encode image");
                    out.getFD().sync();
                }
                if (!temporary.renameTo(file(c))) throw new IOException("Cannot save image");
                // Do not recycle the old shared bitmap: existing views can still be drawing it.
                cached = oriented;
                new MaterialSettings(c).wallpaper(true);
                success = true;
            } catch (IOException | RuntimeException | OutOfMemoryError ignored) {
                temporary.delete();
            } finally {
                if (decoded != null && decoded != cached) decoded.recycle();
                if (oriented != null && oriented != decoded && oriented != cached) oriented.recycle();
            }
            boolean ok = success;
            MAIN.post(() -> result.done(ok));
        });
    }

    public static Drawable drawable(int surface, int scrimPercent) {
        return cached == null ? null : new WallpaperDrawable(cached, surface, scrimPercent, null);
    }
    /** Draw global chat wallpaper in the same window coordinates as the app surface.
     * Ignore Telegram's legacy parallax/bounds; per-chat wallpaper drawables are untouched. */
    public static boolean drawInView(Drawable drawable, Canvas canvas, android.view.View view) {
        if (!(drawable instanceof WallpaperDrawable) || view == null) return false;
        WallpaperDrawable wallpaper = (WallpaperDrawable) drawable;
        wallpaper.setBounds(0, 0, view.getWidth(), view.getHeight());
        wallpaper.draw(canvas, view);
        return true;
    }
    public static boolean applyTo(android.view.View view, int surface, int scrimPercent) {
        Bitmap bitmap = cached;
        if (bitmap == null) return false;
        Drawable old = view.getBackground();
        if (old instanceof WallpaperDrawable) {
            WallpaperDrawable wallpaper = (WallpaperDrawable) old;
            if (wallpaper.bitmap == bitmap && wallpaper.surface == surface && wallpaper.scrimPercent == scrimPercent) return true;
        }
        view.setBackground(new WallpaperDrawable(bitmap, surface, scrimPercent, view));
        return true;
    }
    public static boolean appliedTo(android.view.View view) { return view.getBackground() instanceof WallpaperDrawable; }
    private static final class WallpaperDrawable extends Drawable {
        final Bitmap bitmap;
        final int surface, scrimPercent;
        final java.lang.ref.WeakReference<android.view.View> anchor;
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        final RectF destination = new RectF();
        final int[] location = new int[2];
        int alpha = 255;
        WallpaperDrawable(Bitmap bitmap, int surface, int scrimPercent, android.view.View view) {
            this.bitmap = bitmap; this.surface = surface; this.scrimPercent = scrimPercent;
            anchor = new java.lang.ref.WeakReference<>(view);
        }
        @Override public void draw(Canvas canvas) { draw(canvas, anchor.get()); }
        void draw(Canvas canvas, android.view.View view) {
            Rect b = getBounds();
            if (b.isEmpty()) return;
            float width = b.width(), height = b.height(), offsetX = 0, offsetY = 0;
            if (view != null && view.isAttachedToWindow()) {
                android.view.View window = view.getRootView();
                width = window.getWidth(); height = window.getHeight();
                view.getLocationInWindow(location); offsetX = location[0]; offsetY = location[1];
            }
            // Anchor the crop to the window, not each fragment's differently-sized content area.
            float scale = Math.max(width / bitmap.getWidth(), height / bitmap.getHeight());
            float w = bitmap.getWidth() * scale, h = bitmap.getHeight() * scale;
            float left = (width - w) / 2 - offsetX, top = (height - h) / 2 - offsetY;
            destination.set(left, top, left + w, top + h);
            int save = canvas.save(); canvas.clipRect(b);
            paint.setAlpha(alpha); canvas.drawBitmap(bitmap, null, destination, paint);
            paint.setColor(surface); paint.setAlpha(Math.round(alpha * scrimPercent / 100f));
            canvas.drawRect(b, paint); canvas.restoreToCount(save);
        }
        @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
