package com.th3nekit.finegram.materialpreview;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.widget.FrameLayout;
import androidx.exifinterface.media.ExifInterface;
import com.th3nekit.finegram.appearance.material.MaterialSettings;
import com.th3nekit.finegram.appearance.material.MaterialWallpaper;
import java.io.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Standalone Android regression runner; touches only the disposable preview package. */
public final class MaterialPreviewInstrumentation extends Instrumentation {
    private int assertions;
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    private void check(boolean ok, String message) {
        assertions++; if (!ok) throw new AssertionError(message);
    }
    private boolean importImage(Context context, File file) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        boolean[] result = new boolean[2];
        MaterialWallpaper.importImage(context, Uri.fromFile(file), ok -> {
            result[0] = ok; result[1] = Looper.myLooper() == Looper.getMainLooper(); done.countDown();
        });
        check(done.await(10, TimeUnit.SECONDS), "Wallpaper import timed out");
        check(result[1], "Result callback was not on the main thread");
        return result[0];
    }
    private static byte[] read(File file) throws IOException {
        try (InputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[4096]; int n;
            while ((n = in.read(bytes)) >= 0) out.write(bytes, 0, n);
            return out.toByteArray();
        }
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Context context = getTargetContext();
            MaterialSettings settings = new MaterialSettings(context);
            settings.reset(); settings.scrim(0); check(settings.scrim() == 80, "Minimum readable scrim");
            settings.scrim(1000); check(settings.scrim() == 95, "Maximum scrim"); settings.scrim(82);
            settings.seed(0x123456); check(settings.seed() == 0xff123456, "Opaque seed normalization");
            File source = new File(context.getCacheDir(), "test-wallpaper.jpg");
            Bitmap image = Bitmap.createBitmap(640, 480, Bitmap.Config.RGB_565); image.eraseColor(Color.rgb(75, 105, 90));
            try (FileOutputStream out = new FileOutputStream(source)) { image.compress(Bitmap.CompressFormat.JPEG, 90, out); }
            image.recycle();
            ExifInterface exif = new ExifInterface(source); exif.setAttribute(ExifInterface.TAG_ORIENTATION, "6"); exif.saveAttributes();
            check(importImage(context, source), "Valid image import failed");
            check(settings.wallpaper(), "Imported wallpaper was not enabled");
            File saved = new File(context.getFilesDir(), "material-wallpaper.jpg");
            BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(saved.getPath(), bounds);
            check(bounds.outWidth == 480 && bounds.outHeight == 640, "EXIF orientation was not applied");
            runOnMainSync(() -> {
                FrameLayout view = new FrameLayout(context);
                check(MaterialWallpaper.applyTo(view, Color.WHITE, 82), "Wallpaper not applied");
                Drawable original = view.getBackground();
                for (int i = 0; i < 100; i++) {
                    check(MaterialWallpaper.applyTo(view, Color.WHITE, 82), "Repeated application failed");
                    check(view.getBackground() == original, "Same wallpaper replaced during resume/update");
                }
                MaterialWallpaper.applyTo(view, Color.BLACK, 82);
                check(view.getBackground() != original, "Real theme change did not update the surface");
            });
            byte[] before = read(saved);
            try (FileOutputStream out = new FileOutputStream(source)) { out.write(new byte[]{1, 2, 3, 4}); }
            check(!importImage(context, source), "Malformed image was accepted");
            check(java.util.Arrays.equals(before, read(saved)), "Failed import changed the previous wallpaper");
            Bitmap wide = Bitmap.createBitmap(8192, 8, Bitmap.Config.RGB_565); wide.eraseColor(Color.BLUE);
            try (FileOutputStream out = new FileOutputStream(source)) { wide.compress(Bitmap.CompressFormat.JPEG, 90, out); }
            wide.recycle(); check(importImage(context, source), "Large image sampling failed");
            BitmapFactory.decodeFile(saved.getPath(), bounds);
            check(bounds.outWidth <= 2048 && bounds.outHeight <= 2048, "Decoded wallpaper exceeded 2048 px");
            source.delete();
            result.putString("stream", "PASS: " + assertions + " native wallpaper/preference assertions: EXIF, safe sampling, corrupt-file rollback, UI-thread callback and 100 idempotent reapplications.\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "FAIL: " + error + "\n"); finish(Activity.RESULT_CANCELED, result);
        }
    }
}
