/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.objects;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.annotation.NonNull;

public class Arc {

    private final float mRelativeXPos;
    private final float mRelativeYPos;
    private final float mRadius;
    private final @NonNull Paint mPaint = new Paint();
    private final @NonNull RectF mRect = new RectF();
    private float mStartAngle;
    private float mSweepAngle;

    public Arc(float posX, float posY, float radius, float startAngle, float sweepAngle, int color) {
        mRelativeXPos = posX;
        mRelativeYPos = posY;
        mRadius = radius;
        mStartAngle = startAngle;
        mSweepAngle = sweepAngle;
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
    }

    public void draw(Canvas canvas, SpaceObject parent, float spaceCenterX, float spaceCenterY) {

        final float screenScale = parent.getScreenScale();
        final float parentScale = parent.getOriginalSize();
        mPaint.setStrokeWidth(parent.getStrokeWidth());

        float posX = mRelativeXPos * screenScale * parentScale;
        float posY = mRelativeYPos * screenScale * parentScale;
        float radius = mRadius * screenScale * parentScale;

        float translationX = (parent.getPosX() - spaceCenterX)*screenScale + parent.getScreenWidth()/2;
        float translationY = (parent.getPosY() - spaceCenterY)*screenScale + parent.getScreenHeight()/2;

        float angle = parent.getRotation();
        final float cos = (float) Math.cos(angle);
        final float sin = (float) Math.sin(angle);
        // x2= x*cos(a) + y*sin(a) + translationX;
        float newX = (posX*cos + posY*sin) + translationX;
        // y2 = y*cos(a) - x*sin(a) + translationY;
        float newY = (posY*cos - posX*sin) + translationY;

        float startAngle = (float) (mStartAngle - (angle * 180 / Math.PI));

        mRect.set(newX - radius, newY - radius, newX + radius, newY + radius);
        canvas.drawArc(mRect, startAngle, mSweepAngle, false, mPaint);
    }

}
