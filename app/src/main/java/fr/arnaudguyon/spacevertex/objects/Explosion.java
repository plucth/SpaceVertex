/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.objects;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;

import androidx.annotation.NonNull;

public class Explosion extends SpaceObject {

    private static final int NUMBER_OF_LINES = 9;
    private static final long DISPLAY_DURATION = 700;
    private static final long TOTAL_DURATION = 2000;

    private final float mScreenSize;
    private final float[] mLinesX = new float[NUMBER_OF_LINES];
    private final float[] mLinesY = new float[NUMBER_OF_LINES];
    private final long mTime;
    private final @NonNull SpaceObject mParent;
    private boolean mExplosionFinished = false;

    public Explosion(@NonNull SpaceObject parent) {
        mParent = parent;
        mScreenSize = parent.getOriginalSize() * parent.getScreenScale();
        mTime = SystemClock.uptimeMillis();
    }

    public boolean isExplosionFinished() {
        return mExplosionFinished;
    }

    @Override
    public void preDraw(Scene scene, float frameDuration) {
    }

    @Override
    protected float[] getPointDefinitions() {
        return null;
    }

    @Override
    protected float getOriginalSize() {
        return 0;
    }

    @Override
    protected void buildStructure(Scene scene) {
        float angle = (float) (Math.random() * 2 * Math.PI);
        for (int i = 0; i < NUMBER_OF_LINES; ++i) {
            float size = (float) ((mScreenSize / 4) + Math.random() * mScreenSize);
            mLinesX[i] = (float) (size * Math.cos(angle));
            mLinesY[i] = (float) (size * Math.sin(angle));
            angle += (float) (Math.PI * 2 / NUMBER_OF_LINES);
            angle = (float) (angle % (2 * Math.PI));
        }
        Paint paint = getPaint();
        paint.setColor(0xFFFFAAAA); // TODO: red to white with transparency
        paint.setAntiAlias(true);   // TODO: Stroke
    }

    @Override
    public void draw(Canvas canvas, float spaceCenterX, float spaceCenterY) {

        final long elapsed = (SystemClock.uptimeMillis() - mTime);
        if (elapsed > TOTAL_DURATION) {
            mExplosionFinished = true;
            destroy();
            return;
        } else if (elapsed > DISPLAY_DURATION) {
            return;
        }

        float screenScale = mParent.getScreenScale();
        float xScreen = mParent.getScreenWidth() / 2 + (mParent.getPosX() - spaceCenterX) * screenScale;
        float yScreen = mParent.getScreenHeight() / 2 + (mParent.getPosY() - spaceCenterY) * screenScale;

        float scale = elapsed / 50.f;
        for (int i = 0; i < NUMBER_OF_LINES; ++i) {
            float xStop = xScreen + mLinesX[i] * scale;
            float yStop = yScreen + mLinesY[i] * scale;
            canvas.drawLine(xScreen, yScreen, xStop, yStop, getPaint());
        }
    }

}
