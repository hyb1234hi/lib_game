/*
 * Copyright 2013 MicaByte Systems
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.micabyte.android.graphics;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;

import com.micabyte.android.BuildConfig;

import java.lang.ref.SoftReference;

/**
 * ImageHandler is a singleton class that is used to manage bitmaps resources used programmatically in
 * the app (i.e., not bitmaps assigned in layouts). By allocating and managing them in a central
 * cache, we avoid the problem of leaking bitmaps in the code (assuming one doesn't copy them) and
 * reuse bitmaps where possible. Bitmaps are also placed in a SoftReference so that - at least in
 * theory - they are purged when memory runs low.
 *
 * @author micabyte
 */
public class ImageHandler {
    private static final String TAG = ImageHandler.class.getName();
    // Default Config for Bitmap Retrieval
    private static final Bitmap.Config DEFAULT_CONFIG = Bitmap.Config.ARGB_8888;
    // Application context
    private final Resources resources;
    // Shortcut to the Display Density
    public static float density;
    // Bitmap cache
    private final SparseArray<SoftReference<Bitmap>> cachedBitmaps = new SparseArray<>();
    private final SparseArray<Bitmap> persistBitmaps = new SparseArray<>();

    private ImageHandler(Context c) {
        resources = c.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        density = metrics.density;
    }

    @SuppressWarnings("CallToSystemGC")
    public void release() {
        cachedBitmaps.clear();
        System.gc();
    }

    public Bitmap get(int key) {
        return get(key, DEFAULT_CONFIG, false);
    }

    public Bitmap get(int key, boolean persist) {
        return get(key, DEFAULT_CONFIG, persist);
    }

    public Bitmap get(int key, Bitmap.Config config) {
        return get(key, config, false);
    }

    Bitmap get(int key, Bitmap.Config config, boolean persist) {
        if (key == 0)
            if (BuildConfig.DEBUG) Log.d(TAG, "Null resource sent to get()", new Exception());
        Bitmap ret;
        if (persist) {
            ret = persistBitmaps.get(key);
            if (ret != null) return ret;
        }
        SoftReference<Bitmap> ref = cachedBitmaps.get(key);
        if (ref != null) {
            ret = ref.get();
            if (ret != null) {
                return ret;
            }
        }
        ret = loadBitmap(key, config);
        if (persist)
            persistBitmaps.put(key, ret);
        else
            cachedBitmaps.put(key, new SoftReference<>(ret));
        return ret;
    }

    @SuppressWarnings("deprecation")
    private Bitmap loadBitmap(int key, Bitmap.Config bitmapConfig) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = bitmapConfig;
        opts.inPurgeable = true;
        opts.inInputShareable = true;
        return BitmapFactory.decodeResource(resources, key, opts);
    }

    public Bitmap getSceneBitmap(int bkg, int left, int right) {
        Bitmap bitmap = get(bkg);
        if (bitmap == null) return null;
        Bitmap output =
                Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        int color = 0xff424242;
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF rectF = new RectF(rect);
        float roundPx = 12;
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        if (left > 0) {
            canvas.drawBitmap(get(left), 0, 0, null);
        }
        if (right > 0) {
            canvas.drawBitmap(get(right), 0, 0, null);
            // canvas.drawBitmap(get(right), bitmap.getWidth()/2, 0, null);
        }
        return output;
    }

    public BitmapFactory.Options getDimensions(int key) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = BitmapSurfaceRenderer.DEFAULT_CONFIG;
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, key, opt);
        return opt;
    }

    // Instance Code
    private static ImageHandler instance;

    public static ImageHandler getInstance(Context c) {
        checkInstance(c);
        return instance;
    }

    private static void checkInstance(Context c) {
        if (instance == null) instance = new ImageHandler(c);
    }

}