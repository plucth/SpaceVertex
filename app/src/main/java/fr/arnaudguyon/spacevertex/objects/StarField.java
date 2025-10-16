/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.objects;

import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class StarField extends SpaceObject {

    static class StarData {
        float mX, mY;
        float[] mCoords = new float[4*2];
        int mColor;
        float mSpeed;
        float mStrokeWidth;
    }

    private final int mNbStars;
    protected boolean mBigStar;
    protected ArrayList<StarData> mStars;
    private final @NonNull Paint mPaint = new Paint();

    public StarField(int nbStars, boolean bigStar) {
        mNbStars = nbStars;
        mBigStar = bigStar;
    }

    @Override
    public void preDraw(Scene scene, float frameDuration) {
    }

    @Override
    public void draw(Canvas canvas, float spaceCenterX, float spaceCenterY) {

        final float width = getScreenWidth();
        final float height = getScreenHeight();

        final float screenScale = getScreenScale();
        for(StarData data : mStars) {

            mPaint.setColor(data.mColor);
            mPaint.setStrokeWidth(data.mStrokeWidth);

            float x = (data.mX - spaceCenterX*screenScale*data.mSpeed) % width;
            if (x < 0) {
                x += width;
            }
            float y = (data.mY - spaceCenterY*screenScale*data.mSpeed) % height;
            if (y < 0) {
                y += height;
            }

            canvas.drawLine(x+data.mCoords[0], y+data.mCoords[1], x+data.mCoords[4], y+data.mCoords[5], mPaint);
            canvas.drawLine(x+data.mCoords[2], y+data.mCoords[3], x+data.mCoords[6], y+data.mCoords[7], mPaint);
        }
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

        mStars = new ArrayList<>(mNbStars);

        float screenScale = getScreenScale();
        final float width = getScreenWidth();
        final float height = getScreenHeight();
        float bigStroke = Math.max(height / 400f, 2);
        float smallStroke = Math.max(height / 800f, 1);

        for(int i=0; i<mNbStars; ++i) {

            StarData data = new StarData();

            boolean isFront = mBigStar;

            data.mX = (float) (Math.random() * width);
            data.mY = (float) (Math.random() * height);

            float size = isFront ? 20.f*screenScale : 12.f*screenScale;
            float angle = (float) (Math.random() * Math.PI * 2);
            for(int loop = 0; loop<4; ++loop) {
                data.mCoords[loop*2 + 0] = (float) (Math.cos(angle) * size);
                data.mCoords[loop*2 + 1] = (float) (Math.sin(angle) * size);
                angle += Math.PI / 2;
            }
            int red = (int) (Math.random() * 256);
            int grn = (int) (Math.random() * 256);
            int blu = (int) (Math.random() * 256);
            if (isFront) {
                red |= 0x80;
                grn |= 0x80;
                blu |= 0x80;
                data.mSpeed = 1;
                data.mStrokeWidth = bigStroke;
            } else {
                red &= 0x7F;
                grn &= 0x7F;
                blu &= 0x7F;
                data.mSpeed = 0.5f;
                data.mStrokeWidth = smallStroke;
            }
            data.mColor = 0xFF000000 + (red<<16) + (grn<<8) + blu;
            mStars.add(data);
        }
        mPaint.setColor(0xFF444444);
        mPaint.setAntiAlias(true);

    }

}
