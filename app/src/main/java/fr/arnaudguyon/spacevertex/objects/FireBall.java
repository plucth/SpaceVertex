/*
 * Copyright (c) 2016–2025 Arnaud GUYON
 * This source code is licensed under the MIT License.
 * See LICENSE file for details.
 */
package fr.arnaudguyon.spacevertex.objects;

import android.graphics.Canvas;
import android.os.SystemClock;

public class FireBall extends SpaceObject {

    private static final long FIRE_DURATION = 3000;

    private float mShipSize = 1;
    private float mSpeedX = 1;
    private float mSpeedY = 0.5f;
    private long mFireDate;

    public FireBall(float shipSize) {
        mShipSize = shipSize;
    }

    @Override
    protected float[] getPointDefinitions() {
        return null;
    }

    @Override
    protected float getOriginalSize() {
        return mShipSize;
    }

    @Override
    protected void buildStructure(Scene scene) {
        Circle circle = new Circle(0, 0, 0.3f, 0xFF00FFFF);
        addCircle(circle);
        mFireDate = SystemClock.uptimeMillis();
    }

    @Override
    public void preDraw(Scene scene, float frameDuration) {
        final long now = SystemClock.uptimeMillis();
        if (now - mFireDate > FIRE_DURATION) {
            destroy();
        }
        setPos(getPosX() + (mSpeedX * frameDuration), getPosY() + (mSpeedY * frameDuration));
    }

    @Override
    public void draw(Canvas canvas, float spaceCenterX, float spaceCenterY) {
        super.draw(canvas, spaceCenterX, spaceCenterY);
    }

    public void setSpeed(float speedX, float speedY) {
        mSpeedX = speedX;
        mSpeedY = speedY;
    }

}
