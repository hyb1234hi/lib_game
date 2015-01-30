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
package com.micabyte.android.map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import com.micabyte.android.BaseObject;
import com.micabyte.android.graphics.SurfaceRenderer;

/**
 * HexMap superclass
 * <p/>
 * This implementation works for pointy-side up HexMaps. Needs to be adjusted
 * if it is going to be used for flat-side up maps.
 *
 * @author micabyte
 */
public abstract class HexMap extends BaseObject {
    public static boolean standardOrientation = true;
    public static int mapWidth;
    public static int mapHeight;
    public static int tileSlope;
    public static Rect tileRect = new Rect();
    protected TileMapZone[][] zones;
    protected float scaleFactor;
    protected final Point viewPortOrigin = new Point();
    protected final Point viewPortSize = new Point();
    protected final Rect destRect = new Rect();
    protected int windowLeft;
    protected int windowTop;
    protected int windowRight;
    protected int windowBottom;
    private final Paint tilePaint = new Paint();
    protected final Paint tileText = new Paint();
    // Draw
    protected final Canvas canvas = new Canvas();

    protected HexMap(String id, String name) {
        super(id, name, 0);
        tilePaint.setAntiAlias(true);
        tilePaint.setFilterBitmap(true);
        tilePaint.setDither(true);
        Paint select = new Paint();
        select.setStyle(Paint.Style.STROKE);
        select.setColor(Color.RED);
        select.setStrokeWidth(2);

    }

    protected void setHexMap(Context c, TileMapZone[][] map) {
        zones = map;
        mapHeight = map[0].length;
        mapWidth = map.length;
        tileRect = new Rect(0, 0, map[0][0].getWidth(c), map[0][0].getHeight(c));
        tileSlope = tileRect.height() / 4;
    }

    public void drawBase(Context c, SurfaceRenderer.ViewPort p) {
        if (p.getBitmap() == null) {
            Log.e("HM", "Viewport bitmap is null");
            return;
        }
        canvas.setBitmap(p.getBitmap());
        scaleFactor = p.getZoom();
        int yOffset = (tileRect.height() - tileSlope);
        int xOffset;
        p.getOrigin(viewPortOrigin);
        p.getSize(viewPortSize);
        windowLeft = viewPortOrigin.x;
        windowTop = viewPortOrigin.y;
        windowRight = viewPortOrigin.x + viewPortSize.x;
        windowBottom = viewPortOrigin.y + viewPortSize.y;
        if (standardOrientation) {
            // Clip tiles not in view
            int iMn = (windowLeft / tileRect.width()) - 1;
            if (iMn < 0) iMn = 0;
            int jMn = (windowTop / (tileRect.height() - tileSlope)) - 1;
            if (jMn < 0) jMn = 0;
            int iMx = (windowRight / tileRect.width()) + 2;
            if (iMx >= mapWidth) iMx = mapWidth;
            int jMx = (windowBottom / (tileRect.height() - tileSlope)) + 2;
            if (jMx >= mapHeight) jMx = mapHeight;
            // Draw Tiles
            for (int i = iMn; i < iMx; i++) {
                for (int j = jMn; j < jMx; j++) {
                    if (zones[i][j] != null) {
                        xOffset = (j % 2) == 0 ? tileRect.width() / 2 : 0;
                        destRect.left = (int) (((i * tileRect.width()) - windowLeft - xOffset) / scaleFactor);
                        destRect.top = (int) (((j * (tileRect.height() - tileSlope)) - windowTop - yOffset) / scaleFactor);
                        destRect.right = (int) ((((i * tileRect.width()) + tileRect.width()) - windowLeft - xOffset) / scaleFactor);
                        destRect.bottom = (int) ((((j * (tileRect.height() - tileSlope)) + tileRect.height()) - windowTop - yOffset) / scaleFactor);
                        zones[i][j].drawBase(c, canvas, tileRect, destRect, tilePaint);
                    }
                }
            }
        } else {
            // Clip tiles not in view
            int iMn = mapWidth - (windowRight / tileRect.width()) - 2;
            if (iMn < 0) iMn = 0;
            int jMn = mapHeight - ((windowBottom / (tileRect.height() - tileSlope)) + 2);
            if (jMn < 0) jMn = 0;
            int iMx = mapWidth - ((windowLeft / tileRect.width()) + 1);
            if (iMx >= mapWidth) iMx = mapWidth - 1;
            int jMx = mapHeight - ((windowTop / (tileRect.height() - tileSlope)) + 1);
            if (jMx >= mapHeight) jMx = mapHeight - 1;
            // Draw Tiles
            for (int i = iMx; i >= iMn; i--) {
                for (int j = jMx; j >= jMn; j--) {
                    if (zones[i][j] != null) {
                        xOffset = (j % 2) == 1 ? tileRect.width() / 2 : 0;
                        destRect.left = (int) ((((mapWidth - i - 1) * tileRect.width()) - windowLeft - xOffset) / scaleFactor);
                        destRect.top = (int) ((((mapHeight - j - 1) * (tileRect.height() - tileSlope)) - windowTop - yOffset) / scaleFactor);
                        destRect.right = (int) (((((mapWidth - i - 1) * tileRect.width()) + tileRect.width()) - windowLeft - xOffset) / scaleFactor);
                        destRect.bottom = (int) (((((mapHeight - j - 1) * (tileRect.height() - tileSlope)) + tileRect.height()) - windowTop - yOffset) / scaleFactor);
                        zones[i][j].drawBase(c, canvas, tileRect, destRect, tilePaint);
                    }
                }
            }
        }
    }

    public abstract void drawLayer(Context c, SurfaceRenderer.ViewPort p);

    public abstract void drawFinal(Context c, SurfaceRenderer.ViewPort p);

    public abstract Point getViewPortOrigin(int x, int y, SurfaceRenderer.ViewPort p);

}